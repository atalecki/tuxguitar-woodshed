---
title: Sound Chain and the default instrument, slice 2f
description: The Sound Chain dialog says what a track without an instrument of its own plays and why, offers "Default instrument" instead of General MIDI, and "Keep in song" makes the default the track's own.
sidebar:
  order: 25
---

## What it tests

Commits `68d7c2997` and `e895c86ac` on `feature/default-instrument`. In Tools > Sound Chain, the source row of a track
without an instrument of its own now says what plays and why (for example **Source: Woodshed Guitar (default for guitar
tracks)**, or **Source: General MIDI, Acoustic Grand Piano (no default for other tracks)**). The **General MIDI source**
button is now **Default instrument**. A new **Keep in song** button makes the default instrument that plays the track's
own instrument, its state (and Woodshed Guitar's profile) stored in the song: the track no longer follows Settings, and
sounds the same on another computer. Keep in song is undone with Edit > Undo.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `e895c86ac` or
later on the branch.

- Have a `.nam` capture of your own at hand that sounds clearly different from a clean amp (say **Lead.nam**).
- Start TuxGuitar with the TuxGuitar Synthesizer as the MIDI Port and an audio device. In Settings > Sound, Default
  instruments: Guitar tracks **Woodshed Guitar** with the Guitar profile **Lead.nam**, Other tracks **General MIDI**; OK.
- File > New. Enter a few notes on track 1 (guitar). Add a second track set to **Acoustic Grand Piano**, with a few
  notes.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Select track 1, open Tools > Sound Chain. | The first row reads **Source: Woodshed Guitar (default for guitar tracks)**. Next to **Default instrument** a **Keep in song** button is enabled. There is no "General MIDI source" button any more. | not run |
| 2 | Close it, select track 2 (piano) and open Tools > Sound Chain. | **Source: General MIDI, Acoustic Grand Piano (no default for other tracks)**; **Keep in song** is greyed (nothing to keep). | not run |
| 3 | Choose a VST3 instrument for track 2 with **Use as source**. | The row reads just the instrument's name (as before); **Keep in song** is greyed. Press **Default instrument**: the row is back to the General MIDI one. | not run |
| 4 | Back on track 1's Sound Chain, press **Keep in song** and wait a moment. | The row now reads **Source: Woodshed Guitar** (the track's own instrument), **Keep in song** greys out, and the song is marked modified. Play: track 1 still sounds through Lead. | not run |
| 5 | Open Settings > Sound and set the Guitar profile back with **Shipped profile**, OK. Play track 1. | Track 1 still sounds through Lead: a kept instrument no longer follows Settings. (A new guitar track would play the clean amp.) | not run |
| 6 | Save the song. Close TuxGuitar, rename **Lead.nam** to **Lead.nam.off**, start TuxGuitar, open the song and play track 1. | Still Lead: the profile is in the song. | not run |
| 7 | Edit > Undo (with a fresh Keep in song first if you reopened the song: press Keep in song, then Undo). | The row reads **Source: Woodshed Guitar (default for guitar tracks)** again and the track follows Settings again (plays the clean amp after step 5). | not run |
| 8 | With TuxGuitar running, rename `lib/tuxguitar-vst3-host.exe` in the install folder to `tuxguitar-vst3-host.exe.off` (Windows lets a running program be renamed). File > New, enter notes on track 1, Play, then open its Sound Chain. | The row reads **Source: General MIDI, Steel String Acoustic Guitar (Woodshed Guitar could not be loaded)** and **Keep in song** is greyed. | not run |
| 9 | Clean up: rename the host and Lead.nam back. | (Clean-up.) | not run |

## Notes

- The tests cover each source row (default plays, no default, not installed, could not be loaded, not scanned yet, an
  instrument of the track's own, made-up German), Keep in song with the profile file moved away and the defaults
  changed afterwards, its undo, and its refusals (no default plays; an instrument of the track's own; a plugin that does
  not open, saying why). Check what is seen and heard.
- "Default instrument" runs the same action as "General MIDI source" did (its shortcut, if any, is unchanged): the song
  still stores it as General MIDI, which another computer plays as its own default.
