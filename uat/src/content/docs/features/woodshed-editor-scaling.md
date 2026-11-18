---
title: Woodshed Guitar's editor, scaled and stretched
description: Woodshed Guitar's editor is drawn at the screen's display scale in the system font, and can be dragged wider; its sliders stretch, its height stays.
sidebar:
  order: 31
---

## What it tests

Commits `095124f07`, `5e72bce15` and `8b27d3f6c` (a long profile name ends in "…") on `feature/sound-chain-panel`. Woodshed Guitar's editor (the window Edit...
opens) used to be a fixed 460×230 window in a small 96-DPI font, so it looked tiny at 150% or 175% display scale. Now:

- it is drawn at the display scale of the screen it is on, in the same font Windows uses for messages, and follows the
  window to a screen of another scale;
- its border can be dragged: wider, the two sliders and the profile lines stretch while the labels, buttons and values
  keep their width; narrower, it stops where the labels, sliders and values still fit; its height does not change;
- a profile name too long for the line ends in "…", and resting the mouse pointer on it shows the whole line.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `8b27d3f6c` or
later on the branch. The install folder is
`desktop/build-scripts/tuxguitar-windows-swt-x86_64/target/tuxguitar-9.99-SNAPSHOT-windows-swt-x86_64`.

- File > New. In the Sound Chain panel (or Tools > Sound Chain...), pick "Woodshed Guitar" in the VST3 instrument list
  and press **Use as source**; pick the source and keep it picked.
- For steps 5–6, a second monitor set to another scale helps; without one, change the scale of the only monitor.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Windows Settings > Display > Scale **100%**. Press **Edit...** on the source. | Woodshed Guitar's window opens in front of the main window: Load profile..., the profile line, Input gain and Output level with their sliders and values, Quality. The text is in the same font as Windows' own dialogs (Segoe UI), not a smaller bitmap font. | not run |
| 2 | Close it. Set the scale to **150%** (sign out if Windows asks), start TuxGuitar again and open the editor. | Everything is 1.5 times as large: the text is as readable as TuxGuitar's own dialogs at that scale; nothing overlaps or is cut off. | not run |
| 3 | Same at **175%**. | Everything is 1.75 times as large; nothing overlaps or is cut off. | not run |
| 4 | Drag the right border of the editor's window to the right, then back to the left as far as it goes. Then drag the bottom border. | Wider: the two sliders and the profile line grow, the values stay at the right edge, the labels, buttons and Quality keep their width. Narrower: it stops before any label, slider or value is cut off. The height does not change. | not run |
| 5 | With two monitors of different scales: drag the editor's window from one monitor to the other. | When it crosses over, the editor redraws at the other monitor's scale: larger or smaller text and controls, the same layout. | not run |
| 6 | Move a slider, then choose Light in Quality. | They work as before; Edit > Undo takes each change back. | not run |
| 7 | Load profile..., choose a .nam file with a long name (copy one to a name of 80 characters or more). | Its name shows on the profile line; where the line is too long for the window it ends in "…" (one character, right after the last letter that fits, no space before it) rather than being cut at the window's edge. | not run |
| 8 | Rest the mouse pointer on the cut profile line. | After a moment a tooltip shows the whole line, "Profile: <the whole file name>". | not run |
| 9 | Drag the editor wider until the whole name fits; rest the pointer on the line again. | The whole name shows, without "…"; no tooltip appears on it. Drag it narrower: the "…" comes back as soon as the name no longer fits. | not run |
| 10 | Same as steps 7–9 at **150%** display scale. | The cut follows the larger text: it still ends in "…" right where the line ends; the tooltip shows the whole line. | not run |

## Notes

- The scenarios in nam_guitar_instrument.feature check the scale it is told (96 and 144 DPI), its font size, the width
  it stretches to (900 wide: sliders 700) and the narrowest it goes (320 at 96 DPI); woodshed-guitar-layout-test checks
  every box of the layout, including long translations. This page checks how it looks.
- Woodshed Guitar's texts are English only for now (its language follows Windows; it will follow TuxGuitar's Language
  when the editor moves into the panel, slice 4).
- The profile line was a word-wrapping text, so the rest of a long name fell onto a second line below its box and
  Windows' end ellipsis never applied. Since `8b27d3f6c` the line is drawn by the editor: woodshed-guitar-layout-test
  checks where it is cut (as many characters as fit, spaces before the "…" dropped, a character of two UTF-16 units
  never halved); the scenarios check that a long name ends in "…" with the whole line in its tooltip, and that a
  name that fits has none. The line's own text stays the whole name, so a screen reader reads it all.
