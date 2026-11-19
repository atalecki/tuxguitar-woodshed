---
title: Default instrument follow-ups, slice 2d+
description: A missing profile plays the shipped one, bass tracks get their own shipped profile, a default that failed is not retried, and the defaults apply once the startup scan is done.
sidebar:
  order: 23
---

## What it tests

Commits `9734ea9b5` to `2cb40515c` on `feature/default-instrument`. Four follow-ups to slice 2d:

- a chosen profile whose file is missing plays the profile TuxGuitar ships for that kind (no longer the dry voice), and
  the status line says the chosen file was not found;
- TuxGuitar ships a second profile, **RiffSmith Bass Clean**, and bass tracks start on it (no longer "same as guitar");
- a default instrument that could not be loaded is remembered: later Plays play General MIDI at once, until the
  defaults change, the VST3 folders are scanned again or the song is opened again;
- once the VST3 scan TuxGuitar starts with is done, the defaults are applied at once, so the first Play (and a note
  entered before it) already uses them.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `2cb40515c` or
later on the branch, then close TuxGuitar again. The install folder is
`desktop/build-scripts/tuxguitar-windows-swt-x86_64/target/tuxguitar-9.99-SNAPSHOT-windows-swt-x86_64`.

- Open `%APPDATA%\tuxguitar\config\plugins\tuxguitar-audio.cfg` and remove every line starting with
  `audio.default-instrument.`, so the defaults are TuxGuitar's own.
- Have a `.nam` capture of your own at hand that sounds clearly different from a clean amp (a high-gain one, say
  **Lead.nam**). Note its full path.
- Start TuxGuitar. In Settings > Sound, choose the TuxGuitar Sequencer and the TuxGuitar Synthesizer, and an audio
  device. Close TuxGuitar again (so the next start begins with those settings).

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Look in the install folder's `profiles` folder and open `WOODSHED-GUITAR-NOTICE.txt`. | `profiles` holds `RiffSmith Clean.nam`, `RiffSmith Bass Clean.nam` and `RiffSmith-LICENSE.txt`; the licence file and the notice name both captures by Matt Hartman under the MIT License. | not run |
| 2 | Start TuxGuitar. File > New. Wait a few seconds, then, without pressing Play, click a string on the fretboard (or enter a note with the keyboard) on track 1. | The note entered sounds as Woodshed Guitar through the clean amp, not General MIDI's steel-string guitar: the defaults were applied as soon as the scan was done. | not run |
| 3 | Enter a few notes on track 1. Add a second track, set its instrument to **Fingered Electric Bass** in its properties, and enter a few low notes. Play each track soloed. | Track 1 sounds through the clean guitar amp; track 2 through a clean bass amp (rounder and darker than the guitar's, and quieter: the bass capture is about 6 dB softer). | not run |
| 4 | Close TuxGuitar. Add `audio.default-instrument.guitar.profile=<full path of Lead.nam>` to `tuxguitar-audio.cfg`. Start TuxGuitar, rebuild the two tracks and play each. | Track 1 sounds through Lead; track 2 (bass) still through the clean bass amp: the guitar profile is not the bass one. | not run |
| 5 | Close TuxGuitar, rename `Lead.nam` to `Lead.nam.off`. Start TuxGuitar, rebuild track 1 and press Play. | Track 1 sounds through the clean guitar amp (never the dry plucked string). The status line under the fretboard shows **Lead.nam was not found: guitar tracks play RiffSmith Clean, the profile TuxGuitar ships.** for about 5 seconds. | not run |
| 6 | Close TuxGuitar, rename the file back and remove the guitar profile line. In the configuration set the bass profile to a file that does not exist: `audio.default-instrument.bass.profile=C:/nowhere/Gone.nam`. Start TuxGuitar, rebuild track 2 (bass) and play it. | Track 2 sounds through the clean bass amp; the status line says **Gone.nam was not found: bass tracks play RiffSmith Bass Clean, the profile TuxGuitar ships.** | not run |
| 7 | Close TuxGuitar and remove the bass profile line. Start TuxGuitar, File > New, enter notes on track 1. While TuxGuitar runs, rename `lib/tuxguitar-vst3-host.exe` in the install folder to `tuxguitar-vst3-host.exe.off` (Windows lets a running program be renamed), then File > New again, enter notes and press Play. | The guitar track plays General MIDI and the status line says **Woodshed Guitar could not be loaded: guitar tracks play General MIDI.** | not run |
| 8 | Rename the host back while TuxGuitar runs. Press Play again (twice). | Still General MIDI, at once, with no wait: the failed default is not tried again for this song. | not run |
| 9 | Open Tools > Sound Chain for the track and press **Scan again**; close the dialog and press Play. | Woodshed Guitar plays again: a new scan tries the default again. | not run |
| 10 | Repeat step 7 (host renamed, new song, Play: General MIDI), rename it back, then File > Open the song again (or File > New and enter notes), and press Play. | Woodshed Guitar plays: another song tries the default again. | not run |
| 11 | Clean up: rename the host back if needed, remove the `audio.default-instrument.` lines. | (Clean-up.) | not run |

## Notes

- The tests cover the shipped profile per kind playing exactly as the file loaded by hand, the bass profile being its
  own, the missing profile's hint (also in made-up German), a failed default not tried again and tried again after each
  of the three events, and the defaults applied when the startup scan is done (playing stops when they are applied with
  the TuxGuitar Synthesizer, nothing happens with another MIDI Port). Check what is heard and seen.
- Settings > Sound gets the default instruments in slice 2e; until then the configuration file is edited by hand.
