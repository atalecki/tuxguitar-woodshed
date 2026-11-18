---
title: Default instruments greyed for other MIDI Ports
description: Settings > Sound greys the Default instruments group, saying why, while the MIDI Port chosen is not the TuxGuitar Synthesizer, and makes it editable at once when the port changes to it.
sidebar:
  order: 27
---

## What it tests

Commit `27644a28d` on `feature/default-instrument-follow-ups`. Default instruments play only on the TuxGuitar
Synthesizer. In Settings > Sound, the **Default instruments** group is now greyed while the MIDI Port chosen on the page
is another one, with the line **Default instruments play only when the MIDI Port is the TuxGuitar Synthesizer.** under
it. Choosing the TuxGuitar Synthesizer in the MIDI Port drop-down of the same page makes the group editable at once,
before OK; choosing another port greys it again at once.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `27644a28d` or
later on the branch. At least one MIDI Port other than the TuxGuitar Synthesizer must be listed in Settings > Sound
(Windows lists **Microsoft GS Wavetable Synth**).

- In Settings > Sound choose the TuxGuitar Synthesizer as the MIDI Port and an audio device; OK. Close TuxGuitar.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Start TuxGuitar. Open Settings > Sound. | The MIDI Sequencer and MIDI Port drop-downs show those chosen in Setup (their listing was rewritten). The Default instruments group is editable: each kind's drop-down can be opened; the guitar and bass **Choose...** buttons are enabled (Woodshed Guitar is their choice); no line under the group. | not run |
| 2 | In the MIDI Port drop-down, choose **Microsoft GS Wavetable Synth** (or any other port). Do not press OK. | At once the four drop-downs and the Choose... / Shipped buttons are greyed, and the line **Default instruments play only when the MIDI Port is the TuxGuitar Synthesizer.** appears under the group. | not run |
| 3 | Choose the **TuxGuitar Synthesizer** again in the MIDI Port drop-down. | At once the group is editable again and the line is gone. Shipped stays greyed while the shipped profile is chosen. | not run |
| 4 | Choose the other port again and press OK. Open Settings > Sound again. | The page opens with the other port and the group already greyed, the line under it. | not run |
| 5 | With the group greyed, press **Defaults** (the page's button), then OK. Open Settings > Sound again. | The MIDI Port is back to TuxGuitar's default, and the group follows it (editable when the default port is the TuxGuitar Synthesizer). | not run |
| 6 | Clean up: choose the TuxGuitar Synthesizer as the MIDI Port; OK. | (Clean-up.) | not run |

## Notes

- What is greyed, and the line, are decided in `TGDefaultInstrumentChoices` and checked by its tests; this page checks
  the dialog shows it, and follows the port drop-down at once.
- The line is in the translation files; only English has it for now (other languages show the English text).
