---
title: Interleaved practice, slice 3b
description: Start a live practice session from the Transport menu, flag measures while it plays, pause and resume it.
sidebar:
  order: 4
---

## What it tests

Commit `094103093` on `feature/interleaved-practice`. Transport > Interleaved Practice starts a live session of turns
chosen at random from a pool of measures, lines or pages. Composition > Difficulty flags the measure at the caret 1x
to 5x. During a session only flags and the undo or redo of a flag are allowed. Pause keeps the session, and Play
plays the paused turn again from its Count In.

## Setup

Build and launch with the VS Code task **app: build + run**. In Settings > Sound, choose the TuxGuitar Sequencer and the
TuxGuitar Synthesizer. Open or write a song with at least 16 measures whose measures you can tell apart by ear, for
example a different note in each measure. Open View > Transport so the Transport window shows.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Open the Transport menu. | It has an **Interleaved Practice...** item below Play Mode. | not run |
| 2 | Choose Transport > Interleaved Practice... | A dialog shows: Each turn plays (A measure, A line, A page), Pool from, Pool to, Number of turns, Measures per line, Lines per page, and two checkboxes, both checked: No consecutive repeats, Complete cycle before reshuffle. The first time it shows a measure, pool 1 to 4, 12 turns, 4 and 4. | not run |
| 3 | Set the pool from 60 to 70 measures and click Start, on a song of fewer than 70 measures. | An error message says "The pool reaches past the song's N measures." Nothing plays. | not run |
| 4 | Open the dialog again. | It shows the settings of step 3, not the defaults. | not run |
| 5 | Set A measure, pool 1 to 4, 6 turns, Count In on in the Transport menu, then Start. | A Count In, then one of measures 1 to 4. Then another Count In and another measure, and so on, six times. No measure plays twice in a row. The player stops by itself after the sixth turn. | not run |
| 6 | Watch the Transport window during step 5. | Under the position it shows "Turn N of 6: Measure M", following the turns. The text goes once the session ends. | not run |
| 7 | Watch the score during step 5. | The played beat is highlighted in the turn's measure, not elsewhere, also in a song with repeat signs. | not run |
| 8 | Start a session of 12 turns. During turn 3, press Pause. | The sound stops. The Transport window still shows "Turn 3 of 12: ...". Stop is still enabled. | not run |
| 9 | Press Play. | The paused turn plays again from its Count In, then the session goes on with turns 4, 5 and so on. | not run |
| 10 | Pause again, then press Stop. | The session ends. The turn text goes. Play then plays the song as written. | not run |
| 11 | Put the caret in measure 2. Open Composition > Difficulty. | Items 1x (not flagged) to 5x. 1x is checked. | not run |
| 12 | Choose 3x, then open Composition > Difficulty again. | 3x is checked. Edit > Undo is enabled, and undoing brings 1x back. | not run |
| 13 | Start a session (pool 1 to 4, 12 turns). While it plays, open Composition > Difficulty. | The items are enabled. Other Composition items, such as Tempo, are greyed out. | not run |
| 14 | While the session plays, flag the caret's measure 3x. | The session goes on. From the next cycle on, that measure comes up three times per cycle. | not run |
| 15 | While it plays, press Ctrl+Z (or the toolbar's Undo). | The flag is undone and the session goes on. | not run |
| 16 | Start a session of measures 1 to 2, 6 turns. While it plays, flag measure 2 as 3x. | An error message says "Measure 2 comes up 3 times in a cycle of 4 turns and would come twice in a row." The flag stays 1x. The session goes on, and the app stays responsive: Stop, Pause and menus still work. | not run |
| 17 | While a session plays, try another edit, for example typing a fret number. | Nothing changes, and the session goes on. | not run |
| 18 | While a session plays, click a measure in the score. | The session ends and the song plays on from there. | not run |
| 19 | In Settings > Sound, choose another sequencer, such as the Java Sound one, and start a session. | An error message says the session needs a sequencer that takes turns added while it plays, names the chosen sequencer, and suggests the TuxGuitar Sequencer. | not run |

## Notes

- Known and not to report: with a VST3 instrument, the note held when the player pauses or moves rings on, because
  the plugin ignores All Notes Off.
- While a session is paused, menus such as Composition > Tempo still look enabled. Choosing them does nothing until
  the session ends.
- The turns, the pause and resume, the flag rules and the refusals are already covered by the automated tests. This
  page checks what is seen and heard.
