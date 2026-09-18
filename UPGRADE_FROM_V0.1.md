# SoundAlert v0.2.0 — Clean Upgrade from v0.1.x

The v0.2.0 source tree replaces the v0.1.x implementation. Do not merge the new files on top of
the old Java/Kotlin source tree without removing the legacy files.

Legacy files that must not be compiled in v0.2.0:

- `app/src/main/java/com/bridgeconn/soundalert/AppBus.kt`
- `app/src/main/java/com/bridgeconn/soundalert/audio/HeuristicSoundClassifier.kt`
- `app/src/main/java/com/bridgeconn/soundalert/ui/Theme.kt`
- `app/src/test/java/com/bridgeconn/soundalert/audio/HeuristicSoundClassifierTest.kt`

The GitHub Actions workflows in this package remove these legacy files automatically before
compiling, so a repository upgraded by overlaying files can still build.

For a clean repository, remove the four files permanently. On Linux/macOS run:

`bash scripts/cleanup-v01.sh`

On Windows PowerShell run:

`powershell -ExecutionPolicy Bypass -File scripts/cleanup-v01.ps1`

The YAMNet fetch step is also invoked through Bash and no longer depends on the executable bit
being preserved by ZIP extraction.
