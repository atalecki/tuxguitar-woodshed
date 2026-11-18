---
title: Undo after the profile file changed keeps the default out of the song
description: With the default instrument's editor open, undoing the keep made by a change reloads Woodshed Guitar's profile; when the profile file changed on disk meanwhile, that reload no longer keeps the default in the song again.
sidebar:
  order: 42
---

## What it tests

Commits `5147f2fdd` and `c3153c554` on branch `fix/default-reload-no-rekeep`, after `2e813ceec`. Edit on a track playing the default
instrument opens Woodshed Guitar's editor; your first change there keeps the default in the song (one Undo step), and
Undo takes it out again, Woodshed Guitar loading the settings' profile again. When the profile file had changed on disk
since you pressed Edit, that reload used to count as a change of yours, so the default was kept in the song again at
once (Undo seemed to do nothing). Now a load Woodshed Guitar did because TuxGuitar asked for it never counts as a change;
loading a profile yourself in its editor (Load profile..., Update from file) still does.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `c3153c554` or
later.

- MIDI Port: the TuxGuitar Synthesizer, with an audio device. View > Show Sound Chain on (F8).
- Two NAM captures that sound clearly different, for example `A.nam` and `B.nam`. Copy `A.nam` to a folder of your own
  as `Test profile.nam`. In Settings > Sound > Default instruments, choose **Woodshed Guitar** for guitar tracks with
  **Test profile.nam** as the profile.
- A new song with one guitar track and a few notes. Back up your TuxGuitar settings first if you want to keep your
  chosen profile, and set it back when done.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Pick the source of track 1 in the Sound Chain panel and press **Edit**. | Woodshed Guitar's editor shows "Profile: Test profile.nam". The source row says "(default for guitar tracks)". | not run |
| 2 | Move the **Output level** control in the editor. | The source row drops "(default for guitar tracks)": the default is kept in the song (one Undo step). | not run |
| 3 | Outside TuxGuitar, copy `B.nam` over `Test profile.nam` (replace the file). | Nothing changes in TuxGuitar yet (the editor may note that the file changed). | not run |
| 4 | Press Ctrl+Z (Edit > Undo). Wait a second for the profile to load. | The source row says "(default for guitar tracks)" again and stays so. The editor stays open, Output level back where it was, and the profile now sounds like B. Edit > Undo no longer offers to take back a keep. | not run |
| 5 | Play a few notes. | They sound through the B capture, as the default does now. | not run |
| 6 | Move the Output level control again. | The default is kept in the song again (your change counts), one Undo step. | not run |
| 7 | Press Ctrl+Z, then in the editor press **Load profile...** and choose `A.nam`. | Loading a profile yourself keeps the default in the song, as any change of yours does. | not run |

## Notes

- Before this change, step 4 left the source row without "(default for guitar tracks)": the default was kept again
  by the reload, and a further Undo was needed.
- Covered by tests: `default_instruments.feature`, "undoing the keep after the profile file changed on disk does not
  keep the default again"; `TestVst3Host` (a load TuxGuitar asks for is told as no change).
