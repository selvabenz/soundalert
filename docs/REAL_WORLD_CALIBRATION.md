# Real-world calibration plan

v0.2.0 ships with YAMNet + SoundAlert filtering, not a custom India-trained neural model. The next accuracy step requires representative recordings.

Collect short, consent-safe environmental samples without speech where practical:

## Positive
- compact car horns
- motorcycles/scooters
- auto-rickshaw horns
- buses
- trucks/air horns
- ambulance/police/fire sirens
- actual home doorbells
- different knocking styles

## Negative
- speech and crowds
- music/loudspeakers
- engines without honking
- wind
- construction
- normal traffic without horn
- television
- dogs
- household appliances and utensils

Test phone placement in hand, shirt/trouser pocket, bag, dashboard/holder, table and different rooms.

Never collect or retain private conversations for calibration. Label only the environmental event needed for model tuning.
