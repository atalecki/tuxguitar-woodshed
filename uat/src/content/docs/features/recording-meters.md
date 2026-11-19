---
title: Recording window, input meters
description: The Recording window's input meters are the Sound Chain panel's meters, with a lamp that Reset clip and a click both clear.
sidebar:
  order: 39
---

## What it tests

Commit `12ccf9539` on `feature/sound-chain-meters`. Each armed track's row in the Recording window now shows the same
meter as the Sound Chain panel: a bar that jumps up and falls back smoothly, a line held 2 seconds at the highest point,
and a box at the right end that fills with "!" once the input reached -0.1 dB. The box, the "CLIP" word and the
**Reset clip** button are one state: Reset clip, or a click on the meter, clears them.

## Setup

Build and launch with the VS Code task **app: build + run**. In **Tools > Settings > Sound**, choose an audio device
with an input (a microphone or an interface). Add an audio track and arm it, then open the Recording window.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1   | Look at the armed track's row. | The track's name, a meter (in place of the old progress bar) with a 0 dB mark near its right end and an empty box after it, the level ("-inf dBFS" in silence), and **Reset clip**. | not run |
| 2   | Speak or play into the input. | The bar jumps up with the sound and falls back smoothly; a thin line stays at the highest point for about 2 seconds. The level text follows the loudest moment of about the last two thirds of a second. | not run |
| 3   | Hover the meter. | A tooltip such as "Voice: peak -12.3 dB, held -8.1 dB". | not run |
| 4   | Make the input very loud (a clap close to the microphone, or the gain turned up). | The box fills with "!", and "CLIP" appears. The tooltip ends with "Clipped: click the meter to clear." | not run |
| 5   | Press **Reset clip**. | The box goes back to an outline and "CLIP" disappears. | not run |
| 6   | Clip again, then click the meter itself. | The box and "CLIP" clear, as with Reset clip. | not run |
| 7   | Repeat steps 1 and 2 at 150% display scaling, and with a Windows high-contrast theme (restart TuxGuitar after switching). | The meter scales with the text and uses the theme's colours; the bar, the stripes above 0 dB and a lit box all stay visible. | not run |

## Notes

- The input level's value, the clip threshold, the clip indicator and Reset clip are covered by the tests
  (`recording/recording_line_in.feature`); this page checks what only a person can see.
- Not checked in the app by the developer: the user was running a manual test at the time, so the app was not launched.
