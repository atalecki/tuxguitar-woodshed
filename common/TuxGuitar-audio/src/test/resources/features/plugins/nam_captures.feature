@planned
Feature: Dropping a NAM capture on a track
  A NAM capture (a .nam file: a Neural Amp Modeler model of an amp or pedal) dropped on a track sounds that track
  through the capture: TuxGuitar adds the capture player, the VST3 effect that plays NAM captures, at the end of the
  track's sound chain with the capture loaded, or gives the capture to the capture player the track already has, so
  trying one tone after another is one drop each. Each drop is one undo step. Which installed plugin is the capture
  player is a TuxGuitar setting, by default the first installed of the capture players TuxGuitar recognises
  (TGCapturePlayer: Gateway, the TONE3000 plugin, the old Neural Amp Modeler); the scenarios set it to the test
  plugin "Test Capture".

  Background:
    Given a new song with one guitar track and 4 measures of 4/4 at 120 bpm
    And I have set the MIDI Port to "TuxGuitar Synthesizer"
    And I have set the audio device to "Test Interface"
    And the VST3 folders hold the test plugins
    And the capture player is the VST3 effect "Test Capture"
    And I have set the General MIDI program of track 1's channel to "Distortion Electric Guitar"
    And there is a NAM capture "Gain 3.nam" that multiplies the signal by 3.0
    And there is a NAM capture "Gain 2.nam" that multiplies the signal by 2.0

  Scenario: dropping a NAM capture on a track adds the capture player with that capture at the end of its sound chain
    When I drop the NAM capture "Gain 3.nam" on track 1
    Then the action succeeds
    And track 1's sound chain reads "Distortion Electric Guitar > Test Capture"
    And VST3 effect 1 of track 1 plays the NAM capture "Gain 3.nam"

  Scenario: a track sounds through the NAM capture dropped on it
    Given I have added an audio track recording input channel 1
    And track 2 has a take of a 440 Hz tone peaking at 0.1 from measure 1 beat 1 to measure 2 beat 1
    And I have dropped the NAM capture "Gain 3.nam" on track 2
    When I play the song from measure 1 beat 1
    Then track 2 sounds a 440 Hz tone peaking at 0.3 within 0.01 in measure 1

  Scenario: the capture player goes after the effects the track already has
    Given I have added the VST3 effect "Test Gain" to track 1
    When I drop the NAM capture "Gain 3.nam" on track 1
    Then track 1's sound chain reads "Distortion Electric Guitar > Test Gain > Test Capture"

  Scenario: dropping another NAM capture on the track changes the capture its capture player plays
    Given I have dropped the NAM capture "Gain 3.nam" on track 1
    When I drop the NAM capture "Gain 2.nam" on track 1
    Then track 1's sound chain reads "Distortion Electric Guitar > Test Capture"
    And VST3 effect 1 of track 1 plays the NAM capture "Gain 2.nam"

  # the last capture player of the sound chain takes the capture; the others keep theirs
  Scenario: with two capture players on the track, the last one takes the capture
    Given I have dropped the NAM capture "Gain 3.nam" on track 1
    And I have added the VST3 effect "Test Capture" to track 1
    When I drop the NAM capture "Gain 2.nam" on track 1
    Then track 1's sound chain reads "Distortion Electric Guitar > Test Capture > Test Capture"
    And VST3 effect 1 of track 1 plays the NAM capture "Gain 3.nam"
    And VST3 effect 2 of track 1 plays the NAM capture "Gain 2.nam"

  Scenario: a disabled capture player is given the capture and stays disabled
    Given I have dropped the NAM capture "Gain 3.nam" on track 1
    And I have disabled VST3 effect 1 of track 1
    When I drop the NAM capture "Gain 2.nam" on track 1
    Then track 1's sound chain reads "Distortion Electric Guitar > [Test Capture]"
    And VST3 effect 1 of track 1 plays the NAM capture "Gain 2.nam"

  Scenario: dropping a NAM capture on a track with a VST3 instrument puts the capture player after it
    Given I have chosen the VST3 instrument "Test Sine" for track 1
    When I drop the NAM capture "Gain 3.nam" on track 1
    Then track 1's sound chain reads "Test Sine > Test Capture"

  # Undo and redo

  Scenario: dropping a NAM capture that added the capture player is one undo step
    Given I have dropped the NAM capture "Gain 3.nam" on track 1
    When I undo
    Then track 1's sound chain reads "Distortion Electric Guitar"

  Scenario: dropping a NAM capture that changed the capture is one undo step
    Given I have dropped the NAM capture "Gain 3.nam" on track 1
    And I have dropped the NAM capture "Gain 2.nam" on track 1
    When I undo
    Then track 1's sound chain reads "Distortion Electric Guitar > Test Capture"
    And VST3 effect 1 of track 1 plays the NAM capture "Gain 3.nam"

  Scenario: an undone NAM capture drop can be redone
    Given I have dropped the NAM capture "Gain 3.nam" on track 1
    And I have undone it
    When I redo
    Then track 1's sound chain reads "Distortion Electric Guitar > Test Capture"
    And VST3 effect 1 of track 1 plays the NAM capture "Gain 3.nam"

  # Refusals

  Scenario: dropping a NAM capture with no capture player installed is refused, naming the plugin to install
    Given the VST3 folders hold no capture player
    When I drop the NAM capture "Gain 3.nam" on track 1
    Then the action is refused
    And the refusal names the plugin "Test Capture"
    And track 1's sound chain reads "Distortion Electric Guitar"

  Scenario: dropping a file that is not a NAM capture on a track is refused
    Given there is a text file "notes.txt"
    When I drop the file "notes.txt" on track 1
    Then the action is refused
    And track 1's sound chain reads "Distortion Electric Guitar"

  Scenario: dropping a NAM capture during a recording is refused
    Given I have added an audio track recording input channel 1
    And I have armed track 2
    And I am recording from measure 1 beat 1
    When I drop the NAM capture "Gain 3.nam" on track 1
    Then the action is refused
    And track 1's sound chain reads "Distortion Electric Guitar"

  # Saving

  Scenario: a dropped NAM capture comes back after saving and reopening the song
    Given I have dropped the NAM capture "Gain 3.nam" on track 1
    And I have saved the song as a TuxGuitar file
    When I open it again
    Then track 1's sound chain reads "Distortion Electric Guitar > Test Capture"
    And VST3 effect 1 of track 1 plays the NAM capture "Gain 3.nam"
