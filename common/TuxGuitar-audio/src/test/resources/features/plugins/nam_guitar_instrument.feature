Feature: Woodshed Guitar, the guitar instrument that plays through a NAM capture
  Woodshed Guitar is a VST3 instrument TuxGuitar ships in its own plugins folder, so a tablature plays back with a real
  amp tone. Each note plucks a string of its own (a synthesized plucked string: a clean signal, as a guitar's DI out
  gives), the strings ring together, and the sum plays through its profile: the NAM capture (a .nam file, a Neural Amp
  Modeler model of an amp or pedal) loaded into it, the same signal on the left and the right. With no profile loaded
  it sounds its dry voice, never nothing. The profile is loaded in its own editor (Load profile...), kept in the song
  with the capture's contents, and played at the capture's own sample rate. It runs in its helper process like every
  VST3 plugin, and is offered in the Sound Chain like any VST3 instrument (it is not the default instrument).

  Background:
    Given a new song with one guitar track and 4 measures of 4/4 at 120 bpm
    And I have set the MIDI Port to "TuxGuitar Synthesizer"
    And I have set the audio device to "Test Interface"
    And the VST3 folders hold the test plugins and TuxGuitar's own plugins
    And there is a NAM capture "Gain 3.nam" that multiplies the signal by 3.0

  # Finding it

  Scenario: TuxGuitar scans its own plugins folder with the system's VST3 folders, and finds Woodshed Guitar there
    Given TuxGuitar is installed with Woodshed Guitar in its own plugins folder, and no VST3 plugin is installed on the system
    When I scan the VST3 folders
    Then the VST3 instruments read "Woodshed Guitar"
    And the VST3 effects read ""

  Scenario: Woodshed Guitar is offered as a VST3 instrument with the others
    When I scan the VST3 folders
    Then the VST3 instruments read "Test Click, Test Sine, Woodshed Guitar"

  # The voice

  Scenario Outline: a note sounds its pitch
    Given I have entered fret <fret> on string <string>
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    When I play the song from measure 1 beat 1
    Then track 1 sounds a <pitch> Hz note within 1% in measure 1

    Examples:
      | fret | string | pitch  |
      | 0    | 6      | 82.41  |
      | 5    | 1      | 440    |
      | 12   | 1      | 659.26 |

  Scenario Outline: a note is plucked at a level that follows its dynamic
    Given I have entered fret 5 on string 1 at dynamic <dynamic>
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    When I play the song from measure 1 beat 1
    Then track 1 peaks at <peak> within 1% in measure 1

    Examples:
      | dynamic | peak  |
      | fff     | 0.5   |
      | mf      | 0.311 |
      | ppp     | 0.059 |

  Scenario: the notes of a chord ring together, each on a string of its own
    Given I have entered fret 5 on string 1
    And I have entered fret 5 on string 2
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    When I play the song from measure 1 beat 1
    Then track 1 sounds a 440 Hz tone and a 329.6 Hz tone from measure 1 beat 1.5 to measure 1 beat 2, each peaking above 0.01

  # a quarter note at 120 bpm ends at beat 2; a string left alone would ring for seconds. A damped note is heard while it
  # is held (to beat 2), not in the 80 ms it takes to die away
  Scenario: a note is damped when it ends
    Given I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    When I play the song from measure 1 beat 1
    Then track 1 peaks at 0.374 within 1% in measure 1
    And track 1 sounds nothing above -60 dBFS from measure 1 beat 2.5 to measure 2 beat 1

  Scenario: a bend raises the pitch of its note only
    Given I have entered fret 5 on string 1 with a full bend
    And I have entered fret 5 on string 2
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    When I play the song from measure 1 beat 1
    Then track 1 sounds a 440 Hz tone rising to 493.9 Hz within 1 Hz from measure 1 beat 1 to measure 1 beat 2
    And track 1 sounds a 329.6 Hz tone that stays within 1 Hz in measure 1

  @planned
  Scenario: Woodshed Guitar sounds the same signal on the left and the right
    Given I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    When I play the song from measure 1 beat 1
    Then track 1's left and right sides are the same, sample for sample

  # The profile. Track 2 holds the same note as track 1, on a channel of its own, and stays dry: it is what Woodshed
  # Guitar sounds with no profile.

  @planned
  Scenario: the profile plays the voice through the NAM capture
    Given I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    When Woodshed Guitar of track 1 loads the NAM capture "Gain 3.nam"
    And I play the song from measure 1 beat 1
    Then track 1 sounds 3.0 times what track 2 sounds, sample for sample within 0.001
    And Woodshed Guitar of track 1 plays the profile "Gain 3.nam"

  @planned
  Scenario Outline: Input gain and Output level scale what Woodshed Guitar sounds
    Given I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    And I have set "<parameter>" of Woodshed Guitar of track 1 to <dB>
    When I play the song from measure 1 beat 1
    Then track 1 sounds <times> times what track 2 sounds, sample for sample within 0.001

    Examples:
      | parameter    | dB    | times |
      | Input gain   | -20.0 | 0.3   |
      | Output level | -20.0 | 0.3   |
      | Output level | 0.0   | 3.0   |

  @planned
  Scenario: with no profile loaded, Woodshed Guitar plays no profile
    Given I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    Then Woodshed Guitar of track 1 plays no profile

  @planned
  Scenario: loading a profile modifies the song
    Given I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have saved the song as a TuxGuitar file
    When Woodshed Guitar of track 1 loads the NAM capture "Gain 3.nam"
    Then the song is modified

  @planned
  Scenario: loading a profile is one undo step
    Given I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    When I undo
    Then Woodshed Guitar of track 1 plays no profile

  # Refusals: the plugin keeps the profile it had, and its editor shows why (here, what the plugin says). Each refusal
  # is of a kind the plugin names with it, so TuxGuitar can tell a capture of a newer kind (A2 or later: a file version
  # or an architecture NeuralAmpModelerCore does not read) from a file that is no capture at all

  @planned
  Scenario Outline: a file Woodshed Guitar cannot play is refused, saying why, and the profile stays
    Given there is a text file "notes.txt"
    And there is a text file "Broken.nam"
    And there is a NAM capture "Odd.nam" of the architecture "Transformer"
    And there is a NAM capture "Next.nam" of the file version "0.9.0"
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    And I have saved the song as a TuxGuitar file
    When Woodshed Guitar of track 1 loads the file "<file>"
    Then Woodshed Guitar of track 1 refuses it as <kind>, saying something starting "<refusal>"
    And Woodshed Guitar of track 1 plays the profile "Gain 3.nam"
    And the song is not modified

    Examples:
      | file      | kind  | refusal                                                                                                       |
      | notes.txt | other | notes.txt is not a NAM capture: Woodshed Guitar plays .nam files                                               |
      | Gone.nam  | other | Gone.nam could not be read                                                                                     |
      | Broken.nam| other | Broken.nam could not be loaded:                                                                                |
      | Odd.nam   | newer | Odd.nam is a newer kind of NAM capture (A2 or later) than Woodshed Guitar plays: play it with the TONE3000 plugin |
      | Next.nam  | newer | Next.nam is a newer kind of NAM capture (A2 or later) than Woodshed Guitar plays: play it with the TONE3000 plugin |

  # the A2 capture NeuralAmpModelerCore comes with (a SlimmableContainer, file version 0.7.0) is one it reads
  @planned
  Scenario: an A2 capture that NeuralAmpModelerCore reads plays in Woodshed Guitar
    Given there is the A2 NAM capture "A2.nam" that NeuralAmpModelerCore comes with
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    When Woodshed Guitar of track 1 loads the NAM capture "A2.nam"
    And I play the song from measure 1 beat 1
    Then Woodshed Guitar of track 1 plays the profile "A2.nam"
    And track 1 peaks above 0.001 in measure 1

  # TuxGuitar offers to play a capture of a newer kind with a capture player (TGCapturePlayer: the TONE3000 plugin
  # first, else Gateway; here "Test Capture"): Woodshed Guitar goes back to no profile, the capture player is added after
  # it and its editor opened, so the capture is loaded there (TuxGuitar cannot give it the file: its state is its own).
  # Needs the plugin to tell TuxGuitar of a refusal made in its own editor (a notice the host does not have yet).
  @planned
  Scenario: a capture of a newer kind is refused, and TuxGuitar offers to play it with the capture player
    Given the capture player is the VST3 effect "Test Capture"
    And there is a NAM capture "Odd.nam" of the architecture "Transformer"
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    When Woodshed Guitar of track 1 loads the file "Odd.nam"
    Then TuxGuitar offers "Use the TONE3000 plugin for this"

  @planned
  Scenario: using the capture player for a capture of a newer kind is one undo step
    Given the capture player is the VST3 effect "Test Capture"
    And there is a NAM capture "Odd.nam" of the architecture "Transformer"
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    And Woodshed Guitar of track 1 has refused the file "Odd.nam"
    When I use the capture player for it
    Then track 1's sound chain reads "Woodshed Guitar > Test Capture"
    And Woodshed Guitar of track 1 plays no profile
    And the plugin's own editor of VST3 effect 1 of track 1 is open
    When I undo
    Then track 1's sound chain reads "Woodshed Guitar"
    And Woodshed Guitar of track 1 plays the profile "Gain 3.nam"

  @planned
  Scenario: with no capture player installed, the refusal of a newer kind says to install the TONE3000 plugin
    Given the VST3 folders hold no capture player
    And there is a NAM capture "Odd.nam" of the architecture "Transformer"
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    When Woodshed Guitar of track 1 loads the file "Odd.nam"
    Then TuxGuitar says "Odd.nam needs the TONE3000 plugin: install it, free, from tone3000.com"
    And TuxGuitar offers nothing

  # Saving

  @planned
  Scenario: the profile comes back after saving and reopening the song
    Given I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    And I have saved the song as a TuxGuitar file
    When I open it again
    Then Woodshed Guitar of track 1 plays the profile "Gain 3.nam"

  # the song holds the capture's contents, as a song opened on another computer needs them
  @planned
  Scenario: a reopened song plays its profile when the NAM capture's file is gone
    Given I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    And I have saved the song as a TuxGuitar file
    And the NAM capture "Gain 3.nam" has been moved away
    When I open it again
    And I play the song from measure 1 beat 1
    Then track 1 sounds 3.0 times what track 2 sounds, sample for sample within 0.001
    And Woodshed Guitar of track 1 plays the profile "Gain 3.nam"

  # Sample rates

  # a capture made at 48000 Hz played on the Test Interface's 44100 Hz: the voice is resampled into the capture's rate
  # and back, so pitch and timing stay; the delay that adds is reported as the plugin's latency, which plugin delay
  # compensation makes up for, so track 1 stays in time with track 2
  @planned
  Scenario: a profile made at another sample rate keeps pitch and timing, its delay reported as latency
    Given there is a NAM capture "Gain 3 at 48 kHz.nam" that multiplies the signal by 3.0, made at 48000 Hz
    And I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    When Woodshed Guitar of track 1 loads the NAM capture "Gain 3 at 48 kHz.nam"
    And I play the song from measure 1 beat 1
    Then track 1 sounds 3.0 times what track 2 sounds, sample for sample within 0.01
    And Woodshed Guitar of track 1 reports a latency above 0 samples

  @planned
  Scenario: a profile made at the device's sample rate adds no latency
    Given I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    When Woodshed Guitar of track 1 loads the NAM capture "Gain 3.nam"
    Then Woodshed Guitar of track 1 reports a latency of 0 samples

  # Its editor (what it shows, the file dialog, the real tone and its cost are checked by hand: README)

  @planned
  Scenario: Woodshed Guitar has an editor of its own
    Given I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    When I open the editor of the source of track 1
    Then the plugin's own editor of the source of track 1 is open
