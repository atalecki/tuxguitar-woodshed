---
title: Built-in click for Count In and the Metronome
description: On the TuxGuitar Synthesizer, Count In and the Metronome sound a short click made in code instead of a General MIDI drum, at the percussion track's Volume and Balance, with an accent on beat 1.
sidebar:
  order: 11
---

## What it tests

Commits `4e98b272d` to `78e1d7b58` on `feature/builtin-click` (`51b2ae767`, `9ad052c96` and `2a45b9cf9` are the
change; the others are its spec, tests, a refactor and the settings label), and the accent on beat 1, `8ce6c57a6`.
On the TuxGuitar Synthesizer, Count In and the Metronome no longer play Gervill's General MIDI drum kit (a side stick by
default). They sound the **built-in click**: a short, bright blip (a 1500 Hz tone that dies away within 30 ms) at the
click sound's velocity. **Beat 1 of each measure is accented**: a higher (2000 Hz) and slightly louder (+3 dB) blip with
the same short shape. The Metronome accents the first beat of every measure, as the measure's time signature says; Count
In accents the first click of each measure it counts. It now sounds for Count In too when the song has a percussion track (before, that Count In played the
percussion track's own drum sound). Its level and pan follow the percussion track's Volume and Balance, as the drums
did. Any other MIDI Port still clicks the General MIDI drum note, unchanged.

## Setup

Build and launch with the VS Code task **app: build + run**, at commit `8ce6c57a6` or later. Use headphones for
step 6.

You need two songs, saved first:

- **Song A**, no percussion track: File > New, one guitar track, 4 measures of 4/4 at 120 bpm, a whole note C in each
  measure. Save it as `click-a.tg`.
- **Song B**, the same with a percussion track: Track > Add Track, and leave it empty. In View > Show Instruments, tick
  Percussion Channel on the new track's channel, Volume full, Balance centre. Save it as `click-b.tg`.
- **Song C**, for the accent in other meters: Song A saved as `click-c.tg`, then measure 2 set to 3/4 and measure 3 to
  5/4 (Composition > Time Signature, "to the end" unticked, each time with the caret in that measure).

Settings > Sound: MIDI Port "TuxGuitar Synthesizer" (choose an audio device in Tools > Audio Device... if asked). The
click sound is the default: Drum note 37, Velocity 95.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Open Song B. Transport > Count In on, Transport > Metronome off. Press Play. | Four short, bright blips a beat apart, then the guitar; the first blip is higher and a little louder ("ONE two three four"). No drum (no side stick, no wooden knock): before this change this Count In played the percussion track's General MIDI side stick. | not run |
| 2 | Stop. Count In off, Metronome on. Press Play. | A blip on every beat while the guitar plays, in time with it; beat 1 of every measure is the higher, slightly louder blip, beats 2 to 4 the plain one. The accent is clear but not jarring. | not run |
| 3 | Stop. Transport > Interleaved Practice..., each turn a measure, pool 1 to 4, 2 turns, Start. | Each turn's Count In is four blips, the first accented; with the Metronome on, beat 1 of each measure of the turn is accented. | not run |
| 4 | Stop. Settings > Sound: Velocity 127, OK. Press Play. Then Velocity 30, OK, Play again. | At 127 the blips are clearly louder than at 95; at 30 clearly softer. Same sounds, only the level changes: beat 1 stays accented at every velocity. | not run |
| 5 | Stop. Settings > Sound: Velocity 95, Drum note 42, OK. Press Play. | The very same blips as with note 37 (accent included): on the TuxGuitar Synthesizer the drum note is not heard. The note field's label reads "Drum note (other MIDI Ports)". | not run |
| 6 | Stop. In View > Show Instruments, set the percussion channel's Balance hard left. Press Play. Then hard right, and Play again. | Hard left: the blips are in the left ear only. Hard right: in the right ear only. The guitar stays where it was. | not run |
| 7 | Stop. Balance back to centre, Volume to about a quarter. Press Play. | The blips are clearly softer, in the centre. | not run |
| 8 | Stop. Open Song A (no percussion track). Count In and Metronome on. Press Play. | Four blips (the first accented), then a blip on every beat (beat 1 accented), as loud as in step 2 and in the centre. | not run |
| 9 | Stop. Open Song C. Metronome on, Count In off. Press Play. | Measure 1: accent, then 3 plain blips; measure 2 (3/4): accent, then 2; measure 3 (5/4): accent, then 4; measure 4: accent, then 3. The accent always lands on the first beat of the measure the cursor is in. | not run |
| 10 | Stop. Put the caret in measure 1 beat 3 of Song A and press Play (Metronome on). | The first blip (beat 3) is plain; the accent comes on beat 1 of measure 2. | not run |
| 11 | Stop. Transport > Count In ticks: 8 (Song A, Count In on). Press Play. | Eight blips before the guitar, the 1st and the 5th accented (two counted measures). Put the ticks back as they were afterwards. | not run |
| 12 | Stop. Settings > Sound: MIDI Port "Gervill" (or, on Windows, "Microsoft GS Wavetable Synth"), Drum note 37. Press Play on Song A. | General MIDI side-stick clicks, as before this change, for Count In and the Metronome, all alike (no accent on another MIDI Port). | not run |
| 13 | Stop. Settings > Sound: Drum note 42, OK. Press Play. | A closed hi-hat instead of the side stick: other MIDI Ports still play the drum note, every beat the same. | not run |

## Notes

- The automated tests cover the click's sound sample by sample (synth-core's `TestClickProcessor`: 1500 Hz, 0.2 ms
  rise, falling by e every 5 ms, silent at 30 ms, level velocity / 127, left = right, any note; the accent 2000 Hz and
  3 dB louder with the same envelope), which clicks are accented live and offline (the feature
  `transport/the_click_accent`, lib's `TestMidiPlayerMetronome`, `TestMidiPlayerCountDown` and `TestSequenceParserTurn`:
  time signature changes, a start mid-measure, 8 and 6 Count In ticks, other MIDI Ports unaccented), that the
  synthesizer's own drums play it with no latency (`TestSynthClock`), the drum tap offline (`TestDrumTap`), that the
  clicks follow the percussion channel's Volume and Balance (`TestDrumMix`, lib's `TestMidiPlayerMetronome`), that
  Count In goes to the drums with a percussion track too (lib's `TestMidiPlayerCountDown`), and that the clicks add no
  plugin delay compensation (the features' "the Count In and the Metronome add no latency"). What they cannot check is
  how the click sounds to you.
- Known, not to report here: playing live with Play looped on and nothing delaying the sound (no General MIDI track,
  no plugin with latency), an audio track's take can sound a few milliseconds past the end of the last pass. Pinned in
  the features; reported separately.
