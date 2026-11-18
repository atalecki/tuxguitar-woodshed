---
title: Plugin editors inside the app, 1 - the editor area of the Sound Chain panel
description: The picked plugin's own editor shows inside the Sound Chain panel, across its width, as high as it needs; Hide editor gives the room back.
sidebar:
  order: 32
---

## What it tests

Branch `feature/embedded-editors`, unit 4f (the commit "audio-ui: the Sound Chain panel shows the picked plugin's
editor inside it", after `94c24303d`). Picking a button of the chain in the Sound Chain panel shows that plugin's own
editor under the chain and action lines, inside TuxGuitar's window; Edit became Hide editor / Show editor.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at that commit or later.

- MIDI Port: the TuxGuitar Synthesizer, with an audio device. Settings > Sound, Default instruments: Guitar tracks
  **Woodshed Guitar** (shipped profile). View > Show Sound Chain on (F8).
- For steps 8 and 9 a third-party VST3 effect whose editor has a fixed size (TONE3000, AmpliTube...), installed in a
  VST3 folder TuxGuitar scans.
- Step 11 needs a second screen at another scale (for example 100 % and 150 %); skip it without one.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | File > New. Look at the Sound Chain panel at the foot of the window. | The source button "Source: Woodshed Guitar (default for guitar tracks)" is picked. Under the action line a line reads "Woodshed Guitar" with **Open in window** at the right, and under it Woodshed Guitar's own editor (Load profile..., Input gain, Output level, Quality) across the whole width of the window. The score above is shorter by the editor's height; nothing is cut or overlaps. | not run |
| 2 | Drag Input gain and Output level in the panel. Play a few notes. | The sliders move smoothly, their values update; the sound follows. The song is marked as changed (the default is kept in the song). | not run |
| 3 | Make the main window narrower, then wider again (drag its border, or restore / maximise). | The editor follows the window's width each time: its sliders get shorter or longer, its labels stay whole. The panel's height does not change. | not run |
| 4 | Press **Hide editor** (the first button of the action line). | The editor and its line disappear; the score gets the room back. The button now reads **Show editor**. | not run |
| 5 | Close TuxGuitar and start it again. | The editor stays hidden and the button reads Show editor (remembered). Press **Show editor**: the editor shows again. | not run |
| 6 | Press F8 (View > Show Sound Chain off), then F8 again. | The whole panel goes, then comes back with the editor showing, as before. | not run |
| 7 | Add a second track (Track > Add track) and move the caret to it, then back to track 1. | The panel follows the caret: the second track's source editor shows (or nothing, for a track whose source is no VST3 plugin), then track 1's again. | not run |
| 8 | On track 1 add your third-party VST3 effect (VST3 effect: pick it, **Add**), then pick its button in the chain. | Its editor shows in the panel at its own size, at the left; the rest of the row is the panel's background. Woodshed Guitar's editor is gone from the panel (one editor at a time) and keeps playing. | not run |
| 9 | Click in that effect's editor, turn a knob. | The plugin reacts as in its own window. | not run |
| 10 | Pick the source button again. | Woodshed Guitar's editor shows again, as it was. | not run |
| 11 | (Two screens of different scale) Move the main window to the other screen. | The editor is redrawn at that screen's scale, sharp, and still fills the width. | not run |
| 12 | Open the Sound Chain dialog (**Sound Chain...**), pick the source and press its **Edit...**. | Woodshed Guitar's editor opens in a window of its own; the panel says "Woodshed Guitar's editor is in its own window." with **Put back in the panel**. Press it: the window closes and the editor is in the panel again. | not run |

## Notes

- TuxGuitar's own window is per-monitor DPI aware (version 2), measured in unit 4a; an editor inside it takes that
  awareness, so it is drawn at each screen's scale.
- The tests check the host side with a stand-in window (TestVst3Host: inside, filling the width, too large, the DPI
  awareness taken, answered at once while TuxGuitar's thread is busy) and the decisions of the area (TestEditorDock);
  this page checks what is seen in the running app.
- Open in window and its remembering, an editor too large for the panel, and a plugin that stops responding have
  pages of their own.
