---
title: Channel Settings without the VST3 types
description: The Channel Settings window no longer offers vst3-instrument or vst3-effect; VST3 plugins are chosen in the Sound Chain panel.
sidebar:
  order: 41
---

## What it tests

Commit `3b3258fdb` on `feature/sound-chain-meters`. The **Channel Settings** window (the older Synthesizer settings)
offered `vst3-instrument` and `vst3-effect` among its types. Choosing one added a sound chain element that named no
plugin: it played nothing, and before commit `e67b21bcf` picking it in the Sound Chain panel put errors in the console.
The window no longer offers those two types. VST3 plugins are chosen in the Sound Chain panel, as before.

## Setup

Build and launch with the VS Code task **app: build + run**. In **Tools > Settings > Sound**, set the MIDI Port to
**TuxGuitar Synthesizer**. Open a song with a guitar track, any song from the examples. Press **F8** if the Sound Chain
panel is hidden.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1   | Open **View > Show Instruments**. On the guitar track's channel, click the **Settings** button (its tooltip says "Settings"). | The **Channel Settings** window opens. | not run |
| 2   | Click **Set MIDI receiver**. Open the **Type** list. | The list has the other receiver types, such as `gervill` (General MIDI), but not `vst3-instrument`. Click **Cancel**. | not run |
| 3   | Click **Add audio processor**. Open the **Type** list. | The list has no `vst3-effect`. If it is empty, the window has no other effect types to offer; that is expected. Click **Cancel** and close the window. | not run |
| 4   | In the Sound Chain panel, choose a VST3 instrument for the source (VST3 instrument drop-down), such as Woodshed Guitar. Play a few notes. | The source's button reads the plugin's name, its editor opens in the editor area, and you hear it. | not run |
| 5   | Add a VST3 effect with the panel's effect drop-down and **Add**. Play. | The effect's button appears after the source, and you hear the effect. | not run |
| 6   | Open the Channel Settings window again for the same channel. | The receiver field shows `vst3-instrument` and the effect is listed: the window still shows a chain the panel made, though it no longer offers those types. Close it without changing anything. | not run |
| 7   | Open a song saved with an element added by type before this change (see [A VST3 element that names no plugin](../vst3-element-without-plugin/)), if you have one. | It opens; the panel's button reads its type, and picking it opens nothing, without an error. | not run |

## Notes

- Only the two VST3 types left the lists. Every other type the window offered before is still there.
- The tests cover the lists (`TestSynthManager`, `TestVst3Processors`); only the window itself needs a person.
