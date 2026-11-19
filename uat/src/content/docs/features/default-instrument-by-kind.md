---
title: Default instrument by track kind, slice 2b
description: A track without an instrument of its own plays the default VST3 instrument of its kind instead of General MIDI.
sidebar:
  order: 20
---

## What it tests

Commit `d825bdb11` on `feature/default-instrument`. A track whose sound chain has no instrument of its own (no sound
chain, or a chain whose source is General MIDI) now plays the default instrument of its kind: guitar, bass, drums or
other, decided by the track's General MIDI instrument. The song itself does not change. Play and File > Export audio
sound the same default. There is no Settings page for the defaults yet (slice 2e), so this page sets them in the
configuration file by hand.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `d825bdb11`
or later on the branch. Then close TuxGuitar again, so it does not overwrite the configuration file you edit next.

1. Open `%APPDATA%\tuxguitar\config\plugins\tuxguitar-audio.cfg` in a text editor and add these two lines, which make
   **Woodshed Guitar** (the VST3 instrument TuxGuitar ships) the default instrument for **other** tracks:

   ```
   audio.default-instrument.other.class=57534847554954415200000000000001
   audio.default-instrument.other.name=Woodshed Guitar
   ```

2. Start TuxGuitar again. In Settings > Sound, choose the TuxGuitar Sequencer and the TuxGuitar Synthesizer, and an
   audio device.
3. File > New. Enter a few single notes and a chord on track 1. Add a second track (Track > Add track) and enter the
   same notes on it.
4. Open Track 2's properties and set its instrument to **Acoustic Grand Piano**. Leave track 1 on its guitar
   instrument.

Woodshed Guitar with no profile plays a clean, dry plucked string, clearly not a piano.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Solo track 2 (the piano) and press Play. | Track 2 sounds as Woodshed Guitar's dry plucked string, not as a General MIDI piano. | not run |
| 2 | Solo track 1 (the guitar) and press Play. | Track 1 sounds as before: the General MIDI steel-string guitar (guitar tracks have no default yet in this slice). | not run |
| 3 | Open Tools > Sound Chain for track 2. | Its source still reads **Acoustic Grand Piano**: the song keeps saying General MIDI (the row that says what plays comes in slice 2f). | not run |
| 4 | Add the effect of any VST3 plugin you have (an amp or a delay, say) to track 2's sound chain and press Play. | Track 2 sounds as Woodshed Guitar through that effect. | not run |
| 5 | Change track 2's instrument to **Electric Grand Piano** (still "other") and press Play. | Still Woodshed Guitar. | not run |
| 6 | Change track 2's instrument to **Clean Electric Guitar** (a guitar) and press Play. | Track 2 now sounds as the General MIDI clean guitar: guitar tracks have no default. | not run |
| 7 | Set track 2 back to Acoustic Grand Piano, then File > Export audio (WAVE) and play the file in another player. | Track 2 sounds as Woodshed Guitar in the file, as it did in TuxGuitar. | not run |
| 8 | Save the song (File > Save as, a `.tg` file), close it and open it again. Press Play. | It opens unmodified and track 2 plays Woodshed Guitar again. | not run |
| 9 | Close TuxGuitar, delete the two lines from `tuxguitar-audio.cfg`, start TuxGuitar, open the song and press Play. | Track 2 plays the General MIDI piano again: the song did not keep the default. | not run |

## Notes

- Guitar and bass tracks getting Woodshed Guitar with a profile by default is slice 2d; what happens when a default is
  missing or does not load (and the status-line hint) is slice 2c.
- Pressing Play right after TuxGuitar starts, before its VST3 scan has finished (a few seconds), may still play General
  MIDI; the next Play plays the default.
- The tests already cover each kind (guitar, bass, drums, other), effects kept, an instrument of the track's own winning
  over the default, and offline and live playing the same: check only what is heard.
