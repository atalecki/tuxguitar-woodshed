---
title: A VST3 element that names no plugin
description: Picking a sound chain element that is a VST3 type with no plugin chosen no longer throws; its button reads its type.
sidebar:
  order: 40
---

## What it tests

Commit `e67b21bcf` on `feature/sound-chain-meters`. The **Channel Settings** window (the older Synthesizer settings)
can add a source or an effect by its type alone, `vst3-instrument` or `vst3-effect`, with no plugin chosen. A song can
keep such an element. Before this commit, the Sound Chain panel showed its button as "Source: null", and picking it put
a `NullPointerException` and "The VST3 plugin null could not be opened" in the console. Now the button reads the type,
and picking it opens nothing, without an error.

## Setup

Build and launch with the VS Code task **app: build + run**, from a terminal you can see (the errors went there). In
**Tools > Settings > Sound**, set the MIDI Port to **TuxGuitar Synthesizer**. Open a song with a guitar track, any song
from the examples. Press **F8** if the Sound Chain panel is hidden.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1   | Open **View > Show Instruments**. On the guitar track's channel, click the **Settings** button (its tooltip says "Settings"). | The **Channel Settings** window opens. | not run |
| 2   | Click **Set MIDI receiver**. Under **Type**, choose `vst3-instrument`. Click **OK**. | The window's receiver field shows `vst3-instrument`. No error appears in the terminal. | not run |
| 3   | Click **Add audio processor**. Under **Type**, choose `vst3-effect`. Click **OK**. Close the window. | The effect is listed in the window before you close it. No error in the terminal. | not run |
| 4   | Click a note of the guitar track. Look at the Sound Chain panel. | The buttons read "Source: vst3-instrument", then "vst3-effect". Neither reads "null". | not run |
| 5   | Click the "Source: vst3-instrument" button. | It is picked (pressed). The editor area under the chain stays empty: no editor opens, and no line says the editor is opening. The terminal shows no `NullPointerException` and no "could not be opened". | not run |
| 6   | Click the "vst3-effect" button. | The same: picked, nothing opens, no error in the terminal. | not run |
| 7   | Click another track, then the guitar track again. | The panel follows the tracks. No error in the terminal. | not run |
| 8   | Save the song, close it, and open it again. Pick the guitar track and click both buttons again. | The buttons still read "Source: vst3-instrument" and "vst3-effect", and picking them still opens nothing, without an error. | not run |
| 9   | In the panel, choose a real VST3 instrument for the source (VST3 instrument drop-down), such as Woodshed Guitar. | The source's button reads the plugin's name, and its editor opens in the editor area as before. | not run |
| 10  | Add a real VST3 effect with the panel's effect drop-down and **Add**. Pick its button. | Its editor opens in the editor area as before. | not run |

## Notes

- An element that names no plugin still plays nothing. This page checks only that the panel stops treating it as a
  plugin. Whether the Channel Settings window should still offer the VST3 types is a separate question.
- The tests cover the panel's rows and the editor area's input (`TestSoundChainCommands`, `TestSoundChainRows`). Only
  the console's silence and the buttons' text need a person.
