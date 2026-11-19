---
title: A VST3 instrument's note ends at Stop, Pause and a jump
description: A note a VST3 instrument holds when you press Stop or Pause, or when playback jumps (moving the caret while playing, a practice session playing a turn again), ends there instead of ringing on.
sidebar:
  order: 40
---

## What it tests

Commits `8650cdc4f`, `95691ce07` and `182427965` on branch `fix/vst3-all-notes-off`, after `2e813ceec`. TuxGuitar silences a channel with MIDI's All
Notes Off when playback stops or jumps. VST3 has no such message, so a VST3 instrument (Woodshed Guitar, a third-party
instrument) ignored it and the note it held rang on. Now TuxGuitar ends every note it started on that instrument, on the
sample the player stops or jumps at.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `182427965` or
later.

- MIDI Port: the TuxGuitar Synthesizer, with an audio device you can hear.
- A guitar track playing **Woodshed Guitar** (the default instrument for guitar tracks, or chosen in Sound Chain). A
  third-party VST3 instrument with a long sustain (a pad, an organ) shows it even more clearly; use it as well if you
  have one.
- Tempo 60 bpm. In measure 1, a **whole note** (fret 5 on string 1). Leave measures 2 and 3 empty, and put a quarter
  note on beat 1 of measure 4.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Put the caret on measure 1 and press Play. On beat 2, press Stop. | The note stops when you press Stop (Woodshed Guitar damps it at once, as a player muting the string). It does not ring on to its natural end. | not run |
| 2 | Press Play again from measure 1. On beat 2, press Pause. | The note stops at the Pause, as in step 1. | not run |
| 3 | Press Play from measure 1. On beat 2, click measure 4 in the score. | The note of measure 1 stops at the click; then measure 4's note plays. | not run |
| 4 | With a third-party sustaining VST3 instrument as the track's source (if you have one), repeat steps 1 to 3. | Each time the held note stops at once (or with that instrument's own short release), never sustaining on. | not run |
| 5 | Flag measure 1 as difficult and start Transport > Interleaved Practice with Count In on. During measure 1's whole note, press Pause, then Play. | The Count In before the turn plays again sounds its clicks alone: the paused note is not heard under them. | not run |
| 6 | Stop the session. Turn Play looped on over measure 1 and press Play from measure 1. Listen to three passes. | Each pass starts with a fresh note; the note of the pass before does not linger under it. | not run |

## Notes

- Before this change, in steps 1 to 5 the held note rang on until it died away by itself (with a sustaining instrument,
  until you played the same note again).
- Offline renders (File > Export audio, the offline practice render) also end a note that ends exactly where the song
  ends or a loop jumps back; that is covered by the tests (`TestGeneralMidiRender`), so it is not checked here.
- General MIDI programs (Gervill) already stopped at Stop and at a jump; they are unchanged.
