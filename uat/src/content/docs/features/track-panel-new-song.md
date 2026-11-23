---
title: Track panel after opening a song
description: Opening a song whose track plays a MIDI instrument (e.g. Flute) shows no "is not a VST3 plugin" error.
sidebar:
  order: 45
---

## What it tests

Branch `fix/panel-editor-new-song`, commit `fe44ac320`. Opening a file while the Track panel showed a plugin's editor
popped two error messages, "Flute is not a VST3 plugin: it has no editor to open". Opening the song stopped the old
song's plugins, which closed the editor. The panel then tried to open that editor again, but on the new song's
track, which plays Flute through General MIDI and has no editor. Now the panel opens an editor only for a plugin the
track it shows still has. A Flute track shows no editor and no error. A track that plays the same plugin as before
shows its editor again.

## Setup

Close any running TuxGuitar first. Check out the commit above, then build and launch with the VS Code task
**app: build + run**.

- Settings > Sound: MIDI Port **TuxGuitar Synthesizer**. Default instruments: Guitar tracks **Woodshed Guitar**.
  Other tracks: no default (General MIDI).
- Make a Flute song: File > New, open the track's settings (Track > Properties, or the instrument box), set the
  instrument to **Flute**, enter a note, and save it as `flute.tg`. Do the same with a guitar instrument (e.g.
  "Distortion Guitar") and save it as `guitar.tg`.

## Steps

| #   | Do | Expect | Result |
| --- | -- | ------ | ------ |
| 1   | Open `guitar.tg`. Show the Track panel (F8) and click the **Source** chip. | Woodshed Guitar's editor shows in the panel, or the panel says it is in a window of its own. | not run |
| 2   | File > Open `flute.tg`. | No error message appears. The panel shows the Flute track, its source General MIDI "Flute", and no editor. Woodshed Guitar's editor window, if one was open, closes. | not run |
| 3   | Click the **Source** chip of the Flute track, then **Hide editor** / **Show editor**. | Nothing opens and no error message appears. | not run |
| 4   | File > Open `guitar.tg` again. | No error message. Woodshed Guitar's editor shows again, where it was in step 1. | not run |
| 5   | Hide the Track panel (F8). Open `flute.tg`, then show the panel again (F8). | No error message. The panel shows the Flute track with no editor. | not run |
| 6   | Play `flute.tg` (Space). | The flute is heard (General MIDI). No error message appears. | not run |

## Notes

- Asking directly for the editor of a General MIDI source is still refused with that message. The Sound Chain
  dialog's **Edit** is greyed for it, so a person cannot reach it there. `hosting_vst3_plugins.feature` covers it.
- The two scenarios of `plugins/track_panel_editor.feature` cover the panel's reaction to the song opened. This page
  checks what is seen in the running app.
