---
title: Plugin editors inside the app, 5 - parameter rows for plugins without an editor
description: A plugin without an editor of its own shows TuxGuitar's parameter rows inside the Sound Chain panel, in as many columns as fit, and a default instrument's first change there keeps it in the song.
sidebar:
  order: 36
---

## What it tests

Branch `feature/embedded-editors-follow-ups`, unit 4i (the commit "audio-ui: a plugin without an editor of its own
shows TuxGuitar's parameter rows inside the Sound Chain panel", after `6493f3564`). Picking such a plugin in the
Sound Chain panel shows rows of name, slider, value and **Set** under its line, where an embedded editor goes, instead of
only "<plugin>: parameters". On Linux and macOS every plugin shows these rows, as no plugin editor can be embedded
there.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at that commit or later.

- MIDI Port: the TuxGuitar Synthesizer, with an audio device. View > Show Sound Chain on (F8).
- TuxGuitar's test plugins **Test Tone** (an instrument) and **Test Clip 0.5** (an effect), neither with an editor of
  its own. Copy `Test Tone.vst3` and `Test Clip 0.5.vst3` from `common/TuxGuitar-vst3/native-build/VST3/Release/`
  (built by the audio tests) into a VST3 folder TuxGuitar scans, then Sound Chain... > **Scan again**.
- Settings > Sound, Default instruments: Guitar tracks **Test Tone**; OK.
- For step 7, a plugin with many parameters and no editor of its own, if you have one (on Linux or macOS any plugin
  will do); skip it without one.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | File > New. Look at the Sound Chain panel. | The source button "Source: Test Tone (default for guitar tracks)" is picked. Under the action line a line reads "Test Tone: parameters", and under it one row: **Level:**, a slider at the right end, **1.0**, **Set**. The song is not marked as changed. | not run |
| 2 | Drag the Level slider to about the middle and let go. Play a few notes. | The value field shows the new value. The source button now reads "Source: Test Tone" (kept in the song) and the song is marked as changed. The notes are quieter. The rows stay in the panel. | not run |
| 3 | Edit > Undo once. | The source button reads "Source: Test Tone (default for guitar tracks)" again and the song is no longer changed (one undo step). The Level row shows the default's value again. | not run |
| 4 | Type 0.25 in the value field and press **Set**. | The slider moves to a quarter; the source is kept again ("Source: Test Tone"). | not run |
| 5 | Add **Test Clip 0.5** as an effect (VST3 effect: pick it, **Add**) and pick its button in the chain. | The line reads "Test Clip 0.5: parameters" with one row, **Ceiling**. Test Tone's row is gone (one plugin at a time). Moving the slider is one Undo step. | not run |
| 6 | Make the main window narrow, then wide again. | The rows stay whole; with one row there is one column at the left, the slider taking the spare width. The score above is never squeezed by more than the rows need. | not run |
| 7 | (A plugin with many parameters, no editor of its own) Add it and pick its button. Then make the window narrower and shorter. | The rows fill as many columns as the panel's width holds, reading down each column first. The panel grows to the longest column but never past half of the window's height: beyond that a scroll bar appears at the right of the rows. Narrower, there are fewer columns and more rows. | not run |
| 8 | Open the Sound Chain dialog (**Sound Chain...**), pick Test Clip 0.5 and press **Edit...**. | TuxGuitar's **Parameters** window opens as before (one row, **Ceiling**, with **Close** under it). Moving its slider moves the same parameter: the panel's row shows the new value too after an Undo/Redo. | not run |
| 9 | Hide editor, then Show editor; F8 twice. | The rows go and come back with the plugin's current values. | not run |

## Notes

- The tests check the column rule (TestParameterColumns: as many columns as fit, down each column first, height capped
  at half the window), the set command the rows use (TestSoundChainCommands: the default kept with its value), and when
  the area shows rows (TestEditorDock). This page checks what is seen.
- The Parameters window and the panel use the same rows (TGPluginParameterRowsView); in the window the extra width of a
  resized window now goes to the sliders only.
- Later, `fix/parameter-slider-width` stops a slider at about 400 pixels (step 6's slider no longer takes all the
  spare width): see "Parameter sliders keep their row together".
