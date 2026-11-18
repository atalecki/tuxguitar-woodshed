---
title: Sound Chain panel, 2 - the docked panel
description: The selected track's sound chain at the foot of the main window, edited there; View > Show Sound Chain and F8 show or hide it.
sidebar:
  order: 30
---

## What it tests

Commit `44fd335ad` on `feature/sound-chain-panel`. A panel at the foot of the main window, under the fretboard and
above the quiet status line, shows the sound chain of the track the caret is on: the track's name, then one button per
element in the order the sound flows (the source, then each effect), one of them picked. Under it are the actions on
the picked element and on the chain, as in the Sound Chain dialog, and **Sound Chain...**, which opens the dialog
(Scan again and the scan report stay there). It is a plugin of its own, "TuxGuitar Sound Chain panel", so it can be
turned off in Tools > Plugins.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `44fd335ad` or
later on the branch. The install folder is
`desktop/build-scripts/tuxguitar-windows-swt-x86_64/target/tuxguitar-9.99-SNAPSHOT-windows-swt-x86_64`.

- To see the first start after the update, delete
  `%APPDATA%\tuxguitar\config\plugins\tuxguitar-sound-chain-panel.cfg` before launching (it holds only whether the
  panel is shown).
- MIDI Port: the TuxGuitar Synthesizer, with an audio device. Settings > Sound, Default instruments: Guitar tracks
  **Woodshed Guitar** (shipped profile). View > Show Fretboard on.
- At least one VST3 effect with an editor of its own (TONE3000 or Test Gain) and one without (Test Clip).

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Start TuxGuitar (first start, see Setup). File > New. | Under the fretboard, above the status line, the panel shows "Track 1" and a picked button "Source: Woodshed Guitar (default for guitar tracks)". Under it: Edit..., Disable, Move left, Remove (greyed: the source is picked), "VST3 effect:" with its list and Add, "VST3 instrument:" with its list, Use as source, Default instrument, Keep in song, and Sound Chain.... Labels and buttons line up on their middles. | not run |
| 2 | Open the View menu. | After Show Fretboard: **Show Sound Chain**, checked, with **F8** next to it. | not run |
| 3 | Click in the score, press **F8**. Press F8 again. | The panel goes, the score grows; then it comes back. View > Show Sound Chain is unchecked, then checked. | not run |
| 4 | Hide the panel with View > Show Sound Chain. Close TuxGuitar and start it again. Show it again (F8) and restart once more. | After the first restart the panel stays hidden; after the second it is shown: the choice is remembered. | not run |
| 5 | Pick an effect in the VST3 effect list and press **Add**. Add a second one. | Buttons "1. ..." and "2. ..." appear after the source, in that order. | not run |
| 6 | Click "2. ...". | It is picked (pressed), the source is not. Move left, Remove, Disable are enabled; Edit... too for a VST3 plugin. | not run |
| 7 | Press **Move left**. | The two effects swap: the moved one now reads "1. ...". | not run |
| 8 | Pick "1. ..." and press **Disable**. | Its button reads "1. [...]" and the button now says **Enable**. Press it: back to "1. ...". | not run |
| 9 | Pick the effect with its own editor and press **Edit...**. | The plugin's window opens in front of the main window, titled "<plugin> – Track 1". | not run |
| 10 | Add Test Clip, pick it, press **Edit...**. | TuxGuitar's Parameters window opens. | not run |
| 11 | Pick the source, press **Keep in song**. | The source button reads "Source: Woodshed Guitar" and Keep in song is greyed; Edit... opens Woodshed Guitar's window. Press **Default instrument**: "(default for guitar tracks)" comes back. | not run |
| 12 | Press **Remove** on an effect, then Edit > Undo, then Edit > Redo. | The effect goes, comes back, goes again; the panel shows each step. | not run |
| 13 | Track > Add Track. Click in the score of track 2, then back in track 1. | The panel shows "Track 2" with its source picked, then "Track 1" with its chain; the source is picked after each switch. | not run |
| 14 | Enter some notes on track 1 and press Play; while it plays, move the caret with the arrow keys. | Playback goes on smoothly; the panel does not flicker or redraw on each move. | not run |
| 15 | Press **Sound Chain...**. In the dialog, add an effect and close it. | The dialog opens on the same track while the panel stays; the panel then shows the new effect. | not run |
| 16 | Settings > Sound: choose another MIDI Port (for example the Microsoft GS Wavetable Synth); OK. | At the right of the action line a quiet line reads "Sound chains are heard only with the TuxGuitar Synthesizer as MIDI Port". The buttons still work. Choose the TuxGuitar Synthesizer again: the line goes. | not run |
| 17 | View > Show Main Toolbar off, then make the window narrower than the action line. | The action line wraps its groups onto a third line, in order (the edit buttons, the effect group, the instrument group, Sound Chain...). Widen it: back on one line. Main toolbar on again. (With the main toolbar shown, the window keeps the toolbar's width, so it does not get that narrow.) | not run |
| 18 | Make the window short (for example 1366×768 with the fretboard shown). | The score gives way first; the panel and fretboard keep their height. Nothing is cut off at the bottom. | not run |
| 19 | Tools > Plugins: untick "TuxGuitar Sound Chain panel". | The panel and View > Show Sound Chain go at once; F8 does nothing. Tools > Sound Chain... still opens the dialog. Tick it again: both come back. | not run |
| 20 | Tools > Plugins: untick "TuxGuitar audio". | View > Show Sound Chain is greyed. Tick it again (restart if asked). | not run |
| 21 | Tools > Keyboard Shortcuts: find "Show Sound Chain", give it another key, OK. | The new key toggles the panel and shows next to the View item; F8 no longer does. Restore F8 afterwards. | not run |

## Notes

- Which buttons are enabled for each kind of row, what the changes do, when the panel reads the chain again, the line
  wrapping and the MIDI Port line are covered by TestSoundChainRows, TestSoundChainCommands, TestSoundChainFollow and
  TestSoundChainStrip; this page checks what is seen and felt in the running app.
- Announced defaults: switching to another track picks its source; the panel never hides itself on a short window;
  no toolbar button for the panel yet; no level meters yet.
- The chain line wraps onto more lines when it is wider than the window (it does not scroll sideways), and the
  elements read "1. ...", "2. ..." without arrows between them.
- Known, pinned: after removing the picked effect, nothing is picked (as in the dialog).
