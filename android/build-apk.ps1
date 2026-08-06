# Build APK sem Android Studio (Windows)
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

# Detectar Android SDK
$SdkPath = $env:ANDROID_HOME
if (-not $SdkPath) { $SdkPath = $env:ANDROID_SDK_ROOT }
if (-not $SdkPath) {
    $DefaultSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $DefaultSdk) { $SdkPath = $DefaultSdk }
}

if (-not $SdkPath -or -not (Test-Path $SdkPath)) {
    Write-Error "Android SDK nao encontrado. Defina ANDROID_HOME ou instale o SDK."
}

# Criar local.properties
$EscapedSdk = $SdkPath -replace "\\", "/"
$LocalProps = "sdk.dir=$($EscapedSdk -replace ':', '\:')"
Set-Content -Path "local.properties" -Value $LocalProps -Encoding UTF8
Write-Host "SDK: $SdkPath"

# Baixar Gradle Wrapper se necessario
$WrapperJar = Join-Path $ScriptDir "gradle\wrapper\gradle-wrapper.jar"
if (-not (Test-Path $WrapperJar)) {
    Write-Host "Baixando gradle-wrapper.jar..."
    $Url = "https://github.com/gradle/gradle/raw/v8.9.0/gradle/wrapper/gradle-wrapper.jar"
    Invoke-WebRequest -Uri $Url -OutFile $WrapperJar
}

Write-Host ""
Write-Host "=== Compilando APK Debug ===" -ForegroundColor Cyan
& .\gradlew.bat assembleDebug --no-daemon

if ($LASTEXITCODE -eq 0) {
    $ApkPath = Join-Path $ScriptDir "app\build\outputs\apk\debug\app-debug.apk"
    Write-Host ""
    Write-Host "=== APK gerado com sucesso! ===" -ForegroundColor Green
    Write-Host "Arquivo: $ApkPath"
} else {
    Write-Error "Falha na compilacao."
}
