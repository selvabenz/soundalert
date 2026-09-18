$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Out = Join-Path $Root "app\src\main\assets\yamnet.tflite"
$Url = "https://tfhub.dev/google/lite-model/yamnet/classification/tflite/1?lite-format=tflite"
$Expected = "10c95ea3eb9a7bb4cb8bddf6feb023250381008177ac162ce169694d05c317de"
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Out) | Out-Null
Invoke-WebRequest -Uri $Url -OutFile $Out -MaximumRedirection 10
$Actual = (Get-FileHash -Algorithm SHA256 $Out).Hash.ToLowerInvariant()
if ($Actual -ne $Expected) { throw "YAMNet checksum mismatch: $Actual" }
Write-Host "YAMNet ready: $Out"
