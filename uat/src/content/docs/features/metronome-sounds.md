---
title: Metronome sounds
description: Count In and the Metronome click with a sound you choose (built-in recordings, the code-made Click or your own WAV files), beat 1 its own sound or the same one accented, a click volume in dB up to +12, Try, and a Transport > Metronome Sound submenu.
sidebar:
  order: 42
---

## What it tests

Commits `0ac13dbb8` to `7bd1ed70b` on `feature/metronome-sounds`. Count In and the Metronome no longer only play the
short code-made blip at a velocity. In **Tools > Settings > Sound**, the **Count In / Metronome click** group chooses:

- **Beat 1**: "Same sound, accented" (the other beats' sound, higher and louder) or any sound.
- **Other beats**: **Click** (today's code-made blip, still the default), seven real recordings (**Wood Block**,
  **Cowbell**, **Snare Drum**, **Side Stick**, **Claves**, **Hi-Hat**, **Hand Clap**; CC0, from the Versilian
  Community Sample Library), then your own WAV files.
- **Add sound...** copies a WAV file into TuxGuitar's own sounds folder; **Remove sound** deletes that copy.
- **Click volume**: a slider from -30 to +12 dB (today's loudest click is 0 dB), with **Try** to hear beat 1 then an
  other beat with the choices before pressing OK.

**Transport > Metronome Sound** switches the other beats' sound at once, also while playing. The recordings start on
the beat (the silence before each was trimmed to 1 ms, commit `03b697616`). The choice is TuxGuitar's, not the song's.

## Setup

Build and launch commit `7bd1ed70b` (or later) with the VS Code task **app: build + run** (lib and synth-core changed:
run **maven: install shared modules** first if the task does not). Use headphones.

- In **Tools > Settings > Sound**, set the MIDI Port to **TuxGuitar Synthesizer**.
- A song: **File > New**, one guitar track, 4 measures of 4/4 at 120 bpm, a quarter note on every beat.
- Files to add: a short percussive WAV of your own (call it `Beep.wav`), a text file of a few lines renamed to `notes.wav`,
  and a silent WAV: one second of silence exported from Audacity as WAV, 32-bit float, named `quiet.wav`.
- TuxGuitar's own sounds folder is `%APPDATA%\tuxguitar\click-sounds` (it is made by the first **Add sound...**). Its
  settings file is `%APPDATA%\tuxguitar\config\tuxguitar.cfg`.
- Press **F8** to show the Sound Chain panel, for the Master meter.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1   | Open **Tools > Settings > Sound**. Look at **Count In / Metronome click**. | Rows: **Beat 1:** a drop-down showing "Same sound, accented"; **Other beats:** a drop-down showing "Click", then **Add sound...** and **Remove sound** (greyed); **Click volume:** a slider, "-3 dB" beside it, then **Try**. No "Drum note" or "Velocity" spinners. | not run |
| 2   | Open the **Other beats** drop-down, then the **Beat 1** drop-down. | Other beats: Click, Wood Block, Cowbell, Snare Drum, Side Stick, Claves, Hi-Hat, Hand Clap. Beat 1: "Same sound, accented" first, then the same eight. | not run |
| 3   | With Other beats **Click** and Beat 1 "Same sound, accented", press **Try**. | A higher, slightly louder blip, then 0.4 s later a lower blip: today's click. Nothing plays the song. | not run |
| 4   | Listen-check **Wood Block**: choose it in Other beats, press **Try** a few times. | Beat 1 (2 semitones higher, a little louder), then the plain wood block. Each starts at once with a clean attack: no pop or click before it, no gap of silence, no crackle or distortion, no cut-off tail. | not run |
| 5   | Listen-check **Cowbell** the same way. | As step 4: clean start on the beat, no clipping. | not run |
| 6   | Listen-check **Snare Drum**. | As step 4. Its ring lasts about a second. | not run |
| 7   | Listen-check **Side Stick**. | As step 4. | not run |
| 8   | Listen-check **Claves**. | As step 4. | not run |
| 9   | Listen-check **Hi-Hat**. | As step 4. | not run |
| 10  | Listen-check **Hand Clap**. | As step 4. The clap is heard right away (before, it came about 44 ms late). All eight sounds are about equally loud at the same volume. | not run |
| 11  | Set Beat 1 to **Cowbell**, Other beats to **Side Stick**. Press **Try**. | A cowbell, then a side stick: beat 1 plays its own sound, not higher or accented. | not run |
| 12  | Drag the **Click volume** slider all the way left, then all the way right, watching the number. Press **Try** at each end. | The number follows the slider at once: "-30 dB" at the left end, "+12 dB" at the right end, with a plus sign above 0 ("+6 dB"), "0 dB" in the middle-right. The slider cannot go past either end. At -30 dB Try is very quiet; at +12 dB it is clearly louder than at 0 dB, and the Master meter's clip lamp may light. | not run |
| 13  | Press **Cancel**, then open Settings > Sound again. | The group shows what was kept before (Same sound accented, Click, -3 dB): Try and the changes were not saved. | not run |
| 14  | Set Other beats **Wood Block**, Beat 1 "Same sound, accented", volume **0 dB**, press **OK**. Turn on **Transport > Count In** and **Transport > Metronome**. Play the song from measure 1. | Count In clicks wood block, its accents on beat 1 (higher, louder); then the Metronome clicks a wood block on every beat, beat 1 accented, exactly on the beat with the song's notes (no flam). | not run |
| 15  | Back in Settings > Sound, press **Add sound...**. | A file chooser titled "Add sound..." offering WAV files. Pick `Beep.wav`. "Beep" appears at the end of Other beats and is selected; **Remove sound** is enabled; nothing shows on the line under the group. **Try** plays it (beat 1 higher). `%APPDATA%\tuxguitar\click-sounds` holds `Beep.wav`; your original is still where it was. | not run |
| 16  | **Add sound...** `notes.wav`, then `quiet.wav`. | Under the group: "notes.wav was not added: it is not a WAV file.", then "quiet.wav was not added: there is no sound in it." Neither is in the drop-downs or the folder. | not run |
| 17  | **Add sound...** `Beep.wav` again. | "Beep (2)" appears and is selected; the folder holds `Beep (2).wav`. | not run |
| 18  | Set Beat 1 to **Beep (2)** too (Other beats is Beep (2)). Press **Remove sound**. | "Beep (2)" is gone from both drop-downs; Beat 1 and Other beats both show **Click**; Remove sound is greyed. `Beep (2).wav` is gone from the folder. | not run |
| 19  | Set Other beats to **Beep**, Beat 1 "Same sound, accented", press **OK**. Close Settings. Copy any other WAV into `%APPDATA%\tuxguitar\click-sounds` by hand (call it `Aaa.wav`). Reopen Settings > Sound. | "Aaa" is listed after the eight built-in sounds and before "Beep" (own sounds A to Z). Beep is still selected. Press Cancel. | not run |
| 20  | Open **Transport > Metronome Sound** (right under Metronome). | A submenu: Click, Wood Block, Cowbell, Snare Drum, Side Stick, Claves, Hi-Hat, Hand Clap, Aaa, Beep, a separator, then **Sounds and volume...**. **Beep** has the radio mark. | not run |
| 21  | With the Metronome on, Play, and while it plays pick **Transport > Metronome Sound > Cowbell**. | Within a beat the Metronome clicks cowbell, beat 1 accented; the song plays on without a stop. Reopen the submenu: Cowbell is marked. Settings > Sound shows Cowbell in Other beats (Cancel). | not run |
| 22  | In Settings set Beat 1 to **Claves**, OK. Pick **Transport > Metronome Sound > Snare Drum**, Play. | Beat 1 clicks claves, the other beats snare: the submenu changes only the other beats. | not run |
| 23  | Pick **Transport > Metronome Sound > Sounds and volume...**. | Settings opens on the **Sound** page (not the first page). | not run |
| 24  | Set Other beats to **Beep**, Beat 1 "Same sound, accented", OK. Quit TuxGuitar and start it again. Play with the Metronome on, without opening Settings. | Beep clicks at once, beat 1 higher: own sounds play after a restart. | not run |
| 25  | Quit TuxGuitar. Delete `Beep.wav` from the folder. Start TuxGuitar, play with the Metronome on, then open Settings > Sound. | The Metronome clicks the code-made Click. Settings shows **Click** in Other beats; "Beep" is not listed. | not run |
| 26  | Old settings: quit TuxGuitar. In `tuxguitar.cfg` delete the lines starting `midi.click.beat-one`, `midi.click.other` and `midi.click.volume`, and add `midi.click.note=56` and `midi.click.velocity=64`. Start TuxGuitar, open Settings > Sound. | Beat 1 "Same sound, accented", Other beats **Cowbell** (General MIDI note 56), volume **-6 dB** (velocity 64). Press OK, quit: the file now has `midi.click.other=cowbell` and `midi.click.volume=-6`, and no `midi.click.note` or `midi.click.velocity`. | not run |
| 27  | Settings > Sound: MIDI Port **Microsoft GS Wavetable Synth** (or any other port), Other beats **Cowbell**, Beat 1 "Same sound, accented", volume **-6 dB**, OK. Play with Count In and the Metronome on. | Count In and the Metronome play the General MIDI cowbell of that port, beat 1 louder. With **Wood Block**: the port's high wood block; with an own sound: its side stick. At -18 dB clearly quieter. At 0 dB and at +12 dB equally loud, beat 1 no louder than the others (a MIDI velocity tops out at 127). | not run |
| 28  | Set the MIDI Port back to **TuxGuitar Synthesizer**, Other beats **Hand Clap**, volume **+6 dB**, OK. Open a song with an audio track (a recorded take; record a few seconds first if none). Play it with Count In and the Metronome on. | Its playback is rendered ahead (the offline path): Count In and the Metronome play the hand clap, beat 1 higher, as loud as in a song without takes at +6 dB, on the beat with the take. | not run |
| 29  | Settings > Sound, press **Defaults**. Reopen Settings > Sound. | Back to "Same sound, accented", **Click**, "-3 dB". | not run |

## Notes

- On another MIDI Port the Metronome's clicks are notes in the song's sequence: a **Transport > Metronome Sound** pick
  made while playing is heard from the next Play there. On the TuxGuitar Synthesizer it is heard at once.
- **File > Export audio** leaves the Metronome out, as before.
- Sound names and the new texts are in English in every language until translated.
- The tests already cover: each recording's licence, source and shipped SHA-256, its peak and that it starts within
  1 ms; the evening of own files; the volume and velocity arithmetic; the old settings' migration; what Add sound
  refuses and the numbered names. Steps 4 to 10 are the ear's part: a clean, on-beat start and no distortion.
