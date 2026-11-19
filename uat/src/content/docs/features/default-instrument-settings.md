---
title: Default instruments in Settings, slice 2e
description: Settings > Sound chooses the default instrument of each kind of track and the guitar and bass profiles, applied to the open song on OK.
sidebar:
  order: 24
---

## What it tests

Commit `5f37c062d` on `feature/default-instrument`. Settings > Sound has a new group, **Default instruments (TuxGuitar
Synthesizer)**: for guitar, bass, drum and other tracks a drop-down of General MIDI and the VST3 instruments the scan
found, and for guitar and bass the profile Woodshed Guitar plays (the one TuxGuitar ships until a `.nam` file is
chosen, **Shipped profile** going back to it). OK applies a change to the open song at once; playing stops if it was
playing, as when the MIDI Port changes. No more editing `tuxguitar-audio.cfg` by hand.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `5f37c062d` or later
on the branch.

- Open `%APPDATA%\tuxguitar\config\plugins\tuxguitar-audio.cfg` (with TuxGuitar closed) and remove every line starting
  with `audio.default-instrument.`, so the defaults are TuxGuitar's own.
- Have a `.nam` capture of your own at hand that sounds clearly different from a clean amp (say **Lead.nam**), and,
  optionally, a VST3 instrument installed on the system (a piano, say). Without one, only General MIDI and Woodshed
  Guitar are offered.
- Start TuxGuitar. In Settings > Sound choose the TuxGuitar Sequencer and the TuxGuitar Synthesizer and an audio device,
  OK. File > New; enter a few notes on track 1 (guitar). Add a second track set to **Fingered Electric Bass** and a
  third set to **Acoustic Grand Piano**, each with a few notes.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Open Settings > Sound. | Under Count In / Metronome click, a group **Default instruments (TuxGuitar Synthesizer)** with four rows: Guitar tracks and Bass tracks show **Woodshed Guitar**, Drum tracks and Other tracks show **General MIDI**. Under Guitar and under Bass a **Profile** row reads **RiffSmith Clean (shipped with TuxGuitar)** and **RiffSmith Bass Clean (shipped with TuxGuitar)**; their **Shipped profile** buttons are greyed (the shipped one is already chosen). | not run |
| 2 | Open the Other tracks drop-down. | It offers General MIDI, Woodshed Guitar and every VST3 instrument installed; no effect plugin (no TONE3000, no gain) is offered. | not run |
| 3 | Press **Choose...** on the Guitar profile row. | A file dialog opens offering only NAM profiles (`.nam`). Cancel it: the row still reads RiffSmith Clean. | not run |
| 4 | Press **Choose...** again and pick **Lead.nam**. | The row shows the full path of Lead.nam and **Shipped profile** is no longer greyed. | not run |
| 5 | Set Other tracks to **Woodshed Guitar** (or your piano), press OK. Without pressing Play, enter a note on track 3 (the piano track). | The note sounds as the instrument just chosen, not General MIDI's piano: OK applied it at once. | not run |
| 6 | Play track 1 (soloed). | The guitar sounds through Lead. | not run |
| 7 | Start playing the song, and while it plays open Settings > Sound, set the Guitar profile back with **Shipped profile**, OK. | Playing stops (as when the MIDI Port changes). Press Play: the guitar sounds through the clean amp again. | not run |
| 8 | While the song plays, open Settings > Sound and press OK without changing anything. | Playing goes on: nothing changed, nothing applied. | not run |
| 9 | Open Settings > Sound and set Guitar tracks to **General MIDI**. | The Guitar profile row's **Choose...** and **Shipped profile** buttons grey out (a profile is Woodshed Guitar's). OK, then play track 1: General MIDI's steel-string guitar. | not run |
| 10 | Set Bass tracks to **General MIDI** and Drum tracks to your VST3 instrument (if any), OK; close TuxGuitar and start it again. Open Settings > Sound. | The choices are as you left them (kept in `tuxguitar-audio.cfg`). | not run |
| 11 | Close TuxGuitar. In `tuxguitar-audio.cfg`, set `audio.default-instrument.other.class=0123456789ABCDEF0123456789ABCDEF` and `audio.default-instrument.other.name=Missing Piano`. Start TuxGuitar, open Settings > Sound. | Other tracks shows **Missing Piano (not installed)**: a default set on another computer is not lost by opening the settings. | not run |
| 12 | Press **Defaults** in Settings. | Guitar and bass go back to Woodshed Guitar with the shipped profiles, drums and other to General MIDI, applied at once. | not run |

## Notes

- The tests cover what is offered (General MIDI, the scanned instruments, a default not installed), choosing per kind,
  the profile only for Woodshed Guitar on guitar and bass, the shipped profile until a file is chosen, defaults equal or
  not (nothing applied when they are the same), and applying at once (playing stops with the TuxGuitar Synthesizer,
  nothing happens with another MIDI Port). The dialog itself is checked here.
- The group is shown whatever the MIDI Port; its title says the defaults are the TuxGuitar Synthesizer's (other MIDI
  Ports play General MIDI).
- The texts go through TuxGuitar's translation files; other languages show English until translated.
