---
title: A damaged take no longer keeps a song from opening
description: A song whose take cannot be read opens without it, with a warning saying so and why; saving it keeps the damaged take's data as it was.
sidebar:
  order: 19
---

## What it tests

Commits on `fix/damaged-take-opens`:

- `a36dbec47` (the refusal pinned before it, in `3bfaa5e3e`): a song whose take cannot be read opens without that take.
  The track plays silence where the take played; every other track and take is as it was. Before, the whole song was
  refused with "A take in this song could not be read: Not a wave file".
- `a6e7593e1`: once the song is open, a warning dialog reads "A take in this song could not be read and was left out:
  Not a wave file", or "2 takes in this song could not be read and were left out: Not a wave file" for several (one
  dialog, each reason once).
- Saving the song writes the damaged take's data back into the file exactly as it was, so nothing of the file is lost.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `a6e7593e1` or
later. Settings > Language: English. An audio device chosen in Tools > Audio Device..., Settings > Sound: MIDI Port
"TuxGuitar Synthesizer". You need an archive tool that can replace a file inside a zip (such as 7-Zip) and a short text
file (any `.txt`).

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | New song. Enter a few notes in measures 1 and 2 of track 1. Add two audio tracks (Tools > Add Audio Track, twice). Arm track 2 only, record about 4 seconds of something you can recognise (Tools > Record, then Stop), disarm it. Arm track 3 only and record something else over the same measures, disarm it. File > Save As a TuxGuitar file, `damaged.tg`. Close the song. | Saved as usual. Play before closing: you hear track 1's notes and both takes. | not run |
| 2 | Copy `damaged.tg` to `damaged.zip` and open it in the archive tool. In its `takes` folder, replace `1.wav` (track 2's take) with your text file renamed `1.wav`. Save the archive and rename it back to `damaged.tg` (over the first). | The archive shows `version.txt`, `content.xml` and the folder `takes` with `1.wav` and `2.wav`. | not run |
| 3 | File > Open `damaged.tg`. | The song opens. A warning dialog titled "Warning" reads "A take in this song could not be read and was left out: Not a wave file", with no Java class names. Behind it the song is already shown with its three tracks. | not run |
| 4 | Close the dialog and play from the start. | Track 1's notes and track 3's take sound as recorded. Track 2 is silent where its take was; its measures still show that a take is there. | not run |
| 5 | File > Save As `damaged-saved.tg`. Copy it to a `.zip` and open it in the archive tool; open `takes/1.wav` in a text editor. | Saved with no error. `takes/1.wav` still holds your text file's words, unchanged; `takes/2.wav` is a wave file. | not run |
| 6 | File > Open `damaged-saved.tg`. | The same warning as in step 3; the song opens as in step 4. | not run |
| 7 | In `damaged.tg`, replace `takes/2.wav` too with your text file (as in step 2), then File > Open it. | One warning dialog: "2 takes in this song could not be read and were left out: Not a wave file". Tracks 2 and 3 are silent where their takes were; track 1 plays. | not run |
| 8 | Open a song with takes that were never damaged (or record a new one, save and reopen it). | It opens with no warning dialog, as before. | not run |
| 9 | Settings > Language: Deutsch (or any language other than English), OK, restart TuxGuitar if asked. Repeat step 3. | The warning reads in English exactly as in step 3 (no translation yet), never `audio.take.warning.left-out` nor `audio.take.refusal.not-wave`. | not run |
| 10 | Settings > Language back to the one you use. Delete `damaged.tg`, `damaged-saved.tg` and their `.zip` copies. | TuxGuitar reads as before. | not run |

## Notes

- The tests cover: the song opening with the warning, a MIDI track and another take still sounding while the damaged
  take's track is silent, one warning for two damaged takes, the warning in a made-up German, and saving again writing
  the damaged entry back byte for byte (a reader that can read it then finds the take where it was). What they cannot
  check is the dialog itself in the running app.
- The damaged take stays in the song as a silent take: it still counts as a take of its track (it keeps its number and
  can be chosen or deleted as any take). Deleting it is the way to drop its data from the file on the next save.
- A file whose zip itself is broken, or whose `content.xml` names a take with no entry at all, is still refused as
  before.
- Step 3 of **Texts through translations** ("A take that cannot be read") expected the old refusal; from this branch on,
  this page replaces it.
