---
title: Set with an unchanged value adds no Undo step
description: Pressing Set in TuxGuitar's parameter rows (the Sound Chain panel's, the Parameters window's) with the value the plugin already has changes nothing and adds no Undo step, for every plugin.
sidebar:
  order: 41
---

## What it tests

Commit `49bed1307` on branch `fix/unchanged-set-no-undo`, after `2e813ceec`. Set with the value a plugin's parameter
already shows added an empty Undo step for the song's own plugins (Edit > Undo then did nothing you could see); a
default instrument's Parameters window already skipped it. Now one rule covers every plugin: a value the parameter rows
show already changes nothing.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at the commit above or
later.

- MIDI Port: the TuxGuitar Synthesizer, with an audio device. View > Show Sound Chain on (F8).
- TuxGuitar's test plugins **Test Clip 0.5** (an effect, parameter Ceiling, 0.5 by default) and **Test Tone** (an
  instrument, parameter Level, 1.0 by default), neither with an editor of its own. Copy
  `common/TuxGuitar-vst3/native-build/VST3/Release/Test Clip 0.5.vst3` and `Test Tone.vst3` (built by the audio tests)
  into a VST3 folder TuxGuitar scans, for example `%CommonProgramFiles%\VST3`, then press **Scan again** in the Sound
  Chain dialog. Remove them again when done.
- A new song with one guitar track.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | In the Sound Chain panel, add the VST3 effect **Test Clip 0.5** to track 1 and pick it. | Its parameter rows show **Ceiling** at 0.5. | not run |
| 2 | Without changing the value, press **Set** on the Ceiling row. Then open the Edit menu. | Edit > Undo is the same as before step 2: it would take back adding Test Clip 0.5. The title bar shows no new unsaved change. | not run |
| 3 | Type **0.7** in the Ceiling value and press **Set**. Then press **Set** again (0.7 unchanged). | The first Set is one Undo step; the second adds none. | not run |
| 4 | Press Ctrl+Z once. | Ceiling goes back to 0.5 (the second Set had nothing to undo). | not run |
| 5 | Press Ctrl+Z once more. | Test Clip 0.5 is removed from the chain. | not run |
| 6 | Choose **Test Tone** as track 1's VST3 instrument, pick the source in the panel, and press **Set** on the Level row at 1.0. Then press Ctrl+Z. | The Set adds no step: Ctrl+Z takes the source back to General MIDI at once. | not run |
| 7 | Drag the Ceiling slider of a plugin away and back to where it was within a quarter of a second, then let go. | No Undo step is added (the slider rests on the value it already had). | not run |

## Notes

- A default instrument's Parameters window (Settings > Default instruments: Test Tone; Edit on the source) already kept
  nothing for an unchanged value; it is unchanged and is checked on the page "Sound Chain panel, 3".
- Covered by tests: `hosting_vst3_plugins.feature`, "setting the value a plugin's parameter already has in TuxGuitar's
  parameter editor adds no undo step".
