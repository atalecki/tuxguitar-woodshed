Feature: The click's accent on beat 1
  On the TuxGuitar Synthesizer, Count In and the Metronome sound the built-in click. Beat 1 of each measure clicks with
  an accent: higher and slightly louder than the other beats (2000 Hz instead of 1500 Hz, 3 dB louder, the same rise
  and fall). The Metronome accents the first beat of each measure, as the song's time signature says there. Count In
  counts backwards from where the song starts, in the time signature of the measure it starts in, so its accents fall
  on the bar lines before that start: 6 clicks before a measure of 4/4 sound "3 4 | 1 2 3 4", 2 before a measure of
  3/4 "2 3". Before a start on another beat it leads into that beat (4 clicks before beat 3 sound "3 4 | 1 2");
  before a start between two beats no click falls on a beat of the song, and its first click is accented, then the
  first of each measure's worth after it.
  Played live or offline, the same clicks are accented. Other MIDI Ports play the drum note of each beat's sound, beat 1
  3 dB louder (a higher velocity) when it is the other beats' sound accented.

  Background:
    Given a new song with one guitar track and 4 measures of 4/4 at 120 bpm
    And I have set the MIDI Port to "TuxGuitar Synthesizer"
    And I have set the audio device to "Test Interface"

  # the click volume (-3 dB by default) sets the level of both
  Scenario: a Count In of one measure accents its first click
    Given I have turned Count In on
    When I play the song from measure 1 beat 1
    Then the output holds 4 Count In clicks, 22050 samples apart within 1 sample
    And the Count In clicks sound the built-in click at -3 dB, accented on click 1 only

  Scenario: a Count In of two measures accents the first click of each
    Given I have turned Count In on with 8 Count In ticks
    When I play the song from measure 1 beat 1
    Then the Count In clicks sound the built-in click at -3 dB, accented on clicks 1 and 5 only

  # "3 4 | 1 2 3 4"
  Scenario: a Count In of 6 clicks before a measure of 4/4 accents the bar line before the downbeat
    Given I have turned Count In on with 6 Count In ticks
    When I play the song from measure 1 beat 1
    Then the Count In clicks sound the built-in click at -3 dB, accented on click 3 only

  Scenario: Count In before a measure of 3/4 counts in measures of 3
    Given I have set the time signature of measure 1 only to 3/4
    And I have turned Count In on with 6 Count In ticks
    When I play the song from measure 1 beat 1
    Then the Count In clicks sound the built-in click at -3 dB, accented on clicks 1 and 4 only

  # "2 3"
  Scenario: a Count In of 2 clicks before a measure of 3/4 has no bar line to accent
    Given I have set the time signature of measure 1 only to 3/4
    And I have turned Count In on with 2 Count In ticks
    When I play the song from measure 1 beat 1
    Then the Count In clicks sound the built-in click at -3 dB, accented on no click

  # "3 4 | 1 2", then the song from beat 3
  Scenario: Count In before a start on beat 3 leads into beat 3
    Given I have turned Count In on
    When I play the song from measure 1 beat 3
    Then the Count In clicks sound the built-in click at -3 dB, accented on click 3 only

  # no click falls on a beat of the song: its first is accented, as before
  Scenario: Count In before a start between two beats accents its first click
    Given I have turned Count In on with 6 Count In ticks
    When I play the song from measure 1 beat 2.5
    Then the Count In clicks sound the built-in click at -3 dB, accented on clicks 1 and 5 only

  Scenario: the Metronome accents beat 1 of each measure, as each measure's time signature says
    Given I have set the time signature of measure 2 only to 3/4
    And I have set the time signature of measure 3 only to 5/4
    And I have turned the Metronome on
    When I play the song from measure 1 beat 1
    Then the output holds 16 Metronome clicks
    And the Metronome clicks sound the built-in click at -3 dB, accented on beat 1 of measures 1 to 4 only

  # the first click is beat 3: not accented
  Scenario: the Metronome started in the middle of a measure accents beat 1 of the measures after
    Given I have turned the Metronome on
    When I play the song from measure 1 beat 3
    Then the output holds 14 Metronome clicks
    And the Metronome clicks sound the built-in click at -3 dB, accented on beat 1 of measures 2 to 4 only

  Scenario: the Metronome and Count In accent the same way at another volume
    Given I have set the click to the code-made Click at 0 dB
    And I have turned Count In on
    And I have turned the Metronome on
    When I play the song from measure 1 beat 1
    Then the Count In clicks sound the built-in click at 0 dB, accented on click 1 only
    And the Metronome clicks sound the built-in click at 0 dB, accented on beat 1 of measures 1 to 4 only

  # live, the clicks are measured in the output (no track sounds): each click's frequency over its first 20 ms, and its
  # peak
  Scenario: while the song plays live, the Metronome accents beat 1 of each measure
    Given I have set the time signature of measure 2 only to 3/4
    And I have turned the Metronome on
    When I play the song live from measure 1 beat 1
    Then the output's left side holds 15 clicks, those on beat 1 of each measure 2000 Hz and 3 dB above the others, at 1500 Hz
