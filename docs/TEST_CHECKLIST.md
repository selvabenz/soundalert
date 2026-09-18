# v0.2.0 device test checklist

## Installation/UI
- App opens to one fixed screen with no scrolling.
- Only large power control and sensitivity slider are visible in normal state.
- ON background slowly pulses.
- OFF background is dark and stable.

## Permissions/background
- First ON requests microphone permission.
- Lock screen after turning ON; foreground listening notification remains active.
- Horn/door vibration can occur while locked.
- Turning OFF releases microphone and stops foreground service.

## Alert reset
- Trigger one alert.
- Ensure no second alert occurs during its vibration.
- Keep the same sound continuous; detector must wait for quiet frames before re-arming.

## False positives
Test without desired alerts:
- conversation/speech
- music/TV
- normal engine noise
- traffic without honking
- fan/AC
- wind
- construction
- dogs
- utensils/household noise

## Positive road sounds
Test safely at different distances:
- car horn
- motorcycle/scooter horn
- auto-rickshaw horn
- bus horn
- truck/air horn
- emergency siren

## Home sounds
- several doorbell tones
- repeated ding-dong
- soft/medium/hard knocking

## Phone placement
- hand
- trouser pocket
- shirt pocket
- bag
- table
- vehicle holder

## Flash
Long-press power for one second. Verify lightning indicator appears and alert flashes occur. Long-press again to disable.

## Wear OS
Install phone and wear APKs from the same workflow artifact. Pair phone/watch and verify all three distinct vibration patterns.
