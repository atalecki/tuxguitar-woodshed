---
title: Saving a plugin edited while nothing plays it
description: A change made to a plugin through TuxGuitar, or undone, is what the song saves, even while no sound engine runs the plugin.
sidebar:
  order: 16
---

## What it tests

Commit `8323a69e2` on `fix/kept-plugin-parameter-save`. When the TuxGuitar Synthesizer is not the MIDI port, a plugin
whose editor you open is kept open on its own. Before this fix, a parameter you then changed through TuxGuitar, or an
editor change you undid, was lost (or came back) when you saved: the song saved what that kept plugin still held. Now
the song saves what you see in it.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `8323a69e2` or
later on the branch.

- File > New. In Settings > Sound, choose a MIDI port other than the TuxGuitar Synthesizer (Java Sound or the system's
  MIDI synthesizer), so no sound chain runs while you edit.
- In track 1's Sound Chain, choose Woodshed Guitar as its instrument and have two `.nam` captures ready (**Clean** and
  **Lead**).

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Open Woodshed Guitar's editor and Load profile... **Clean**. Close the editor. | The song shows as modified. | not run |
| 2 | Open the editor again and Load profile... **Lead**. Close the editor, then Edit > Undo once. | The song is back to **Clean** (Edit > Redo is offered). | not run |
| 3 | Save the song as `kept.tg`, close it and open it again. Open Woodshed Guitar's editor. | It says "Profile: Clean.nam", not Lead. | not run |
| 4 | Open the editor, move **Output level** to about -10 dB, close it. Then Edit > Undo. Save, close, reopen and open the editor. | **Output level** is back at 0 dB, as after the undo. | not run |
| 5 | Without undoing anything, move **Input gain** to about +6 dB in the editor, close it, save, reopen and open the editor. | **Input gain** is at about +6 dB: a change you keep is saved too. | not run |

## Notes

- The tests already cover: a parameter set through TuxGuitar after the plugin's own edit is saved, an editor edit
  undone is not saved, a change the plugin's editor made without telling TuxGuitar is still saved, and the kept plugin
  is not opened again after saving.
- With the TuxGuitar Synthesizer as the MIDI port the plugin runs in the sound chain and was not affected.
