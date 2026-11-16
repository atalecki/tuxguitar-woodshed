---
title: Interleaved practice, units per block
description: Each turn plays a block of so many units in a row, chosen at random from every such run of the pool.
sidebar:
  order: 11
---

## What it tests

Commits `8e1e1d234` to `3eb65f17a` on `feature/practice-blocks`. The Interleaved Practice dialog has a new field,
"Measures per block" (named after the unit chosen: lines, pages or beats per block). Each turn then plays a block of that
many units in a row instead of one unit. The pool's blocks are every such run, sliding by one unit: a pool of measures 1
to 4 in blocks of 2 holds the blocks 1-2, 2-3 and 3-4. The random choice, "No consecutive repeats" and "Complete cycle"
work on blocks. Two overlapping blocks such as 1-2 and 2-3 are different blocks, so one may follow the other. A block of
beats runs on through the barline and ends with its joint. The field cannot go above the number of units the pool
holds, or one fewer while "No consecutive repeats" would refuse a pool of one block (the rule on, the interval clock off
and more than 1 turn). When the limit drops below the value you chose, the field shows the limit, and your value comes
back once the limit rises again. With 1 per block (the default) a session plays exactly as before. Interleaved Click-Up has no such field.

## Setup

Close any running TuxGuitar first: the build copies the app's jars, and a running TuxGuitar holds them locked, so the
build fails. Then build and launch with the VS Code task **app: build + run**, at commit `3eb65f17a` or later on the
branch. In Settings > Sound, choose the TuxGuitar Sequencer and the TuxGuitar Synthesizer. Open View > Transport so the
Transport window shows. Leave Transport > Count In and the Metronome off: with "Count In before each turn" checked in the
dialog (as it is until you uncheck it), each turn counts in by itself, one click per beat of its first measure.

Build this test song. Every beat has its own note, rising, so you can tell by ear where a block starts and stops. Set the
time signatures **before** you enter any notes.

1. File > New: one guitar track, 4/4 at 120 bpm. Add measures until the song has 6 measures.
2. Put the caret in measure 5. Open Composition > Time Signature, choose 3/4 and apply it to the end of the song.
   Measures 1 to 4 are 4/4, measures 5 and 6 are 3/4.
3. Enter quarter notes on the tablature:
   - string 1 (high E): measure 1 frets 1, 2, 3, 4; measure 2 frets 5, 6, 7, 8; measure 3 frets 9, 10, 11, 12;
     measure 4 frets 13, 14, 15, 16;
   - string 2 (B): measure 5 frets 1, 2, 3; measure 6 frets 4, 5, 6.
4. Play the song once. Every beat sounds, 0.5 seconds each. A 4/4 measure lasts 2 seconds, a 3/4 measure 1.5 seconds.

The song holds 22 beats: 16 in measures 1 to 4 and 6 in measures 5 and 6.

## Steps

