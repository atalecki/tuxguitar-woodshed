---
title: Record says why it is refused
description: Pressing Record when it cannot record shows what to do on the status line under the fretboard, in TuxGuitar's language, until the cause is gone or for 5 seconds; during a practice session too, with no error message.
sidebar:
  order: 18
---

## What it tests

Commits on `fix/record-refusal-feedback`:

- `4ea8854f8` (today's behaviour pinned first in `c2f83f855`): when Record is pressed and cannot record, the line under
  the fretboard says what to do: "Arm a track to record", "Choose the TuxGuitar Synthesizer as the MIDI Port to record"
  or "Choose an audio device to record". Before, nothing happened on screen; only the log said why.
- `6e662a9eb`: the hint goes as soon as its cause no longer holds (a track armed, the MIDI Port the TuxGuitar
  Synthesizer, a device chosen, the practice session over), or after 5 seconds, whichever comes first.
- `796bc5f11`: Record pressed during interleaved practice or click-up says "Interleaved practice is under way: stop it to
  record." (or "Interleaved click-up ...") on the same line, quietly. Before, it popped an error message.
- `cdaa0ffaa`: pins that a key bound to Record runs it while it is greyed out, and the line says why (kept as it was).

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `cdaa0ffaa`
or later. Settings > Language: English. An audio device chosen in Tools > Audio Device..., Settings > Sound: MIDI Port
"TuxGuitar Synthesizer". A song with an audio track (Tools > Add Audio Track), not armed, and a few measures of notes
on track 1.

Tools > Record is greyed out while no track is armed, while there is no audio device and during a practice session, so
several steps use a key bound to it: in the Key Binding Editor (Settings), find Record among the actions and bind a free
key such as **F9**. A key bound to a greyed Tools item runs it all the same; that is kept on purpose.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | With no track armed, check that Tools > Record is greyed out, then press **F9**. | Nothing records; the line under the fretboard reads "Arm a track to record". After about 5 seconds it goes and the window takes its room back. No error message. | not run |
| 2 | Press **F9** again, and within 2 seconds arm the audio track (Tools > Arm / Disarm Track). | The hint goes as soon as the track is armed, before its 5 seconds are up. | not run |
| 3 | Settings > Sound: MIDI Port another one, such as "Gervill" (or, on Windows, "Microsoft GS Wavetable Synth"), OK. Tools > Record. | Tools > Record is not greyed out. Pressed, nothing records; the line reads "Choose the TuxGuitar Synthesizer as the MIDI Port to record" for about 5 seconds. | not run |
| 4 | Open the Recording window (Tools > Recording...) and press its Record button. | The same hint for about 5 seconds; the window stays as it was. | not run |
| 5 | Press Tools > Record, then at once Settings > Sound: MIDI Port "TuxGuitar Synthesizer", OK. | The hint goes as soon as the dialog closes on the TuxGuitar Synthesizer (if the dialog took more than 5 seconds, it had gone already). | not run |
| 6 | Tools > Audio Device...: choose an output that cannot open (or unplug the interface), OK, so the window's top line reads "TuxGuitar audio: no device (...)". Press **F9**. | The line reads "Choose an audio device to record" for about 5 seconds, or until a device is chosen again. | not run |
| 7 | Choose your audio device again (the top line of Tools > Audio Device... names it). Press Tools > Record. | The recording starts; no hint shows. Stop. | not run |
| 8 | Keep the audio track armed. Start interleaved practice (Transport > Interleaved Practice..., Pool from 1, Pool to 4, Start). During a turn, check that Tools > Record is greyed out, then press **F9**. | The session plays on; nothing records; the line reads "Interleaved practice is under way: stop it to record." No error message. | not run |
| 9 | Press **F9** again and within 2 seconds press Stop. | The session ends and the hint goes with it. | not run |
| 10 | Start interleaved click-up (Transport > Interleaved Click-Up..., Start) and press **F9** during a step. | The line reads "Interleaved click-up is under way: stop it to record."; the click-up goes on. Stop. | not run |
| 11 | Open a song with a VST3 instrument that takes long to load (as in **Texts through translations**, step 1), press Play at once. | The loading hint ("... is still loading ...") shows as before. | not run |
| 12 | Settings > Language: Deutsch (or any language other than English), OK, restart if asked. Repeat steps 1 and 8. | The hints read in English (no translation yet), never `audio.record.refusal.no-armed-track` nor `transport.practice.refusal.record`; in step 8 the session's name may read in German if that language names it. | not run |
| 13 | Settings > Language back to the one you use; remove the key binding if you do not want it. | TuxGuitar reads as before. | not run |

## Notes

- The tests cover the four hints in English and in a made-up German, that the action shows no error message (during
  a practice session too), that the hint lasts 5 seconds then gives way to the loading hint, that it goes at once when
  its cause goes (each of the four) but not when another cause goes, that the last press is told and a press that
  records clears it, that the log lines are unchanged, and that the MIDI Port does not grey Record out.
- What the tests cannot run is the desktop's key binding: they run the record action directly while Record is greyed
  out, which is what the key does. Steps 1, 6 and 8 check the key itself.
- The log now reads "recording refused: a practice session is under way" for step 8, in English like the other
  refusals (it quoted the shown text before).
- Step 6 needs a device that does not open; if that is hard to bring about, the tests cover it.
