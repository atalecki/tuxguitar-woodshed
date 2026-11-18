---
title: Copy, cut, paste, select all and undo in a plugin's text box
description: While a standard Windows text box in a plugin's editor inside the Sound Chain panel has the focus, Ctrl+C, Ctrl+X, Ctrl+V, Ctrl+A and Ctrl+Z work in that text box instead of running TuxGuitar's shortcuts.
sidebar:
  order: 39
---

## What it tests

Commit `4e60ab5e7` on branch `fix/plugin-text-box-keys` ("vst3: a standard text box in an editor inside TuxGuitar's window keeps
copy, cut, paste, select all and undo", after `2e813ceec`). Before it, those five shortcuts went to TuxGuitar even while
you typed in a plugin's text box (Ctrl+Z undid the score, not your typing). Elsewhere in the editor, and in text boxes a
plugin draws itself, they still go to TuxGuitar. Windows only (other systems embed no plugin editor).

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at that commit or later.

- MIDI Port: the TuxGuitar Synthesizer, with an audio device. View > Show Sound Chain on (F8).
- A third-party VST3 plugin whose editor has a **standard Windows text box** (a preset name, a value you type). To tell
  one: right-click in the box; Windows' own menu (Undo, Cut, Copy, Paste, Delete, Select All) means it is standard.
  Woodshed Guitar has no text box. Without such a plugin, run steps 5 and 6 only.
- For step 5, optionally a plugin whose text box is drawn by the plugin itself (right-click shows the plugin's own menu,
  or none).

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | File > New, type a few notes on the score. Add the plugin to track 1 (VST3 effect or instrument), pick its button so its editor shows in the panel. Click in its standard text box and type a word. | The word appears in the box; the score does not change. | not run |
| 2 | In the box press **Ctrl+A**, then **Ctrl+C**, then **Ctrl+X**. | All the word is selected (TuxGuitar does not select a measure or the whole score); Ctrl+X removes the word from the box. Nothing on the score changes. | not run |
| 3 | In the box press **Ctrl+V**, then **Ctrl+Z**. | The word comes back (pasted), then Ctrl+Z undoes inside the box. The notes you typed on the score are all still there (TuxGuitar's Undo did not run). | not run |
| 4 | Still in the box, press **Ctrl+S**, then **F8**. | Other shortcuts still reach TuxGuitar: the Save dialog opens (cancel it); F8 hides the Sound Chain panel. Press F8 again to show it. | not run |
| 5 | Click on the plugin's editor outside the text box (its background, a knob), or in a text box the plugin draws itself, and press **Ctrl+Z**. | TuxGuitar's Undo runs: the last note typed on the score goes. | not run |
| 6 | Click once on the score and press **Ctrl+Z**. | TuxGuitar's Undo, as always. | not run |

## Notes

- The host tests check this across two processes (TestVst3Host: in an Edit and in a RichEdit box Ctrl+C, X, V, A and
  Z stay with the box; F8, Alt+Left and Ctrl+Shift+C still reach TuxGuitar's window; the same keys typed outside a box
  go to TuxGuitar). This page checks it with a real plugin.
- Only Ctrl with those five letters is kept by the box. Other editing keys (Ctrl+Y, Ctrl+Shift+Z, Ctrl+Left/Right,
  Ctrl+Backspace) still go to TuxGuitar when TuxGuitar has a shortcut on them.
- A plugin's file dialog (Woodshed Guitar's Load profile..., say) is a window of its own, not inside the panel: every
  key there was always the dialog's.
