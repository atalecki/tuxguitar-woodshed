---
title: Woodshed Guitar, slice 3 (Play waits for a loading profile, the file notice)
description: Play holds until every track's capture has loaded, so the first note has the amp tone; the editor's notice about a changed file reads more plainly.
sidebar:
  order: 15
---

## What it tests

Commits `99d67f44b` (Play waits) and `e1bb34a57` (the notice) on `feature/nam-guitar-instrument`. Two follow-ups
to **Woodshed Guitar, slice 2**:

- When you press Play while a track's Woodshed Guitar is still loading its capture (a song just opened, a capture just
  loaded), playback holds until every capture is ready, so the very first note already has the amp tone. If a capture
  takes longer than 5 seconds, Play starts anyway (those notes sound dry until it is ready).
- The editor's notice about a changed `.nam` file now reads "The profile file has changed since it was loaded."

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit
`e1bb34a57` or later on the branch.

- In Settings > Sound, choose the TuxGuitar Sequencer and the TuxGuitar Synthesizer, and an audio device.
- Have a large A1 `.nam` capture of your own (**Big**, ideally several MB), a high-gain one (**Lead**), and a spare copy
  of **Lead** elsewhere (step 5 overwrites it).
- File > New. Enter a chord on beat 1 of measure 1 and a few more bars. Choose Woodshed Guitar as the instrument of
  track 1 (Sound Chain) and Load profile... **Big**. Save the song as `slice3.tg`.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Close `slice3.tg`, open it again, and press Play at once. | Playback may start a moment later than usual, but the first chord already sounds through **Big** (the amp tone), not as the clean dry pluck. | not run |
| 2 | While the song is stopped, open the editor, Load profile... **Lead**, then press Play straight away. | The first note sounds through **Lead**; there is no dry note at the start. | not run |
| 3 | While Play waits (steps 1 and 2), move the mouse over the menus and the tablature. | TuxGuitar stays responsive: menus open, the window repaints, nothing freezes. | not run |
| 4 | With no Woodshed Guitar in the song (another instrument on every track), press Play. | Playback starts as quickly as before this change. | not run |
| 5 | Load profile... **Lead** on track 1 and save. Close the song. In Explorer, copy another capture over **Lead.nam** (same name, other contents). Open the song and open track 1's editor. | Below the profile's line it says exactly "The profile file has changed since it was loaded." with **Update from file** next to it. | not run |

## Notes

- The tests already cover: Play waiting for a capture that takes long to load when a song is played as soon as it is
  reopened (the first note has the tone, sample for sample), the 5-second limit, a plugin that never says it has
  finished, and that a background task of a plugin's editor which changes no sound is not waited for. This page checks
  what a person hears and feels: the moment Play waits, and that the window stays responsive.
- Only Woodshed Guitar (and other VST3 plugins that announce a background state restoration) make Play wait. On
  Windows the wait ends as soon as the capture is ready; Woodshed Guitar ships only for Windows.
- After step 5, put the original **Lead.nam** back.
