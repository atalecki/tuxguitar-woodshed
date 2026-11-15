@practice-workflow @click-up
Feature: Interleaved click-up
  To bring a passage up to speed, I climb a tempo ladder, from a slow start to my goal, adding the passage's sections
  one at a time (Molly Gebrian's "interleaved clicking up"). With one section in play I play it at each rung of the
  ladder. Then, with two, I play both at the first rung, the second alone at the next, both at the next, and so on; with
  more, the section just added is played on its own every other step, between runs that reach back one section further
  each time, until all of them play together. When the ladder runs out before all of them have, the steps go on at the
  goal until they do. Each step has a Count In at its tempo, and the next step follows by itself; the session ends after
  the last one, the whole passage at the goal. A section is a measure, a line or a page, as in interleaved practice.

  Background:
    Given a new song with one guitar track and 64 measures of 4/4 at 120 bpm
    And I have set the MIDI Port to "TuxGuitar Synthesizer"
    And I have set the audio device to "Test Interface"
    And the VST3 folders hold the test plugins
    And I have chosen the VST3 instrument "Test Sine" for track 1
    And track 1 has a note on every beat, MIDI note 35 plus the measure's number

  # The steps and their order (no sound: the steps alone)

  Scenario: the tempo ladder runs from 60% to 100% by 5%, with 1 pass a step, until I change it
    When I set interleaved click-up to measures over measures 1 to 4
    Then interleaved click-up's ladder runs from 60% to 100% by 5%, with 1 pass a step
    And interleaved click-up's "Count In before every pass" is off

  # the ladder: start, start + increment, ... up to the goal, and the goal itself when the increment does not land on it
  # (60% to 72% by 5%: 60, 65, 70, 72). With N sections in play, step i (from 0) plays at rung i the set
  #   N = 1: 1
  #   N = 2: 1-2, 2, 1-2, 2, ...
  #   N = 3: 1-3, 3, 2-3, 3, 1-3, ...
  #   N = 4: 1-4, 4, 3-4, 4, 2-4, 4, 1-4, ...
  # and after the last rung the pattern goes on at the goal until it has played 1-N (the ladder's tail)
  Scenario Outline: the steps add the sections one at a time, each new one alone every other step, and end with all of them at the goal
    When I start interleaved click-up of measures over <sections>, from <start> to <goal> by <increment>
    Then interleaved click-up's steps are "<steps>"

    Examples:
      | sections         | start  | goal    | increment | steps                                                                                                                                                                |
      | measures 1 to 1  | 60%    | 80%     | 5%        | 1@60%, 1@65%, 1@70%, 1@75%, 1@80%                                                                                                                                     |
      | measures 1 to 3  | 60%    | 80%     | 5%        | 1@60%, 1@65%, 1@70%, 1@75%, 1@80%, 1-2@60%, 2@65%, 1-2@70%, 2@75%, 1-2@80%, 1-3@60%, 3@65%, 2-3@70%, 3@75%, 1-3@80%                                                   |
      | measures 1 to 2  | 60%    | 75%     | 5%        | 1@60%, 1@65%, 1@70%, 1@75%, 1-2@60%, 2@65%, 1-2@70%, 2@75%, 1-2@75%                                                                                                   |
      | measures 1 to 2  | 60%    | 72%     | 5%        | 1@60%, 1@65%, 1@70%, 1@72%, 1-2@60%, 2@65%, 1-2@70%, 2@72%, 1-2@72%                                                                                                   |
      | measures 1 to 4  | 60%    | 70%     | 5%        | 1@60%, 1@65%, 1@70%, 1-2@60%, 2@65%, 1-2@70%, 1-3@60%, 3@65%, 2-3@70%, 3@70%, 1-3@70%, 1-4@60%, 4@65%, 3-4@70%, 4@70%, 2-4@70%, 4@70%, 1-4@70%                         |
      | measures 1 to 3  | 80%    | 80%     | 5%        | 1@80%, 1-2@80%, 1-3@80%                                                                                                                                               |
      | measures 1 to 2  | 60%    | 100%    | 50%       | 1@60%, 1@100%, 1-2@60%, 2@100%, 1-2@100%                                                                                                                              |
      | measures 1 to 2  | 60 bpm | 120 bpm | 30 bpm    | 1@60 bpm, 1@90 bpm, 1@120 bpm, 1-2@60 bpm, 2@90 bpm, 1-2@120 bpm                                                                                                      |

  Scenario Outline: a click-up that cannot be played as set is refused, saying why, and one that can is started
    When I start interleaved click-up of <unit>s over <sections>, from <start> to <goal> by <increment>, with <passes> passes a step, with lines of <per line> measures and pages of <per page> lines
    Then starting is <outcome>

    Examples:
      | unit    | sections          | start  | goal    | increment | passes | per line | per page | outcome                                                          |
      | measure | measures 60 to 70 | 60%    | 100%    | 5%        | 1      | 4        | 4        | refused, saying "The passage reaches past the song's 64 measures." |
      | page    | pages 1 to 5      | 60%    | 100%    | 5%        | 1      | 4        | 4        | refused, saying "The passage reaches past the song's 4 pages."     |
      | line    | lines 9 to 8      | 60%    | 100%    | 5%        | 1      | 4        | 4        | refused, saying "The passage is empty."                            |
      | line    | lines 0 to 2      | 60%    | 100%    | 5%        | 1      | 4        | 4        | refused, saying "The passage starts before line 1."                |
      | page    | pages 1 to 4      | 60%    | 100%    | 5%        | 1      | 4        | 4        | accepted                                                         |
      | measure | measures 1 to 4   | 0%     | 100%    | 5%        | 1      | 4        | 4        | refused, saying "The ladder starts below 1%."                      |
      | measure | measures 1 to 4   | 0 bpm  | 120 bpm | 5 bpm     | 1      | 4        | 4        | refused, saying "The ladder starts below 1 bpm."                   |
      | measure | measures 1 to 4   | 1%     | 100%    | 5%        | 1      | 4        | 4        | accepted                                                         |
      | measure | measures 1 to 4   | 80%    | 75%     | 5%        | 1      | 4        | 4        | refused, saying "The ladder's goal is below its start."            |
      | measure | measures 1 to 4   | 80%    | 80%     | 5%        | 1      | 4        | 4        | accepted                                                         |
      | measure | measures 1 to 4   | 60%    | 100%    | 0%        | 1      | 4        | 4        | refused, saying "The ladder climbs by less than 1%."               |
      | measure | measures 1 to 4   | 60 bpm | 120 bpm | 0 bpm     | 1      | 4        | 4        | refused, saying "The ladder climbs by less than 1 bpm."            |
      | measure | measures 1 to 4   | 60%    | 100%    | 1%        | 1      | 4        | 4        | accepted                                                         |
      | measure | measures 1 to 4   | 60%    | 100%    | 5%        | 0      | 4        | 4        | refused, saying "A step needs at least 1 pass."                    |
      | line    | lines 1 to 2      | 60%    | 100%    | 5%        | 1      | 0        | 4        | refused, saying "A line needs at least 1 measure."                 |
      | page    | pages 1 to 2      | 60%    | 100%    | 5%        | 1      | 4        | 0        | refused, saying "A page needs at least 1 line."                    |

  # Playing the steps. At 120 bpm a quarter note is 22050 samples: at 50% 44100, at 75% 29400, at 100% 22050.

  # measures 9 to 11 from 50% to 100% by 25%: 3 rungs; with 3 sections in play the ladder ends on 2-3, so the tail
  # plays 3 and 1-3 at the goal. A step lasts its 4 Count In clicks and its measures' 4 beats each: 352800, 235200,
  # 176400, 529200, 235200, 264600, 705600, 235200, 264600, 176400 and 352800 samples, 3528000 in all
  Scenario: each step plays its sections at its tempo after a Count In at that tempo, and the session stops by itself after the last
    Given I have turned Count In on
    And I have set interleaved click-up to measures over measures 9 to 11, from 50% to 100% by 25%
    When I practise interleaved click-up
    Then the output holds these steps, in order:
      | step | measures   | tempo | Count In clicks apart |
      | 1    | 9          | 50%   | 44100                 |
      | 2    | 9          | 75%   | 29400                 |
      | 3    | 9          | 100%  | 22050                 |
      | 4    | 9 to 10    | 50%   | 44100                 |
      | 5    | 10         | 75%   | 29400                 |
      | 6    | 9 to 10    | 100%  | 22050                 |
      | 7    | 9 to 11    | 50%   | 44100                 |
      | 8    | 11         | 75%   | 29400                 |
      | 9    | 10 to 11   | 100%  | 22050                 |
      | 10   | 11         | 100%  | 22050                 |
      | 11   | 9 to 11    | 100%  | 22050                 |
    And each step sounds the notes of its measures once each and in order, each within 1 sample of its beat at the step's tempo
    And before each step the output holds 4 Count In clicks, the last one a beat at the step's tempo before its first measure, within 1 sample
    And the first Count In click of each step but the first sounds at the end of the step before, within 1 sample
    And the session's last step ends 3528000 samples after the first one started, within 1 sample
    And the player has stopped by itself at the end of step 11

  # two sections from 50% to 100% by 50%: 1@50%, 1@100%, 1-2@50%, 2@100%, then the tail 1-2@100%
  Scenario Outline: a section is a <unit>, and a step plays the measures of its sections
    Given I have turned Count In on
    And I have set interleaved click-up to <unit>s over <sections>, from 50% to 100% by 50%, with lines of 4 measures and pages of 4 lines
    When I practise interleaved click-up
    Then the steps sound measures <measures>, in order, each measure once a step and within 1 sample of its beat at the step's tempo
    And the player has stopped by itself at the end of step 5

    Examples:
      | unit    | sections         | measures                                             |
      | measure | measures 9 to 10 | 9, 9, 9 to 10, 10, 9 to 10                           |
      | line    | lines 2 to 3     | 5 to 8, 5 to 8, 5 to 12, 9 to 12, 5 to 12            |
      | page    | pages 1 to 2     | 1 to 16, 1 to 16, 1 to 32, 17 to 32, 1 to 32         |

  Scenario: with Count In off, each step starts where the step before ended
    Given I have set interleaved click-up to measures over measures 9 to 10, from 50% to 100% by 50%
    When I practise interleaved click-up
    Then the output holds no Count In clicks
    And steps 2 to 5 start at the end of the step before, within 1 sample
    And each step sounds the notes of its measures once each and in order, each within 1 sample of its beat at the step's tempo

  Scenario: Count In ticks set the length of every step's Count In
    Given I have turned Count In on with 2 Count In ticks
    And I have set interleaved click-up to measures over measures 9 to 10, from 50% to 100% by 50%
    When I practise interleaved click-up
    Then before each step the output holds 2 Count In clicks, the last one a beat at the step's tempo before its first measure, within 1 sample

  # passes: each step plays its measures so many times back to back; its Count In comes before the first pass, or with
  # "Count In before every pass" on, before every pass, each at the step's tempo
  Scenario Outline: each step plays its sections <passes> times, with a Count In before <which>
    Given I have turned Count In on
    And I have turned interleaved click-up's "Count In before every pass" <every pass>
    And I have set interleaved click-up to measures over measures 9 to 10, from 50% to 100% by 50%, with <passes> passes a step
    When I practise interleaved click-up
    Then each step sounds the notes of its measures <passes> times back to back, each measure once and in order every time, each within 1 sample of its beat at the step's tempo
    And the output holds <count ins>
    And the player has stopped by itself at the end of step 5

    Examples:
      | passes | which               | every pass | count ins                                         |
      | 2      | its first pass only | off        | a Count In of 4 clicks before the first pass only |
      | 2      | every pass          | on         | a Count In of 4 clicks before every pass          |
      | 1      | every pass          | on         | a Count In of 4 clicks before every pass          |

  # Tempo

  # line 1: measures 1 and 2 at 120 bpm, 3 and 4 at 60 bpm. At 50% they play at 60 and 30 bpm (44100 and 88200
  # samples a beat), the Count In at 60 bpm: step 1 lasts 176400 + 352800 + 705600 = 1234800 samples; at 100% step 2
  # lasts 88200 + 176400 + 352800 = 617400
  Scenario: a tempo change inside a section plays at the step's tempo too, in proportion
    Given I have turned Count In on
    And I have set the tempo from measure 3 to the end to 60 bpm
    And I have set interleaved click-up to lines over lines 1 to 1, from 50% to 100% by 50%, with lines of 4 measures
    When I practise interleaved click-up
    Then each step sounds the notes of its measures once each and in order, each within 1 sample of its beat at the step's tempo
    And step 1 lasts 1234800 samples and step 2 617400, within 1 sample
    And before step 1 the output holds 4 Count In clicks, 44100 samples apart within 1 sample

  # a ladder in bpm is relative to the written tempo of section 1's first measure (here 90 bpm): 45 bpm plays it at
  # half its tempo, as 50% does; a quarter at 45 bpm is 58800 samples, at 60 bpm 44100, at 90 bpm 29400
  Scenario Outline: a ladder in bpm plays each step at its bpm, as a ladder in percent of the written tempo does
    Given I have turned Count In on
    And I have set the tempo from measure 1 to the end to 90 bpm
    And I have set interleaved click-up to measures over measures 9 to 9, from <start> to <goal> by <increment>
    When I practise interleaved click-up
    Then before step 1 the output holds 4 Count In clicks, <first apart> samples apart within 1 sample
    And before step 2 the output holds 4 Count In clicks, <second apart> samples apart within 1 sample
    And each step sounds the notes of its measures once each and in order, each within 1 sample of its beat at the step's tempo

    Examples:
      | start  | goal   | increment | first apart | second apart |
      | 50%    | 100%   | 50%       | 58800       | 29400        |
      | 45 bpm | 90 bpm | 45 bpm    | 58800       | 29400        |
      | 60 bpm | 90 bpm | 30 bpm    | 44100       | 29400        |

  # measure 1 at 120 bpm, measure 2 at 60: the ladder's bpm are those of measure 1, the passage's first. Step 4
  # (2@120 bpm) plays measure 2 at its own 60 bpm (44100 samples a beat), and step 3 (1-2@60 bpm) at 30 (88200)
  Scenario: a ladder in bpm follows the written tempo of the passage's first measure, and later measures keep their proportion
    Given I have turned Count In on
    And I have set the tempo from measure 2 to the end to 60 bpm
    And I have set interleaved click-up to measures over measures 1 to 2, from 60 bpm to 120 bpm by 60 bpm
    When I practise interleaved click-up
    Then interleaved click-up's steps are "1@60 bpm, 1@120 bpm, 1-2@60 bpm, 2@120 bpm, 1-2@120 bpm"
    And before step 4 the output holds 4 Count In clicks, 44100 samples apart within 1 sample
    And step 3 sounds measure 2's beats 88200 samples apart, within 1 sample

  # the Percentage of Tempo and Training Mode take no part: the ladder alone sets the tempo, and they are as they were
  Scenario: the Percentage of Tempo and Training Mode take no part in a click-up
    Given I have turned Count In on
    And I have set the Percentage of Tempo to 50
    And I have turned Training Mode on from 50% to 100% by 10% over measures 1 to 4
    And I have set interleaved click-up to measures over measures 9 to 9, from 100% to 100% by 5%
    When I practise interleaved click-up
    Then before step 1 the output holds 4 Count In clicks, 22050 samples apart within 1 sample
    And each step sounds the notes of its measures once each and in order, each within 1 sample of its beat at the step's tempo
    And Training Mode is on from 50% to 100% by 10% over measures 1 to 4

  Scenario: TuxGuitar shows each step's first measure during its Count In, then follows the step as it sounds
    Given I have turned Count In on
    And I have set interleaved click-up to lines over lines 1 to 2, from 50% to 100% by 50%, with lines of 4 measures
    When I practise interleaved click-up
    Then during each step's Count In TuxGuitar reports beat 1 of the step's first measure
    And during each step TuxGuitar reports each of its beats within 1 sample of the output sounding it

  # a step's end is a jump in song time: the take fades out before it, and starts at full level after it
  Scenario: an audio track's take fades out at the end of every step but the last, and comes back at full level
    Given I have turned Count In on
    And I have added an audio track recording input channel 1
    And track 2 has a take of a constant level of 0.4 from measure 1 beat 1 to the end of measure 64
    And I have set interleaved click-up to measures over measures 9 to 10, from 50% to 100% by 50%
    When I practise interleaved click-up
    Then track 2 fades from 0.4 to 0 over the last 256 samples of steps 1 to 4, within 1 sample
    And track 2's first sample of each step is 0.4

  # The session live

  # live, as offline: the takes follow the steps the player lays, each at its step's tempo. Track 2 alone sounds on the
  # output's right side
  Scenario: live, an audio track's take fades out at the end of every step but the last, and comes back at full level
    Given I have turned Count In on
    And I have set the Balance of track 1's channel to 0
    And I have added an audio track recording input channel 1
    And I have set the Balance of track 2's channel to 127
    And track 2 has a take of a constant level of 0.4 from measure 1 beat 1 to the end of measure 64
    And I have set interleaved click-up to measures over measures 9 to 10, from 50% to 100% by 50%
    When I practise interleaved click-up live
    Then the output's right side fades from 0.4 to 0 over the last 256 samples of steps 1 to 4, within 1 sample
    And the output's right side steps up by 0.4 on the first sample of each step

  Scenario: live, each step plays its sections at its tempo after a Count In at that tempo, and the session stops by itself
    Given I have turned Count In on
    And I have set interleaved click-up to measures over measures 9 to 11, from 50% to 100% by 25%
    When I practise interleaved click-up live
    Then the output holds 11 steps, measures "9, 9, 9, 9 to 10, 10, 9 to 10, 9 to 11, 11, 10 to 11, 11, 9 to 11" at 50%, 75%, 100%, 50%, 75%, 100%, 50%, 75%, 100%, 100% and 100%
    And each step sounds the notes of its measures once each and in order, each within 1 sample of its beat at the step's tempo
    And the player stopped by itself within 512 samples of step 11's end

  # measures 9 to 10 from 50% to 100% by 50%: 9@50%, 9@100%, 9-10@50%, 10@100%, 9-10@100%
  Scenario: Pause keeps the session: Play plays the paused step again from its Count In, then the steps after it
    Given I have turned Count In on
    And I have set interleaved click-up to measures over measures 9 to 10, from 50% to 100% by 50%
    When I practise interleaved click-up live, pausing during step 3 and playing again
    Then after Play, step 3's measures 9 and 10 sound again at 50% after a Count In of 4 beats 44100 samples apart
    And then steps 4 and 5 sound, each after its Count In
    And the player stopped by itself within 512 samples of step 5's end

  # Repeat step plays the step under way again at once, from its Count In; Previous step plays the step before it (or,
  # during step 1, step 1 again); then the steps go on from there. The player sees its end at the end of a block of 512
  # samples, which falls up to two blocks after the step ends as heard, as the choice moves the steps against the blocks
  Scenario Outline: <command> during step <during> plays step <again> again from its Count In, then the steps after it
    Given I have turned Count In on
    And I have set interleaved click-up to measures over measures 9 to 10, from 50% to 100% by 50%
    When I practise interleaved click-up live, choosing <command> during step <during>'s first measure
    Then the steps the output holds started are <played>
    And step <again>'s second time starts with its Count In within 512 samples of choosing it
    And the player stopped by itself within 1024 samples of step 5's end

    Examples:
      | command       | during | again | played              |
      | Repeat step   | 3      | 3     | 1, 2, 3, 3, 4, 5    |
      | Previous step | 3      | 2     | 1, 2, 3, 2, 3, 4, 5 |
      | Previous step | 1      | 1     | 1, 1, 2, 3, 4, 5    |
      | Repeat step   | 5      | 5     | 1, 2, 3, 4, 5, 5    |

  Scenario: Stop ends the session
    Given I have turned Count In on
    And I have set interleaved click-up to measures over measures 9 to 10, from 50% to 100% by 50%
    When I practise interleaved click-up live and stop at step 2's first measure beat 3
    Then the output holds 2 steps started
    And interleaved click-up has ended

  Scenario: moving the position during a session ends it, and the song plays on from there
    Given I have set interleaved click-up to measures over measures 9 to 10, from 50% to 100% by 50%
    When I practise interleaved click-up live, moving the position to measure 40 beat 1 during step 2
    Then the player plays on from measure 40 beat 1
    And interleaved click-up has ended

  Scenario: editing the song ends the session
    Given I have set interleaved click-up to measures over measures 9 to 10, from 50% to 100% by 50%
    When I practise interleaved click-up live, removing measure 40 during step 2
    Then the player stopped within 512 samples of the edit, during step 2
    And the song has 63 measures
    And interleaved click-up has ended

  # a flag weighs a unit in interleaved practice's random choice; a click-up has no choice, so its steps stay as they were
  Scenario: flagging a measure during a session keeps it and its steps
    Given I have set interleaved click-up to measures over measures 9 to 10, from 50% to 100% by 50%
    When I practise interleaved click-up live, flagging measure 10 as 3x difficult during step 2
    Then the output holds 5 steps, measures "9, 9, 9 to 10, 10, 9 to 10" at 50%, 100%, 50%, 100% and 100%
    And measure 10 is flagged 3x difficult
