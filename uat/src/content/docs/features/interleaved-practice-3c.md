---
title: Interleaved practice, slice 3c
description: Live takes follow the turns, the caret moves freely in a session, refused edits are greyed out, and the turn label is translated.
sidebar:
  order: 5
---

## What it tests

Commit `2156aef95` on `feature/interleaved-practice`. During a live practice session an audio track's take plays the
turn's measures and fades out at each turn's end. Clicking the score or moving the caret moves only the caret, so the
session goes on, and Composition > Difficulty flags the measure the caret is on. Every menu item and toolbar button of
an edit the session refuses is greyed out, also while paused. The Transport window's turn label comes from the app's
messages.

## Setup

Build and launch with the VS Code task **app: build + run**. In Settings > Sound, choose the TuxGuitar Sequencer and the
TuxGuitar Synthesizer, and choose an audio device in Tools > Audio Device.... Open or write a song with at least 16
measures whose measures you can tell apart by ear, for example a different note in each measure. Open View > Transport
so the Transport window shows. For steps 1 to 4, add an audio track (Tools > Add Audio Track), arm it (Tools > Arm /
Disarm Track) and record a take over measures 1 to 8 (Tools > Record) of something you can follow, such as counting
the measures out loud ("one", "two", ...) or a steady hum.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | With Count In on, start Transport > Interleaved Practice... with each turn a measure, pool 1 to 8, 4 turns. | In each turn the take plays the turn's measure: you hear what you recorded over that measure (for example "three" in measure 3), not the take from the song's start. | not run |
| 2 | Listen to the end of each turn during step 1. | The take stops at the turn's end with a short fade, no click. It is silent during the next turn's Count In. | not run |
| 3 | Listen to the start of each turn during step 1. | The take starts at full level on the turn's first beat, with no fade in. | not run |
| 4 | Add a VST3 echo effect to the audio track (its Sound Chain...) and practise again. | The echo of the take's end rings on into the next Count In, as across a loop's jump back. | not run |
| 5 | Start a session of 12 turns (pool 1 to 4). While it plays, click a measure in the score outside the turn. | The caret goes to that measure. The session goes on: the turn plays to its end and the next turn follows. The Transport window still shows the turn. | not run |
| 6 | While the session plays, press the arrow keys. | The caret moves. The session goes on; the player does not jump. | not run |
| 7 | While the session plays, click measure 2 and choose Composition > Difficulty > 3x. | The flag goes to measure 2 (open the menu again: 3x is checked with the caret in measure 2). The session goes on. | not run |
| 8 | Pause the session. Move the caret with the arrow keys and by clicking. Flag the caret's measure 2x. | The caret moves; the flag goes to the caret's measure. Play then plays the paused turn again from its Count In. | not run |
| 9 | While the session is paused, open the Edit, Composition, Measure, Beat, Track and Tools menus. | Items of edits are greyed out, such as Cut, Paste, Composition > Tempo, Measure > Add, Beat items, Track > Remove, Tools > Transpose. Composition > Difficulty items are enabled. Undo is enabled only when the edit to undo is a flag. Measure > First, Previous, Next and Last stay enabled. | not run |
| 10 | While the session plays, look at the same menus and at the main toolbar. | The same items and the matching toolbar buttons are greyed out. The toolbar's Undo and Redo follow the same rule as the menu's. | not run |
| 11 | Press Stop. Open the same menus with the player stopped. | Every item is enabled again, as before the session. | not run |
| 12 | Play the song without a session and open the menus while it plays. | The items greyed out are the same as before this slice: nothing new is greyed. | not run |
| 13 | During a session, watch the Transport window. | Under the position it shows "Turn N of 12: Measure M" (or Line, Page), as in slice 3b. | not run |
| 14 | During a session, press the Transport window's next-measure button (or Measure > Next). | The player moves, the session ends, and the song plays on from there. | not run |
| 15 | In a 64-measure song, open Transport > Interleaved Practice... with each turn a measure, and try to set Pool from / Pool to above 64, by typing and with the arrows (commit 02a05a4a3). | Neither field goes above 64. | not run |
| 16 | In the same dialog, choose each turn a line, measures per line 5; then a page, lines per page 3. Then change measures per line to 4. | The pool fields top out at 13 (lines), then 5 (pages), then 6 (pages of 3 lines of 4 measures); a value above the new top comes down to it. | not run |
| 17 | Start a session with each turn a measure, pool 1 to 64, then Stop it. Open a 16-measure song and open the dialog again (it remembers the last start). | Pool from / Pool to show at most 16. | not run |

## Notes

- Known and not to report: with a VST3 instrument, the note held when the player pauses or moves rings on, because
  the plugin ignores All Notes Off.
- Moving the caret right past the song's last measure adds a measure, which is an edit: during a session it is
  refused, as other edits are.
- The turn label's words come from the app's messages; only English has them, other languages show the English words.
- The take's fade at a turn's end, live, and the rules for what is greyed out and where the caret moves are covered
  by the automated tests. This page checks what is seen and heard.
