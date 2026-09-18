# SoundAlert v0.2.0 — Automatic Reliable Alert

v0.2.0 redesigns SoundAlert around elderly, deaf and hard-of-hearing users: the interface is simpler while the detection engine becomes substantially smarter.

## Highlights

- one fixed, non-scrollable screen;
- large ON/OFF control;
- sensitivity slider retained;
- full-screen immersive pulsing/blinking visual state;
- large horn, door and siren pictograms;
- YAMNet on-device environmental sound classification;
- car/vehicle horn, truck/air horn and bicycle-bell recognition;
- doorbell, ding-dong and knocking recognition;
- emergency siren recognition;
- explicit false-alert filtering for speech, music, engines, wind, construction, traffic without horn, television, dogs and household noise;
- 2-of-3-frame confirmation;
- detector reset after vibration and quiet-frame re-arm;
- automatic Road/Home/Uncertain threshold adaptation with no GPS;
- automatic background calibration profiles;
- stronger screen-off operation with microphone foreground service and partial wake lock;
- optional flashlight patterns (camera permission requested only when flash is enabled);
- Wear OS companion haptics;
- automatic GitHub build artifacts and tag-based Release assets;
- Compose removed to reduce package size.

## Vibration patterns

- Horn: LONG — short — LONG
- Door: short short — pause — short short
- Siren: triple-long — pause — triple-long

## UI philosophy

**One screen. One switch. One sensitivity control. Everything else automatic.**

Long-press the power control for one second to toggle the optional flashlight alert. Normal users do not need to choose Road/Home modes or understand classifier settings.

## Important limitations

The current model uses general YAMNet horn classes plus SoundAlert filtering. It is intended to detect horns from Indian road vehicles but is not yet a custom India-trained model and does not identify vehicle type. Real-world recordings are needed for the next accuracy-tuning milestone.

Road Alert remains a supplementary accessibility aid and should not be the user's only traffic warning mechanism.
