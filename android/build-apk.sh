#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

SDK_PATH="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"

if [ ! -d "$SDK_PATH" ]; then
    echo "ERRO: Android SDK nao encontrado em $SDK_PATH"
    echo "Defina ANDROID_HOME ou instale o SDK."
    exit 1
fi

echo "sdk.dir=$SDK_PATH" > local.properties
echo "SDK: $SDK_PATH"

WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Baixando gradle-wrapper.jar..."
    curl -fsSL -o "$WRAPPER_JAR" \
        "https://github.com/gradle/gradle/raw/v8.9.0/gradle/wrapper/gradle-wrapper.jar"
fi

chmod +x gradlew

echo ""
echo "=== Compilando APK Debug ==="
./gradlew assembleDebug --no-daemon

APK="app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "=== APK gerado com sucesso! ==="
echo "Arquivo: $SCRIPT_DIR/$APK"