Unless a step says otherwise, keep "No consecutive repeats", "Complete cycle before reshuffle" and "Count In before each
turn" checked, and the interval clock off.

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Open Transport > Interleaved Practice.... | A new row, "Measures per block:", below "Pool to:" and above "Number of turns:". It shows 1 the first time the dialog opens in this run of TuxGuitar. | not run |
| 2 | Choose "A line", then "A page", then "A beat", then "A measure" again in "Each turn plays". | The new field's label reads "Lines per block:", then "Pages per block:", then "Beats per block:", then "Measures per block:" again. No label is cut off. | not run |
| 3 | With "A measure", set the pool from 1 to 4 and Number of turns to 12 (as the dialog opens the first time). Try to set Measures per block to 4 or more, with the arrow and by typing (for example 9), then to 0. | The field stops at 3, one less than the pool's 4 measures: with "No consecutive repeats" on and more than 1 turn, a pool of one block would be refused at Start. It does not go below 1. | not run |
| 4 | Uncheck "No consecutive repeats". Set Measures per block to 4. Check "No consecutive repeats" again. Then set Number of turns to 1. | Unchecked, the arrow goes up to 4. Checked again, the field drops to 3. With 1 turn it shows 4 again, the value you chose. | not run |
| 5 | Set Number of turns back to 12. Check "Interval clock". Then uncheck it. | 12 turns: the field drops to 3. Clock on: it shows 4 again and the arrow goes up to 4 (a timed pool of one block plays it every turn, so the rule does not apply). Clock off: it drops to 3. | not run |
| 6 | Set Number of turns to 1 and Measures per block to 4 (pool 1 to 4). Set Pool from 5. Then set Pool to 6. Then set Pool from 1. | Pool from 5 empties the pool (from after to): the field drops to 1. Pool to 6 (measures 5 to 6): it shows 2. Pool from 1 (measures 1 to 6): it shows 4 again, the value you chose, and the arrow goes up to 6. | not run |
| 7 | Still with 1 turn, set Measures per block to 6. Set Measures per line to 2. Choose "A line". Then set Measures per line to 3. | Choosing "A line": Pool to drops to 3 (the song's 3 lines) and the field, now "Lines per block:", shows 3. Measures per line 3: Pool to drops to 2 (two lines of 3 measures) and Lines per block to 2. | not run |
| 8 | Choose "A beat". Note the field's highest value. Then set the pool from 1 to 6 (measures) and try the field again. Then set Number of turns to 12. | With pool 1 to 2 (measures, from step 7) the field goes up to 8 beats. With pool 1 to 6 it goes up to 22. With 12 turns it goes up to 21. | not run |
| 9 | Practise: "A measure", pool 1 to 4, Measures per block 2, Number of turns 9. Start. | Each turn is a Count In of 4 clicks, then two measures in a row, 8 rising notes: measures 1-2 (frets 1 to 8), 2-3 (frets 5 to 12) or 3-4 (frets 9 to 16). Each group of 3 turns plays each of the three blocks once, in random order. The same block never plays twice in a row, but two overlapping blocks may (for example 1-2 then 2-3). | not run |
| 10 | Watch the Transport window's label during step 9. | "Turn 1 of 9: Measures 2–3" (a capital M, with a dash between the numbers), naming the block you hear, then "Turn 2 of 9: ..." and so on. | not run |
| 11 | After step 9, open Transport > Interleaved Practice... again. | Measures per block shows 2, with the rest of the last start's settings. | not run |
| 12 | Practise: "A line", Measures per line 2, pool 1 to 3, Lines per block 2, 4 turns. | Each turn plays lines 1-2 (measures 1 to 4) or lines 2-3 (measures 3 to 6). The two blocks take turns: 1-2, 2-3, 1-2, 2-3, or the other way round. The label reads "Turn 1 of 4: Lines 1–2" or "Lines 2–3". | not run |
| 13 | Practise: "A page", Measures per line 1, Lines per page 2, pool 1 to 3, Pages per block 2, 4 turns. | Pages are measures 1-2, 3-4 and 5-6. Each turn plays pages 1-2 (measures 1 to 4) or 2-3 (measures 3 to 6), taking turns as in step 12. The label reads "Pages 1–2" or "Pages 2–3". | not run |
| 14 | Practise: "A beat", pool 1 to 2 (measures), Beats per block 2, 7 turns. Listen for the block that crosses the barline. | 7 turns play the 7 blocks of two beats once each, in random order. The block of measure 1 beat 4 to measure 2 beat 1 plays fret 4, then fret 5, then the joint fret 6 (the note on the next beat) for one beat, then stops. The label reads "Turn N of 7: Measure 1 beat 4 to measure 2 beat 1". Every block ends with its joint: 3 notes in all. | not run |
| 15 | Practise: "A beat", pool 5 to 6 (the 3/4 measures), Beats per block 2, 5 turns. Listen for the block that crosses the barline. | 5 blocks once each. Each turn counts in with 3 clicks. The block of measure 5 beat 3 to measure 6 beat 1 plays B-string frets 3, then 4, then the joint fret 5, then stops. The label reads "Measure 5 beat 3 to measure 6 beat 1". The block of measure 6 beats 2 to 3 ends on the song's last beat, so it has no joint: frets 5 and 6, then the turn ends. | not run |
| 16 | Practise: "A measure", pool 1 to 4, Measures per block 2, interval clock on, 1 minute per turn, a 10-second break, a 2-minute session, "Count In before every pass" unchecked. | A Count In, then the same two measures over and over, back to back, for a minute: the turn loops its whole block. The label reads "Turn 1: Measures 2–3, 1:02 left" (the block's 4-second passes run past the minute to the end of a pass), counting down each second. In the break, silence and "Break, 0:10 left. Next: Measures 1–2" (the next block). Then turn 2 plays the block named, a different one. The session ends after turn 2. | not run |
| 17 | Practise: "A measure", pool 1 to 4, Measures per block 1, 4 turns. | As before this change: each turn plays one measure, each of measures 1 to 4 once. The label reads "Turn 1 of 4: Measure 3" (the measure's word and number, no dash). | not run |
| 18 | Open Transport > Interleaved Click-Up.... | No "per block" field: the dialog's rows are the same as before this change. | not run |

## Notes

- The automated tests already check which blocks a pool holds, the refusal of a block larger than the pool, the weight
  of a block (its most difficult measure), the notes a block of beats plays through the barline to its joint, and the
  wording of the Transport label. These steps check what the tests cannot: the dialog, what you hear and what you read.
- Only English has the new labels. Other languages show the English text for them until translated.
