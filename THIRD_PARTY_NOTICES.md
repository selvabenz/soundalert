# Third-party components

## YAMNet
SoundAlert downloads the TensorFlow YAMNet TFLite audio-event classification model during CI from TensorFlow Hub. YAMNet predicts AudioSet environmental sound classes and is used only for on-device inference.

Model download used by CI:
`https://tfhub.dev/google/lite-model/yamnet/classification/tflite/1?lite-format=tflite`

Expected SHA-256:
`10c95ea3eb9a7bb4cb8bddf6feb023250381008177ac162ce169694d05c317de`

## MediaPipe Tasks Audio
SoundAlert uses `com.google.mediapipe:tasks-audio:1.0.0` for on-device audio classification.

## Google Play services Wearable
SoundAlert uses the Wearable Data Layer client for phone-to-Wear-OS alert messages when a compatible companion watch app is installed.
