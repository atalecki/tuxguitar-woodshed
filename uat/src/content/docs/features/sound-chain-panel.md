---
title: Sound Chain panel, 2 - the docked panel
description: The selected track's sound chain at the foot of the main window, edited there; View > Show Sound Chain and F8 show or hide it.
sidebar:
  order: 30
---

## What it tests

Commit `44fd335ad` on `feature/sound-chain-panel`, with `92b40ca90` (Edit on a default source) and `e7c2bb88b`
(arrows between the chain's buttons). A panel at the foot of the main window, under the fretboard and
above the quiet status line, shows the sound chain of the track the caret is on: the track's name, then one button per
element in the order the sound flows (the source, then each effect, an arrow "→" leading into each effect), one of them
picked. Under it are the actions on
the picked element and on the chain, as in the Sound Chain dialog, and **Sound Chain...**, which opens the dialog
(Scan again and the scan report stay there). It is a plugin of its own, "TuxGuitar Sound Chain panel", so it can be
turned off in Tools > Plugins.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `e7c2bb88b` or
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
| 1 | Start TuxGuitar (first start, see Setup). File > New. | Under the fretboard, above the status line, the panel shows "Track 1" and a picked button "Source: Woodshed Guitar (default for guitar tracks)". Under it: Edit... (enabled: it opens the default instrument's editor), Disable, Move left, Remove (greyed: the source is picked), "VST3 effect:" with its list and Add, "VST3 instrument:" with its list, Use as source, Default instrument, Keep in song, and Sound Chain.... Labels and buttons line up on their middles. | not run |
| 2 | Open the View menu. | After Show Fretboard: **Show Sound Chain**, checked, with **F8** next to it. | not run |
| 3 | Click in the score, press **F8**. Press F8 again. | The panel goes, the score grows; then it comes back. View > Show Sound Chain is unchecked, then checked. | not run |
| 4 | Hide the panel with View > Show Sound Chain. Close TuxGuitar and start it again. Show it again (F8) and restart once more. | After the first restart the panel stays hidden; after the second it is shown: the choice is remembered. | not run |
| 5 | Pick an effect in the VST3 effect list and press **Add**. Add a second one. | Two buttons appear after the source, in that order, an arrow "→" before each: "Source: ..." → "effect A" → "effect B". The effects carry no numbers (the arrows show the order; the dialog's list keeps "1.", "2."). | not run |
| 6 | Click the second effect's button. | It is picked (pressed), the source is not. Move left, Remove, Disable are enabled; Edit... too for a VST3 plugin. | not run |
| 7 | Press **Move left**. | The two effects swap: the moved one now comes right after the source's arrow. | not run |
| 8 | Pick the first effect and press **Disable**. | Its button reads "[...]" and the button now says **Enable**. Press it: the brackets go. | not run |
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
| 22 | File > New (guitar default: Woodshed Guitar). Pick the source button and press **Edit...**. | Woodshed Guitar's own window opens in front of the main window, titled "Woodshed Guitar – Track 1". The source button still reads "(default for guitar tracks)"; the song is not marked as changed. | not run |
| 23 | Close that window by its close box without touching anything. | Nothing changes: the source button still reads "(default for guitar tracks)", the song is not marked as changed, Edit > Undo has nothing new to undo. | not run |
| 24 | Press **Edit...** again and drag the Output level slider. Play a few notes meanwhile. | When you let go, the source button reads "Source: Woodshed Guitar" (no "default for ..."), Keep in song is greyed, the song is marked as changed. The window stays open where it is and the sound goes on without a gap or a reload; the new level is heard. | not run |
| 25 | Drag Input gain too. Then Edit > Undo once, and once more. | The first undo takes back only Input gain (the window stays open). The second takes back the keep: the source button reads "(default for guitar tracks)" again and the window closes (the default instrument plays afresh, see Notes). | not run |
| 26 | Edit > Redo twice. Then Settings > Sound: choose another guitar profile, OK. | The track has its own Woodshed Guitar again, with both changes. After the Settings change it still sounds as kept: it no longer follows the settings. | not run |
| 27 | Add four or five effects and make the window narrower than the chain line (main toolbar off, as in step 17). | The chain line wraps onto more lines, in order; an arrow always stays with the button it leads into, so a wrapped line starts with "→ effect", never ends on an arrow alone. No sideways scroll bar. Widen it: back on one line. | not run |

## Notes

- Which buttons are enabled for each kind of row, what the changes do, when the panel reads the chain again, the line
  wrapping and the MIDI Port line are covered by TestSoundChainRows, TestSoundChainCommands, TestSoundChainFollow and
  TestSoundChainStrip; this page checks what is seen and felt in the running app.
- Announced defaults: switching to another track picks its source; the panel never hides itself on a short window;
  no toolbar button for the panel yet; no level meters yet.
- The chain line wraps onto more lines when it is wider than the window (it does not scroll sideways); since
  `e7c2bb88b` an arrow "→" leads into each effect and wraps with it (TestSoundChainStrip), and the effects carry no
  numbers in the panel (the dialog keeps them).
- Since `92b40ca90`, Edit... on a default source opens the default instrument's editor; its first change keeps the
  default in the song (default_instruments.feature, TestDefaultInstrumentEditor). The same plugin goes on playing after
  the keep, so its window stays open (TestDefaultSource). Pinned: undoing the keep closes the window, because the
  default instrument then starts afresh from the settings.
- Known, pinned: after removing the picked effect, nothing is picked (as in the dialog).
