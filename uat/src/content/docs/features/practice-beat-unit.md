---
title: A beat as the practice unit
description: Practise one beat and its joint, the note on the next beat, in Interleaved Practice and Interleaved Click-Up.
sidebar:
  order: 8
---

## What it tests

Commits `8099e6d3f` to `36fd8b497` on `feature/interleaved-click-up`. "A beat" is now a unit in both practice dialogs. A
beat turn (or a click-up section) plays one beat, one click of the time signature, and then the joint. The joint is the
notes that start exactly on the next beat, in score order, and they sound for that one extra beat. Notes held over from
the beat stop where the joint starts. The joint plays even when it lies past the pool or passage. The song's last beat has
no joint. The pool and the passage are still given in measures, and their labels say "(measures)". The Transport window
names a beat by its measure and beat.

## Setup

Build and launch with the VS Code task **app: build + run**, at commit `36fd8b497` or later on the branch. In Settings >
Sound, choose the TuxGuitar Sequencer and the TuxGuitar Synthesizer. Open View > Transport so the Transport window shows.
Check that the Percentage of Tempo in the Transport window is 100 and the Metronome is off. The click counts below
assume the Transport window's Count In ticks are not set. With ticks set, every Count In has that many clicks instead of
one per beat of the time signature.

Build this test song. Every beat has its own note, rising, so you can tell by ear where a turn starts and where it stops.
Set the time signatures **before** you enter any notes. Changing a time signature after notes are in place leaves silent
beats.

1. File > New: one guitar track, 4/4 at 120 bpm. Add measures (Measure > Add Measure, at the end) until the song has
   6 measures.
2. Put the caret in measure 5. Open Composition > Time Signature, choose 6/8 and apply it to the end of the song.
   Measures 1 to 4 are 4/4 and measures 5 and 6 are 6/8, all still empty.
3. Enter the notes on the tablature. On string 1 (high E), use quarter notes:
   - measure 1: frets 1, 2, 3, 4;
   - measure 2: fret 5 (beat 1), then fret 6 as a **half note** (beats 2 and 3), then fret 8 (beat 4);
   - measure 3: frets 9, 10, 11, 12;
   - measure 4: frets 13, 14, 15, 16.

   On string 2 (B), use eighth notes:
   - measure 5: frets 1, 2, 3, 4, 5, 6;
   - measure 6: frets 7, 8, 9, 10, 11, 12.
4. Play the song once from the start. Every beat should sound, with no gaps except the held fret 6.

At 120 bpm a 4/4 beat (a quarter) lasts 0.5 seconds, and a 6/8 beat (an eighth) lasts 0.25 seconds. At 60% the tempo is
72 bpm, at 70% it is 84 and at 80% it is 96.

## Steps

