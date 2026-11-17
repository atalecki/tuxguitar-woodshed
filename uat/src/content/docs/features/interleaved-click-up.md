---
title: Interleaved click-up
description: Climb a tempo ladder over a passage, adding its sections one at a time, from Transport > Interleaved Click-Up, with Repeat step and Previous step.
sidebar:
  order: 7
---

## What it tests

Commits `c2e36782b` to `64e5a8276` on `feature/interleaved-click-up` (the two owner fixes are `3bdcd4a62`, Count In before each step, and `64e5a8276`, the tempo heard). Interleaved click-up splits a passage into
sections (measures, lines or pages) and plays a series of steps. Each step plays a Count In at its tempo, then a run of
sections. The tempo climbs a ladder from a start to a goal, and the sections come in one at a time: the newest alone
every other step, between runs that reach back one section further each time. The Transport window shows the step,
and Repeat Step and Previous Step go back over the steps. Each step's Count In is the click-up's own ("Count In before
each step"), whether or not Transport > Count In is on. While a session plays, the main toolbar's tempo and the
Transport window's Metronome number show the tempo heard: the step's.

## Setup

Build and launch with the VS Code task **app: build + run**. In Settings > Sound, choose the TuxGuitar Sequencer and the
TuxGuitar Synthesizer. Open or write a song of at least 8 measures at 120 bpm in 4/4 whose measures you can tell apart by
ear, for example a different note in each measure. Leave Transport > Count In off: the click-up counts in by itself,
4 clicks in 4/4. Open View > Transport so the Transport window shows.

At 120 bpm written, 60% is 72 bpm (a measure lasts 3.3 seconds), 70% is 84 bpm (2.9 seconds) and 80% is 96 bpm
(2.5 seconds).

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Open the Transport menu. | Below "Interleaved Practice..." are "Interleaved Click-Up...", "Repeat Step" and "Previous Step". Repeat Step and Previous Step are greyed out. In the Transport window, under the position, the practice label is empty and the Previous Step and Repeat Step buttons beside it are greyed out. | not run |
| 2 | Choose Transport > Interleaved Click-Up.... | A dialog titled "Interleaved Click-Up": Each section is "A measure", Passage from 1, Passage to 4, Measures per line 4 and Lines per page 4 (both greyed out), Tempo given in "Percent of the written tempo", Start tempo 60, Goal tempo 100, Increment 5, Passes per step 1, "Count In before each step" checked, "Count In before every pass" unchecked, then the line "40 steps", then Start and Cancel. | not run |
| 3 | Set Passage to past the song's last measure, with the arrows and by typing (for example 99). | The field stops at the song's number of measures. | not run |
| 4 | Set Passage from to 3, then lower Passage to below 3. Then raise Passage from to 5. | Passage to cannot go below Passage from: it stops at 3. Raising Passage from to 5 raises Passage to to 5 with it. | not run |
| 5 | Choose Each section is "A line". Then "A page". Then "A measure" again. | With "A line", Measures per line is enabled and Lines per page greyed out; both passage fields stop at the song's number of lines (2 for a song of 8 measures, with lines of 4). With "A page" both group fields are enabled. With "A measure" both are greyed out again. | not run |
| 6 | Try to set Start tempo, Increment and Passes per step to 0. | Each stops at 1. | not run |
| 7 | Set Start tempo to 80 and try to set Goal tempo below 80. Then raise Start tempo to 120. | Goal tempo stops at 80 (a goal equal to the start is allowed). Raising the start to 120 raises the goal to 120 with it. | not run |
| 8 | Set the passage to measures 1 to 1, Start tempo 80, Goal tempo 80. | The line under the fields reads "1 step". | not run |
| 9 | Set the passage to measures 1 to 3, Start tempo 60, Goal tempo 80, Increment 10, Passes per step 1, Count In before each step checked, Count In before every pass unchecked. Check that Transport > Count In is off. | The line reads "11 steps", and it changes as soon as any of those fields changes. | not run |
| 10 | Press Start. | The dialog closes. A Count In of 4 clicks at 72 bpm, then measure 1 at 72 bpm (60%), although Transport > Count In is off. | not run |
| 11 | Listen to the whole session without touching anything. | 11 steps, each a Count In at its tempo then its measures, in this order: measure 1 at 60%, 70%, 80%; measures 1-2 at 60%; measure 2 at 70%; measures 1-2 at 80%; measures 1-3 at 60%; measure 3 at 70%; measures 2-3 at 80%; measure 3 at 80%; measures 1-3 at 80%. After the last, the player stops by itself, the label empties, and Repeat Step and Previous Step grey out again. | not run |
| 12 | Practise again as in step 9 and watch the Transport window's label. | It reads "Step 1 of 11: measure 1 at 60%", then "Step 2 of 11: measure 1 at 70%", ..., "Step 4 of 11: measures 1–2 at 60%", ..., "Step 9 of 11: measures 2–3 at 80%", ..., "Step 11 of 11: measures 1–3 at 80%". During a session Repeat Step and Previous Step are enabled, in the window and in the Transport menu. | not run |
| 13 | Open the dialog again. Choose Tempo given in "Beats per minute (bpm)". | The dialog opens with the settings of the last start. Switching to bpm sets Start tempo 72, Goal tempo 120 (the written tempo of measure 1), Increment 5, and the line reads "35 steps". Switching back to percent sets 60, 100, 5. | not run |
| 14 | In bpm, set Start tempo 72, Goal tempo 96, Increment 12 ("11 steps"). Start. | The same order of measures as in step 11, at 72, 84 and 96 bpm. The label reads "Step 1 of 11: measure 1 at 72 bpm", and later "Step 5 of 11: measure 2 at 84 bpm". | not run |
| 15 | Practise as in step 9 with Passes per step 2. | Each step plays its measures twice after one Count In: for example step 4 is a Count In, then measures 1, 2, 1, 2. | not run |
| 16 | Practise as in step 15 with "Count In before every pass" checked. | A Count In before each of the two passes of every step. | not run |
| 17 | Open the dialog. Uncheck "Count In before each step", then check it again. | Unchecked, "Count In before every pass" greys out (keeping its tick); checked again, it is enabled. | not run |
| 18 | Turn Transport > Count In on. Practise as in step 9 with "Count In before each step" unchecked. | No Count In at all, though Transport > Count In is on: each step starts right where the one before ended. | not run |
| 19 | In the Transport window, with Count In on, set the Count In ticks beside it to 2, then turn Count In off. Practise as in step 9 with "Count In before each step" checked. Afterwards turn Count In on, set the ticks back to 4 and turn it off again. | Every step has a Count In of 2 clicks at its tempo, though Count In is off. | not run |
| 20 | Check the Percentage of Tempo is 100 in the Transport window. Practise as in step 9 and watch the main toolbar's tempo ("= 120" beside the note) and the number on the Transport window's Metronome button. | During step 1 both read 72, during step 2 84, during step 3 96, during step 4 72 again (they follow the step under way, its Count In included). | not run |
| 21 | Let the session of step 20 end by itself. Then pause one during a step. | After the session both read the written tempo again: the toolbar "= 120". Paused, the toolbar shows the written tempo at the caret (120), as when the song is stopped. | not run |
| 22 | Set the Percentage of Tempo to 50. Practise as in step 9, then stop it and play the song with Play. Then start Interleaved Practice (Transport > Interleaved Practice...). Set the Percentage of Tempo back to 100 afterwards. | During the click-up the toolbar still reads 72, 84, 96: the Percentage of Tempo takes no part. Playing the song it reads 60 (120 at 50%). During Interleaved Practice it reads 60 too, the session's tempo. | not run |
| 23 | Practise as in step 9. During step 5 (measure 2 at 70%), press Repeat Step in the Transport window. Later, during a step, choose Transport > Repeat Step. | At once a Count In at 84 bpm, then step 5 again; the label stays "Step 5 of 11"; then steps 6 to 11 as before. The menu item does the same. | not run |
| 24 | Practise as in step 9. During step 5, press Previous Step. Then, during step 1 of another session, press Previous Step. | At once step 4 (measures 1–2 at 60%) from its Count In, the label "Step 4 of 11: measures 1–2 at 60%", then steps 5 to 11. During step 1, step 1 plays again from its Count In. | not run |
| 25 | Practise as in step 9. During step 5, press Pause. Press Previous Step, then Play. | Paused, the label still shows the session; Repeat Step and Previous Step stay enabled. After Previous Step the label reads "Step 4 of 11: measures 1–2 at 60%" and nothing plays. Play plays step 4 from its Count In, then steps 5 to 11. | not run |
| 26 | Practise as in step 9. During step 5, press Pause, then Repeat Step, then Play. Then pause again and press Play without anything else. | Each time Play plays the paused step again from its Count In, then the steps after it. | not run |
| 27 | Practise as in step 9. During a step, press Stop. | The player stops and the session ends: the label empties and Repeat Step and Previous Step grey out. | not run |
| 28 | Practise as in step 9. During a step, click a measure outside the step in the score, or press the Transport window's next-measure button. | The session ends, the label empties, and the song plays on from there. | not run |
| 29 | Practise as in step 9. During a step, open the Edit, Composition, Measure, Beat and Track menus, then pause and open them again. Try an edit, for example typing a fret number. | Items of edits are greyed out, playing and paused (as in Interleaved Practice). Composition > Difficulty, Repeat Step and Previous Step stay enabled. The edit does nothing and the session goes on. | not run |
| 30 | Practise as in step 9. During step 2, flag measure 2 as 3x (Composition > Difficulty). | The flag is taken (measure 2 shows 3x) and the session goes on in the same order as step 11: a flag changes nothing in a click-up. | not run |
| 31 | Choose another sequencer in Settings > Sound (for example the Java Sound one), then start a click-up. | An error message: "Interleaved click-up needs a sequencer that takes turns added while it plays, and ... does not: choose the TuxGuitar Sequencer in Settings > Sound." Interleaved Practice started the same way still says "Interleaved practice needs a sequencer ...". | not run |

## Notes

- Known and not to report: with a VST3 instrument, the note held when Repeat Step, Previous Step or Pause cuts a step
  short rings on, because the plugin ignores All Notes Off. It is fixed on its own branch.
- Smooth scrolling during a session now paces itself by the step's tempo, not the Percentage of Tempo (commit
  `04d878570`): checked in steps 20 to 24 of What a practice session lets through.
- The toolbar's tempo now rounds to the nearest bpm (125 at 75% shows 94), as the Transport window's always did.
- Repeat Step and Previous Step have no default keyboard shortcut. They can be given one in Tools > Shortcuts.
- A section that is a line or a page is a fixed group counted from measure 1 (Measures per line, Lines per page), as in
  Interleaved Practice, whatever the score's layout shows.
- The automated tests cover the order of the steps (the ladder and its tail), the tempo of each step and of its Count
  In, Count In before each step (on or off, Transport > Count In on or off, its ticks), the tempo scale heard step by
  step (MidiPlayer.getHeardScale), the passes, Count In before
  every pass, tempo changes inside a passage, a bpm ladder read against the passage's
  first measure, Pause, Repeat Step and Previous Step (playing and paused), Stop, a position move, an edit and a flag
  during a session, the session ending by itself, and the refusals. This page checks what is seen and heard.
