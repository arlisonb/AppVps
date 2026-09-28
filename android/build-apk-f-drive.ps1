# Build APK usando F: para Gradle/temp (evita disco C: cheio)
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$gradleHome = "F:\Projetos\AppVps\.gradle-home"
$tmpDir = "F:\Projetos\AppVps\.tmp"
New-Item -ItemType Directory -Force -Path $gradleHome, $tmpDir | Out-Null

$env:GRADLE_USER_HOME = $gradleHome
$env:TEMP = $tmpDir
$env:TMP = $tmpDir

$SdkPath = $env:ANDROID_HOME
if (-not $SdkPath) { $SdkPath = $env:ANDROID_SDK_ROOT }
if (-not $SdkPath) {
    $SdkPath = Join-Path $env:LOCALAPPDATA "Android\Sdk"
}
if (-not (Test-Path $SdkPath)) {
    Write-Error "Android SDK nao encontrado em $SdkPath"
}

$env:ANDROID_HOME = $SdkPath
$EscapedSdk = $SdkPath -replace "\\", "/"
Set-Content -Path "local.properties" -Value "sdk.dir=$($EscapedSdk -replace ':', '\:')" -Encoding UTF8

Write-Host "SDK: $SdkPath"
Write-Host "GRADLE_USER_HOME: $gradleHome"
Write-Host ""
Write-Host "=== Compilando APK Debug ===" -ForegroundColor Cyan

& .\gradlew.bat assembleDebug --no-daemon --gradle-user-home $gradleHome

if ($LASTEXITCODE -eq 0) {
    $ApkPath = Join-Path $ScriptDir "app\build\outputs\apk\debug\app-debug.apk"
    Write-Host ""
    Write-Host "=== APK gerado com sucesso! ===" -ForegroundColor Green
    Write-Host "Arquivo: $ApkPath"
    Write-Host "Instale no celular para atualizar o app ja instalado."
} else {
    Write-Error "Falha na compilacao."
}
