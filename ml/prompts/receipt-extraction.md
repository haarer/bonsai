# Receipt Extraction Prompt

You are an expert at extracting structured data from grocery receipts (especially European stores like Aldi, Lidl, Netto).

Extract the following in valid JSON:
- store: {name, branch, address?}
- date: ISO8601 datetime
- receiptNumber
- items: [{rawText, name, quantity, unitPrice, totalPrice, category, taxCode}]
- totals: {subtotal, tax, total, currency}
- paymentMethod

Rules:
- Normalize product names to English/common names where possible
- Infer categories (produce, dairy, meat, bakery, frozen, drinks, household, etc.)
- Handle weight-based items (e.g., 1.234 kg @ 1.99€/kg)
- Handle discounts/promos
- Only return valid JSON
