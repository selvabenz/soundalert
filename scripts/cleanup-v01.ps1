$Root = Split-Path -Parent $PSScriptRoot
$Files = @(
  "app/src/main/java/com/bridgeconn/soundalert/AppBus.kt",
  "app/src/main/java/com/bridgeconn/soundalert/audio/HeuristicSoundClassifier.kt",
  "app/src/main/java/com/bridgeconn/soundalert/ui/Theme.kt",
  "app/src/test/java/com/bridgeconn/soundalert/audio/HeuristicSoundClassifierTest.kt"
)
foreach ($File in $Files) {
  $Path = Join-Path $Root $File
  if (Test-Path $Path) { Remove-Item -Force $Path }
}
Write-Host "Legacy v0.1 sources removed."
