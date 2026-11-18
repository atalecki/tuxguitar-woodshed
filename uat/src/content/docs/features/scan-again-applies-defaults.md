---
title: Scan again applies the default instruments
description: After Scan again in the Sound Chain dialog, the default instruments are heard at once when nothing plays; while the song plays, it goes on and the next Play uses them.
sidebar:
  order: 28
---

## What it tests

Commit `1383c5379` on `feature/default-instrument-follow-ups`. Pressing **Scan again** in Tools > Sound Chain used to
leave the default instruments as they were until the next Play. Now, once the scan is done:

- if nothing is playing, the defaults are applied at once, as when TuxGuitar starts: a note entered sounds the default
  instrument the scan found, without pressing Play;
- if the song is playing, it plays on without a break, and the next Play uses the defaults.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `1383c5379` or
later on the branch. The install folder is
`desktop/build-scripts/tuxguitar-windows-swt-x86_64/target/tuxguitar-9.99-SNAPSHOT-windows-swt-x86_64`.

- Start TuxGuitar with the TuxGuitar Synthesizer as the MIDI Port and an audio device. In Settings > Sound, Default
  instruments: Guitar tracks **Woodshed Guitar** (shipped profile); OK. Close TuxGuitar.
- Rename `vst3/Woodshed Guitar.vst3` in the install folder to `Woodshed Guitar.vst3.off`, so the scan at startup does
  not find it.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Start TuxGuitar. File > New. Enter a note on track 1 (guitar). | It sounds General MIDI's guitar, and the status line says Woodshed Guitar is not installed. | not run |
| 2 | While TuxGuitar runs, rename `Woodshed Guitar.vst3.off` back to `Woodshed Guitar.vst3`. Open Tools > Sound Chain for track 1 and press **Scan again**. Wait until the dialog's status line says what the scan found. Close the dialog. Without pressing Play, enter a note on track 1 (or click a string on the fretboard). | The note sounds Woodshed Guitar through the clean amp at once: the defaults were applied when the scan was done. | not run |
| 3 | Close TuxGuitar, rename the plugin to `.off` again, start TuxGuitar, File > New, enter a few measures of notes on track 1 and press Play (with the loop on, or a long song, so it keeps playing). | General MIDI plays. | not run |
| 4 | While it plays, rename the plugin back, open Tools > Sound Chain and press **Scan again**. | The song plays on, General MIDI, without stopping or a gap. | not run |
| 5 | Stop, then press Play again. | Woodshed Guitar plays: the next Play picked up the defaults. | not run |
| 6 | Clean up: make sure the plugin is named `Woodshed Guitar.vst3`. | (Clean-up.) | not run |

## Notes

- The scenarios in default_instruments.feature check both cases (applied at once when stopped, the song left playing
  otherwise); this page checks what is heard.
- Applying the defaults when stopped is the same step as after the startup scan and after Settings > Sound's OK.
