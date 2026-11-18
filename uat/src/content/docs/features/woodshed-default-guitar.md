---
title: Woodshed Guitar as the default guitar and bass, slice 2d
description: Guitar and bass tracks without an instrument of their own play Woodshed Guitar through the profile from the settings, or the clean amp TuxGuitar ships.
sidebar:
  order: 22
---

## What it tests

Commit `ddc58f81c` on `feature/default-instrument`. A guitar or bass track without an instrument of its own now plays
**Woodshed Guitar** (TuxGuitar's own instrument) instead of General MIDI, through a profile: the one chosen in the
settings for guitar (bass uses the guitar one until a bass profile is chosen), else **RiffSmith Clean**, an A2 capture of
a clean amp and cabinet that TuxGuitar now ships. Play waits while the profile loads, so the first note already sounds
through the amp. A chosen profile whose file is missing leaves Woodshed Guitar's dry voice, and the status line says so.
The song itself does not change. There is no Settings page yet (slice 2e), so the profiles are set in the configuration
file by hand.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `ddc58f81c` or
later on the branch, then close TuxGuitar again. The build copies the shipped profile to `profiles` in the install
folder `desktop/build-scripts/tuxguitar-windows-swt-x86_64/target/tuxguitar-9.99-SNAPSHOT-windows-swt-x86_64`.

- Open `%APPDATA%\tuxguitar\config\plugins\tuxguitar-audio.cfg` and remove every line starting with
  `audio.default-instrument.` (left over from the earlier pages), so the defaults are TuxGuitar's own.
- Have a `.nam` capture of your own at hand that sounds clearly different from a clean amp (a high-gain one, say
  **Lead.nam**), and a second one (**Bass.nam**, any capture). Note their full paths.
- Start TuxGuitar. In Settings > Sound, choose the TuxGuitar Sequencer and the TuxGuitar Synthesizer, and an audio
  device. Wait a few seconds for the VST3 scan.
- File > New. Enter a few single notes and a chord on track 1 (a guitar track). Add a second track, set its
  instrument to **Fingered Electric Bass** in its properties, and enter a few low notes on it.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Look in the install folder. | A `profiles` folder next to `vst3` holds `RiffSmith Clean.nam` and `RiffSmith-LICENSE.txt`; `WOODSHED-GUITAR-NOTICE.txt` names RiffSmith Clean by Matt Hartman under the MIT License. | not run |
| 2 | Solo track 1 and press Play. | The guitar sounds as Woodshed Guitar through a clean amp (a full, rounded tone, not General MIDI's steel-string guitar and not the thin dry pluck). The status line says nothing. | not run |
| 3 | Solo track 2 (the bass) and press Play. | The bass notes sound through the same clean amp. | not run |
| 4 | Close TuxGuitar. Add the line `audio.default-instrument.guitar.profile=<full path of Lead.nam>` to `tuxguitar-audio.cfg` (forward slashes or doubled backslashes). Start TuxGuitar, rebuild the song as in setup, and play each track. | Both tracks now sound through Lead (the bass uses the guitar profile until one is chosen for it). | not run |
| 5 | Close TuxGuitar, add `audio.default-instrument.bass.profile=<full path of Bass.nam>`, start it again and play each track. | Track 1 still sounds through Lead; track 2 now through Bass. | not run |
| 6 | Press Play, Stop, Play again a few times. | Each Play starts on its first note through the amp; no note is heard dry first, and no "is still loading" hint appears. | not run |
| 7 | Save the song as a `.tg` file. Play it, then look at the title bar and File menu. | Playing did not mark the song as modified (no unsaved-changes prompt on close). Tools > Sound Chain for track 1 still reads its General MIDI program. | not run |
| 8 | Close TuxGuitar. Rename `Lead.nam` to `Lead.nam.off`. Start TuxGuitar, open the saved song and press Play. | Track 1 sounds as Woodshed Guitar's dry plucked string (no amp, not General MIDI). The status line shows **Lead.nam was not found: guitar tracks play Woodshed Guitar without an amp.** for about 5 seconds, once for the song. | not run |
| 9 | Close TuxGuitar, rename the file back, and set `audio.default-instrument.guitar.class=` (empty) in the configuration. Start TuxGuitar, open the song and press Play. | Track 1 plays General MIDI's guitar: General MIDI chosen for guitar tracks is kept. Track 2 (bass) still plays Woodshed Guitar through Bass. | not run |
| 10 | Clean up: remove the `audio.default-instrument.` lines again. | (Clean-up.) | not run |

## Notes

- **CPU (a quick measurement, not a gate).** Playing RiffSmith Clean costs one Woodshed Guitar instance about 5.2% of
  one core at full Quality and 2.8% at Light (`woodshed-guitar-time`, 44.1 kHz, blocks of 512, a six-string chord each
  second, 20 s of audio). Ten instances running at once took about 6% of one core each (about 60% of one core in all)
  on this 20-thread machine. Each channel without an instrument of its own runs its own instance in its own process.
- The tests cover the profile chosen per kind, the bass using the guitar one, the shipped profile playing exactly as
  the file loaded by hand, the missing profile and its hint (also in made-up German), Play waiting for a 40 MB
  profile live, and the song staying unmodified. Check what is heard and seen.
- A profile set in the configuration is read when TuxGuitar starts; slice 2e applies a change from Settings at once.
