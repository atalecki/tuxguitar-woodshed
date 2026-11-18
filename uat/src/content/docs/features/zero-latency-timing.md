---
title: Timing without plugin delay compensation
description: With no plugin delay (the usual case now that the built-in click adds none), an audio track's take stops exactly where the song stops, and the player stops right after the end of a song or a timed practice session is heard.
sidebar:
  order: 18
---

## What it tests

Commits `ad6c8cf29` (the take stops where the song stops) and `21f03ea45` (the player stops where the end is heard) on
`fix/zero-latency-timing`. Since the built-in click, most songs have no plugin delay compensation, which showed two
timing faults:

1. With Play looped on, an audio track's take sounded on past the end of the last pass, to the end of the block of
   audio the song stopped in (up to about 12 ms): the first click of the next pass could be heard. Now the take stops
   on the very sample the song stops on, as the notes do; with plugin delay, as late as every other track.
2. The player stopped a block of audio (about 12 ms) late when the song's or a practice session's end was heard less
   than a MIDI tick before the end of a block, as a timed Interleaved Practice session's end was with no plugin delay.
   Now it stops right after the end is heard, whatever the delay.

## Setup

Build and launch with the VS Code task **app: build + run** at commit `21f03ea45` or later. Use headphones.

- Settings > Sound: MIDI Port "TuxGuitar Synthesizer", the TuxGuitar Sequencer, and an audio device in Tools > Audio
  Device....
- **Song A**: File > New, one guitar track, 4 measures of 4/4 at 120 bpm, a quarter note on every beat. Add an audio
  track (Track > Add Audio Track) and record or import a take on it that sounds a click or a sharp sound on every beat
  from measure 1 to the end of measure 4 (a metronome recording is ideal). Save it as `timing-a.tg`.
- A VST3 effect that reports latency, if you have one (for steps 3 and 6); any plugin with a "lookahead" setting does.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Open `timing-a.tg`. Select measures 2 to 3 and turn Play looped on (Loop Range measures 2 to 3). Play from measure 2, and after two passes turn Play looped off during the third. | The song stops at the end of measure 3. The take's last click is beat 4 of measure 3: no click of measure 2's beat 1 follows it, not even a short one. | not run |
| 2 | Repeat step 1 a few times. | Every time the take stops with the notes, cleanly; never a short extra click after the last beat. | not run |
| 3 | If you have a latency plugin: add it to track 1's sound chain and repeat step 1. | The take and the notes stop together, both a little later; the take's last click is not cut short. | not run |
| 4 | Remove any latency plugin. Transport > Interleaved Practice...: measures over measures 1 to 4, Interval clock on, 1 minute per turn, 10 seconds' break, session length 2 minutes, Start. | Turn 1 plays for about a minute, a 10-second break, then turn 2. | not run |
| 5 | Listen to the end of turn 2. | The last note sounds in full and the player stops right after it: the play button returns to Play at once, without cutting the last note and without a lag. | not run |
| 6 | If you have a latency plugin: add it to track 1's sound chain and repeat steps 4 and 5. | The same: the last note is not cut short, and the player stops right after it is heard. | not run |
| 7 | Pause during turn 1's break, wait a few seconds, then Play. | Turn 2's Count In starts at once; at turn 2's end the player stops right after the last note, as in step 5. | not run |

## Notes

- The tests already cover, sample for sample: the take falling silent on the sample the last pass ends on (and nothing
  after it) with no plugin delay and with 600 and 1000 samples of it; the timed session's stop on the first block of
  audio after the last turn's end is heard, with no plugin delay and with 600 and 2048 samples of it, paused in a break;
  a monitored input sounding on where the song stops. What they cannot check is what you hear on a real audio device.
- The player stops at the end of a block of audio (about 12 ms at 44.1 kHz): "right after" means within that.
