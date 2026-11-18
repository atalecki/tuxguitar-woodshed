---
title: Woodshed Guitar's editor, scaled and stretched
description: Woodshed Guitar's editor is drawn at the screen's display scale in the system font, and can be dragged wider; its sliders stretch, its height stays.
sidebar:
  order: 31
---

## What it tests

Commits `095124f07` and `5e72bce15` on `feature/sound-chain-panel`. Woodshed Guitar's editor (the window Edit...
opens) used to be a fixed 460×230 window in a small 96-DPI font, so it looked tiny at 150% or 175% display scale. Now:

- it is drawn at the display scale of the screen it is on, in the same font Windows uses for messages, and follows the
  window to a screen of another scale;
- its border can be dragged: wider, the two sliders and the profile lines stretch while the labels, buttons and values
  keep their width; narrower, it stops where the labels, sliders and values still fit; its height does not change.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `5e72bce15` or
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
| 7 | Load profile..., choose a .nam file. | Its name shows on the profile line; a long name ends in "..." rather than running under the window's edge (see Notes). | not run |

## Notes

- The scenarios in nam_guitar_instrument.feature check the scale it is told (96 and 144 DPI), its font size, the width
  it stretches to (900 wide: sliders 700) and the narrowest it goes (320 at 96 DPI); woodshed-guitar-layout-test checks
  every box of the layout, including long translations. This page checks how it looks.
- Woodshed Guitar's texts are English only for now (its language follows Windows; it will follow TuxGuitar's Language
  when the editor moves into the panel, slice 4).
- Seen while building, pinned: at the narrowest width a long profile line was cut at the window's edge without "...".
  Report what you see in step 7.
