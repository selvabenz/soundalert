# Real-world test plan

Test only in controlled, safe conditions; do not ask a participant to depend on the app in moving traffic during calibration.

## Road positives
- short car horn
- long car horn
- motorcycle horn
- bus/truck horn
- horn behind the user at several distances
- phone in hand, pocket, bag, bicycle/motorcycle mount

## Road negatives
- human speech
- music
- engine acceleration
- braking
- construction equipment
- whistles
- vehicle reversing beepers
- wind

Record only detection outcomes (sound type, distance, phone placement, detected/missed, confidence) unless participants separately consent to audio recording. The current app itself does not record audio.

## Home positives
- actual installed doorbell from different rooms
- repeated bell rings
- hard and soft knocks
- different doors

## Home negatives
- dishes
- TV
- phone ringtone
- speech
- clapping
- dropped objects

## Acceptance targets for a future trained model
Define targets only after representative recordings are collected. Track both false negatives (missed important sounds) and false positives (nuisance vibration); a safety-related alert should not be judged from accuracy alone.
