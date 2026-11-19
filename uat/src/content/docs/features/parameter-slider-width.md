---
title: Parameter sliders keep their row together
description: A parameter row's slider grows with the room its column spares, but stops at about 400 pixels, so name, slider, value and Set stay together at the left of the Sound Chain panel.
sidebar:
  order: 38
---

## What it tests

Commit `dff929fd4` on branch `fix/parameter-slider-width` ("audio, audio-ui: a parameter slider stops at about 400 pixels, so its
row stays together", after `2e813ceec`). Before it, a plugin with few parameters (Test Tone: one Level row) showed its
slider stretched across the whole panel, about 1800 pixels, with the value and **Set** far away at the right edge.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at that commit or later.

- MIDI Port: the TuxGuitar Synthesizer, with an audio device. View > Show Sound Chain on (F8).
- TuxGuitar's test plugin **Test Tone** (an instrument without an editor of its own): copy `Test Tone.vst3` from
  `common/TuxGuitar-vst3/native-build/VST3/Release/` (built by the audio tests) into a VST3 folder TuxGuitar scans, then
  Sound Chain... > **Scan again**.
- Settings > Sound, Default instruments: Guitar tracks **Test Tone**; OK.
- For step 4, a plugin with many parameters and no editor of its own, if you have one; skip it without one.
- For step 5, a second screen or a Windows display scale above 100% (Settings > System > Display > Scale), if you have
  one; skip it without.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Maximise the main window. File > New. Look at the Sound Chain panel. | Under "Test Tone: parameters" one row at the left: **Level:**, a slider about 400 pixels long (about a fifth of a 1920-pixel-wide screen), **1.0**, **Set**, side by side. The rest of the line to the right is empty. | not run |
| 2 | Drag the Level slider to about the middle and let go; then type 0.25 and press **Set**. | The slider and the value follow each other as before; each is one Undo step. | not run |
| 3 | Restore the window and make it narrower step by step, as far as it goes. | The row stays whole and at the left. Once the panel is narrow enough the slider gets shorter, never shorter than about 240 pixels. Widen again: it grows back to about 400 and no more. | not run |
| 4 | (A plugin with many parameters, no editor of its own) Add it and pick its button. Make the window narrower, then wide again. | As many columns as before this change; in each, the rows read down the column. The sliders take what their columns spare, up to about 400 pixels each; a wide window leaves the room at the right of the last column empty instead of stretching the sliders. | not run |
| 5 | (Display scale above 100%, or a second screen with another scale) Move the window there, or change the scale, and look at step 1's row again. | The slider is as long compared with the text as at 100%: about 400 pixels times the scale (600 at 150%). | not run |
| 6 | Open the Sound Chain dialog (**Sound Chain...**), pick Test Tone and press **Edit...**. | TuxGuitar's **Parameters** window looks as before: one row with a short slider (about 240 pixels), **Close** under it. | not run |

## Notes

- The tests check the rule (TestParameterColumns: the narrowest slider widened by what each column spares, at most
  400, at least 240). This page checks what is seen.
- The Parameters window shares the rule; it is packed to its rows and cannot be resized, so its sliders stay at the
  narrowest width as before.
