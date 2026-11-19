---
title: Track panel, sound per track (MIDI or VST3)
description: The caret's track's sound profile docked at the right of the score, with a per-track switch between its General MIDI program and a VST3 instrument.
sidebar:
  order: 43
---

## What it tests

Branch `feature/track-sound-panel`, commit `036770c5f` (the switch fix; the Track panel came in `d4a87a4ea`; the
sound model in `be011b50f` and `9e8d8ea7a`). The Sound Chain panel moves from the foot of the window to a Track panel
docked at the right of the score, like Guitar Pro 8's Track inspector: the track's colour and name, then SOUNDS with
a **MIDI** / **VST3** switch per track, the instrument, the sound chain top to bottom with its meters, and its
actions. MIDI plays the track's General MIDI program inside TuxGuitar; VST3 plays the track's VST3 instrument or the
default instrument of its kind. Switching to MIDI and back restores the same VST3 instrument with its settings. The
picked plugin's editor stays at the foot of the window.

## Setup

Close any running TuxGuitar first. Check out the commit above, then build and launch with the VS Code task
**app: build + run** (Windows: it builds `desktop\build-scripts\tuxguitar-windows-swt-x86_64` and starts the app).

- Settings > Sound: MIDI Port **TuxGuitar Synthesizer**, an audio device that plays. Default instruments: Guitar
  tracks **Woodshed Guitar** (the shipped profile).
- Tools > Plugins: "TuxGuitar Sound Chain panel" enabled; View > Show Sound Chain on (F8).
- A VST3 effect of your own installed in a VST3 folder TuxGuitar scans (any effect; one whose name is long helps
  step 15).

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | File > New. Look at the right of the window. | A panel about 300 px wide stands right of the score, a thin divider at its left edge. Top to bottom: a coloured square (the track's colour) and a field with the track's name; **SOUNDS**; two buttons **MIDI** and **VST3**, VST3 pressed; "Instrument:" with "Default instrument (Woodshed Guitar)" and **Keep in song**; the chain: "Source: Woodshed Guitar (default for guitar tracks)" with a meter under it, an arrow ↓, "Track out" with its meter, ↓ "Master" with its meter; **Hide editor**, **Disable**, **Up**, **Remove**; "VST3 effect:" with a drop-down and **Add**; **Sound Chain...**. Woodshed Guitar's own editor shows at the foot of the window, above the status line. There is no horizontal Sound Chain line at the foot any more. | not run |
| 2 | Drag the divider at the panel's left edge to the left, then far to the right. | The panel widens and narrows, the score takes the rest. It stops growing at about 600 px and shrinking at about 220 px. | not run |
| 3 | Close TuxGuitar and start it again. | The panel comes back at the width you left it. | not run |
| 4 | Press F8, then F8 again. | The panel, its divider and the editor at the foot all go: the score fills the window, no strip left at the right. Then all of them come back. | not run |
| 5 | Enter a few notes on track 1 and press Play. | You hear Woodshed Guitar. The meters under the source, Track out and Master move. | not run |
| 6 | Press **MIDI**. | MIDI is pressed, VST3 not. The row now reads "Program:" with the track's General MIDI program (a new guitar track: the steel-string acoustic guitar); Keep in song is gone. The source reads "Source: " and the program's name, without "(default for guitar tracks)". The editor at the foot disappears (General MIDI has none). | not run |
| 7 | Press Play. | You hear the General MIDI acoustic guitar of TuxGuitar's synthesizer, not Woodshed Guitar. | not run |
| 8 | Pick "Distortion Guitar" in the Program drop-down. Press Play. | You hear the General MIDI distortion guitar. The source row names it. | not run |
| 9 | Edit > Undo twice. Then Edit > Redo twice. | The first Undo brings back the acoustic guitar program; the second goes back to VST3 (VST3 pressed, "Default instrument (Woodshed Guitar)"). Redo goes to MIDI, then to Distortion Guitar again. | not run |
| 10 | Press **VST3**. In the Instrument drop-down pick **Woodshed Guitar** (the plain entry, not "Default instrument..."). In its editor at the foot, move Output level well down. | The source reads "Source: Woodshed Guitar" (the track's own now). The sound gets quieter. | not run |
| 11 | Press **MIDI**, play, then press **VST3** and play again. | On MIDI you hear General MIDI; back on VST3 it is Woodshed Guitar again, Output level where you left it (still quieter), its editor at the foot shows that setting. | not run |
| 12 | In "VST3 effect:" pick your effect and press **Add**. Play on VST3, press MIDI and play again. | The effect appears in the chain under the source, an arrow ↓ before it, with its own meter. You hear it on VST3 and, after the switch, on MIDI too; the effect stays in the chain in both. | not run |
| 13 | Pick the effect in the chain. Try **Disable**/**Enable**, **Up**, **Hide editor**/**Show editor**, then **Remove**. | Disable greys its meter and you no longer hear it; Enable brings it back. Up is greyed (it is the first effect). Hide editor / Show editor hides and shows its editor at the foot. Remove takes it out of the chain. | not run |
| 14 | In the header, type a new name in the name field and press Enter. Click the coloured square and choose another colour. Then Edit > Undo twice. | The track's name changes in the track list, its colour changes on the square and in the track list. Each Undo takes back one of them. | not run |
| 15 | Add an effect whose name is long (or narrow the panel to its minimum). Hover over its entry in the chain. | A name too long for the panel ends in "…"; the tooltip shows the whole name. | not run |
| 16 | Set track 1 to MIDI, File > Save As a .tg file, File > Close, then open the file again. Press **VST3**. | The track opens on MIDI with its program. VST3 brings back the VST3 instrument it had, with its settings. | not run |
| 17 | Track > Add Track, then make its channel a percussion channel (Track > Properties, or the channel settings). Move the caret to it and press **MIDI**. | The Program drop-down lists numbered programs ("Program #0" to "Program #127"), as the channel settings do for percussion. | not run |
| 18 | Add an audio track (Track > Add audio track) and move the caret to it. | No MIDI / VST3 switch and no instrument row; the chain reads "Source: takes". | not run |
| 19 | Settings > Sound: pick another MIDI Port than the TuxGuitar Synthesizer. | Under **Sound Chain...** a quiet line says sound chains are heard only with the TuxGuitar Synthesizer. Set it back. | not run |
| 20 | Settings > Language: pick another language, then back to English. | No label is blank or shows a raw key; the new texts stay in English where the language has no translation yet. | not run |
| 21 | Settings > Sound, Default instruments: set Other tracks to none (General MIDI). Track > Add Track, set its instrument to a piano (Track > Properties, or the channel settings), move the caret to it and press **VST3**. Pick **Woodshed Guitar** in the Instrument drop-down. Press Play and keep it playing; while it plays, pick "Default instrument (General MIDI)", then **Woodshed Guitar** again. | Each pick is heard at once: the General MIDI piano right after the first pick, Woodshed Guitar right after the second. The track never goes silent. | not run |

## Notes

- The tests already check that MIDI plays General MIDI and not the default instrument (offline render and live), that
  the switch back restores the same VST3 instrument with its state, that effects apply in both, undo/redo, save and
  reopen, and audio tracks. Here the point is what you see and hear in the app.
- An older TuxGuitar opening a song saved with a track on MIDI plays that track's default instrument (or General MIDI
  without one): it does not know the MIDI setting. Do not report it.
