---
title: Woodshed Guitar, the NAM guitar instrument
description: A VST3 instrument shipped with TuxGuitar plays the tablature as a plucked guitar through a NAM capture (A1 or A2).
sidebar:
  order: 13
---

## What it tests

Commit `c6ed63007` on `feature/nam-guitar-instrument`. TuxGuitar ships a VST3 instrument, **Woodshed Guitar**, in its
own plugins folder (`vst3` in the install directory). Each note plucks a string; the strings play through a NAM capture
(a `.nam` file, A1 or A2) loaded in its editor, so the tablature sounds through a real amp tone. With no capture it
plays a clean, dry guitar. The song keeps the capture's contents, so it still plays after the file moves or on another
computer.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `c6ed63007` or
later on the branch. The build makes the plugin (the first build downloads NeuralAmpModelerCore and Eigen) and copies it
to `desktop/build-scripts/tuxguitar-windows-swt-x86_64/target/tuxguitar-9.99-SNAPSHOT-windows-swt-x86_64/vst3`.

- In Settings > Sound, choose the TuxGuitar Sequencer and the TuxGuitar Synthesizer, and an audio device.
- Have two or three `.nam` captures of your own at hand, in a folder you can move files out of: an A1 capture (say a clean
  amp, **Clean**), an A2 capture if you have one (**A2**), and a high-gain one (**Lead**). Copy them first: step 9 moves
  one away.
- File > New. Enter a few bars on track 1: single notes on several strings, a chord, a note with a full bend (Effects >
  Bend), and a passage with notes of different dynamics (ppp and fff).

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Open track 1's Sound Chain and look at the VST3 instruments offered. | **Woodshed Guitar** is listed with the other VST3 instruments (it was found in TuxGuitar's own `vst3` folder). | not run |
| 2 | Choose Woodshed Guitar as track 1's instrument and press Play. | The notes sound as a clean, plucked guitar at their pitches; a chord's notes ring together; a note stops ringing soon after it ends (no long hang); the ppp notes are clearly softer than the fff ones. Nothing is silent. | not run |
| 3 | Listen to the bent note. | Only that note rises a whole tone and holds; the other notes of the chord keep their pitch. | not run |
| 4 | Click Edit for the instrument (Woodshed Guitar's own editor). | A small window: a **Load profile...** button, the line "No profile: the dry guitar is heard", sliders for **Input gain** and **Output level** (each showing "0.0 dB") and a **Quality** list showing Full. | not run |
| 5 | Click **Load profile...** and look at the file dialog. | It shows `.nam` files ("NAM captures (*.nam)"), with "All files" as the other choice. | not run |
| 6 | Choose **Clean** and press Play. | The editor shows "Profile: Clean.nam". The tablature now sounds through the amp tone of the capture. Edit > Undo is available (one step), and the song shows as modified. | not run |
| 7 | Load **Lead** (and **A2** if you have one), playing after each. | Each sounds clearly different, and the editor names it. An A2 capture loads and plays like an A1. | not run |
| 8 | In Load profile..., choose a file that is not a capture (a `.txt`, or a `.wav` via All files). | The editor shows "Not loaded: ..." with the reason; the profile line still names the previous capture, which still plays. Undo offers nothing new. | not run |
| 9 | Save the song, close it, and open it again: press Play. Then close it, move **Lead** (the loaded capture) out of its folder, open the song again and press Play. | Both times the song plays through **Lead** and the editor names it, even with the file gone. | not run |
| 10 | Edit > Undo after loading a capture (steps 6 or 7). | The previous profile (or the dry guitar) is heard again, and the editor shows it. | not run |
| 11 | In the editor, drag **Output level** down, then **Input gain** up; with an A2 capture loaded, set **Quality** to Light. Then Edit > Undo once. | Output level changes the loudness; Input gain drives the capture harder (more distortion on a high-gain capture); each slider shows its value in dB as it moves. Light sounds a little different from Full and costs less CPU. Each drag or choice is one undo step: the undo puts Quality back to Full and the list shows it. | not run |
| 12 | CPU feel: add three more guitar tracks (Track > Add Track), give each Woodshed Guitar with a full-size capture and some notes, and play the song. Watch Task Manager. | Playback stays smooth, without dropouts or crackles; TuxGuitar's helper processes (one per plugin) each use a few percent of a core. | not run |
| 13 | In Settings > Sound, set the audio device to 44.1 kHz and play a capture made at 48 kHz (most are). | Pitch and timing are unchanged: Woodshed Guitar stays in time with another track playing a General MIDI program. | not run |

## Notes

- The tests already cover pitch, levels, chords, damping, bends, the gain of a test capture, A1 and A2 test captures,
  refusals, undo, saving with the capture's contents, and the resampling's latency. This page checks what a person
  hears and sees: the real tone, the editor, the file dialog, and the CPU cost.
- Measured with the timing tool (`woodshed-guitar-time`, 256-frame blocks, per instance on one core of the
  development machine): A1 standard 8.4% at 44.1 kHz (resampled) and 7.6% at 48 kHz; A2 full 5.3% and 4.5%; A2 light
  2.9% and 2.2%.
- Woodshed Guitar is not yet the default instrument, and its editor is a window of its own until the docked Sound Chain
  panel comes.
- Known, not a defect of this change: a VST3 instrument ignores All Notes Off, so a note held when playback moves or
  stops may ring on.
