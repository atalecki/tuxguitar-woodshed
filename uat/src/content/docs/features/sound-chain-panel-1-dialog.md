---
title: Sound Chain panel, 1 - the dialog still works
description: The Sound Chain dialog greys, enables and changes the chain exactly as before, now through the code the docked panel shares.
sidebar:
  order: 29
---

## What it tests

Commits `fd36d3214` and `c977e0242` on `feature/sound-chain-panel`; step 7 changed with `92b40ca90`. Nothing new is visible: the Sound Chain dialog now
decides which buttons are enabled, and runs its changes, through the code the docked Sound Chain panel (next pages)
shares with it. This page checks that the dialog behaves exactly as it did.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `c977e0242` or
later on the branch. The install folder is
`desktop/build-scripts/tuxguitar-windows-swt-x86_64/target/tuxguitar-9.99-SNAPSHOT-windows-swt-x86_64`.

- MIDI Port: the TuxGuitar Synthesizer, with an audio device. Settings > Sound, Default instruments: Guitar tracks
  **Woodshed Guitar** (shipped profile).
- At least one VST3 effect without an editor of its own installed (for example TuxGuitar's test plugin "Test Clip"),
  and one with its own editor (TONE3000 or "Test Gain").

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | File > New. Tools > Sound Chain... | The dialog opens on track 1. The list shows "Source: Woodshed Guitar (default for guitar tracks)". Nothing is picked: Up, Remove, Disable and Edit... are greyed; Add, Use as source, Default instrument and Keep in song are enabled. | not run |
| 2 | Pick an effect in the VST3 effect drop-down and press **Add** twice (two effects). | Rows "1. ..." and "2. ..." appear after the source. | not run |
| 3 | Pick row 1. | Up is greyed (it is the first effect); Remove and Disable are enabled; Edit... is enabled for a VST3 plugin. | not run |
| 4 | Pick row 2 and press **Up**. | The two effects swap. | not run |
| 5 | Pick row 1 and press **Disable**. | The row reads in brackets, "1. [..]", and the button now says **Enable**. Press it: the brackets go. | not run |
| 6 | Pick row 2 and press **Remove**. | Row 2 goes. Nothing is picked now. | not run |
| 7 | Pick the source row. | Up, Remove and Disable are greyed. Edit... is enabled (since `92b40ca90`): pressing it opens Woodshed Guitar's own window, the default instrument's, in front of the main window. Close it with its close box without changing anything: the source row still reads "(default for guitar tracks)", and Edit > Undo has nothing new to undo. | not run |
| 8 | Add the effect without an editor of its own (Test Clip), pick its row and press **Edit...**. | TuxGuitar's Parameters window opens, one row per parameter. | not run |
| 9 | Pick the row of the effect with its own editor (TONE3000 or Test Gain) and press **Edit...**. | The plugin's own window opens in front of the main window, titled "<plugin> – <track name>". | not run |
| 10 | Press **Keep in song**, then pick the source row. | The source row now reads "Source: Woodshed Guitar" (no "default for ..."): the track has its own instrument. Keep in song is greyed now, Edit... is enabled; pressing it opens Woodshed Guitar's window in front of the main window. | not run |
| 11 | Press **Default instrument**. | The source row reads "Source: Woodshed Guitar (default for guitar tracks)" again. | not run |
| 12 | Pick an instrument in the VST3 instrument drop-down and press **Use as source**. | The source row names that instrument. | not run |
| 13 | Edit > Undo, several times. | Each change goes back one step, in reverse order, and the dialog (if still open) shows each step after reopening it. | not run |
| 14 | Add an audio track (Tools > Add Audio Track) and open Tools > Sound Chain... on it. | The source row reads "Source: takes". Use as source, Default instrument and Keep in song are greyed; Add is enabled. | not run |

## Notes

- TestSoundChainRows checks which control is enabled for every kind of row; TestSoundChainCommands checks each change
  against the real VST3 host. This page checks the dialog in the running app.
- Known, pinned: after removing the picked effect, nothing is picked (as before this change).
- Since `92b40ca90`, Edit... on a default source opens the default instrument's editor; what its first change does is
  checked on the panel's page (steps 22-26), and the dialog shares the same decision.
