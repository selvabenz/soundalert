# Architecture

## Data flow

Microphone → `AudioRecord` → `HeuristicSoundClassifier` → confidence/debounce → `Haptics` + `AppBus` + alert notification.

## Why a foreground service

The app needs to continue microphone capture when the activity is no longer the foreground screen. The service is declared as a microphone foreground service and is started only after the user grants `RECORD_AUDIO` and explicitly starts listening.

## Detection boundary

`HeuristicSoundClassifier` has no Android dependencies. This is deliberate: it can be unit tested on a desktop JVM and later replaced with a model-based implementation.

## Haptics

Road and Home use separate waveform timing arrays. The notification channels have their normal vibration disabled so Android's generic notification vibration does not mask the accessibility pattern.

## No audio persistence

PCM buffers are overwritten frame by frame. There is no file writer, database, upload client, or Internet permission in v0.1.0.
