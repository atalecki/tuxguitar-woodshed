---
title: Default instrument fallback, slice 2c
description: A default instrument that is not installed or cannot be loaded plays General MIDI, and the status line says so quietly.
sidebar:
  order: 21
---

## What it tests

Commit `a3b838814` on `feature/default-instrument`. When the default instrument of a track's kind is not installed on
this computer, or is installed but cannot be loaded, the track plays General MIDI instead (before, a default that could
not be loaded would have been silent). The status line under the fretboard says so quietly, in TuxGuitar's language,
the first time it happens for a song (and again after the defaults change): for 5 seconds, or until the cause is gone.
The log keeps the reason in English. An instrument a track chose itself that cannot be loaded stays silent, as before.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `a3b838814` or
later on the branch, then close TuxGuitar again so it does not overwrite the file you edit next.

1. Open `%APPDATA%\tuxguitar\config\plugins\tuxguitar-audio.cfg` in a text editor and add these lines. The first two
   name a **guitar** default that no computer has (**Missing Piano**); the last two make **Woodshed Guitar** the
   default for **other** tracks:

   ```
   audio.default-instrument.guitar.class=0123456789ABCDEF0123456789ABCDEF
   audio.default-instrument.guitar.name=Missing Piano
   audio.default-instrument.other.class=57534847554954415200000000000001
   audio.default-instrument.other.name=Woodshed Guitar
   ```

2. Start TuxGuitar. In Settings > Sound, choose the TuxGuitar Sequencer and the TuxGuitar Synthesizer, and an audio
   device. Wait a few seconds after it opens (the VST3 scan runs in the background).
3. File > New. Enter a few notes on track 1 (a guitar track). Add a second track and set its instrument to **Acoustic
   Grand Piano** in its properties; enter a few notes on it too.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Solo track 1 (the guitar) and press Play. | Track 1 sounds as the General MIDI steel-string guitar (not silence). The status line under the fretboard shows **Missing Piano is not installed: guitar tracks play General MIDI.** and the line goes away by itself about 5 seconds later. No dialog opens. | not run |
| 2 | Stop, then press Play again. | The guitar plays as before, and the status line stays empty: the hint is said once per song. | not run |
| 3 | File > New, enter a note on track 1 and press Play. | The hint shows again for the new song. | not run |
| 4 | Open `%APPDATA%\tuxguitar\log\tuxguitar-0.log` and search for "Missing Piano". | A WARN line in English: "the default instrument for guitar tracks, Missing Piano (0123456789ABCDEF0123456789ABCDEF) plays General MIDI instead: it is not installed". | not run |
| 5 | Go back to the first song (or redo setup step 3). Solo track 2 (the piano) and press Play. | Track 2 plays Woodshed Guitar's dry plucked string (it is installed and loads), and the status line says nothing about track 2. | not run |
| 6 | Leave TuxGuitar open. In the TuxGuitar install folder (`desktop/build-scripts/tuxguitar-windows-swt-x86_64/target/tuxguitar-9.99-SNAPSHOT-windows-swt-x86_64/lib`), rename `tuxguitar-vst3-host.exe` to `tuxguitar-vst3-host.exe.off`, so no VST3 plugin can be loaded. File > New, add the piano track again with notes, solo it and press Play. | Track 2 sounds as the General MIDI piano (not silence). The status line shows **Woodshed Guitar could not be loaded: other tracks play General MIDI.** for about 5 seconds. The log has a WARN line with the reason ("... could not be loaded: ... the VST3 host did not start ..."). | not run |
| 7 | Still without the host: open Tools > Sound Chain for the piano track, choose **Woodshed Guitar** as its own instrument ("Use as source"), and press Play. | The piano track is silent: an instrument the track chose itself does not fall back. No new hint. | not run |
| 8 | Close TuxGuitar, rename the host back to `tuxguitar-vst3-host.exe`, and remove the four lines from `tuxguitar-audio.cfg`. | (Clean-up.) | not run |

## Notes

- The hint text goes through TuxGuitar's translation files (`audio.default-instrument.*`); every language but English
  shows the English text until a translator adds it. The tests check a made-up German text.
- Pressing Play right after TuxGuitar starts, before the VST3 scan has finished, plays General MIDI without a hint;
  the next Play plays the default (or tells why not).
- Guitar and bass defaulting to Woodshed Guitar with a profile is slice 2d; Settings to choose the defaults is 2e; the
  Sound Chain dialog saying what plays is 2f.
