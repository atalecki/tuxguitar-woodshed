---
title: Sound Chain panel, 3 - the default's editor after Undo, and Parameters for a default
description: Undoing the keep that a default instrument's first change made leaves its editor open where it was; a default VST3 instrument without its own editor is edited in TuxGuitar's Parameters window.
sidebar:
  order: 31
---

## What it tests

Branch `feature/sound-chain-panel-follow-ups`: commits `347003d81`, `26cc8fb97` and `122628115` (Undo of the keep
leaves the default's editor open, the same plugin playing the default again) and `afdc251f2` (a default VST3 instrument
without an editor of its own: Edit opens TuxGuitar's Parameters window, whose first change keeps it in the song).

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `afdc251f2` or
later on the branch. The install folder is
`desktop/build-scripts/tuxguitar-windows-swt-x86_64/target/tuxguitar-9.99-SNAPSHOT-windows-swt-x86_64`.

- MIDI Port: the TuxGuitar Synthesizer, with an audio device. Settings > Sound, Default instruments: Guitar tracks
  **Woodshed Guitar** (shipped profile). View > Show Sound Chain on.
- For steps 7 to 12: TuxGuitar's test instrument **Test Tone** (`5f3ff2d23`; no editor of its own; it sounds a plain sine at its
  Level). Copy `common/TuxGuitar-vst3/native-build/VST3/Release/Test Tone.vst3` (built by the audio tests) into a VST3
  folder TuxGuitar scans, for example `%CommonProgramFiles%\VST3`, then press **Scan again** in the Sound Chain dialog.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | File > New. Enter a few notes on track 1. In the Sound Chain panel pick the source button ("Source: Woodshed Guitar (default for guitar tracks)") and press **Edit...**. Move the window to a corner of the screen and make it wider. | Woodshed Guitar's window opens in front of the main window. It stays where you put it, at the width you gave it. | not run |
| 2 | Drag the **Output level** slider down to about -6 dB. | The source button reads "Source: Woodshed Guitar" (kept in the song); the song is marked as changed. The window stays where it is. | not run |
| 3 | Edit > Undo. | The source button reads "(default for guitar tracks)" again. The window **stays open, in the same place and size**; its Output level is back at 0 dB and its profile line shows the profile chosen in Settings. Play: the track sounds as the default does (no -6 dB). | not run |
| 4 | In the same window drag **Input gain**. | The source button reads "Source: Woodshed Guitar" again (one new undo step); the window stays open. Edit > Undo: back to the default, the window still open. | not run |
| 5 | Drag Output level again, then Edit > Undo, then Edit > Redo. | After Undo: as in step 3. After Redo: "Source: Woodshed Guitar", Output level back where you dragged it, the window still open in the same place. Play: the change is heard. | not run |
| 6 | Close the window with its close box. Edit > Undo. | The source reads "(default for guitar tracks)"; no window opens by itself. | not run |
| 7 | Settings > Sound: Default instruments, Guitar tracks **Test Tone**; OK. File > New. Pick the source button and press **Edit...**. | TuxGuitar's **Parameters** window opens (not a plugin window), one row: **Level** at 1.0. The source button still reads "(default for guitar tracks)"; the song is not marked as changed. | not run |
| 8 | Close the Parameters window without touching it. | Nothing changes: still "(default for guitar tracks)", the song not marked as changed, nothing new to undo. | not run |
| 9 | Press **Edit...** again, type 1.0 in Level's box and press **Set**. | Nothing changes (1.0 is what it has): still "(default for guitar tracks)", not marked as changed. | not run |
| 10 | Drag the Level slider to about the middle and let go. | The source button reads "Source: Test Tone" (kept in the song); the song is marked as changed; the Parameters window stays open. Play: the notes are quieter. | not run |
| 11 | Drag Level again, then Edit > Undo once. | The first undo takes back only the second move; the source still reads "Source: Test Tone". | not run |
| 12 | Edit > Undo once more. Then move Level in the still open window. | The source reads "(default for guitar tracks)" again and the Parameters window stays open, showing Level 1.0. Moving Level keeps the default in the song again ("Source: Test Tone"). | not run |
| 13 | Settings > Sound: Guitar tracks back to **Woodshed Guitar**; OK. | Tracks that still follow the default play Woodshed Guitar again; a track kept with Test Tone keeps it. | not run |

## Notes

- What the tests cover: TestDefaultSource (the synthesizer hands the same processor back to the default on Undo),
  TestVst3Processors (Woodshed Guitar back to how it opened, its profile loaded again), TestDefaultInstrumentEditor and
  default_instruments.feature (the editor stays open in the same window, its next change keeps the default again,
  Redo; Test Tone's Parameters window, its first value keeping the default, a value it already has keeping nothing);
  this page checks what is seen and heard in the running app.
- After Undo Woodshed Guitar loads the profile chosen in Settings again, in the background: while the song plays, the
  old tone may be heard for a moment before the short crossfade.
- Pinned: Set with the value a song's own plugin already has still makes an undo step that changes nothing (as before);
  only a default instrument's Parameters window skips it.
