Feature: Level meters in the Sound Chain panel
  Under each button of the track's sound chain a meter shows the level after that element: the source, then each
  effect. "Track out" shows the track's level after its Volume and Balance, left and right: what joins the mix, cut at
  full scale. "Master" shows the whole mix, the click included. A meter's peak rises at once and falls back; its clip
  lamp lights when the sound reaches -0.1 dB, and stays lit until its meter is clicked or Play is pressed again. An
  effect switched off measures nothing. The meters measure only while the panel shows them.

  Background:
    Given a new song with one guitar track and 4 measures of 4/4 at 120 bpm
    And I have set the MIDI Port to "TuxGuitar Synthesizer"
    And I have set the audio device to "Test Interface"
    And the VST3 folders hold the test plugins
    And I have added an audio track recording input channel 1
    And track 2 has a take of a 440 Hz tone peaking at 0.4 from measure 1 beat 1 to measure 2 beat 1

  # 0.4 is -8.0 dB; Test Gain doubles it (0.8, -1.9 dB); Test Clip 0.5 holds it at 0.5 (-6.0 dB)
  Scenario: each meter shows the level after its element
    Given I have added the VST3 effect "Test Gain" to track 2
    And I have added the VST3 effect "Test Clip 0.5" to track 2
    And the Sound Chain panel shows the meters of track 2
    When I play the song live from measure 1 beat 1 with the meters showing
    Then the "source" meter peaked at -8.0 dB within 0.1 dB
    And the "effect 1" meter peaked at -1.9 dB within 0.1 dB
    And the "effect 2" meter peaked at -6.0 dB within 0.1 dB
    And the "source" meter's clip lamp is out

  # 0.5 at Volume 32 of 127, Balance 64 of 127 (constant power: cos and sin of 64/127 of a right angle)
  Scenario: the Track out meter shows the track's Volume and Balance, left and right
    Given I have added the VST3 effect "Test Gain" to track 2
    And I have added the VST3 effect "Test Clip 0.5" to track 2
    And I have set the Volume of track 2's channel to 32
    And the Sound Chain panel shows the meters of track 2
    When I play the song live from measure 1 beat 1 with the meters showing
    Then the "Track out" meter peaked at -21.1 dB on the left and -21.0 dB on the right, within 0.1 dB

  # 0.4 (not cut by Test Clip 0.5) at Volume 32 of 127, all on the left
  Scenario: Balance all to the left leaves the Track out meter's right side silent
    Given I have added the VST3 effect "Test Clip 0.5" to track 2
    And I have set the Volume of track 2's channel to 32
    And I have set the Balance of track 2's channel to 0
    And the Sound Chain panel shows the meters of track 2
    When I play the song live from measure 1 beat 1 with the meters showing
    Then the "Track out" meter peaked at -19.9 dB within 0.1 dB
    And the "Track out" meter was silent on the right

  # 0.4 four times louder is 1.6 (+4.1 dB): above full scale, kept between effects, so a quarter of it is 0.4 again
  Scenario: between effects the level may go above 0 dB, and that meter's lamp lights
    Given I have added the VST3 effect "Test Gain" to track 2
    And I have set "Gain" of VST3 effect 1 of track 2 to 4.0
    And I have added the VST3 effect "Test Gain" to track 2
    And I have set "Gain" of VST3 effect 2 of track 2 to 0.25
    And the Sound Chain panel shows the meters of track 2
    When I play the song live from measure 1 beat 1 with the meters showing
    Then the "effect 1" meter peaked at 4.1 dB within 0.1 dB
    And the "effect 1" meter's clip lamp is lit
    And the "effect 2" meter peaked at -8.0 dB within 0.1 dB
    And the "effect 2" meter's clip lamp is out

  Scenario: a track pushed over full scale is cut there, lights its Track out lamp, and a click on the meter clears it
    Given I have added the VST3 effect "Test Gain" to track 2
    And I have set "Gain" of VST3 effect 1 of track 2 to 8.0
    And the Sound Chain panel shows the meters of track 2
    When I play the song live from measure 1 beat 1 with the meters showing
    Then the "Track out" meter peaked at 0.0 dB within 0.1 dB
    And the "Track out" meter's clip lamp is lit
    And the "Master" meter's clip lamp is lit
    When I click the "Track out" meter
    Then the "Track out" meter's clip lamp is out
    And the "Master" meter's clip lamp is lit

  Scenario: pressing Play again clears the clip lamps
    Given I have added the VST3 effect "Test Gain" to track 2
    And I have set "Gain" of VST3 effect 1 of track 2 to 8.0
    And the Sound Chain panel shows the meters of track 2
    When I play the song live from measure 1 beat 1 with the meters showing
    Then the "Track out" meter's clip lamp is lit
    Given I have set "Gain" of VST3 effect 1 of track 2 to 0.5
    When I play the song live from measure 1 beat 1 with the meters showing
    Then the "Track out" meter's clip lamp is out
    And the "effect 1" meter's clip lamp is out
    And the "Master" meter's clip lamp is out

  Scenario: a disabled effect shows no level
    Given I have added the VST3 effect "Test Gain" to track 2
    And I have disabled VST3 effect 1 of track 2
    And the Sound Chain panel shows the meters of track 2
    When I play the song live from measure 1 beat 1 with the meters showing
    Then the "effect 1" meter is off
    And the "source" meter peaked at -8.0 dB within 0.1 dB

  Scenario: the Master meter shows the mix, the click included
    Given I have turned the Metronome on
    And I have muted track 2
    And the Sound Chain panel shows the meters of track 2
    When I play the song live from measure 1 beat 1 with the meters showing
    Then the "Master" meter peaked above -30.0 dB

  Scenario: nothing is measured while the panel does not show the meters
    When I play the song live from measure 1 beat 1
    Then no level was measured

  Scenario: playing offline (an export) measures nothing, even with the meters showing
    Given the Sound Chain panel shows the meters of track 2
    When I play the song from measure 1 beat 1
    Then no level was measured
