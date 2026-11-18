---
title: Plugin editors inside the app, 6 - keys while an editor has the focus
description: With a plugin's editor inside the Sound Chain panel focused, TuxGuitar's shortcuts with Ctrl, Alt or a function key still work; plain keys go to the plugin; a click on the score gives the keys back.
sidebar:
  order: 37
---

## What it tests

Branch `feature/embedded-editors-follow-ups`, unit 4j (the commit "vst3, audio: TuxGuitar's shortcuts with Ctrl, Alt
or a function key work while a plugin's editor inside the panel has the focus", after the 4i commits). While you type in
a plugin's editor inside the Sound Chain panel, shortcuts such as F8, Ctrl+S, Ctrl+Z or Alt+Left reach TuxGuitar;
Space, letters, arrows and Enter go to the plugin. Windows only (other systems embed no plugin editor).

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at that commit or later.

- MIDI Port: the TuxGuitar Synthesizer, with an audio device. Settings > Sound, Default instruments: Guitar tracks
  **Woodshed Guitar**. View > Show Sound Chain on (F8).
- For step 6, a third-party VST3 plugin whose editor has a text field or reacts to keys (a preset name field, a
  plugin's own shortcuts), if you have one.
- Tools > Keyboard Shortcuts shows TuxGuitar's shortcuts; the steps use the default ones.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | File > New. Type a few notes on the score (click a string, type 3, 5...). Click inside Woodshed Guitar's editor in the panel (on its background, or a slider). | The editor has the focus (a slider you clicked moves with the arrow keys). | not run |
| 2 | With the focus still in the editor, press **Ctrl+Z**. | The last note typed is undone (TuxGuitar's Undo), although the focus is in the plugin's editor. | not run |
| 3 | Still in the editor, press **Ctrl+Y** (Redo), then **Ctrl+S**. | The note comes back; the Save dialog opens (or the song is saved). | not run |
| 4 | Click in the editor again and press **F8**. | The Sound Chain panel hides (View > Show Sound Chain). Press F8 again (the focus is back in TuxGuitar): the panel shows again with the editor. | not run |
| 5 | Click in the editor and press **Space**, then a **letter**, then the **arrow keys**. | Nothing plays or moves on the score: these keys stay with the plugin (a focused slider moves with the arrows). | not run |
| 6 | (A third-party plugin with a text field) Pick its button so its editor shows in the panel; click in its text field and type a word with Space and Backspace. | The text goes into the plugin's field; the score does not change. Ctrl+Z there undoes TuxGuitar's last change, not the plugin's typing (TuxGuitar's shortcut wins). | not run |
| 7 | Click once on the score, then press **Space**. | The keys are TuxGuitar's again: Space starts (and pressing it again stops) playback. | not run |
| 8 | Put the caret on the second string of the score, click in the editor and press **Alt+Up**. | The caret moves up one string on the score (TuxGuitar's Alt+Up), not inside the plugin. | not run |
| 9 | Open the Sound Chain dialog, **Edit...** the source so Woodshed's editor opens in a window of its own; click in it and press Ctrl+Z. | Nothing is undone: an editor in a window of its own keeps every key (only an editor inside the panel passes TuxGuitar's shortcuts). | not run |

## Notes

- The tests check the host side across two processes (TestVst3Host: Ctrl+Z, F8 and Alt+Left reach the window the
  editor is in with their modifiers held, Space, a plain Z, Ctrl+A and Ctrl+Shift+Z stay with the plugin; keys
  changed on the next embed; none passed from a window of its own; a key the plugin peeks at is passed once) and which
  shortcuts are passed (TestEditorKeys). This page checks that TuxGuitar runs them.
- Shortcuts on punctuation keys (Ctrl++, Ctrl+-, whose key depends on the keyboard layout) and Shift-only shortcuts
  stay with the plugin.
- A shortcut you change in Tools > Keyboard Shortcuts is passed from the next time an editor opens in the panel.
