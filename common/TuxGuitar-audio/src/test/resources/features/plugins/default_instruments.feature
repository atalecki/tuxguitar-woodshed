Feature: Default instruments by track kind
  A track whose sound chain has no instrument of its own (no sound chain, or one whose source is General MIDI) plays
  the default instrument of its kind, a VST3 instrument chosen once in the settings for each kind: guitar, bass, drums
  and other. The kind comes from the track's General MIDI instrument: a percussion channel is drums, the General MIDI
  Guitar programs (Nylon String Acoustic Guitar to Harmonics Electric Guitar) guitar, the Bass programs (Acoustic Bass
  to Synth Bass 2) bass, anything else other. The song keeps saying "General MIDI": nothing in it changes, so each
  computer plays its own defaults. A sound chain's effects stay after the default instrument. A track with an instrument
  of its own plays it. The defaults are a setting of the computer, found among the VST3 plugins the scan found by
  their class id; a kind without one plays General MIDI. Play and an offline render (an export) play the same default.
  (Test Sine sounds a pure sine at each note's pitch, peaking at 0.5 at fff; a General MIDI program sounds harmonics.)

  Background:
    Given a new song with one guitar track and 4 measures of 4/4 at 120 bpm
    And I have set the MIDI Port to "TuxGuitar Synthesizer"
    And I have set the audio device to "Test Interface"
    And the VST3 folders hold the test plugins
    And I have entered fret 5 on string 1 at dynamic fff

  Scenario Outline: a track without an instrument of its own plays the default instrument of its kind
    Given I have set the General MIDI program of track 1's channel to "<program>"
    And the default instrument for <kind> tracks is "Test Sine"
    When I play the song from measure 1 beat 1
    Then track 1 sounds a 440 Hz tone peaking at 0.5 within 0.01 in measure 1
    And track 1 sounds a 440 Hz tone and no 880 Hz tone above -60 dBFS in measures 1

    Examples:
      | program                       | kind   |
      | Steel String Acoustic Guitar  | guitar |
      | Distortion Electric Guitar    | guitar |
      | Fingered Electric Bass        | bass   |
      | Acoustic Grand Piano          | other  |

  Scenario: a percussion track plays the default instrument for drums
    Given I make track 1's channel a percussion channel
    And the default instrument for drums tracks is "Test Sine"
    When I play the song from measure 1 beat 1
    Then track 1 sounds a 440 Hz tone peaking at 0.5 within 0.01 in measure 1

  Scenario: a track plays the default instrument of its own kind, not another kind's
    Given I have set the General MIDI program of track 1's channel to "Acoustic Grand Piano"
    And the default instrument for guitar tracks is "Test Click"
    And the default instrument for other tracks is "Test Sine"
    When I play the song from measure 1 beat 1
    Then track 1 sounds a 440 Hz tone peaking at 0.5 within 0.01 in measure 1

  Scenario: the effects of a sound chain whose source is General MIDI process the default instrument
    Given I have added the VST3 effect "Test Gain" to track 1
    And I have set "Gain" of VST3 effect 1 of track 1 to 0.5
    And the default instrument for guitar tracks is "Test Sine"
    When I play the song from measure 1 beat 1
    Then track 1 sounds a 440 Hz tone peaking at 0.25 within 0.01 in measure 1

  Scenario: a track with an instrument of its own plays it, whatever the default
    Given I have chosen the VST3 instrument "Test Sine" for track 1
    And the default instrument for guitar tracks is "Test Click"
    When I play the song from measure 1 beat 1
    Then track 1 sounds a 440 Hz tone peaking at 0.5 within 0.01 in measure 1

  Scenario: the default instrument plays live too
    Given the default instrument for guitar tracks is "Test Sine"
    When I play the song live from measure 1 beat 1
    Then the output sounds a 440 Hz tone and no 880 Hz tone above -60 dBFS in measures 1 to 1, live

  Scenario: playing the default instrument leaves the song as it was
    Given the default instrument for guitar tracks is "Test Sine"
    And I have saved the song as a TuxGuitar file
    When I play the song from measure 1 beat 1
    Then the song is not modified
    And track 1's sound chain reads "Steel String Acoustic Guitar"
