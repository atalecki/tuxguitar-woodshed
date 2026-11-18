---
title: Record says why it is refused
description: Pressing Record when it cannot record shows what to do on the status line under the fretboard, for 5 seconds, in TuxGuitar's language.
sidebar:
  order: 18
---

## What it tests

Commit `48234de58` on `fix/record-refusal-feedback` (today's behaviour pinned first in `c1195d011`). When Record is
pressed and cannot record, the line under the fretboard says what to do for 5 seconds: "Arm a track to record",
"Choose the TuxGuitar Synthesizer as the MIDI Port to record" or "Choose an audio device to record". Before, nothing
happened on screen; only the log said why.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `48234de58`
or later. Settings > Language: English. An audio device chosen in Tools > Audio Device..., Settings > Sound: MIDI Port
"TuxGuitar Synthesizer". A song with an audio track (Tools > Add Audio Track), not armed.

Tools > Record is greyed out while no track is armed and while there is no audio device, so steps 1 and 4 use a key
bound to it: in the Key Binding Editor (Settings), find Record among the actions and bind a free key such as **F9**.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | With no track armed, press **F9**. | Nothing records; the line under the fretboard reads "Arm a track to record". After about 5 seconds it goes and the window takes its room back. No error message. | not run |
| 2 | Arm the audio track (Tools > Arm / Disarm Track). Settings > Sound: MIDI Port another one, such as "Gervill" (or, on Windows, "Microsoft GS Wavetable Synth"), OK. Tools > Record. | Tools > Record is not greyed out. Pressed, nothing records; the line reads "Choose the TuxGuitar Synthesizer as the MIDI Port to record" for about 5 seconds. | not run |
| 3 | Open the Recording window (Tools > Recording...) and press its Record button. | The same hint for about 5 seconds; the window stays as it was. | not run |
| 4 | Settings > Sound: MIDI Port "TuxGuitar Synthesizer", OK. Tools > Audio Device...: choose an output that cannot open (or unplug the interface), OK, so the window's top line reads "TuxGuitar audio: no device (...)". Press **F9**. | The line reads "Choose an audio device to record" for about 5 seconds. | not run |
| 5 | Choose your audio device again (the top line of Tools > Audio Device... names it). Press Tools > Record. | The recording starts; no hint shows. Stop. | not run |
| 6 | Open a song with a VST3 instrument that takes long to load (as in **Texts through translations**, step 1), press Play at once. | The loading hint ("... is still loading ...") shows as before. | not run |
| 7 | Settings > Language: Deutsch (or any language other than English), OK, restart if asked. Repeat step 1. | The hint reads in English (no translation yet), never `audio.record.refusal.no-armed-track`. | not run |
| 8 | Settings > Language back to the one you use; remove the key binding if you do not want it. | TuxGuitar reads as before. | not run |

## Notes

- The tests cover the three hints in English and in a made-up German, that the action shows no error message, that
  the hint lasts 5 seconds then gives way to the loading hint, that the last press is told and a press that records
  clears it, that the log lines are unchanged, and that the MIDI Port does not grey Record out.
- Record refused during a practice session still shows an error message, as before (it is greyed out then too).
- Step 4 needs a device that does not open; if that is hard to bring about, the tests cover it.
