@practice-workflow @interleaved-recall
Feature: Interleaved practice
  To learn a piece by heart, I practise its measures, lines or pages in a random order instead of from start to end,
  so that I recall each part on its own and not only from the part before it. A session plays a number of turns. Each
  turn is one unit of the song chosen from the units I practise (the pool), or a block of so many of them in a row,
  with a Count In before it in that unit's own time signature and tempo, interleaved practice's own whether or not Count In is on; the next turn's Count In
  starts as soon as a unit ends, until the last turn or Stop.
  A line is a fixed group of measures and a page a fixed group of lines, both set for the session, whatever the
  score's layout shows. Measures I flag as difficult come up more often.

  Background:
    Given a new song with one guitar track and 64 measures of 4/4 at 120 bpm
    And I have set the MIDI Port to "TuxGuitar Synthesizer"
    And I have set the audio device to "Test Interface"
    And the VST3 folders hold the test plugins
    And I have chosen the VST3 instrument "Test Sine" for track 1
    And track 1 has a note on every beat, MIDI note 35 plus the measure's number

  # Units and the pool

  Scenario Outline: each turn plays one <unit> of the pool after a Count In of one measure, and the session stops by itself
    Given I have set interleaved practice to <unit>s over <pool>, with lines of 4 measures and pages of 4 lines
    When I practise 6 turns
    Then each turn sounds the notes of one <unit> of <pool>, each measure once and in order, each within 1 sample of its beat
    And before each turn the output holds 4 Count In clicks, 22050 samples apart within 1 sample
    And the last Count In click of each turn sounds 22050 samples before the turn's first measure, within 1 sample
    And the player has stopped by itself at the end of turn 6

    Examples:
      | unit    | pool             |
      | measure | measures 1 to 16 |
      | line    | lines 1 to 8     |
      | page    | pages 1 to 4     |

  Scenario Outline: lines and pages are fixed groups of measures, the last one holding what is left
    Given I have set interleaved practice to lines over lines 1 to 2, with lines of <per line> measures and pages of <per page> lines
    Then interleaved practice's line <line> is measures <line from> to <line to>
    And interleaved practice's page <page> is measures <page from> to <page to>

    Examples:
      | per line | per page | line | line from | line to | page | page from | page to |
      | 4        | 4        | 3    | 9         | 12      | 2    | 17        | 32      |
      | 4        | 4        | 16   | 61        | 64      | 4    | 49        | 64      |
      | 5        | 3        | 13   | 61        | 64      | 5    | 61        | 64      |
      | 1        | 1        | 64   | 64        | 64      | 64   | 64        | 64      |

  # the weight rule: with "No consecutive repeats" on, the heaviest unit's weight w and a cycle's length L (the weights
  # added up) allow up to L turns when 2w <= L + 1, and any number when 2w <= L
  Scenario Outline: a session that cannot be played as set is refused, saying why, and one that can is started
    Given I have flagged measure 1 as <flag> difficult
    And I have turned interleaved practice's "No consecutive repeats" <repeats rule>
    When I start interleaved practice of <unit>s over <pool> for <turns> turns, with lines of <per line> measures and pages of <per page> lines
    Then starting is <outcome>

    Examples:
      | flag | repeats rule | unit    | pool              | turns | per line | per page | outcome                                                                                       |
      | 1x   | on           | measure | measures 60 to 70 | 6     | 4        | 4        | refused, saying "The pool reaches past the song's 64 measures."                               |
      | 1x   | on           | measure | measures 1 to 64  | 6     | 4        | 4        | accepted                                                                                      |
      | 1x   | on           | line    | lines 9 to 8      | 6     | 4        | 4        | refused, saying "The pool is empty."                                                          |
      | 1x   | on           | page    | pages 1 to 5      | 6     | 4        | 4        | refused, saying "The pool reaches past the song's 4 pages."                                   |
      | 1x   | on           | page    | pages 1 to 4      | 6     | 4        | 4        | accepted                                                                                      |
      | 1x   | on           | measure | measures 5 to 5   | 2     | 4        | 4        | refused, saying "Measure 5 would come twice in a row."                                        |
      | 1x   | on           | measure | measures 5 to 5   | 1     | 4        | 4        | accepted                                                                                      |
      | 1x   | off          | measure | measures 5 to 5   | 6     | 4        | 4        | accepted                                                                                      |
      | 3x   | on           | measure | measures 1 to 3   | 5     | 4        | 4        | accepted                                                                                      |
      | 3x   | on           | measure | measures 1 to 3   | 6     | 4        | 4        | refused, saying "Measure 1 comes up 3 times in a cycle of 5 turns and would come twice in a row." |
      | 3x   | on           | measure | measures 1 to 4   | 12    | 4        | 4        | accepted                                                                                      |
      | 5x   | off          | measure | measures 1 to 4   | 6     | 4        | 4        | accepted                                                                                      |
      | 1x   | on           | measure | measures 1 to 4   | 0     | 4        | 4        | refused, saying "A session needs at least 1 turn."                                            |
      | 1x   | on           | line    | lines 1 to 2      | 6     | 0        | 4        | refused, saying "A line needs at least 1 measure."                                            |
      | 1x   | on           | page    | pages 1 to 2      | 6     | 4        | 0        | refused, saying "A page needs at least 1 line."                                               |

  # Count In

  Scenario Outline: each turn's Count In is in its first measure's time signature and tempo, at the Percentage of Tempo
    Given I have set the time signature of measure 10 only to <signature>
    And I have set the tempo of measure 10 only to <bpm> bpm
    And I have set the Percentage of Tempo to <percent>
    And I have set interleaved practice to measures over measures 9 to 10
    When I practise 2 turns
    Then before the turn of measure 10 the output holds <clicks> Count In clicks, <apart> samples apart within 1 sample
    And before the turn of measure 9 the output holds 4 Count In clicks, <apart 9> samples apart within 1 sample

    Examples:
      | signature | bpm | percent | clicks | apart | apart 9 |
      | 3/4       | 90  | 100     | 3      | 29400 | 22050   |
      | 6/8       | 120 | 100     | 6      | 11025 | 22050   |
      | 4/4       | 120 | 50      | 4      | 44100 | 44100   |

  Scenario: a line's Count In follows its first measure, not a measure later in the line
    Given I have set the tempo from measure 10 to the end to 90 bpm
    And I have set interleaved practice to lines over lines 3 to 4, with lines of 4 measures
    When I practise 2 turns
    Then before the turn of line 3 the output holds 4 Count In clicks, 22050 samples apart within 1 sample
    And before the turn of line 4 the output holds 4 Count In clicks, 29400 samples apart within 1 sample

  # a turn's Count In is interleaved practice's own, "Count In before each turn", on until I turn it off; Count In
  # (Transport > Count In) only lends it its length. The next turn's Count In starts as soon as a turn ends
  Scenario Outline: with "Count In before each turn" on, each turn has its Count In, Count In <count in> or not
    Given I have turned Count In <count in>
    And I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    When I practise 3 turns
    Then before each turn the output holds 4 Count In clicks, 22050 samples apart within 1 sample
    And the first Count In click of turns 2 and 3 sounds at the end of the turn before, within 1 sample

    Examples:
      | count in |
      | on       |
      | off      |

  Scenario: with "Count In before each turn" off, each turn starts where the turn before ended, even with Count In on
    Given I have turned Count In on
    And I have turned interleaved practice's "Count In before each turn" off
    And I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    When I practise 3 turns
    Then the output holds no Count In clicks
    And turns 2 and 3 start at the end of the turn before, within 1 sample

  Scenario Outline: Count In ticks set the length of every turn's Count In, Count In <count in> or not
    Given I have set the Count In ticks to 2
    And I have turned Count In <count in>
    And I have set interleaved practice to measures over measures 1 to 4
    When I practise 3 turns
    Then before each turn the output holds 2 Count In clicks, 22050 samples apart within 1 sample

    Examples:
      | count in |
      | on       |
      | off      |

  # live, as offline: the turn's own Count In, whatever Count In says
  Scenario Outline: live, with "Count In before each turn" on, each turn has its Count In, Count In <count in> or not
    Given I have turned Count In <count in>
    And I have set interleaved practice to measures over measures 1 to 4
    When I practise 3 turns live
    Then before each turn the output holds 4 Count In clicks, 22050 samples apart within 1 sample
    And the player stopped by itself within 512 samples of turn 3's end

    Examples:
      | count in |
      | on       |
      | off      |

  # the player sees the session's end at the end of a block of 512 samples, which falls up to two blocks after the turn
  # ends as heard: here turn 3 ends 0.8 of a block into one
  Scenario: live, with "Count In before each turn" off, each turn starts where the turn before ended, even with Count In on
    Given I have turned Count In on
    And I have turned interleaved practice's "Count In before each turn" off
    And I have set interleaved practice to measures over measures 1 to 4
    When I practise 3 turns live
    Then each turn sounds the notes of one measure of measures 1 to 4, each within 1 sample of its beat
    And the player stopped by itself within 1024 samples of turn 3's end

  # Choosing the turns (no sound: the choosing alone, for many random seeds)

  Scenario: "No consecutive repeats", "Complete cycle before reshuffle" and "Count In before each turn" are on until I turn them off
    When I set interleaved practice to lines over lines 1 to 4
    Then interleaved practice's "No consecutive repeats" is on
    And interleaved practice's "Complete cycle before reshuffle" is on
    And interleaved practice's "Count In before each turn" is on

  Scenario: every cycle visits each unit once, and no unit comes twice in a row, not even from one cycle to the next
    Given I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    When interleaved practice chooses 12 turns, with every random seed from 1 to 400
    Then turns 1 to 4, turns 5 to 8 and turns 9 to 12 each choose lines 1 to 4 once each
    And no two turns in a row choose the same line
    And over the seeds, turns 1 to 4 take each of the 24 orders of lines 1 to 4

  Scenario: without "No consecutive repeats", a cycle may start with the unit the cycle before ended with
    Given I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    And I have turned interleaved practice's "No consecutive repeats" off
    When interleaved practice chooses 8 turns, with every random seed from 1 to 200
    Then turns 1 to 4 and turns 5 to 8 each choose lines 1 to 4 once each
    And with at least one of the seeds, turns 4 and 5 choose the same line

  Scenario: without "Complete cycle before reshuffle", every turn chooses any unit but the one before, each as often
    Given I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    And I have turned interleaved practice's "Complete cycle before reshuffle" off
    When interleaved practice chooses 40000 turns, with random seed 1
    Then no two turns in a row choose the same line
    And each of lines 1 to 4 is chosen for 10000 turns, within 3%

  # Difficult measures

  Scenario: in every cycle, a flagged measure comes up as many times as its weight, never twice in a row
    Given I have flagged measure 12 as 3x difficult
    And I have flagged measure 27 as 3x difficult
    And I have set interleaved practice to measures over measures 1 to 32
    When interleaved practice chooses 72 turns, with every random seed from 1 to 200
    Then turns 1 to 36 and turns 37 to 72 each choose measures 12 and 27 three times each and every other measure of the pool once
    And no two turns in a row choose the same measure

  Scenario: a flagged measure is as many times as likely to come first in a cycle as its weight
    Given I have flagged measure 12 as 3x difficult
    And I have flagged measure 27 as 3x difficult
    And I have set interleaved practice to measures over measures 1 to 32
    When interleaved practice chooses 1 turn, with every random seed from 1 to 72000
    Then measure 12 is chosen with 6000 of the seeds, within 7%
    And measure 1 is chosen with 2000 of the seeds, within 12%

  Scenario: without cycles or the repeat rule, a flagged measure is chosen as many times as often as its weight
    Given I have flagged measure 12 as 3x difficult
    And I have flagged measure 27 as 3x difficult
    And I have set interleaved practice to measures over measures 1 to 32
    And I have turned interleaved practice's "Complete cycle before reshuffle" off
    And I have turned interleaved practice's "No consecutive repeats" off
    When interleaved practice chooses 36000 turns, with random seed 1
    Then measures 12 and 27 are each chosen for 3000 turns, within 7%
    And every other measure of the pool is chosen for 1000 turns, within 15%

  Scenario: flagging a measure during a session changes the turns from the next cycle on
    Given I have set interleaved practice to measures over measures 1 to 4
    When interleaved practice chooses 10 turns with random seed 1, while I flag measure 2 as 3x difficult during turn 2
    Then turns 1 to 4 choose measures 1 to 4 once each
    And turns 5 to 10 choose measure 2 three times and measures 1, 3 and 4 once each
    And no two turns in a row choose the same measure
    And measure 2 is flagged 3x difficult

  # live, the player chooses a turn while the turn before plays: a flag set during a cycle's last turn still counts from
  # the next cycle, none of whose turns has started
  Scenario: flagging a measure during a cycle's last turn, live, changes the turns from the next cycle on
    Given I have set interleaved practice to measures over measures 1 to 4
    When I practise 10 turns live, flagging measure 2 as 3x difficult during turn 4
    Then turns 1 to 4 choose measures 1 to 4 once each
    And turns 5 to 10 choose measure 2 three times and measures 1, 3 and 4 once each
    And no two turns in a row choose the same measure
    And measure 2 is flagged 3x difficult

  Scenario: flagging a measure during a session is refused when the next cycles could not be played
    Given I have set interleaved practice to measures over measures 1 to 2
    When interleaved practice chooses 6 turns with random seed 1, while I flag measure 2 as 3x difficult during turn 1
    Then the flag is refused, saying "Measure 2 comes up 3 times in a cycle of 4 turns and would come twice in a row."
    And turns 1 to 6 choose measures 1 and 2 three times each
    And measure 2 is flagged 1x difficult

  # undoing or redoing a flag during a session is a flag: it follows the same rule
  Scenario: undoing a flag during a session changes the turns from the next cycle on
    Given I have flagged measure 2 as 3x difficult
    And I have set interleaved practice to measures over measures 1 to 4
    When interleaved practice chooses 10 turns with random seed 1, while I undo during turn 2
    Then turns 1 to 6 choose measure 2 three times and measures 1, 3 and 4 once each
    And turns 7 to 10 choose measures 1 to 4 once each
    And no two turns in a row choose the same measure
    And measure 2 is flagged 1x difficult

  Scenario: redoing a flag during a session changes the turns from the next cycle on
    Given I have flagged measure 2 as 3x difficult
    And I have undone it
    And I have set interleaved practice to measures over measures 1 to 4
    When interleaved practice chooses 10 turns with random seed 1, while I redo during turn 2
    Then turns 1 to 4 choose measures 1 to 4 once each
    And turns 5 to 10 choose measure 2 three times and measures 1, 3 and 4 once each
    And no two turns in a row choose the same measure
    And measure 2 is flagged 3x difficult

  Scenario Outline: undoing or redoing a flag during a session is refused when the next cycles could not be played
    Given I have flagged measure 2 as 3x difficult
    And I have <taken it back>
    And I have set interleaved practice to measures over measures 1 to 2
    When interleaved practice chooses 6 turns with random seed 1, while I <edit> during turn 1
    Then the <edit> is refused, saying "Measure 2 comes up 3 times in a cycle of 4 turns and would come twice in a row."
    And turns 1 to 6 choose measures 1 and 2 three times each
    And measure 2 is flagged 1x difficult

    Examples:
      | taken it back                     | edit |
      | flagged measure 2 as 1x difficult | undo |
      | undone it                         | redo |

  Scenario Outline: a line or a page is as difficult as its most difficult measure
    Given I have flagged measure 11 as 3x difficult
    And I have flagged measure 12 as 2x difficult
    When I set interleaved practice to <unit>s over <pool>, with lines of 4 measures and pages of 4 lines
    Then interleaved practice's <unit> <number> weighs <weight>

    Examples:
      | unit | pool         | number | weight |
      | line | lines 1 to 4 | 3      | 3x     |
      | line | lines 1 to 4 | 2      | 1x     |
      | page | pages 1 to 4 | 1      | 3x     |
      | page | pages 1 to 4 | 2      | 1x     |

  Scenario: undoing a flag takes it back
    Given I have flagged measure 12 as 3x difficult
    When I undo
    Then measure 12 is flagged 1x difficult

  Scenario: redoing a flag puts it back
    Given I have flagged measure 12 as 3x difficult
    And I have undone it
    When I redo
    Then measure 12 is flagged 3x difficult

  Scenario: flags are saved with the song, in a file older versions still open
    Given I have flagged measure 12 as 3x difficult
    And I have flagged measure 27 as 5x difficult
    When I save the song as a TuxGuitar file
    Then it is saved as file format 2.0
    And a reader of file format 2.0 opens it with 64 measures
    And once I have opened it again, measure 12 is flagged 3x difficult, measure 27 5x and measure 13 1x

  Scenario Outline: a measure can be flagged from 1x to 5x difficult, nothing else
    When I flag measure 12 as <flag> difficult
    Then the action is refused
    And measure 12 is flagged 1x difficult

    Examples:
      | flag |
      | 0x   |
      | 6x   |

  # Playing a unit

  Scenario: a line plays its measures once, ignoring repeat signs inside it
    Given I have repeated measures 5 to 8 once
    And I have set interleaved practice to lines over lines 2 to 3, with lines of 4 measures
    When I practise 2 turns
    Then each turn sounds the notes of one line of lines 2 to 3, each measure once and in order, each within 1 sample of its beat

  Scenario: a measure of a second ending plays on its own
    Given I have repeated measure 2 once, with measure 3 as the first ending and measure 4 as the second
    And I have set interleaved practice to measures over measures 3 to 4
    When I practise 2 turns
    Then each turn sounds the notes of one measure of measures 3 to 4, each within 1 sample of its beat

  Scenario: a measure of an ending no pass reaches still plays on its own
    Given I have repeated measure 2 once, with measure 3 as the first ending and measure 4 as a third, which no pass reaches
    And I have set interleaved practice to measures over measures 3 to 4
    When I practise 2 turns
    Then each turn sounds the notes of one measure of measures 3 to 4, each within 1 sample of its beat
    And the player has stopped by itself at the end of turn 2

  Scenario: TuxGuitar shows each turn's first measure during its Count In, then follows the turn as it sounds
    Given I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    When I practise 3 turns
    Then during each turn's Count In TuxGuitar reports beat 1 of the turn's first measure
    And during each turn TuxGuitar reports each of its beats within 1 sample of the output sounding it

  # a turn's end is a jump in song time: the take fades out before it, and starts at full level after it
  Scenario: an audio track's take fades out at the end of every turn but the last, and comes back at full level
    Given I have added an audio track recording input channel 1
    And track 2 has a take of a constant level of 0.4 from measure 1 beat 1 to the end of measure 64
    And I have set interleaved practice to measures over measures 1 to 8
    When I practise 3 turns
    Then track 2 fades from 0.4 to 0 over the last 256 samples of turns 1 and 2, within 1 sample
    And track 2's first sample of each turn is 0.4

  # live, as offline: the takes follow the turns the player lays; they play late by plugin delay compensation, as the
  # notes do, so what the output sounds is read as heard. Track 2 alone sounds on the output's right side, but for the
  # Count In clicks, which start where a turn ends and still ring a little where its first measure starts
  Scenario: live, an audio track's take fades out at the end of every turn but the last, and comes back at full level
    Given I have set the Balance of track 1's channel to 0
    And I have added an audio track recording input channel 1
    And I have set the Balance of track 2's channel to 127
    And track 2 has a take of a constant level of 0.4 from measure 1 beat 1 to the end of measure 64
    And I have set interleaved practice to measures over measures 1 to 8
    When I practise 3 turns live
    Then the output's right side fades from 0.4 to 0 over the last 256 samples of turns 1 and 2, within 1 sample
    And the output's right side steps up by 0.4 on the first sample of each turn

  Scenario: an effect's tail rings on into the next turn's Count In
    Given I have added the VST3 effect "Test Echo" to track 1
    And I have set interleaved practice to measures over measures 1 to 8
    When I practise 2 turns
    Then the output holds the echo of turn 1's last note 13230 samples after it, within 1 sample, during turn 2's Count In

  Scenario: the Metronome clicks on every beat of every turn
    Given I have turned the Metronome on
    And I have set interleaved practice to measures over measures 1 to 8
    When I practise 3 turns
    Then the output holds 12 Metronome clicks, each within 1 sample of a beat of a turn

  # The session and the transport

  Scenario: Play looped and Training Mode take no part in a session, which plays at the Percentage of Tempo they start at
    Given I have turned Play looped on over measures 1 to 4
    And I have turned Training Mode on from 50% to 100% by 10% over measures 1 to 4
    And I have set interleaved practice to measures over measures 9 to 12
    When I practise 4 turns
    Then each turn sounds the notes of one measure of measures 9 to 12, each within 1 sample of its beat at 50%
    And every turn lasts 176400 samples, within 1 sample
    And the player has stopped by itself at the end of turn 4
    And Training Mode is on from 50% to 100% by 10% over measures 1 to 4

  Scenario: Stop ends the session
    Given I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    When I practise 6 turns and stop at turn 2's first measure beat 3
    Then the output holds 2 turns started
    And interleaved practice has ended

  Scenario: after a session stopped, Play plays the song as written
    Given I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    And I have practised 6 turns and stopped at turn 2's first measure beat 3
    When I play the song from measure 1 beat 1 to measure 9 beat 1
    Then TuxGuitar reports each of the 32 beats within 1 sample of the output sounding it

  Scenario: Pause keeps the session: Play plays the paused turn again from its Count In, then the turns after it
    Given I have set interleaved practice to measures over measures 1 to 4
    When I practise 3 turns live, pausing during turn 2 and playing again
    Then after Play, turn 2's measure sounds again after a Count In of 4 beats, then turn 3's after another
    And the player stopped by itself within 512 samples of turn 3's end, 16 beats after Play

  Scenario: moving the position during a session ends it, and the song plays on from there
    Given I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    When I practise 6 turns live, moving the position to measure 40 beat 1 during turn 2
    Then the player plays on from measure 40 beat 1
    And interleaved practice has ended

  Scenario: editing the song ends the session
    Given I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    When I practise 6 turns live, removing measure 40 during turn 2
    Then the player stopped within 512 samples of the edit, during turn 2
    And the song has 63 measures
    And interleaved practice has ended

  # mute, solo and markers change no note a turn plays: the session goes on through them, each still an undo step, and
  # through their undo and redo. Had the edit ended it, the player would have stopped at the edit (see above)
  Scenario Outline: <edit> during a session keeps it, and the turns play on
    Given I have added the marker "Intro" at measure 2
    And I have set interleaved practice to measures over measures 1 to 4
    When I practise 3 turns live, <edit> during turn 2
    Then the player stopped by itself within 512 samples of turn 3's end
    And track 1 reads "<track>" and the markers read "<markers>"

    Examples:
      | edit                                         | track | markers            |
      | muting track 1                               | mute  | m2 Intro           |
      | soloing track 1                              | solo  | m2 Intro           |
      | adding the marker "Verse" at measure 3       | -     | m2 Intro, m3 Verse |
      | renaming the marker of measure 2 to "Chorus" | -     | m2 Chorus          |
      | removing the marker of measure 2             | -     |                    |

  Scenario Outline: undoing <an edit> during a session keeps it too
    Given I have <made it>
    And I have set interleaved practice to measures over measures 1 to 4
    When I practise 3 turns live, undoing during turn 2
    Then the player stopped by itself within 512 samples of turn 3's end
    And track 1 reads "-" and the markers read ""

    Examples:
      | an edit  | made it                               |
      | a mute   | muted track 1                         |
      | a solo   | soloed track 1                        |
      | a marker | added the marker "Intro" at measure 2 |

  Scenario Outline: redoing <an edit> during a session keeps it too
    Given I have <made it>
    And I have undone it
    And I have set interleaved practice to measures over measures 1 to 4
    When I practise 3 turns live, redoing during turn 2
    Then the player stopped by itself within 512 samples of turn 3's end
    And track 1 reads "<track>" and the markers read "<markers>"

    Examples:
      | an edit  | made it                               | track | markers  |
      | a mute   | muted track 1                         | mute  |          |
      | a solo   | soloed track 1                        | solo  |          |
      | a marker | added the marker "Intro" at measure 2 | -     | m2 Intro |

  # Tools > Record is greyed out during a session, playing or paused; the action refuses too, and the session goes on
  Scenario: Record during a session is refused, saying why, and the session goes on
    Given I have added an audio track recording input channel 1
    And I have armed track 2
    And I have set interleaved practice to measures over measures 1 to 4
    When I practise 3 turns live, choosing Record during turn 2
    Then Record is refused, saying "Interleaved practice is under way: stop it to record."
    And the player stopped by itself within 512 samples of turn 3's end
    And track 2's takes read ""

  # The interval clock: timed turns that loop their unit, with a break between turns, for a session of so many minutes.
  # At 120 bpm in 4/4 a measure lasts 2 seconds (88200 samples), a line of 4 measures 8 seconds, a page 32 seconds, and
  # a Count In of 4 clicks 2 seconds. A turn starts with its first Count In click (or, with "Count In before each turn"
  # off, its first measure) and ends where its last pass ends.

  Scenario: the interval clock is off until I turn it on, set to turns of 3 minutes, breaks of 30 seconds and sessions of 30 minutes
    When I set interleaved practice to lines over lines 1 to 4
    Then interleaved practice's interval clock is off, set to turns of 3 minutes, breaks of 30 seconds and sessions of 30 minutes
    And "Count In before every pass" is off

  # a pass starts only before the turn's time is up, and then plays to its end. Without a Count In before each turn there
  # is none to put before every pass (the dialog greys "Count In before every pass" out), Count In on or not
  Scenario Outline: with the interval clock, a turn loops its <unit> until the turn's time is up, and the pass under way plays to its end
    Given I have turned Count In <count in>
    And I have turned interleaved practice's "Count In before each turn" <each turn>
    And I have turned "Count In before every pass" <every pass>
    And I have set the Percentage of Tempo to <percent>
    And I have set interleaved practice to <unit>s over <pool>, with lines of 4 measures and pages of 4 lines
    And I have turned the interval clock on, with turns of 1 minute, breaks of 10 seconds and sessions of 1 minute
    When I practise the session
    Then the output holds 1 turn started
    And the turn sounds the notes of one <unit> of <pool> <passes> times back to back, each measure once and in order every time, each within 1 sample of its beat at <percent>%
    And the output holds <count ins>
    And the turn ends <samples> samples after it started, within 1 sample
    And the player has stopped by itself within 512 samples of the turn's end

    # seconds the turn lasts: 60, 66, 64, 60, 64, 66, 68
    Examples:
      | unit    | pool             | count in | each turn | every pass | percent | passes | count ins                                         | samples |
      | measure | measures 1 to 4  | off      | on        | off        | 100     | 29     | a Count In of 4 clicks before the first pass only | 2646000 |
      | line    | lines 1 to 4     | on       | on        | off        | 100     | 8      | a Count In of 4 clicks before the first pass only | 2910600 |
      | line    | lines 1 to 4     | on       | off       | off        | 100     | 8      | no Count In clicks                                | 2822400 |
      | line    | lines 1 to 4     | off      | on        | on         | 100     | 6      | a Count In of 4 clicks before every pass          | 2646000 |
      | line    | lines 1 to 4     | on       | off       | on         | 100     | 8      | no Count In clicks                                | 2822400 |
      | page    | pages 1 to 4     | off      | on        | off        | 100     | 2      | a Count In of 4 clicks before the first pass only | 2910600 |
      | line    | lines 1 to 4     | off      | on        | off        | 50      | 4      | a Count In of 4 clicks before the first pass only | 2998800 |

  # a pass of 3.5 seconds (a Count In of 3 clicks, then a measure): pass 18 starts at 59.5 seconds, before the time is up
  Scenario: a pass whose Count In starts before the turn's time is up plays whole, its Count In and its measures
    Given I have set the Count In ticks to 3
    And I have turned "Count In before every pass" on
    And I have set interleaved practice to measures over measures 1 to 4
    And I have turned the interval clock on, with turns of 1 minute, breaks of 10 seconds and sessions of 1 minute
    When I practise the session
    Then the turn sounds the notes of one measure of measures 1 to 4 18 times, each after a Count In of 3 clicks
    And the turn ends 2778300 samples after it started, within 1 sample

  # line 1: measures 1 and 2 at 120 bpm (2 seconds each), 3 and 4 at 60 bpm (4 seconds each), 12 seconds a pass
  Scenario: a pass lasts as long as its measures take at their own tempos
    Given I have set the tempo from measure 3 to the end to 60 bpm
    And I have set interleaved practice to lines over lines 1 to 1, with lines of 4 measures
    And I have turned the interval clock on, with turns of 1 minute, breaks of 10 seconds and sessions of 1 minute
    When I practise the session
    Then the turn sounds the notes of line 1 5 times back to back, each measure once and in order every time, each within 1 sample of its beat
    And the turn ends 2734200 samples after it started, within 1 sample

  # another turn follows only when the turn's end and its break come before the session's time is up; the session
  # ends at the end of a turn, never in a break (session ends at 142, 218 and 132 seconds)
  Scenario Outline: the session's time decides the last turn, which plays to its end, with no break after it
    Given I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    And I have turned the interval clock on, with turns of 1 minute, breaks of <break> seconds and sessions of <session> minutes
    When I practise the session
    Then the output holds <turns> turns started
    And each turn sounds the notes of one line of lines 1 to 4 8 times back to back, after a Count In of 4 clicks
    And each turn but the first starts <break samples> samples after the turn before ended, within 1 sample, the output silent in between
    And the session's last turn ends <end> samples after the first one started, within 1 sample
    And the player has stopped by itself within 512 samples of the session's last turn's end

    Examples:
      | break | break samples | session | turns | end     |
      | 10    | 441000        | 2       | 2     | 6262200 |
      | 10    | 441000        | 3       | 3     | 9613800 |
      | 0     | 0             | 2       | 2     | 5821200 |

  Scenario: a session whose time is up exactly at a turn's end ends there
    Given I have set interleaved practice to measures over measures 1 to 4
    And I have turned the interval clock on, with turns of 1 minute, breaks of 0 seconds and sessions of 2 minutes
    When I practise the session
    Then the output holds 2 turns started
    And the session's last turn ends 5292000 samples after the first one started, within 1 sample

  # with the interval clock, a session's one unit comes back turn after turn: "No consecutive repeats" cannot apply
  Scenario: with the interval clock, a pool of one unit plays it every turn
    Given I have set interleaved practice to lines over lines 5 to 5, with lines of 4 measures
    And I have turned the interval clock on, with turns of 1 minute, breaks of 10 seconds and sessions of 3 minutes
    When I practise the session
    Then the output holds 3 turns started
    And each turn sounds the notes of line 5 8 times back to back, after a Count In of 4 clicks

  Scenario: during a break TuxGuitar shows the next turn's first measure, then follows each pass as it sounds
    Given I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    And I have turned the interval clock on, with turns of 1 minute, breaks of 10 seconds and sessions of 2 minutes
    When I practise the session
    Then during the break TuxGuitar reports beat 1 of turn 2's first measure
    And during each turn TuxGuitar reports each of its beats within 1 sample of the output sounding it
    And the output holds 2 turns started

  # the time left is rounded up to the second, and counts to the turn's end (the pass under way included)
  Scenario: live, TuxGuitar reports the turn and the time left in it, and during a break the next turn and the time left in the break
    Given I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    And I have turned the interval clock on, with turns of 1 minute, breaks of 10 seconds and sessions of 2 minutes
    When I practise the session live
    Then TuxGuitar reports, so many seconds after the first turn started:
      | seconds | report                                                         |
      | 1       | turn 1, the line turn 1 sounds, 65 seconds left                |
      | 65.5    | turn 1, the line turn 1 sounds, 1 second left                  |
      | 66.5    | the break before turn 2, the line turn 2 sounds, 10 seconds left |
      | 75.5    | the break before turn 2, the line turn 2 sounds, 1 second left |
      | 76.5    | turn 2, the line turn 2 sounds, 66 seconds left                |

  # live, the next turn is chosen when its break starts and shown all through it: a flag set during the break counts
  # from the cycle after the one that turn opens (turns 3 and 4 make the second cycle)
  Scenario: flagging a measure during a break keeps the turn shown, and changes the turns from the cycle after
    Given I have set interleaved practice to measures over measures 1 to 2
    And I have turned the interval clock on, with turns of 1 minute, breaks of 10 seconds and sessions of 5 minutes
    When I practise the session live, flagging measure 2 as 2x difficult during the break before turn 3
    Then turn 3 sounds the measure TuxGuitar reported during that break
    And turns 1 and 2, and turns 3 and 4, choose measures 1 and 2 once each
    And the output holds 5 turns started
    And measure 2 is flagged 2x difficult

  # a timed session of turns of 1 minute without breaks for 6 minutes can hold 6 turns: 4 after the first cycle
  Scenario: flagging a measure during a timed session is refused when the turns it can still hold could not be played
    Given I have set interleaved practice to measures over measures 1 to 2
    And I have turned the interval clock on, with turns of 1 minute, breaks of 0 seconds and sessions of 6 minutes
    When interleaved practice chooses its turns with random seed 1, while I flag measure 2 as 3x difficult during turn 1
    Then the flag is refused, saying "Measure 2 comes up 3 times in a cycle of 4 turns and would come twice in a row."
    And turns 1 to 6 choose measures 1 and 2 three times each
    And measure 2 is flagged 1x difficult

  # each pass's end is a jump in song time, as a turn's end is
  Scenario: an audio track's take fades out at the end of every pass but the session's last, and comes back at full level
    Given I have added an audio track recording input channel 1
    And track 2 has a take of a constant level of 0.4 from measure 1 beat 1 to the end of measure 64
    And I have set interleaved practice to measures over measures 1 to 8
    And I have turned the interval clock on, with turns of 1 minute, breaks of 10 seconds and sessions of 2 minutes
    When I practise the session
    Then track 2 fades from 0.4 to 0 over the last 256 samples of every pass but the session's last, within 1 sample
    And track 2's first sample of each pass is 0.4

  Scenario: an effect's tail rings on into the break
    Given I have added the VST3 effect "Test Echo" to track 1
    And I have set interleaved practice to measures over measures 1 to 8
    And I have turned the interval clock on, with turns of 1 minute, breaks of 10 seconds and sessions of 2 minutes
    When I practise the session
    Then the output holds the echo of turn 1's last note 13230 samples after it, within 1 sample, during the break

  # 2 turns of 29 passes of 4 beats
  Scenario: the Metronome clicks on every beat of every pass, and not in the break
    Given I have turned the Metronome on
    And I have set interleaved practice to measures over measures 1 to 8
    And I have turned the interval clock on, with turns of 1 minute, breaks of 10 seconds and sessions of 2 minutes
    When I practise the session
    Then the output holds 232 Metronome clicks, each within 1 sample of a beat of a pass
    And the output holds no Metronome click during the break

  # with the interval clock the weight rule counts the most turns the session can hold: its time over a turn's time
  # plus a break, rounded up (a turn lasts at least its time, a break follows every turn but the last); a pool of one
  # unit is not held to the rule
  Scenario Outline: a timed session that cannot be played as set is refused, saying why, and one that can is started
    Given I have flagged measure 1 as <flag> difficult
    And I have turned interleaved practice's "No consecutive repeats" <repeats rule>
    When I start interleaved practice of measures over <pool> with the interval clock, with turns of <turn> minutes, breaks of <break> seconds and sessions of <session> minutes
    Then starting is <outcome>

    Examples:
      | flag | repeats rule | pool             | turn | break | session | outcome                                                                                      |
      | 2x   | on           | measures 1 to 2  | 1    | 0     | 3       | accepted                                                                                     |
      | 2x   | on           | measures 1 to 2  | 1    | 0     | 4       | refused, saying "Measure 1 comes up 2 times in a cycle of 3 turns and would come twice in a row." |
      | 2x   | on           | measures 1 to 2  | 1    | 30    | 4       | accepted                                                                                     |
      | 2x   | on           | measures 1 to 2  | 1    | 10    | 4       | refused, saying "Measure 1 comes up 2 times in a cycle of 3 turns and would come twice in a row." |
      | 5x   | off          | measures 1 to 2  | 1    | 0     | 30      | accepted                                                                                     |
      | 1x   | on           | measures 1 to 1  | 1    | 0     | 30      | accepted                                                                                     |
      | 1x   | on           | measures 1 to 2  | 0    | 10    | 4       | refused, saying "A turn needs at least 1 minute."                                            |
      | 1x   | on           | measures 1 to 2  | 1    | -1    | 4       | refused, saying "A break cannot be shorter than 0 seconds."                                  |
      | 1x   | on           | measures 1 to 2  | 2    | 10    | 1       | refused, saying "A session needs at least the minutes of one turn."                          |
      | 1x   | on           | measures 1 to 2  | 2    | 10    | 2       | accepted                                                                                     |

  Scenario: Pause during a timed turn keeps the session: Play plays the paused pass again from a Count In, then the turn's passes left
    Given I have set interleaved practice to measures over measures 1 to 4
    And I have turned the interval clock on, with turns of 1 minute, breaks of 10 seconds and sessions of 1 minute
    When I practise the session live, pausing during pass 3 and playing again
    Then after Play, the turn's measure sounds again after a Count In of 4 beats, then 26 more times back to back
    And the player stopped by itself within 512 samples of the turn's end, 56 seconds after Play

  Scenario: Pause during a break keeps the session: Play starts the next turn's Count In at once
    Given I have set interleaved practice to measures over measures 1 to 4
    And I have turned the interval clock on, with turns of 1 minute, breaks of 10 seconds and sessions of 2 minutes
    When I practise the session live, pausing 5 seconds into the break and playing again
    Then after Play, turn 2's Count In of 4 beats starts within 512 samples
    And the player stopped by itself within 512 samples of turn 2's end, 60 seconds after Play

  Scenario: Stop during a break ends the session
    Given I have set interleaved practice to measures over measures 1 to 4
    And I have turned the interval clock on, with turns of 1 minute, breaks of 10 seconds and sessions of 2 minutes
    When I practise the session live and stop 5 seconds into the break
    Then the output holds 1 turn started
    And interleaved practice has ended

  Scenario: moving the position during a break ends the session, and the song plays on from there
    Given I have set interleaved practice to measures over measures 1 to 4
    And I have turned the interval clock on, with turns of 1 minute, breaks of 10 seconds and sessions of 2 minutes
    When I practise the session live, moving the position to measure 40 beat 1 5 seconds into the break
    Then the player plays on from measure 40 beat 1
    And interleaved practice has ended

  # A beat and its joint (README, A beat; The joint): a beat is the unit of its measure's time signature; a turn plays
  # its beat, then the joint, the notes that start on the next beat, for one beat. The pool is a range of measures. At
  # 120 bpm in 4/4 a beat lasts 22050 samples, and the Background's notes are quarter notes. A beat reads as `m3b2`.

  Scenario Outline: with beats as the unit, the pool's measures give their beats, in score order
    Given I have set the time signature of measure 4 only to <signature>
    When I start interleaved practice of beats over measures 3 to 4 for 1 turn
    Then interleaved practice's pool is the <count> beats "<beats>"

    Examples:
      | signature | count | beats                                                      |
      | 4/4       | 8     | m3b1, m3b2, m3b3, m3b4, m4b1, m4b2, m4b3, m4b4             |
      | 6/8       | 10    | m3b1, m3b2, m3b3, m3b4, m4b1, m4b2, m4b3, m4b4, m4b5, m4b6 |

  Scenario: with beats as the unit, every cycle chooses each beat of the pool once, never the same beat twice in a row
    Given I have set interleaved practice to beats over measures 3 to 4
    When interleaved practice chooses 16 turns, with every random seed from 1 to 200
    Then turns 1 to 8 and turns 9 to 16 each choose the beats of measure 3 once each and those of measure 4 once each
    And no two turns in a row choose the same beat

  Scenario: a beat is as difficult as its measure
    Given I have flagged measure 4 as 3x difficult
    When I start interleaved practice of beats over measures 3 to 4 for 1 turn
    Then interleaved practice's beats weigh "1x, 1x, 1x, 1x, 3x, 3x, 3x, 3x"

  Scenario: in every cycle, each beat of a flagged measure comes up as many times as the measure's weight, never twice in a row
    Given I have flagged measure 4 as 2x difficult
    And I have set interleaved practice to beats over measures 3 to 4
    When interleaved practice chooses 24 turns, with every random seed from 1 to 200
    Then turns 1 to 12 and turns 13 to 24 each choose the beats of measure 3 once each and those of measure 4 twice each
    And no two turns in a row choose the same beat

  Scenario Outline: a pool of beats is a range of measures, refused in measures when it does not lie in the song
    When I start interleaved practice of beats over <pool> for 6 turns
    Then starting is <outcome>

    Examples:
      | pool              | outcome                                                         |
      | measures 60 to 70 | refused, saying "The pool reaches past the song's 64 measures." |
      | measures 0 to 2   | refused, saying "The pool starts before measure 1."             |
      | measures 9 to 8   | refused, saying "The pool is empty."                            |
      | measures 1 to 64  | accepted                                                        |

  # the beats of one measure weigh alike: only a measure of one beat (1/4, here measure 2) can hold more than half a
  # cycle. Measures 1 and 2 give 5 beats; with measure 2 flagged 5x a cycle is 9 turns
  Scenario Outline: a session of beats whose weights cannot keep "No consecutive repeats" is refused, naming the beat
    Given I have set the time signature of measure 2 only to 1/4
    And I have flagged measure 2 as <flag> difficult
    When I start interleaved practice of beats over <pool> for <turns> turns
    Then starting is <outcome>

    Examples:
      | flag | pool              | turns | outcome                                                                                                  |
      | 1x   | measures 2 to 2   | 2     | refused, saying "Measure 2 beat 1 would come twice in a row."                                            |
      | 1x   | measures 2 to 2   | 1     | accepted                                                                                                 |
      | 5x   | measures 1 to 2   | 9     | accepted                                                                                                 |
      | 5x   | measures 1 to 2   | 10    | refused, saying "Measure 2 beat 1 comes up 5 times in a cycle of 9 turns and would come twice in a row." |

  Scenario: each turn plays its beat, then the joint's note, and lasts two beats
    Given I have set interleaved practice to beats over measures 3 to 4
    When I practise 8 turns
    Then each turn sounds the note of one beat of measures 3 to 4, then the note of the beat after it, each within 1 sample of its beat
    And every turn lasts 44100 samples, within 1 sample
    And the player has stopped by itself at the end of turn 8

  # beat 1: measure 3's half note is cut at the joint, on which no note starts; beat 2: that half note, started before
  # the beat, is not played, and the joint plays only the note on its first tick, not the one half way through it;
  # beat 4: the joint's half note sounds for the joint's beat
  Scenario Outline: the joint sounds only the notes on its first tick, and a turn only the notes that start in it
    Given track 1 has these notes in measures 3 and 4 instead of one on every beat:
      | position | MIDI note | length         |
      | m3b1     | 60        | a half note    |
      | m3b3     | 62        | an eighth note |
      | m3b3.5   | 64        | an eighth note |
      | m3b4     | 65        | a quarter note |
      | m4b1     | 66        | a half note    |
      | m4b3     | 67        | a half note    |
    And I have set interleaved practice to beats over measures 3 to 3
    When I practise 4 turns
    Then the turn of m3b<beat> sounds <notes>, each within 1 sample, and nothing else above -60 dBFS
    And the turn of m3b<beat> lasts 44100 samples, within 1 sample

    Examples:
      | beat | notes                                                         |
      | 1    | MIDI note 60 from m3b1 to m3b2                                |
      | 2    | MIDI note 62 from m3b3 to m3b3.5                              |
      | 3    | MIDI note 62 from m3b3 to m3b3.5, 64 to m3b4, then 65 to m4b1 |
      | 4    | MIDI note 65 from m3b4 to m4b1, then 66 to m4b2               |

  Scenario Outline: the joint plays even when its beat lies outside the pool, and the song's last beat has none
    Given I have set interleaved practice to beats over measures <measure> to <measure>
    When I practise 4 turns
    Then the turn of m<measure>b4 sounds <notes>, each within 1 sample of its beat
    And the turn of m<measure>b4 lasts <samples> samples, within 1 sample

    Examples:
      | measure | notes                           | samples |
      | 4       | MIDI note 39, then MIDI note 40 | 44100   |
      | 64      | MIDI note 99 alone              | 22050   |

  # in score order the first ending's measure 3 is followed by the second ending's measure 4, not by measure 2
  Scenario: the joint after a first ending's last beat is the second ending's first beat
    Given I have repeated measure 2 once, with measure 3 as the first ending and measure 4 as the second
    And I have set interleaved practice to beats over measures 3 to 3
    When I practise 4 turns
    Then the turn of m3b4 sounds MIDI note 38, then MIDI note 39, each within 1 sample of its beat

  # the joint is a beat of measure 4, an eighth in 6/8 (11025 samples): its note is cut at the eighth's end
  Scenario: the joint lasts a beat of its own measure: an eighth when a 4/4 measure is followed by one in 6/8
    Given I have set the time signature of measure 4 only to 6/8
    And I have set interleaved practice to beats over measures 3 to 3
    When I practise 4 turns
    Then the turn of m3b4 sounds MIDI note 38 from m3b4 to m4b1, then MIDI note 39 to m4b2, each within 1 sample
    And the turn of m3b4 lasts 33075 samples, within 1 sample

  Scenario Outline: a beat's Count In is in its measure's time signature and tempo, at the Percentage of Tempo
    Given I have set the time signature of measure 10 only to <signature>
    And I have set the tempo of measure 10 only to <bpm> bpm
    And I have set the Percentage of Tempo to <percent>
    And I have set interleaved practice to beats over measures 10 to 10
    When I practise 2 turns
    Then before each turn the output holds <clicks> Count In clicks, <apart> samples apart within 1 sample
    And the last Count In click of each turn sounds <apart> samples before the turn's beat, within 1 sample

    Examples:
      | signature | bpm | percent | clicks | apart |
      | 3/4       | 90  | 100     | 3      | 29400 |
      | 6/8       | 120 | 100     | 6      | 11025 |
      | 4/4       | 120 | 50      | 4      | 44100 |

  # 3 turns of a beat and its joint: 6 beats
  Scenario: the Metronome clicks on each turn's beat and on its joint's
    Given I have turned the Metronome on
    And I have set interleaved practice to beats over measures 1 to 2
    When I practise 3 turns
    Then the output holds 6 Metronome clicks, each within 1 sample of a beat of a turn

  Scenario: TuxGuitar shows each turn's beat during its Count In, then follows the beat and its joint as they sound
    Given I have set interleaved practice to beats over measures 3 to 4
    When I practise 3 turns
    Then during each turn's Count In TuxGuitar reports the turn's beat
    And during each turn TuxGuitar reports its beat and its joint's, each within 1 sample of the output sounding it

  # a Count In of 4 clicks (2 seconds), then passes of a beat and its joint (1 second each): pass 58 starts at 59
  # seconds, before the turn's time is up
  Scenario: with the interval clock, a turn loops its beat, every pass with its joint
    Given I have set interleaved practice to beats over measures 3 to 3
    And I have turned the interval clock on, with turns of 1 minute, breaks of 10 seconds and sessions of 1 minute
    When I practise the session
    Then the output holds 1 turn started
    And the turn sounds the note of one beat of measure 3, then the note of the beat after it, 58 times back to back, each within 1 sample of its beat
    And the turn ends 2646000 samples after it started, within 1 sample

  Scenario: live, each turn plays its beat, then the joint's note, after a Count In of one measure
    Given I have set interleaved practice to beats over measures 3 to 4
    When I practise 4 turns live
    Then each turn sounds the note of one beat of measures 3 to 4, then the note of the beat after it, each within 1 sample of its beat
    And before each turn the output holds 4 Count In clicks, 22050 samples apart within 1 sample
    And the player stopped by itself within 512 samples of turn 4's end

  Scenario: an audio track's take plays through the joint, and fades out over its last 256 samples
    Given I have added an audio track recording input channel 1
    And track 2 has a take of a constant level of 0.4 from measure 1 beat 1 to the end of measure 64
    And I have set interleaved practice to beats over measures 1 to 2
    When I practise 3 turns
    Then every turn lasts 44100 samples, within 1 sample
    And track 2 fades from 0.4 to 0 over the last 256 samples of turns 1 and 2, within 1 sample
    And track 2's first sample of each turn is 0.4

  # Blocks (README, Block; Units per block): each turn plays a block, a run of so many units of the pool in a row, and
  # the pool's blocks are every such run, in score order, sliding by one unit: the random choice, the weights, the rules
  # and the interval clock take a block as they take a unit. A block reads as its first and last unit, `2-3`, a block of
  # beats as its first and last beat, `m3b4-m4b1`, and a block of one unit as that unit, `3`

  Scenario: a turn plays a block of one unit until I change it
    When I set interleaved practice to lines over lines 1 to 4
    Then interleaved practice's units per block is 1

  # beats count through the barline, in score order: measure 3 in 3/4 gives 3 beats, then measure 4's 4 follow
  Scenario Outline: with units per block at <n>, the pool's blocks are every run of <n> of its <unit>s in a row
    Given I have set the time signature of measure 3 only to <signature>
    And I have set interleaved practice's units per block to <n>
    When I start interleaved practice of <unit>s over <pool> for 1 turn, with lines of 2 measures and pages of 2 lines
    Then interleaved practice's pool is the <count> blocks "<blocks>"

    Examples:
      | signature | unit    | pool            | n | count | blocks                                                                      |
      | 4/4       | measure | measures 1 to 6 | 2 | 5     | 1-2, 2-3, 3-4, 4-5, 5-6                                                     |
      | 4/4       | measure | measures 1 to 6 | 3 | 4     | 1-3, 2-4, 3-5, 4-6                                                          |
      | 4/4       | measure | measures 1 to 3 | 1 | 3     | 1, 2, 3                                                                     |
      | 4/4       | measure | measures 5 to 7 | 3 | 1     | 5-7                                                                         |
      | 4/4       | line    | lines 1 to 4    | 2 | 3     | 1-2, 2-3, 3-4                                                               |
      | 4/4       | page    | pages 1 to 3    | 2 | 2     | 1-2, 2-3                                                                    |
      | 4/4       | beat    | measures 3 to 4 | 2 | 7     | m3b1-m3b2, m3b2-m3b3, m3b3-m3b4, m3b4-m4b1, m4b1-m4b2, m4b2-m4b3, m4b3-m4b4 |
      | 3/4       | beat    | measures 3 to 4 | 2 | 6     | m3b1-m3b2, m3b2-m3b3, m3b3-m4b1, m4b1-m4b2, m4b2-m4b3, m4b3-m4b4            |
      | 3/4       | beat    | measures 3 to 4 | 3 | 5     | m3b1-m3b3, m3b2-m4b1, m3b3-m4b2, m4b1-m4b3, m4b2-m4b4                       |

  # the weight rule counts blocks: measures 1 to 4 in blocks of 2 with measure 1 flagged 3x weigh 3x, 1x and 1x, a
  # cycle of 5 turns
  Scenario Outline: a session whose blocks cannot be played as set is refused, saying why, and one that can is started
    Given I have flagged measure 1 as <flag> difficult
    And I have set interleaved practice's units per block to <n>
    When I start interleaved practice of <unit>s over <pool> for <turns> turns
    Then starting is <outcome>

    Examples:
      | flag | n | unit    | pool            | turns | outcome                                                                                                           |
      | 1x   | 0 | measure | measures 1 to 4 | 6     | refused, saying "A block needs at least 1 measure."                                                               |
      | 1x   | 5 | measure | measures 1 to 4 | 6     | refused, saying "A block of 5 measures does not fit in the pool's 4 measures."                                    |
      | 1x   | 5 | beat    | measures 3 to 3 | 6     | refused, saying "A block of 5 beats does not fit in the pool's 4 beats."                                          |
      | 1x   | 4 | measure | measures 1 to 4 | 1     | accepted                                                                                                          |
      | 1x   | 4 | measure | measures 1 to 4 | 2     | refused, saying "The block of measures 1–4 would come twice in a row."                                            |
      | 3x   | 2 | measure | measures 1 to 4 | 5     | accepted                                                                                                          |
      | 3x   | 2 | measure | measures 1 to 4 | 6     | refused, saying "The block of measures 1–2 comes up 3 times in a cycle of 5 turns and would come twice in a row." |
      | 1x   | 2 | beat    | measures 3 to 3 | 6     | accepted                                                                                                          |

  Scenario: every cycle chooses each block once, and only the same block is a repeat, not one that overlaps
    Given I have set interleaved practice to measures over measures 1 to 4
    And I have set interleaved practice's units per block to 2
    When interleaved practice chooses 9 turns, with every random seed from 1 to 200
    Then turns 1 to 3, turns 4 to 6 and turns 7 to 9 each choose the blocks "1-2, 2-3, 3-4" once each
    And no two turns in a row choose the same block
    And with at least one of the seeds, two turns in a row choose blocks that overlap

  # a flag weighs every block that holds its measure
  Scenario Outline: a block is as difficult as its most difficult measure
    Given I have flagged measure 4 as 3x difficult
    And I have set interleaved practice's units per block to 2
    When I start interleaved practice of <unit>s over <pool> for 1 turn
    Then interleaved practice's blocks weigh "<weights>"

    Examples:
      | unit    | pool            | weights                    |
      | measure | measures 2 to 6 | 1x, 3x, 3x, 1x             |
      | beat    | measures 3 to 4 | 1x, 1x, 1x, 3x, 3x, 3x, 3x |

  Scenario Outline: each turn plays its block of <unit>s, each measure once and in order, after its Count In
    Given I have set interleaved practice to <unit>s over <pool>, with lines of 2 measures and pages of 2 lines
    And I have set interleaved practice's units per block to 2
    When I practise 3 turns
    Then each turn sounds the notes of one block of 2 <unit>s of <pool>, each measure once and in order, each within 1 sample of its beat
    And before each turn the output holds 4 Count In clicks, 22050 samples apart within 1 sample
    And the player has stopped by itself at the end of turn 3

    Examples:
      | unit    | pool            |
      | measure | measures 1 to 4 |
      | line    | lines 1 to 4    |
      | page    | pages 1 to 3    |

  # the 7 blocks of 2 beats of measures 3 and 4 (above), m3b4-m4b1 through the barline, each played as its 2 beats and
  # the joint. (A time signature changed here would push the Background's notes on into the next measures, as changing
  # one does in TuxGuitar, so these scenarios keep 4/4.)
  Scenario: a block of beats plays through the barline, then the joint's note
    Given I have set interleaved practice to beats over measures 3 to 4
    And I have set interleaved practice's units per block to 2
    When I practise 7 turns
    Then the turns sound each block of 2 beats of measures 3 to 4 once, its beats, then the note of the beat after it, each within 1 sample of its beat
    And every turn lasts 66150 samples, within 1 sample
    And the player has stopped by itself at the end of turn 7

  # the block m3b4-m4b1: measure 3's last note sounds on through the barline to the joint (a beat alone, m3b4, would cut
  # it at m4b1), and the joint, m4b2, plays only the note on its first tick
  Scenario: a note held through the barline inside a block of beats sounds on to the joint
    Given track 1 has these notes in measures 3 and 4 instead of one on every beat:
      | position | MIDI note | length         |
      | m3b4     | 65        | a half note    |
      | m4b2     | 66        | an eighth note |
      | m4b2.5   | 67        | an eighth note |
    And I have set interleaved practice to beats over measures 3 to 4
    And I have set interleaved practice's units per block to 2
    When I practise 7 turns
    Then the turn of m3b4 sounds MIDI note 65 from m3b4 to m4b2, then 66 to m4b2.5, each within 1 sample, and nothing else above -60 dBFS
    And the turn of m3b4 lasts 66150 samples, within 1 sample

  Scenario: live, each turn plays its block of measures after its Count In
    Given I have set interleaved practice to measures over measures 1 to 4
    And I have set interleaved practice's units per block to 2
    When I practise 3 turns live
    Then each turn sounds the notes of one block of 2 measures of measures 1 to 4, each measure once and in order, each within 1 sample of its beat
    And before each turn the output holds 4 Count In clicks, 22050 samples apart within 1 sample
    And the player stopped by itself within 512 samples of turn 3's end

  Scenario: live, a block of beats plays through the barline, then the joint's note
    Given I have set interleaved practice to beats over measures 3 to 4
    And I have set interleaved practice's units per block to 2
    When I practise 7 turns live
    Then the turns sound each block of 2 beats of measures 3 to 4 once, its beats, then the note of the beat after it, each within 1 sample of its beat
    And before each turn the output holds 4 Count In clicks, 22050 samples apart within 1 sample
    And the player stopped by itself within 512 samples of turn 7's end

  # a pass of a block of 2 measures lasts 4 seconds: after a Count In of 2 seconds, pass 15 starts at 58 seconds
  Scenario: with the interval clock, a turn loops its block until the turn's time is up
    Given I have set interleaved practice to measures over measures 1 to 3
    And I have set interleaved practice's units per block to 2
    And I have turned the interval clock on, with turns of 1 minute, breaks of 10 seconds and sessions of 1 minute
    When I practise the session
    Then the output holds 1 turn started
    And the turn sounds the notes of one block of 2 measures of measures 1 to 3 15 times back to back, each measure once and in order every time, each within 1 sample of its beat at 100%
    And the turn ends 2734200 samples after it started, within 1 sample
