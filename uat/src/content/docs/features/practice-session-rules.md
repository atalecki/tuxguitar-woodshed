---
title: What a practice session lets through
description: During interleaved practice or click-up, the measure moves, mute, solo and markers keep the session going, Record is greyed out, and smooth scrolling keeps pace with the tempo heard.
sidebar:
  order: 9
---

## What it tests

Commits `964703206` to `d5ac22ef7` on `feature/practice-session-rules` (`9f0316b2b` markers, `bb627fde6` and `f56657d6e` mute, solo and markers keep the session, `f204bd2f7` measure moves, `5db26a143` Record), and `9c3a8cf18` to `04d878570` (smooth scrolling). During an Interleaved Practice or Interleaved Click-Up session,
playing or paused:

- Measure First, Previous, Next and Last move only the caret, and the session goes on. Before, while playing they moved
  the player, which ended the session.
- Muting or soloing a track, and adding, changing or removing a marker, keep the session going. Each is still one undo
  step, and undoing or redoing it keeps the session going too. Before, any of these ended the session.
- Tools > Record is greyed out, and Record refuses, saying why.
- Selecting stays refused, as before.
- With Transport > Continuous scrolling on, the score scrolls at the tempo heard, the one the main toolbar shows: the
  step's during a click-up, the session's during interleaved practice. Before, it scrolled at the Percentage of Tempo,
  so in a click-up at 72 bpm on a 120 song the view ran ahead of the sound. Outside a session nothing changes.

## Setup

Build and launch with the VS Code task **app: build + run**. In Settings > Sound, choose the TuxGuitar Sequencer and the
TuxGuitar Synthesizer, and choose an audio device in Tools > Audio Device.... Open or write a song of at least 8
measures at 120 bpm in 4/4, with two tracks, whose measures you can tell apart by ear, for example a different note in
each measure of track 1 and a steady note on every beat of track 2. Open View > Transport so the Transport window
shows, and Marker > List so the marker list shows.

