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
      | fret | string | pitch   |
      | 0    | 6      | 82.41   |
      | 5    | 1      | 440     |
      | 12   | 1      | 659.26  |
      | 22   | 1      | 1174.66 |

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

  Scenario: Woodshed Guitar sounds the same signal on the left and the right
    Given I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    When I play the song from measure 1 beat 1
    Then track 1's left and right sides are the same, sample for sample

  # The profile. Track 2 holds the same note as track 1, on a channel of its own, and stays dry: it is what Woodshed
  # Guitar sounds with no profile.

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

  Scenario: with no profile loaded, Woodshed Guitar plays no profile
    Given I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    Then Woodshed Guitar of track 1 plays no profile

  Scenario: loading a profile modifies the song
    Given I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have saved the song as a TuxGuitar file
    When Woodshed Guitar of track 1 loads the NAM capture "Gain 3.nam"
    Then the song is modified

  Scenario: loading a profile is one undo step
    Given I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    When I undo
    Then Woodshed Guitar of track 1 plays no profile

  # Refusals: the plugin keeps the profile it had, and its editor shows why (here, what the plugin says)

  Scenario Outline: a file Woodshed Guitar cannot play is refused, saying why, and the profile stays
    Given there is a text file "notes.txt"
    And there is a text file "Broken.nam"
    And there is a NAM capture "Odd.nam" of the architecture "Transformer"
    And there is a NAM capture "Next.nam" of the file version "0.9.0"
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    And I have saved the song as a TuxGuitar file
    When Woodshed Guitar of track 1 loads the file "<file>"
    Then Woodshed Guitar of track 1 refuses it, saying something starting "<refusal>"
    And Woodshed Guitar of track 1 plays the profile "Gain 3.nam"
    And the song is not modified

    Examples:
      | file       | refusal                                                          |
      | notes.txt  | notes.txt is not a NAM capture: Woodshed Guitar plays .nam files |
      | Gone.nam   | Gone.nam could not be read                                       |
      | Broken.nam | Broken.nam could not be loaded:                                  |
      | Odd.nam    | Odd.nam could not be loaded:                                     |
      | Next.nam   | Next.nam could not be loaded:                                    |

  # A1 and A2 captures (README: the test captures). What they sound like is no number the scenario knows: they play
  # when the plugin takes them, sound, and sound other than the dry voice of track 2

  Scenario Outline: A1 and A2 captures play
    Given there is the test NAM capture "<capture>"
    And I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    When Woodshed Guitar of track 1 loads the NAM capture "<capture>"
    And I play the song from measure 1 beat 1
    Then Woodshed Guitar of track 1 plays the profile "<capture>"
    And track 1 sounds, every sample a number, peaking above 0.001 in measure 1
    And track 1 sounds other than track 2 in measure 1

    Examples:
      | capture             |
      | A1 standard test.nam |
      | A2 test.nam         |

  # an A2 capture holds a full and a light model (slimmable): Quality chooses, Full until I change it
  Scenario: Quality Light plays an A2 capture's light model
    Given there is the test NAM capture "A2 test.nam"
    And I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And Woodshed Guitar of track 1 has loaded the NAM capture "A2 test.nam"
    And Woodshed Guitar of track 2 has loaded the NAM capture "A2 test.nam"
    And I have set "Quality" of Woodshed Guitar of track 2 to 1.0
    When I play the song from measure 1 beat 1
    Then track 2 sounds, every sample a number, peaking above 0.001 in measure 1
    And track 1 sounds other than track 2 in measure 1

  # Saving

  Scenario: the profile comes back after saving and reopening the song
    Given I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    And I have saved the song as a TuxGuitar file
    When I open it again
    Then Woodshed Guitar of track 1 plays the profile "Gain 3.nam"

  # the song holds the capture's contents, as a song opened on another computer needs them
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
  # compensation makes up for, so track 1 stays in time with track 2 (to the sample; resampling leaves out what lies
  # above 0.45 times the lower rate, so the two differ a little where a pluck starts)
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
    Then track 1 sounds 3.0 times what track 2 sounds, in time to the sample, within 1% RMS
    And Woodshed Guitar of track 1 reports a latency above 0 samples

  Scenario: a profile made at the device's sample rate adds no latency
    Given I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    When Woodshed Guitar of track 1 loads the NAM capture "Gain 3.nam"
    Then Woodshed Guitar of track 1 reports a latency of 0 samples

  # Its editor: Load profile... (a file dialog for .nam files), the profile's name, and why the last file was refused. How
  # it looks and its file dialog are checked by hand (README); what it shows is read from its window

  Scenario: Woodshed Guitar's editor shows its profile
    Given I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have opened the editor of the source of track 1
    When Woodshed Guitar of track 1 loads the NAM capture "Gain 3.nam"
    Then the plugin's own editor of the source of track 1 is open
    And Woodshed Guitar's editor of track 1 shows "Profile: Gain 3.nam" and ""

  Scenario: Woodshed Guitar's editor shows why a file was refused, and the profile it kept
    Given there is a text file "notes.txt"
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    And I have opened the editor of the source of track 1
    When Woodshed Guitar of track 1 loads the file "notes.txt"
    Then Woodshed Guitar's editor of track 1 shows "Profile: Gain 3.nam" and "Not loaded: notes.txt is not a NAM capture: Woodshed Guitar plays .nam files"

  Scenario: with no profile, Woodshed Guitar's editor says the dry guitar is heard
    Given I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    When I open the editor of the source of track 1
    Then Woodshed Guitar's editor of track 1 shows "No profile: the dry guitar is heard" and ""
