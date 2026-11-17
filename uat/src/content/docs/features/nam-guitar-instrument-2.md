---
title: Woodshed Guitar, slice 2 (the song's own copy, stored once, loading in the background)
description: A reopened song plays the capture it was saved with, tracks share one stored capture, and a capture loads without stopping the music.
sidebar:
  order: 14
---

## What it tests

Commit `e534365ea` on `feature/nam-guitar-instrument`. Three changes to Woodshed Guitar (see the page **Woodshed Guitar,
the NAM guitar instrument** for the instrument itself):

- A reopened song always plays the capture it was saved with, even when the `.nam` file on disk has changed since; the
  editor then says so and offers **Update from file**.
- Tracks playing the same capture store it once in the song, so the `.tg` file grows by one capture, not one per track.
- A capture loads in the background: the music keeps playing on the old tone, then the new tone takes over with a short
  (30 ms) crossfade, with no dropout or click.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `e534365ea` or
later on the branch.

- In Settings > Sound, choose the TuxGuitar Sequencer and the TuxGuitar Synthesizer, and an audio device.
- Have three `.nam` captures of your own in a folder you can change: **Clean**, **Lead** (a high-gain one), and a
  large A1 capture if you have one (**Big**). Keep spare copies elsewhere: step 2 overwrites one.
- File > New. Add a second guitar track (Track > Add Track). Enter a few bars of single notes and chords on both tracks,
  and choose Woodshed Guitar as the instrument of both (Sound Chain).

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | On track 1, Load profile... **Lead**. Save the song as `slice2.tg`, close it. | The song saves. | not run |
| 2 | In Explorer, copy **Clean.nam** over **Lead.nam** (same name, other contents). Open `slice2.tg` and press Play. | Track 1 still sounds like **Lead** (the song's own copy), not like Clean. | not run |
| 3 | Open track 1's Woodshed Guitar editor. | Below "Profile: Lead.nam" it says "The profile file has changed since this song was saved", with an **Update from file** button next to it. | not run |
| 4 | Click **Update from file**, then press Play. | The editor shows "Loading Lead.nam..." for a moment, then the line and the button go away; track 1 now sounds like Clean (the file as it is now). The song shows as modified. | not run |
| 5 | Edit > Undo, then press Play. | Track 1 sounds like the old Lead again, and the editor says again that the file has changed, with **Update from file**. | not run |
| 6 | Put the original **Lead.nam** back. Close and reopen `slice2.tg`, open the editor. | No line about the file and no **Update from file** button: the file is what the song holds. | not run |
| 7 | Note the size of `slice2.tg` in Explorer. On track 2, Load profile... **Lead** too, and save. | The file grows by about the size of one capture or less (Lead is stored once for both tracks), not by another full copy. | not run |
| 8 | Close, move **Lead.nam** out of its folder, reopen `slice2.tg` and press Play. | Both tracks sound like Lead. | not run |
| 9 | Press Play and, while it plays, Load profile... **Clean** on track 1 (then **Big**, if you have one). | The music never stops or stutters; the old tone plays on while the editor shows "Loading ...", then the new tone takes over smoothly, without a click. | not run |
| 10 | While it plays, Edit > Undo the load of step 9. | The previous tone comes back the same way: no gap, no click. | not run |
| 11 | With the audio device at 44.1 kHz, load a capture made at 48 kHz on track 1 while the song plays, with track 2 playing the same notes. | After the change both tracks stay together in time (no flam, no echo between them). | not run |

## Notes

- The tests already cover: the song's own copy playing when the file changed, the notice and **Update from file**
  (including its undo), one stored copy for tracks sharing a capture, songs saved before this slice still opening, the
  crossfade's length and the absence of gaps, the editor answering while a capture loads, and the latency of a 48 kHz
  capture loaded while playing. This page checks what a person hears and sees with real captures.
- Live, a song's first notes right after it opens may sound dry for a moment while its captures load (the plugin never
  makes playback wait). Exports wait for them, so an export always plays the capture from its first note.
- Known, pinned, not part of this slice: a parameter that TuxGuitar sets on a plugin kept open while nothing plays it
  is left out of the saved song.
