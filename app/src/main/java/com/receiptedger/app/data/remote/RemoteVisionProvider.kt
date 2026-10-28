package com.receiptedger.app.data.remote

import com.receiptedger.app.data.prefs.AppPreferences
import com.receiptedger.app.domain.model.ExtractedItem
import com.receiptedger.app.domain.model.ExtractedReceipt
import com.receiptedger.app.domain.model.VisionProvider
import android.util.Log
import com.squareup.moshi.Moshi
import java.io.File
import java.io.IOException
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// Shape of the receipt JSON the model returns (see ml/prompts/receipt-extraction.md).
data class VisionData(
    val store: VisionStore? = null,
    val date: String? = null,
    val receiptNumber: String? = null,
    val items: List<VisionItem> = emptyList(),
    val totals: VisionTotals? = null,
)

data class VisionStore(
    val name: String? = null,
    val branch: String? = null,
)

// Numeric fields are String: the model frequently answers with locale-formatted
// text ("30,01 EUR", "25,22 EUR BRUTTO", "13,11") which we parse leniently below.
data class VisionItem(
    val name: String? = null,
    val rawText: String? = null,
    val quantity: String? = null,
    val unitPrice: String? = null,
    val totalPrice: String? = null,
    val category: String? = null,
)

data class VisionTotals(
    val subtotal: String? = null,
    val tax: String? = null,
    val total: String? = null,
    val currency: String? = null,
)

// Envelope of an OpenAI-compatible /v1/chat/completions response.
data class ChatResponse(
    val choices: List<ChatChoice> = emptyList(),
    val error: ChatError? = null,
)

data class ChatChoice(val message: ChatMessage? = null)

data class ChatMessage(val content: String? = null)

data class ChatError(val message: String? = null)

private const val SYSTEM_PROMPT = """You are an expert at extracting structured data from receipts (grocery, fuel, or other retail receipts).

Transcribe values EXACTLY as printed on the receipt. Never guess, infer, or fill in values that are not visible.

Extract the following in valid JSON:
- store: {name, branch, address?}
  - store.name must be the exact store name printed in the receipt header (logo or title line). Do not assume a chain name. If no store name is visible, use null.
- date: ISO8601 datetime — transcribe the printed date including the year exactly
- receiptNumber
- items: [{rawText, name, quantity, unitPrice, totalPrice, category, taxCode}]
- totals: {subtotal, tax, total, currency}
- paymentMethod

Rules:
- Normalize product names to English/common names where possible, but keep the store name exactly as printed
- Infer categories (produce, dairy, meat, bakery, frozen, drinks, household, fuel, other)
- Handle weight-based and volume-based items (e.g., 1.234 kg @ 1.99€/kg, 13.11 L @ 2.289 €/L)
- Handle discounts/promos
- Use null for any value that is not visible on the receipt
- Return ONLY the JSON object. No markdown, no code fences, no commentary."""

@Singleton
class RemoteVisionProvider @Inject constructor(
    private val client: OkHttpClient,
    private val moshi: Moshi,
    private val preferences: AppPreferences,
) : VisionProvider {

    private val chatAdapter = moshi.adapter(ChatResponse::class.java)
    private val receiptAdapter = moshi.adapter(VisionData::class.java)

    override suspend fun extract(image: File, storeHint: String?): ExtractedReceipt =
        withContext(Dispatchers.IO) {
            val s = preferences.settings.first()
            val baseUrl = s.serverUrl.trim().removeSuffix("/")
            if (baseUrl.isEmpty()) {
                throw IOException("Vision server URL is not configured (Settings)")
            }

            val base64 = Base64.getEncoder().encodeToString(image.readBytes())
            val instruction = buildString {
                append("Extract the structured receipt data from this image.")
                if (!storeHint.isNullOrBlank()) append(" Store hint: ${storeHint.trim()}")
            }
            val body = chatRequestBody(s.model.ifBlank { "qwen2.5-vl-3b" }, instruction, base64)

            val request = Request.Builder()
                .url("$baseUrl/v1/chat/completions")
                .post(body.toRequestBody("application/json".toMediaType()))
                .apply {
                    if (s.apiKey.isNotBlank()) {
                        header("Authorization", "Bearer ${s.apiKey.trim()}")
                    }
                }
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = chatAdapter.fromJson(text)?.error?.message
                    throw IOException("extraction failed: ${msg ?: "HTTP ${response.code}"}")
                }
                val content = chatAdapter.fromJson(text)?.choices?.firstOrNull()?.message?.content
                    ?: throw IOException("extraction failed: empty model response")

                val json = stripFences(content)
                val data = try {
                    receiptAdapter.fromJson(json)
                } catch (e: IOException) {
                    Log.e(TAG, "invalid JSON from model: ${e.message}\n$json")
                    throw IOException("extraction failed: model returned invalid JSON")
                }
                    ?: throw IOException("extraction failed: model returned invalid JSON")

                ExtractedReceipt(
                    storeName = data.store?.name.orEmpty(),
                    branch = data.store?.branch.orEmpty(),
                    date = data.date.orEmpty(),
                    receiptNumber = data.receiptNumber.orEmpty(),
                    currency = data.totals?.currency.orEmpty(),
                    items = data.items.map { item ->
                        val quantity = parseAmount(item.quantity) ?: 1.0
                        val unitPrice = parseAmount(item.unitPrice) ?: 0.0
                        ExtractedItem(
                            name = item.name.orEmpty(),
                            quantity = quantity,
                            unitPrice = unitPrice,
                            totalPrice = parseAmount(item.totalPrice) ?: quantity * unitPrice,
                            category = item.category.orEmpty(),
                        )
                    },
                    subtotal = parseAmount(data.totals?.subtotal),
                    tax = parseAmount(data.totals?.tax),
                    total = parseAmount(data.totals?.total),
                )
            }
        }

    private fun chatRequestBody(model: String, instruction: String, imageBase64: String): String =
        buildString {
            append("{")
            append("\"model\":").append(model.jsonString()).append(",")
            append("\"temperature\":0,")
            append("\"messages\":[")
            append("{\"role\":\"system\",\"content\":").append(SYSTEM_PROMPT.jsonString()).append("},")
            append("{\"role\":\"user\",\"content\":[")
            append("{\"type\":\"text\",\"text\":").append(instruction.jsonString()).append("},")
            append("{\"type\":\"image_url\",\"image_url\":{\"url\":\"data:image/jpeg;base64,")
            append(imageBase64)
            append("\"}}]")
            .append("}]")
            .append("}")
        }

    private fun String.jsonString(): String {
        val sb = StringBuilder(length + 2)
        sb.append('"')
        for (c in this) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }

    private fun stripFences(text: String): String {
        var t = text.trim()
        if (t.startsWith("```")) {
            val nl = t.indexOf('\n')
            if (nl >= 0) t = t.substring(nl + 1)
            if (t.endsWith("```")) t = t.dropLast(3)
        }
        return t.trim()
    }

    /**
     * Leniently parse a number the model may return as locale-formatted text:
     * "30,01 EUR", "25,22 EUR BRUTTO", "13,11", "2.289", 1, …
     * A comma is treated as the decimal separator (German receipts); dots are
     * thousands separators in that case.
     */
    private fun parseAmount(value: String?): Double? {
        if (value == null) return null
        val t = value.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }
        if (t.isEmpty()) return null
        return if (t.contains(',')) {
            t.replace(".", "").replace(',', '.').toDoubleOrNull()
        } else {
            t.toDoubleOrNull()
        }
    }

    companion object {
        private const val TAG = "RemoteVision"
    }
}