The first 12 steps use Interleaved Practice. Turn Transport > Count In on for them. Steps 13 to 25 use Interleaved
Click-Up. Turn Transport > Count In off for them, because the click-up counts in by itself.

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Open Transport > Interleaved Practice.... Open the "Each turn plays" list. Choose "A beat", then "A measure", then "A beat" again. | The list holds "A measure", "A line", "A page" and "A beat". With "A beat" the pool fields read "Pool from (measures):" and "Pool to (measures):". With "A measure" they read "Pool from:" and "Pool to:" again. No label is cut off. | not run |
| 2 | With "A beat", try to set Pool to above 6, both with the arrows and by typing (for example 99). | The field stops at 6, the song's number of measures, not its 28 beats. | not run |
| 3 | Set "A beat", pool 1 to 1, Number of turns 4, "Complete cycle before reshuffle" checked, interval clock off. Press Start. | 4 turns, one for each beat of measure 1, in random order. Each turn is a Count In of 4 clicks (0.5 s apart), then the beat's own note, then the joint's note, then nothing until the next Count In. For example, beat 2 plays fret 2 then fret 3, and beat 1 plays fret 1 then fret 2. A turn never sounds a third note. | not run |
| 4 | Watch the Transport window's label during step 3. | It reads "Turn 1 of 4: Measure 1 beat 3" (the beat playing), then "Turn 2 of 4: Measure 1 beat ...", and so on. It names the beat you hear. | not run |
| 5 | Listen for the turn of beat 4 in step 3. Practise again if it has not come up yet. | Fret 4, then fret 5. Fret 5 is measure 2 beat 1, the joint, and it plays although measure 2 is not in the pool. | not run |
| 6 | Practise with pool 2 to 2 and 4 turns, "Complete cycle" checked. Listen for beats 2 and 3. | Beat 2 plays fret 6 for one beat only. The half note is cut where the joint starts, and the joint beat is silent, because no note starts on beat 3. Beat 3 plays a silent beat (fret 6 started before it), then fret 8. | not run |
| 7 | Practise with pool 4 to 4 and 4 turns. Listen for beat 4. | Fret 16 for half a second. Then the joint is measure 5 beat 1 in 6/8: fret 1 on the B string for only a quarter of a second (an eighth), then the next Count In. | not run |
| 8 | Practise with pool 5 to 6, 12 turns, "Complete cycle" checked. | Each turn's Count In is 6 quick clicks, 0.25 s apart: the eighths of 6/8. Then one eighth note and its joint, the next eighth. The label reads "Turn N of 12: Measure 5 beat 3" and so on. The turn of measure 6 beat 6, the song's last beat, plays fret 12 and no joint. | not run |
| 9 | Turn the Metronome on (the Transport window's Metronome button). Practise with pool 1 to 1 and 4 turns. Turn the Metronome off afterwards. | Every turn has 6 clicks, evenly 0.5 s apart: 4 Count In clicks, then a Metronome click on the beat and one on the joint beat. After the joint there is no click until the next Count In. | not run |
| 10 | Set the Percentage of Tempo to 50. Practise with pool 1 to 1 and 4 turns, and watch the main toolbar's tempo (the "= 120" beside the note). Set the Percentage back to 100 afterwards. | The toolbar reads "= 60" during the session. Count In clicks, beat and joint are each 1 s long. | not run |
| 11 | During a session of beats, open the Transport menu and look at the Transport window. | Repeat Step and Previous Step stay greyed out: they belong to Interleaved Click-Up. | not run |
| 12 | Set "A beat", pool 1 to 2, interval clock on, 1 minute per turn, a break of 10 seconds, a session of 2 minutes, "Count In before every pass" unchecked. Press Start, and watch the label. Then practise again with "Count In before every pass" checked. | One Count In, then the beat and its joint over and over, back to back (for example fret 2, fret 3, fret 2, fret 3, ...). The label reads "Turn 1: Measure 1 beat 2, 1:00 left" and counts down. The turn ends at 1:00, after 58 passes of 1 s that follow a 2-second Count In. Then 10 silent seconds read "Break, 0:10 left. Next: Measure M beat B", naming the beat that turn 2 then plays. Turn 2 follows, and the session ends. With a Count In before every pass, each pass is 4 clicks, the beat and the joint (3 s), so a turn plays 20 passes. | not run |
| 13 | Open Transport > Interleaved Click-Up.... Choose Each section is "A beat", then "A line", then "A beat" again. | With "A beat" the fields read "Passage from (measures):" and "Passage to (measures):", and Measures per line and Lines per page are greyed out. With "A line" the labels read "Passage from:" and "Passage to:", and Measures per line is enabled. No label is cut off. | not run |
| 14 | With "A beat", try to set Passage to above 6, with the arrows and by typing. Then try to set Passage to below Passage from. | Passage to stops at 6, the song's measures. It cannot go below Passage from. | not run |
| 15 | Set "A beat", passage 1 to 1, percent, Start tempo 60, Goal tempo 60, Increment 5, Passes per step 1, "Count In before each step" checked, "Count In before every pass" unchecked. Read the line under the fields. Choose "A measure", then "A beat" again. Then set Goal tempo 80 and Increment 10. | The line reads "4 steps", one for each beat of measure 1. With "A measure" it reads "1 step". Back with "A beat", Goal 80 by 10 makes "18 steps". | not run |
| 16 | Set Goal tempo back to 60 ("4 steps"). Press Start. | 4 steps, each a Count In of 4 clicks at 72 bpm, then its beats, then the joint. Step 1: fret 1, then fret 2. Step 2: frets 1, 2, then 3. Step 3: frets 1, 2, 3, then 4. Step 4: frets 1 to 4, then fret 5 (measure 2 beat 1, past the passage). Then the player stops by itself. | not run |
| 17 | Practise again as in step 16 and watch the label. | "Step 1 of 4: measure 1 beat 1 at 60%", then "Step 2 of 4: measure 1 beat 1 to measure 1 beat 2 at 60%", "Step 3 of 4: measure 1 beat 1 to measure 1 beat 3 at 60%" and "Step 4 of 4: measure 1 beat 1 to measure 1 beat 4 at 60%". | not run |
| 18 | Set Goal tempo 80 and Increment 10 ("18 steps"). Press Start. Watch the main toolbar's tempo and the number on the Transport window's Metronome button through the first 5 steps. | Steps 1 to 3 play beat 1 at 72, 84 and 96 bpm. Step 4 plays beats 1-2 at 72 and step 5 plays beat 2 at 84. Both numbers read 72, 84, 96, 72, 84 in turn, each from the step's Count In. The label of step 5 reads "Step 5 of 18: measure 1 beat 2 at 70%". | not run |
| 19 | Practise as in step 18. During step 5 (beat 2 at 70%), press Repeat Step. | At once a Count In at 84 bpm, then fret 2 and fret 3 again. The label stays "Step 5 of 18: measure 1 beat 2 at 70%". Then step 6 (beats 1-2 at 80%) follows. | not run |
| 20 | Practise as in step 18. During step 5, press Previous Step. Then pause during a step, press Previous Step, and press Play. | At once step 4: a Count In at 72 bpm, then frets 1, 2 and the joint, fret 3. The label reads "Step 4 of 18: measure 1 beat 1 to measure 1 beat 2 at 60%". While paused, Previous Step changes the label without sound. Play then plays that step from its Count In. | not run |
| 21 | Set passage 2 to 2, Start tempo 100, Goal tempo 100 ("4 steps"). Press Start. | Step 1: fret 5, then fret 6 for one beat only (the joint, cut at its end). Step 2: fret 5, fret 6 for one beat (cut where the joint starts), then a silent joint beat. Step 3: fret 5, then fret 6 held for two beats, then fret 8. Step 4: frets 5, 6 (two beats), 8, then fret 9 of measure 3. | not run |
| 22 | Set passage 4 to 4 (100 to 100, "4 steps"). Press Start and listen to step 4. | Frets 13 to 16, then the joint fret 1 on the B string for an eighth (a quarter of a second), then the session ends. | not run |
| 23 | Set passage 6 to 6 (100 to 100). Press Start. | Before Start the line reads "6 steps". Each Count In is 6 quick clicks, 0.25 s apart. Step 1 is fret 7, then fret 8. Step 6 plays frets 7 to 12 and no joint after them, because fret 12 is the song's last beat. The label of step 6 reads "Step 6 of 6: measure 6 beat 1 to measure 6 beat 6 at 100%". | not run |
| 24 | Turn the Metronome on. Practise passage 1 to 1, 60 to 60 ("4 steps"). Turn the Metronome off afterwards. | The Metronome clicks every beat at the step's tempo (72 bpm), the joint beat included. Step 1: 4 Count In clicks, a click on beat 1 and a click on the joint, 6 evenly spaced. Nothing clicks after the joint. | not run |
| 25 | Choose another unit again ("A measure") and practise passage 1 to 2, 60 to 60. | Measures play whole, with no joint, as before: step 1 is measure 1, step 2 is measures 1-2. The label reads "measure 1", then "measures 1–2". | not run |

## Notes

- The tester's choices that the owner may overrule, as announced: a note held from the last beat into the joint stops
  where the joint starts, and only notes that start on the joint sound there (steps 6 and 21). A joint note sounds its
  written length, cut at the end of the joint beat. Every pass of a timed turn includes the joint. Grace notes before a
  turn's first beat fall in its Count In, as they do for measures.
- The joint is the next beat in score order. After the last beat of a first ending, it is the second ending's first
  beat. The test song has no repeats, so this page does not check it.
- Interleaved Practice still needs Transport > Count In on to count in, as before. Interleaved Click-Up has its own
  Count In.
- The automated tests cover: the beats of a pool or passage and their order; the click-up's steps for beats; a beat's
  ticks and its joint; across a time-signature change; the song's end; the cut of held notes; the tempo of a
  measure; the Count In; the Metronome; the offline and live sound of beat turns, sample-exact; the interval clock's
  passes; Repeat Step and Previous Step. They also cover the label's wording keys (TestPracticeWording). This page checks
  what is seen and heard, and the dialogs.
- Not run by the developer. The GUI was built ("app: build") but not opened.
