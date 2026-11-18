---
title: Plugin editors inside the app, 3 - Open in window, put back, and where the window was left
description: Open in window is remembered per plugin; Put back in the panel or the window's close box puts the editor back; a window of its own opens where it was left.
sidebar:
  order: 34
---

## What it tests

Branch `feature/embedded-editors`, unit 4h (the commit "vst3, audio: a window of its own opens where it was left;
its close box puts the editor back in the panel; a plugin may ask for its editor", and later). Q6, Q7, O2, O3.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at that commit or later.
MIDI Port: the TuxGuitar Synthesizer. Settings > Sound, Default instruments: Guitar tracks **Woodshed Guitar**. View >
Show Sound Chain on (F8). A second screen helps for step 7, but is not needed.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | File > New. Woodshed Guitar's editor shows in the panel. Press **Open in window**. | The editor goes to a window of its own in front of TuxGuitar, titled "Woodshed Guitar – Track 1". The panel's line reads "Woodshed Guitar's editor is in its own window." with **Put back in the panel**; the score gets its room back. | not run |
| 2 | Move that window to a corner and make it wider. Press **Put back in the panel**. | The window closes; the editor shows in the panel again. | not run |
| 3 | Press **Open in window** again. | The window opens where you left it in step 2, at the width you gave it. | not run |
| 4 | Close TuxGuitar and start it again. File > New. | Woodshed Guitar's editor opens in its window of its own straight away (remembered for this plugin), where you left it; the panel says it is in its own window. | not run |
| 5 | Close that window with its close box (X). | The editor is back in the panel (the close box puts it back), and stays there after a restart of TuxGuitar. | not run |
| 6 | Add another VST3 effect with an editor of its own and pick it, press Open in window; then pick the source again. | The effect's window stays open while Woodshed Guitar's editor shows in the panel (one plugin remembered in a window, the other not). | not run |
| 7 | Leave an editor's window on a second screen, close TuxGuitar, unplug or turn off that screen, start TuxGuitar and open the editor in its window. | The window opens on a screen you can see, not off screen. | not run |
| 8 | (A plugin that can ask for its editor, if you have one: some show a button for it in their own UI.) Use it. | The panel picks that plugin's button and shows its editor; with Hide editor chosen, or for a plugin of another track, it opens in a window of its own. | not run |

## Notes

- The tests cover: TestVst3Host (a window of its own opens in the frame given, at its size when it can be resized,
  moved onto a visible screen; a plugin asking for its editor is heard), TestEditorDock (pop-out, put back, the close
  box, remembered plugins), hosting_vst3_plugins.feature (the close box told, a window opening where it was left, a
  request told to TuxGuitar), TestEditorFrames. This page checks what is seen in the running app.
