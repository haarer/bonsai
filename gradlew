#!/usr/bin/env sh
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"
APP_HOME=$(pwd)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
JAVACMD="java"
exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
