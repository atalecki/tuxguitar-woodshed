@practice-workflow @interleaved-recall
Feature: Interleaved practice
  To learn a piece by heart, I practise its measures, lines or pages in a random order instead of from start to end,
  so that I recall each part on its own and not only from the part before it. A session plays a number of turns. Each
  turn is one unit of the song chosen from the units I practise (the pool), with a Count In before it in that unit's
  own time signature and tempo; the next turn's Count In starts as soon as a unit ends, until the last turn or Stop.
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

  @planned
  Scenario Outline: each turn plays one <unit> of the pool after a Count In of one measure, and the session stops by itself
    Given I have turned Count In on
    And I have set interleaved practice to <unit>s over <pool>, with lines of 4 measures and pages of 4 lines
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

  @planned
  Scenario Outline: each turn's Count In is in its first measure's time signature and tempo, at the Percentage of Tempo
    Given I have turned Count In on
    And I have set the time signature of measure 10 only to <signature>
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

  @planned
  Scenario: a line's Count In follows its first measure, not a measure later in the line
    Given I have turned Count In on
    And I have set the tempo from measure 10 to the end to 90 bpm
    And I have set interleaved practice to lines over lines 3 to 4, with lines of 4 measures
    When I practise 2 turns
    Then before the turn of line 3 the output holds 4 Count In clicks, 22050 samples apart within 1 sample
    And before the turn of line 4 the output holds 4 Count In clicks, 29400 samples apart within 1 sample

  @planned
  Scenario: Count In ticks set the length of every turn's Count In
    Given I have turned Count In on with 2 Count In ticks
    And I have set interleaved practice to measures over measures 1 to 4
    When I practise 3 turns
    Then before each turn the output holds 2 Count In clicks, 22050 samples apart within 1 sample

  @planned
  Scenario: the next turn's Count In starts as soon as a turn ends
    Given I have turned Count In on
    And I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    When I practise 3 turns
    Then the first Count In click of turns 2 and 3 sounds at the end of the turn before, within 1 sample

  @planned
  Scenario: with Count In off, each turn starts where the turn before ended
    Given I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    When I practise 3 turns
    Then the output holds no Count In clicks
    And turns 2 and 3 start at the end of the turn before, within 1 sample

  # Choosing the turns (no sound: the choosing alone, for many random seeds)

  Scenario: "No consecutive repeats" and "Complete cycle before reshuffle" are on until I turn them off
    When I set interleaved practice to lines over lines 1 to 4
    Then interleaved practice's "No consecutive repeats" is on
    And interleaved practice's "Complete cycle before reshuffle" is on

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

  Scenario: flagging a measure during a session is refused when the next cycles could not be played
    Given I have set interleaved practice to measures over measures 1 to 2
    When interleaved practice chooses 6 turns with random seed 1, while I flag measure 2 as 3x difficult during turn 1
    Then the flag is refused, saying "Measure 2 comes up 3 times in a cycle of 4 turns and would come twice in a row."
    And turns 1 to 6 choose measures 1 and 2 three times each
    And measure 2 is flagged 1x difficult

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

  @planned
  Scenario: a line plays its measures once, ignoring repeat signs inside it
    Given I have repeated measures 5 to 8 once
    And I have set interleaved practice to lines over lines 2 to 3, with lines of 4 measures
    When I practise 2 turns
    Then each turn sounds the notes of one line of lines 2 to 3, each measure once and in order, each within 1 sample of its beat

  @planned
  Scenario: a measure of a second ending plays on its own
    Given I have repeated measure 2 once, with measure 3 as the first ending and measure 4 as the second
    And I have set interleaved practice to measures over measures 3 to 4
    When I practise 2 turns
    Then each turn sounds the notes of one measure of measures 3 to 4, each within 1 sample of its beat

  @planned
  Scenario: TuxGuitar shows each turn's first measure during its Count In, then follows the turn as it sounds
    Given I have turned Count In on
    And I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    When I practise 3 turns
    Then during each turn's Count In TuxGuitar reports beat 1 of the turn's first measure
    And during each turn TuxGuitar reports each of its beats within 1 sample of the output sounding it

  # a turn's end is a jump in song time: the take fades out before it, and starts at full level after it
  @planned
  Scenario: an audio track's take fades out at the end of every turn but the last, and comes back at full level
    Given I have turned Count In on
    And I have added an audio track recording input channel 1
    And track 2 has a take of a constant level of 0.4 from measure 1 beat 1 to the end of measure 64
    And I have set interleaved practice to measures over measures 1 to 8
    When I practise 3 turns
    Then track 2 fades from 0.4 to 0 over the last 256 samples of turns 1 and 2, within 1 sample
    And track 2's first sample of each turn is 0.4

  @planned
  Scenario: an effect's tail rings on into the next turn's Count In
    Given I have turned Count In on
    And I have added the VST3 effect "Test Echo" to track 1
    And I have set interleaved practice to measures over measures 1 to 8
    When I practise 2 turns
    Then the output holds the echo of turn 1's last note 13230 samples after it, within 1 sample, during turn 2's Count In

  @planned
  Scenario: the Metronome clicks on every beat of every turn
    Given I have turned Count In on
    And I have turned the Metronome on
    And I have set interleaved practice to measures over measures 1 to 8
    When I practise 3 turns
    Then the output holds 12 Metronome clicks, each within 1 sample of a beat of a turn

  # The session and the transport

  @planned
  Scenario: Play looped and Training Mode take no part in a session, which plays at the Percentage of Tempo they start at
    Given I have turned Play looped on over measures 1 to 4
    And I have turned Training Mode on from 50% to 100% by 10% over measures 1 to 4
    And I have set interleaved practice to measures over measures 9 to 12
    When I practise 4 turns
    Then each turn sounds the notes of one measure of measures 9 to 12, each within 1 sample of its beat at 50%
    And every turn lasts 176400 samples, within 1 sample
    And the player has stopped by itself at the end of turn 4
    And Training Mode is on from 50% to 100% by 10% over measures 1 to 4

  @planned
  Scenario: Stop ends the session
    Given I have turned Count In on
    And I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    When I practise 6 turns and stop at turn 2's first measure beat 3
    Then the output holds 2 turns started
    And interleaved practice has ended

  @planned
  Scenario: after a session stopped, Play plays the song as written
    Given I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    And I have practised 6 turns and stopped at turn 2's first measure beat 3
    When I play the song from measure 1 beat 1 to measure 9 beat 1
    Then TuxGuitar reports each of the 32 beats within 1 sample of the output sounding it

  @planned
  Scenario: moving the position during a session ends it, and the song plays on from there
    Given I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    When I practise 6 turns live, moving the position to measure 40 beat 1 during turn 2
    Then the player plays on from measure 40 beat 1
    And interleaved practice has ended

  @planned
  Scenario: editing the song ends the session
    Given I have set interleaved practice to lines over lines 1 to 4, with lines of 4 measures
    When I practise 6 turns live, removing measure 40 during turn 2
    Then the player stopped within 512 samples of the edit, during turn 2
    And the song has 63 measures
    And interleaved practice has ended
