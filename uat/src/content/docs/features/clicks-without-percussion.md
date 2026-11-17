---
title: Count In and Metronome without a percussion track
description: A song with no percussion track counts in and clicks the Metronome on General MIDI channel 10 of any MIDI Port, without changing the song.
sidebar:
  order: 10
---

## What it tests

Commits `2d8041a2b` to `1d9d03a4c` on `feature/practice-session-rules` (`fbfa93364` is the change; the others are
its tests and the spec). A song with no percussion track used to count in and click the Metronome on nothing when it
played through a MIDI Port other than the TuxGuitar Synthesizer (Java Sound's Gervill, the Microsoft GS Wavetable
Synth, an external MIDI device, FluidSynth): Count In, the Metronome and an interleaved practice or click-up's Count In
were silent. On the TuxGuitar Synthesizer the Metronome already clicked,
but Count In (Transport > Count In, outside a practice session) was silent too.

Now, with no percussion track, the clicks go to General MIDI channel 10, the drum channel every General MIDI
synthesizer has, at a new channel's Volume (127) and Balance (64, centre). On the TuxGuitar Synthesizer they go to its
own General MIDI drums, as the Metronome did already. The song is not changed: no track or channel is added, nothing
can be undone, and the song is not marked modified. A song with a percussion track clicks on that track's channel, as
before.

## Setup

Build and launch with the VS Code task **app: build + run**, at the last commit above.

You need two songs, saved first so the title bar shows no modified mark:

- **Song A**, no percussion track: File > New, one guitar track, 4 measures of 4/4 at 120 bpm, a whole note C in each
  measure. Save it as `clicks-a.tg`.
- **Song B**, the same with a percussion track: Track > Add Track, and leave it empty. In View > Show Instruments, tick
  Percussion Channel on the new track's channel and set its Volume to about a quarter. Save it as `clicks-b.tg`.

The Click sound (Settings > Sound) is the default: Side Stick (note 37). In Settings > Sound, the MIDI Port list has
"Gervill" (Java Sound's synthesizer) and, on Windows, "Microsoft GS Wavetable Synth"; either is a MIDI Port other than
the TuxGuitar Synthesizer.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Settings > Sound: MIDI Port "Gervill", the TuxGuitar Sequencer. Open Song A. Turn Transport > Count In on, Transport > Metronome off. Press Play. | Four side-stick clicks a beat apart, then the guitar. Before this change: no clicks. | not run |
| 2 | Stop. Turn Count In off and Transport > Metronome on. Press Play. | A side-stick click on every beat while the guitar plays, as loud as a new track at full volume, in the centre. | not run |
| 3 | Stop. Look at the title bar, the track list and Edit > Undo. | The title shows no modified mark, the song still has its one track, and Undo has nothing new to undo. | not run |
| 4 | With the Metronome still on, Transport > Interleaved Practice..., each turn a measure, pool 1 to 4, 4 turns, Start. | Each turn has its Count In of four clicks, then its measure with the Metronome clicking. | not run |
| 5 | Settings > Sound: MIDI Port "Microsoft GS Wavetable Synth". Repeat steps 1 and 2. | The same clicks, on the Windows synthesizer's drums. | not run |
| 6 | Settings > Sound: MIDI Port "Gervill". Open Song B. Count In and Metronome on. Press Play. | The clicks are clearly softer than in steps 1 and 2: they play on the percussion track's channel, at its Volume. | not run |
| 7 | Stop. In View > Show Instruments, set the percussion channel's Volume to full. Press Play. | The clicks are as loud as in step 2. | not run |
| 8 | Settings > Sound: MIDI Port "TuxGuitar Synthesizer" (choose an audio device in Tools > Audio Device... if asked). Open Song A. Count In and Metronome on. Press Play. | Four Count In clicks, then a click on every beat, as loud as in step 2. Before this change the Count In was silent here; the Metronome sounded the same as now. | not run |
| 9 | Stop. Open Song B (percussion Volume at a quarter, as saved). Press Play. | The Count In and the Metronome are softer, at the percussion track's Volume, as before this change. | not run |
| 10 | Close Song A without saving. | TuxGuitar does not ask whether to save Song A: playing it did not change it. | not run |

## Notes

- The automated tests cover which channel the clicks go to on a port and on the TuxGuitar Synthesizer, with and without
  a percussion channel (lib's `TestMidiPlayerMetronome`, `TestMidiPlayerCountDown`, `TestMidiPlayerPractice`), and
  that a General MIDI port sends them on MIDI channel 9 (General MIDI channel 10) at bank 128, Volume 127, Balance 64
  (gm-utils' `TestClicksOnAPort`). What they cannot check is that you hear them.
- A song with no percussion track whose channel names MIDI channel 10 in its saved General MIDI channel settings (the
  Channel Settings dialog does not offer it, but a file can carry it) shares that channel with the clicks, and the
  clicks' drum kit takes it over. Not covered here.
