---
title: Interleaved practice, interval clock
description: Timed turns that loop a randomly chosen section, a silent countdown break that names the next section, and a session of so many minutes.
sidebar:
  order: 6
---

## What it tests

Commits `5ae2e0661` to `4a04302cf` on `feature/interleaved-practice`, and `c04d4af15` to `e11d03f82` on
`feature/interleaved-click-up` ("Count In before every pass" only with "Count In before each turn"). With the interval clock on, each turn loops its
section for so many minutes, and the pass under way when the time is up plays to its end. A silent break follows,
counted down in the Transport window, which names the next section. The session ends at the end of a turn once its
minutes are used up.

## Setup

Build and launch with the VS Code task **app: build + run**. In Settings > Sound, choose the TuxGuitar Sequencer and the
TuxGuitar Synthesizer. Open or write a song of at least 16 measures at 120 bpm in 4/4 whose measures you can tell apart
by ear, for example a different note in each measure. A measure then lasts 2 seconds. Leave Transport > Count In off:
with "Count In before each turn" checked in the dialog (as it is until you uncheck it), each turn counts in by itself,
4 clicks (2 seconds). Open View > Transport so the Transport window shows.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Open Transport > Interleaved Practice.... | Below the two rules and "Count In before each turn" (checked) there is an "Interval clock" checkbox, then Minutes per turn, Break between turns (seconds), Session length (minutes) and "Count In before every pass". With the clock unchecked, those four are greyed out and Number of turns is enabled. | not run |
| 2 | Check "Interval clock". | Number of turns is greyed out; the four clock fields are enabled. | not run |
| 3 | Try to set Minutes per turn to 0 or above 60, the break below 0 or above 600, and the session to 0. Then set Minutes per turn to 5 and the session to 3. | Minutes per turn stays from 1 to 60 and the break from 0 to 600. The session cannot go below the minutes per turn: it shows 5, not 3. | not run |
| 4 | Set: each turn a measure, pool 1 to 4, interval clock on, 1 minute per turn, a break of 10 seconds, a session of 3 minutes, "Count In before every pass" unchecked. Start. | A Count In, then the same measure over and over, back to back, with no Count In between passes. | not run |
| 5 | Watch the Transport window during step 4's first turn. | Under the position: "Turn 1: Measure N, 1:00 left" (the measure playing), counting down each second. | not run |
| 6 | Wait for the turn's end. | The turn ends at 1:00 (29 passes of 2 seconds after a 2-second Count In). Then silence for 10 seconds, with "Break, 0:10 left. Next: Measure M" counting down, and the score's cursor on measure M. Then measure M's Count In and turn 2. | not run |
| 7 | Let the session run on. | 3 turns in all, a break between each two and none after the last. The player stops by itself about 3 minutes 20 seconds after Start, at the end of turn 3. No turn chooses the same measure as the one before. | not run |
| 8 | Practise again with each turn a line (lines of 4 measures), pool lines 1 to 4, the same clock. Watch the label at the end of the first turn. | A line lasts 8 seconds, so the minute runs out during the 8th pass. That pass plays to its end, and the turn lasts 1:06. The label shows the time to the real end, 1:06 at the start. | not run |
| 9 | Practise as in step 4 with "Count In before every pass" checked. | A Count In before every pass; a turn plays 15 passes of Count In plus measure (4 seconds each) and lasts 1:00. | not run |
| 10 | Check "Interval clock", then uncheck "Count In before each turn", then check it again. | Unchecked, "Count In before every pass" greys out (keeping its tick); checked again, it is enabled. | not run |
| 11 | Turn Transport > Count In on. Practise as in step 9 ("Count In before every pass" checked) with "Count In before each turn" unchecked. Afterwards turn Transport > Count In off and check "Count In before each turn" again. | No Count In at all, though Transport > Count In is on and "Count In before every pass" keeps its tick: the measure plays 30 times back to back, and the turn lasts 1:00. | not run |
| 12 | During a turn, press Pause, wait, then Play. | The measure playing at the pause plays again after a Count In, then the turn's remaining passes. The label's time left picks up about where it was when that pass started, plus the Count In. | not run |
| 13 | During a break, press Pause, wait, then Play. | The next turn's Count In starts at once; the rest of the break is skipped. | not run |
| 14 | During a break, click a measure in the score and choose Composition > Difficulty > 3x. | The session goes on. The next turn plays the measure the label named during the break (the flag counts from the next cycle). | not run |
| 15 | During a break, press Stop. Then practise again and, during a break, press the Transport window's next-measure button. | Stop ends the session. Next-measure ends it too, and the song plays on from there. | not run |
| 16 | Set the pool to measures 5 to 5 (one measure), clock on, a session of 3 minutes. Start. | The session starts (no "would come twice in a row" refusal): 3 turns of measure 5, with the breaks between them. | not run |
| 17 | Close the dialog after a start, then reopen it. | The clock settings are those of the last start. | not run |
| 18 | Uncheck "Interval clock" and practise 4 turns. | As before the clock: each turn plays its measure once, no breaks, and the label reads "Turn 1 of 4: Measure N". | not run |

## Notes

- A turn's time counts from the start of its Count In. A pass that starts before the time is up plays whole, so a
  turn may run a little over its minutes. The session's last turn is the one after which a further turn plus a break
  would not start before the session's minutes are up.
- The session's time is the time planned: a pause, and what plays again after it, do not count.
- With "No consecutive repeats" on, the check for a timed session counts the most turns it could hold (its minutes
  over a turn's minutes plus a break, rounded up). It may refuse a session that, with turns running over, would have
  had fewer turns.
- Changed with this slice: after a pause, Play now keeps the Percentage of Tempo the session started with. Before, it
  read Training Mode's start percentage again.
- Known and not to report: with a VST3 instrument, the note held when the player pauses or moves rings on, because
  the plugin ignores All Notes Off.
- The automated tests cover the passes, breaks, session end, takes, tails, Metronome, Pause, Stop and flags. This page
  checks what is seen and heard.