"Start a practice session" below means Transport > Interleaved Practice..., each turn a measure, pool 1 to 8,
20 turns, Start. "Start a click-up" means Transport > Interleaved Click-Up..., each section a measure, passage 1 to 4,
Start tempo 60, Goal tempo 100, Increment 10, Start. While a session runs, the Transport window's practice label shows
the turn or step; it empties when the session ends.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Start a practice session. During a turn, choose Measure > Next (or press its shortcut). | The caret moves to the next measure; the turn you hear goes on, and the next turn follows after its Count In. The practice label still shows the turn. | not run |
| 2 | During the session, choose Measure > Previous, Measure > First and Measure > Last, one after the other. | Each moves only the caret (to the measure before, the first, the last). The turns go on as before, and the label still shows the turn. | not run |
| 3 | During the session, click the First, Previous, Next and Last measure buttons of the Transport window, then those of the main toolbar. | The same as step 2: only the caret moves, and the session goes on. | not run |
| 4 | During the session, choose Marker > Next, Marker > Previous, Marker > First and Marker > Last (add two markers first, outside a session, if the song has none). | Only the caret moves to the marker's measure; the session goes on. | not run |
| 5 | Pause the session (Transport > Pause). Repeat steps 1 and 3. Then press Play. | Paused, the caret moves and the label still shows the paused turn. Play plays the paused turn again from its Count In, then the session goes on. | not run |
| 6 | Stop. Without a session, play the song from measure 1 and choose Measure > Next while it plays. | The player jumps to the next measure, as before. | not run |
| 7 | Start a practice session. During a turn, mute track 1 (Track > Mute, or its box in the track table). | Track 1 goes silent at once; track 2 plays on. The session goes on: the turns keep coming and the label still shows the turn. | not run |
| 8 | During the session, unmute track 1, then solo track 2, then unsolo it. | Each change is heard at once (with track 2 soloed, track 1 is silent). The session goes on. | not run |
| 9 | During the session, choose Edit > Undo, twice. Then Edit > Redo. | Edit > Undo and Edit > Redo are not greyed out. Each undoes or redoes the last solo or mute, which you hear, and the session goes on. | not run |
| 10 | During the session, add a marker on the caret's measure (Marker > Add Marker..., a title, OK). | The marker shows on the score and in the marker list; the session goes on. | not run |
| 11 | During the session, change that marker's title (double-click it in the marker list, edit, OK), then remove it (the list's Remove). | The score and the list show the new title, then no marker; the session goes on. | not run |
| 12 | During the session, choose Edit > Undo until the marker is back, then Edit > Redo. | The marker comes back, then goes again; Undo and Redo stay enabled, and the session goes on. | not run |
| 13 | During the session, make an edit the session does not keep: for example Composition > Triplet Feel, or Edit > Undo after a note was entered before the session. | Those items are greyed out while the session is under way, playing or paused, as before. | not run |
| 14 | During the session, open the Tools menu. Then pause and open it again. | Record is greyed out, playing and paused. | not run |
| 15 | Before the session, add an audio track (Tools > Add Audio Track) and arm it, and open the Recording window. Start a practice session and look at the window's Record button, playing and paused. | Greyed out, playing and paused. Nothing is recorded, and the session goes on. | not run |
| 16 | Stop the session. Open the Tools menu. | Record is enabled again (with an armed track and a device). | not run |
| 17 | During the session, try to select: Edit > Select All, Shift with the arrow keys, or drag over the score. | Nothing is selected; the Select All item is greyed out. | not run |
| 18 | Start a click-up. Repeat steps 1, 5, 7, 10 and 14 during it (the label shows the step). | The same as for interleaved practice: the click-up goes on through each, and Record is greyed out. | not run |
| 19 | Start a practice session. During a turn, flag the caret's measure (Composition > Difficulty > 3x). Then Edit > Undo, then Edit > Redo. | The flag is taken and the session goes on; Undo and Redo are enabled and take the flag back and put it again, and the session still goes on. Before this change the app ended the session on a flag (see Notes). | not run |
| 20 | Stop any session. Check the Percentage of Tempo is 100 in the Transport window. Choose View > Layout > Linear Layout and zoom in until about three measures fill the window's width. Turn on Transport > Continuous scrolling (it is greyed out while playing). Play the song from measure 1 and watch the score for a few measures, then stop. | The score glides steadily to the left, the measure being played staying at about the same place near the left edge, in time with the sound. This is the pace to compare the next steps with. | not run |
| 21 | Start a click-up (60 to 100, Increment 10, as in Setup). Watch the score through each step, the toolbar's tempo ("= 72", "= 84" ... "= 120") telling which step plays. | At every step the score glides in time with the sound, the played measure staying near the left edge: slowest at 72, a little faster at each step, at 120 as fast as in step 20. It never races ahead and then waits for the sound, nor falls behind. | not run |
| 22 | Stop. Set the Percentage of Tempo to 50 and start a practice session. Watch the score during the turns, then stop. | Within each turn the score glides in time with the sound, at about half the pace of step 20 (the toolbar reads "= 60"). | not run |
| 23 | With the Percentage of Tempo still 50, start a click-up (as in step 21). | The same as step 21: each step scrolls at its own tempo (72 to 120), not at 50%; the toolbar reads the step's tempo. | not run |
| 24 | Stop. With the Percentage of Tempo still 50, play the song from measure 1 without a session. Then set the Percentage of Tempo back to 100 and turn Continuous scrolling off. | The score glides at half the pace of step 20, in time with the sound, as before this change. | not run |

## Notes

- Measure moves, mute, solo and markers change no note a turn plays, so the session goes on; Stop, moving the play
  position (clicking the timeline or the Transport window's position) and other edits still end it.
- The app keeps each action's edits, and those of the actions it runs, in one undo step. The session used to look
  only at bare edits, so in the app it ended on a flag too, though the tests said it went on; it now looks inside the
  step (step 19). Changing a marker in the Marker editor is one such step (a remove and an update): one Undo takes it
  back.
- If Record ever runs during a session, it refuses with a message: "Interleaved practice is under way: stop it to
  record." (or "Interleaved click-up ..."). The greyed menu item and button keep it from running by hand.
- The automated tests cover which edits keep a session (mute, solo, a marker added, changed or removed, and their undo
  and redo, live, in both modes), that the undo and redo of those edits are neither refused nor greyed out, that
  selecting stays refused, and that Record is refused saying why and greyed out. They cannot see the desktop's measure
  actions, menus or buttons: steps 1 to 6, 14, 15, 17 and 19 check those by hand.
- Smooth scrolling is desktop drawing, which no test can watch. The tests cover the tempo it now follows (the one the
  toolbar shows, the step's or the session's) and that, outside a session, it times a measure exactly as before for
  every Percentage of Tempo; steps 20 to 24 check the scrolling itself.
