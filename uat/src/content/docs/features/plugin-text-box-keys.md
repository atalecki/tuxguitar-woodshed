---
title: Text-editing keys in a plugin's text box
description: While a standard Windows text box in a plugin's editor inside the Sound Chain panel has the focus, its editing keys (copy, cut, paste, select all, undo, redo, word jumps, word selection, word deletion) work in that text box instead of running TuxGuitar's shortcuts.
sidebar:
  order: 39
---

## What it tests

Commits on branch `fix/plugin-text-box-keys`, after `2e813ceec`:

- `4e60ab5e7`: Ctrl+C, Ctrl+X, Ctrl+V, Ctrl+A and Ctrl+Z stay with a standard text box.
- `50bd9ba6b`: so do Ctrl+Y and Ctrl+Shift+Z (redo), Ctrl+Left and Ctrl+Right (a word left or right), Ctrl+Shift+Left
  and Ctrl+Shift+Right (select by word), Ctrl+Backspace and Ctrl+Delete (delete a word).
- `2c3db9455`: a standard text box counts whatever its class is called (plugins made with Delphi or .NET, or giving
  Windows' text box a name of their own).

Before them, those keys went to TuxGuitar even while you typed in a plugin's text box (Ctrl+Z undid the score, Ctrl+Left
moved to the previous measure, Ctrl+Delete cleaned a beat). Elsewhere in the editor, and in text boxes a plugin draws
itself, they still go to TuxGuitar. Windows only (other systems embed no plugin editor).

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `2c3db9455` or
later.

- MIDI Port: the TuxGuitar Synthesizer, with an audio device. View > Show Sound Chain on (F8).
- A third-party VST3 plugin whose editor has a **standard Windows text box** (a preset name, a value you type). To tell
  one: right-click in the box; Windows' own menu (Undo, Cut, Copy, Paste, Delete, Select All) means it is standard. A
  plugin made with Delphi or .NET shows that menu too and counts. Woodshed Guitar has no text box. Without such a
  plugin, run steps 8 and 9 only.
- For step 8, optionally a plugin whose text box is drawn by the plugin itself (right-click shows the plugin's own menu,
  or none).

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | File > New, type a few notes in the first two measures of the score and leave the caret on a note of measure 2. Add the plugin to track 1 (VST3 effect or instrument), pick its button so its editor shows in the panel. Click in its standard text box and type a word. | The word appears in the box; the score does not change. | not run |
| 2 | In the box press **Ctrl+A**, then **Ctrl+C**, then **Ctrl+X**. | All the word is selected (TuxGuitar does not select a measure or the whole score); Ctrl+X removes the word from the box. Nothing on the score changes. | not run |
| 3 | In the box press **Ctrl+V**, then **Ctrl+Z**. | The word comes back (pasted), then Ctrl+Z undoes inside the box. The notes you typed on the score are all still there (TuxGuitar's Undo did not run). | not run |
| 4 | Clear the box and type **one two three**. Press **Ctrl+Left** twice, then **Ctrl+Right** once. | The caret jumps to the start of "three", then of "two", then back to the start of "three". The score's caret stays in measure 2 (TuxGuitar's previous and next measure did not run). | not run |
| 5 | Press **Ctrl+Shift+Right**, then **Ctrl+Shift+Left** twice. | Ctrl+Shift+Right selects "three"; the first Ctrl+Shift+Left takes that back, the second selects "two " before the caret. The score's caret stays in measure 2 (not sent to the first or last measure). | not run |
| 6 | Click just before "two". Press **Ctrl+Delete**, then **End**, then **Ctrl+Backspace**. | Ctrl+Delete deletes "two " (the box reads "one three"); Ctrl+Backspace deletes "three" (an older box may put a small square instead: that is the box's own doing). The note under the score's caret is still there (TuxGuitar's Clean Beat did not run). | not run |
| 7 | Press **Ctrl+Z**, then **Ctrl+Y**, then **Ctrl+Shift+Z**. | Ctrl+Z undoes in the box. Ctrl+Y and Ctrl+Shift+Z redo there if the box has redo; a simple box with one undo level does nothing on them (Ctrl+Z again redoes in such a box). Either way the score does not change (TuxGuitar's Redo did not run). | not run |
| 8 | Still in the box, press **Ctrl+S**, then **F8**. | Other shortcuts still reach TuxGuitar: the Save dialog opens (cancel it); F8 hides the Sound Chain panel. Press F8 again to show it. | not run |
| 9 | Click on the plugin's editor outside the text box (its background, a knob), or in a text box the plugin draws itself, and press **Ctrl+Left**, then **Ctrl+Z**. | TuxGuitar runs them: the score's caret moves to the previous measure, then Undo removes the last note typed on the score. | not run |
| 10 | Click once on the score and press **Ctrl+Z**. | TuxGuitar's Undo, as always. | not run |

## Notes

- The host tests check this across two processes (TestVst3Host): in an Edit all 13 keys stay with the box, in a
  RichEdit some of them; in an Edit and a RichEdit under class names of the plugin's own, the keys stay with the box;
  in a list box, in the view itself and outside a box they go to TuxGuitar's window; F8, Alt+Left, Ctrl+Shift+C and
  Ctrl+Up typed in a box still go there. This page checks it with a real plugin.
- Only those 13 keys are kept by the box. Other keys still go to TuxGuitar when TuxGuitar has a shortcut on them, for
  example Ctrl+Shift+Delete (Remove Track), Ctrl+Shift+C.
- A plugin's file dialog (Woodshed Guitar's Load profile..., say) is a window of its own, not inside the panel: every
  key there was always the dialog's.
