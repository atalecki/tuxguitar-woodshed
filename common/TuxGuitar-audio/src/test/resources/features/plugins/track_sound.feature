Feature: A track's sound, MIDI or VST3
  Each track sounds MIDI or VST3, chosen per track (the Track panel's switch). MIDI plays the track's General MIDI
  program inside TuxGuitar, as it is: the default instrument of its kind does not play in its place. VST3 plays the
  track's own VST3 instrument, else the default instrument of its kind. The effects of the track's sound chain apply in
  both. Switching to MIDI keeps the track's own VST3 instrument in the song, its state with it, so switching back
  restores it; a default instrument is not kept (back on VST3, the default plays again). Each switch is one undo step.
  Old songs and new tracks start on VST3. An audio track sounds its takes: it has no switch.
  (Test Sine sounds a pure sine at each note's pitch, peaking at 0.5 at fff, Test Tone the same times its Level;
  General MIDI's distortion guitar sounds harmonics.)

  Background:
    Given a new song with one guitar track and 4 measures of 4/4 at 120 bpm
    And I have set the MIDI Port to "TuxGuitar Synthesizer"
    And I have set the audio device to "Test Interface"
    And the VST3 folders hold the test plugins
    And I have set the General MIDI program of track 1's channel to "Distortion Electric Guitar"
    And I have entered fret 5 on string 1 at dynamic fff

  Scenario: a new track starts on VST3
    Then track 1's sound is VST3

  Scenario: a song of an older version opens on VST3
    When I open the format 2.0 file "saving/no-audio.tg"
    Then track 1's sound is VST3

  Scenario: a track set to MIDI plays its General MIDI program, not the default instrument of its kind
    Given the default instrument for guitar tracks is "Test Sine"
    When I set track 1's sound to MIDI
    And I play the song from measure 1 beat 1
    Then track 1's sound is MIDI
    And track 1's sound chain reads "Distortion Electric Guitar"
    And track 1 sounds a 880 Hz tone above -60 dBFS in measure 1

  Scenario: a track set to MIDI plays General MIDI in place of its own VST3 instrument
    Given I have chosen the VST3 instrument "Test Sine" for track 1
    When I set track 1's sound to MIDI
    And I play the song from measure 1 beat 1
    Then track 1's sound chain reads "Distortion Electric Guitar"
    And track 1 sounds a 880 Hz tone above -60 dBFS in measure 1

  Scenario: switching back to VST3 restores the same VST3 instrument with its state
    Given I have chosen the VST3 instrument "Test Tone" for track 1
    And I have set "Level" of the VST3 instrument of track 1 to 0.25
    And I have set track 1's sound to MIDI
    When I set track 1's sound to VST3
    And I play the song from measure 1 beat 1
    Then track 1's sound chain reads "Test Tone"
    And "Level" of the VST3 instrument of track 1 is 0.25
    And track 1 sounds a 440 Hz tone peaking at 0.125 within 0.01 in measure 1

  Scenario: a default instrument switched to MIDI and back plays the default again
    Given the default instrument for guitar tracks is "Test Sine"
    And I have set track 1's sound to MIDI
    When I set track 1's sound to VST3
    And I play the song from measure 1 beat 1
    Then the Sound Chain dialog's source row of track 1 reads "Source: Test Sine (default for guitar tracks)"
    And track 1 sounds a 440 Hz tone and no 880 Hz tone above -60 dBFS in measures 1

  # track 2 plays what track 1 plays, on a channel of its own, without the effect
  Scenario: the effects apply on MIDI
    Given I have added a guitar track on a channel of its own
    And I have set the General MIDI program of track 2's channel to "Distortion Electric Guitar"
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1 at dynamic fff
    And I have added the VST3 effect "Test Gain" to track 1
    And I have set "Gain" of VST3 effect 1 of track 1 to 0.5
    And I have set track 1's sound to MIDI
    And I have set track 2's sound to MIDI
    When I play the song from measure 1 beat 1
    Then track 1 sounds 0.5 times what track 2 sounds, sample for sample within 0.001
    And track 1 sounds a 880 Hz tone above -60 dBFS in measure 1

  Scenario: the effects added on MIDI apply back on VST3
    Given I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1 at dynamic fff
    And I have chosen the VST3 instrument "Test Sine" for track 1
    And I have chosen the VST3 instrument "Test Sine" for track 2
    And I have set track 1's sound to MIDI
    And I have added the VST3 effect "Test Gain" to track 1
    And I have set "Gain" of VST3 effect 1 of track 1 to 0.5
    And I have set track 1's sound to VST3
    When I play the song from measure 1 beat 1
    Then track 1 sounds 0.5 times what track 2 sounds, sample for sample within 0.001
    And track 1 sounds a 440 Hz tone peaking at 0.25 within 0.01 in measure 1

  Scenario: each switch is one undo step
    Given I have chosen the VST3 instrument "Test Sine" for track 1
    And I have set track 1's sound to MIDI
    When I undo
    Then track 1's sound is VST3
    And track 1's sound chain reads "Test Sine"
    When I redo
    Then track 1's sound is MIDI
    And track 1's sound chain reads "Distortion Electric Guitar"

  Scenario: the sound and the instrument kept come back after saving and reopening
    Given I have chosen the VST3 instrument "Test Tone" for track 1
    And I have set "Level" of the VST3 instrument of track 1 to 0.25
    And I have set track 1's sound to MIDI
    And I have saved the song as a TuxGuitar file
    And I have opened it again
    Then track 1's sound is MIDI
    When I set track 1's sound to VST3
    Then track 1's sound chain reads "Test Tone"
    And "Level" of the VST3 instrument of track 1 is 0.25

  # live, the TuxGuitar Synthesizer runs the channel already: the switch is heard at once, General MIDI opened in place
  # of the instrument, the instrument opened again in place of General MIDI
  Scenario: while the TuxGuitar Synthesizer runs the song, each switch plays the other sound at once
    Given I have chosen the VST3 instrument "Test Sine" for track 1
    And the TuxGuitar Synthesizer runs the song's sound chains, each change sent to it as the app sends it
    When I set track 1's sound to MIDI
    Then track 1's channel plays "General MIDI" before any Play
    When I set track 1's sound to VST3
    Then track 1's channel plays "Test Sine" before any Play

  Scenario: a switch to the sound the track has is refused
    When I set track 1's sound to VST3
    Then the action is refused
    And track 1's sound is VST3

  Scenario: an audio track has no MIDI or VST3 sound
    Given I have added an audio track recording input channel 1
    When I set track 2's sound to MIDI
    Then the action is refused
    And track 2's sound is takes
