---
title: Track panel fits its width
description: Nothing of the Track panel runs past its right edge; long lines wrap, long names are cut with their whole in a tooltip.
sidebar:
  order: 44
---

## What it tests

Branch `fix/track-panel-fit`, commit `d0b102cd4` (the drop-downs in `b343534ab`). The Track panel docked at the
right of the score kept its content wider than the panel itself (473 points in a 300-point panel), so its right part
ran off the screen: Disable, Remove, Add and the "editor is too large for the panel" line were cut. Now everything in
the panel is laid out at the panel's own width (220 to 600) and never past it, with no horizontal scrollbar. Long
lines wrap. Long names are cut with "…" and shown whole in a tooltip. Drop-down boxes narrow to their row, but their
opened lists stay as wide as their longest name.

## Setup

Close any running TuxGuitar first. Check out the commit above, then build and launch with the VS Code task
**app: build + run**. Maximize the window.

- Settings > Sound: MIDI Port **TuxGuitar Synthesizer**. Default instruments: Guitar tracks **Woodshed Guitar**.
- View > Show Sound Chain on (F8). The caret on a guitar track whose sound is **VST3** (the default instrument).
- Woodshed Guitar's editor must be in a window of its own, so the panel shows the line "Woodshed Guitar's editor is
  too large for the panel: it is in its own window." If it shows inside the panel instead, drag the panel's divider
  to the right until that line appears.
- A VST3 effect with a long name installed (an amp simulator such as AmpliTube 5 will do).

## Steps

| #   | Do | Expect | Result |
| --- | -- | ------ | ------ |
| 1   | Look along the panel's right edge, at the default width. | Every row ends inside the panel, left of its vertical scrollbar. The MIDI / VST3 switches, Disable, Remove, Add and the meters are all fully visible. Nothing is cut at the screen's edge. | not run |
| 2   | Read the line under **Sound Chain...**. | "Woodshed Guitar's editor is too large for the panel: it is in its own window." wraps over two or more lines. **Show** sits on its own line under it, at the left. | not run |
| 3   | Hover the **Instrument** box. | The box shows the name cut ("Default instrument (Woodshed Gu…" or shorter). The tooltip shows the whole name, "Default instrument (Woodshed Guitar)". | not run |
| 4   | Open the **Instrument** drop-down. | The opened list shows every name whole, even when that makes it wider than the box. | not run |
| 5   | Hover the **Source** chip in the chain. | Its name ends in "…" with no space before it, and fits on the chip. The tooltip shows the whole "Source: Woodshed Guitar (default for guitar tracks)". | not run |
| 6   | Drag the panel's divider slowly to the right, as far as it goes. | The content re-fits while you drag: the chip's name gets shorter, the line wraps over more lines. At the narrowest nothing runs past the right edge, and Disable, Remove and Add can still be read. | not run |
| 7   | Drag the divider to the left, as far as it goes. | The panel grows. The chip and the Instrument box show their names whole, and the editor line wraps less or shows the editor inside. Nothing runs past the right edge. | not run |
| 8   | In **VST3 effect**, pick the effect with the long name, hover the box, then click **Add**. | The box shows the name cut if it is too long, with the whole name in its tooltip. The new chip in the chain is cut to the panel's width, its whole name in its tooltip. | not run |
| 9   | Click **MIDI**, then pick a long program in **Program** (e.g. "Electric Guitar (jazz)") and hover the box. | The box fits its row. A name too long for it shows cut, whole in the tooltip. Nothing runs past the right edge. | not run |
| 10  | Settings > Sound: set the MIDI Port to another port than TuxGuitar Synthesizer, then look at the panel. | The line saying the chains are not heard wraps inside the panel; none of it is cut at the right edge. Set the port back afterwards. | not run |
| 11  | Make the window less tall (restore it and drag its bottom edge up) until the panel scrolls. | The panel's vertical scrollbar appears and scrolls the content. No control is hidden under the scrollbar, and none is cut at the right. | not run |
| 12  | Close TuxGuitar and start it again. | The panel opens at the width you left it, fitted the same way. | not run |

## Notes

- Known, not part of this slice: a window narrower than about 1166 points still lays out wider than itself, because
  the main toolbar alone needs about 1158. The Track panel is then pushed off the window's right edge. A later
  slice fixes that, so don't report it here.
- Only the Windows SWT build was checked. JavaFX and Qt builds compile against the same change but were not run.
- The cut of a name ("…", the longest start that fits, no space before the "…") is covered by
  `TestTrackSoundChoices`; this page checks only what is seen in the running app.
