Feature: Woodshed Guitar, the guitar instrument that plays through a NAM capture
  Woodshed Guitar is a VST3 instrument TuxGuitar ships in its own plugins folder, so a tablature plays back with a real
  amp tone. Each note plucks a string of its own (a synthesized plucked string: a clean signal, as a guitar's DI out
  gives), the strings ring together, and the sum plays through its profile: the NAM capture (a .nam file, a Neural Amp
  Modeler model of an amp or pedal) loaded into it, the same signal on the left and the right. With no profile loaded
  it sounds its dry voice, never nothing. The profile is loaded in its own editor (Load profile...), kept in the song
  with the capture's contents (a reopened song plays that copy), and played at the capture's own sample rate. It runs in its helper process like every
  VST3 plugin, and is offered in the Sound Chain like any VST3 instrument; it is also the default instrument of guitar and
  bass tracks (see default_instruments).

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
    And I have set "<parameter>" of the VST3 instrument of track 1 to <dB>
    When I play the song from measure 1 beat 1
    Then track 1 sounds <times> times what track 2 sounds, sample for sample within 0.001

    Examples:
      | parameter    | dB    | times |
      | Input gain   | -20.0 | 0.3   |
      | Output level | -20.0 | 0.3   |
      | Output level | 0.0   | 3.0   |

  # Evening out the loudness. A capture stores how loud it plays a standard input (its loudness, in dB, as NAM's trainer
  # measures it); NAM's own plugin evens every capture out to -18 dB from it (its Normalized output, the default), and so
  # does Woodshed Guitar, so changing profile does not jump the level. A capture that stores none (Gain 3.nam, and the
  # captures of the scenarios above) plays at its own level, as before. Output level scales what is evened out.

  Scenario Outline: a profile that stores its loudness is evened out to -18 dB
    Given there is a NAM capture "Gain 3 at <loudness> dB.nam" that multiplies the signal by 3.0, its loudness <loudness> dB
    And I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    When Woodshed Guitar of track 1 loads the NAM capture "Gain 3 at <loudness> dB.nam"
    And I play the song from measure 1 beat 1
    Then track 1 sounds <times> times what track 2 sounds, sample for sample within 0.001

    # 6 dB below -18 doubles the level (10^(6/20) = 1.995), 6 dB above halves it
    Examples:
      | loudness | times |
      | -24.0    | 5.986 |
      | -18.0    | 3.0   |
      | -12.0    | 1.504 |

  Scenario: Output level scales a profile evened out
    Given there is a NAM capture "Gain 3 at -24 dB.nam" that multiplies the signal by 3.0, its loudness -24.0 dB
    And I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3 at -24 dB.nam"
    And I have set "Output level" of the VST3 instrument of track 1 to -20.0
    When I play the song from measure 1 beat 1
    Then track 1 sounds 0.599 times what track 2 sounds, sample for sample within 0.001

  # the song holds the capture's contents, its loudness with them: a reopened song plays it evened out as it did
  Scenario: a profile evened out plays at the same level after saving and reopening the song
    Given there is a NAM capture "Gain 3 at -24 dB.nam" that multiplies the signal by 3.0, its loudness -24.0 dB
    And I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3 at -24 dB.nam"
    And I have saved the song as a TuxGuitar file
    And the NAM capture "Gain 3 at -24 dB.nam" has been moved away
    When I open it again
    And I play the song from measure 1 beat 1
    Then track 1 sounds 5.986 times what track 2 sounds, sample for sample within 0.001
    And the state of every VST3 plugin is byte for byte what the plugin gave when the song was saved

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

  # Loading in the background: a profile is read and made ready on a thread of Woodshed Guitar's own, so its helper goes
  # on answering and playing; the profile it had plays on until the new one is ready, which then takes over with a
  # crossfade of 30 ms while it sounds (at once after a silent block). Played offline (an export, and the
  # scenarios that do not play live), each block waits for a profile being loaded, so it plays from the next block.
  # Track 1 is heard on the left of the output, track 2 on the right, each at the same Volume, low enough that neither
  # reaches full scale. Live, Play waits for the profile the song holds while it loads (see the last scenario here), so
  # what is measured starts at the first note

  Scenario: Woodshed Guitar answers while it loads a profile, and the one it had stays until the new one is ready
    Given there is the test NAM capture "A1 standard test.nam"
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    And I have opened the editor of the source of track 1
    When Woodshed Guitar of track 1 starts loading the NAM capture "A1 standard test.nam"
    Then Woodshed Guitar's editor of track 1 shows "Profile: Gain 3.nam" and "Loading A1 standard test.nam..."
    And once it has loaded, Woodshed Guitar's editor of track 1 shows "Profile: A1 standard test.nam" and ""

  Scenario: a profile loaded while the song plays live takes over with a crossfade of 30 ms, the sound never stopping
    Given there is a NAM capture "Gain 2.nam" that multiplies the signal by 2.0
    And track 1 has a quarter note on every beat
    And I have added a guitar track on a channel of its own
    And track 2 has a quarter note on every beat
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And I have set the Volume of track 1's channel to 64
    And I have set the Balance of track 1's channel to 0
    And I have set the Volume of track 2's channel to 64
    And I have set the Balance of track 2's channel to 127
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    When I play the song live from measure 1 beat 1, Woodshed Guitar of track 1 loading the NAM capture "Gain 2.nam" at measure 2 beat 1
    Then from measure 1 beat 1 on, the output's left side sounds 3.0 times its right side, then 2.0 times, changing over in 30 ms within 2 ms, never outside those two

  Scenario: a file refused while the song plays live leaves the profile playing
    Given there is a text file "notes.txt"
    And track 1 has a quarter note on every beat
    And I have added a guitar track on a channel of its own
    And track 2 has a quarter note on every beat
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And I have set the Volume of track 1's channel to 64
    And I have set the Balance of track 1's channel to 0
    And I have set the Volume of track 2's channel to 64
    And I have set the Balance of track 2's channel to 127
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    When I play the song live from measure 1 beat 1, Woodshed Guitar of track 1 loading the file "notes.txt" at measure 2 beat 1
    Then from measure 1 beat 1 on, the output's left side sounds 3.0 times its right side, sample for sample within 0.001

  # the resampling's delay is reported from the block the new profile plays, so plugin delay compensation realigns the
  # tracks from the next one
  Scenario: a profile made at another sample rate, loaded while the song plays live, keeps the tracks in time
    Given there is a NAM capture "Gain 3 at 48 kHz.nam" that multiplies the signal by 3.0, made at 48000 Hz
    And track 1 has a quarter note on every beat
    And I have added a guitar track on a channel of its own
    And track 2 has a quarter note on every beat
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And I have set the Volume of track 1's channel to 64
    And I have set the Balance of track 1's channel to 0
    And I have set the Volume of track 2's channel to 64
    And I have set the Balance of track 2's channel to 127
    When I play the song live from measure 1 beat 1, Woodshed Guitar of track 1 loading the NAM capture "Gain 3 at 48 kHz.nam" at measure 2 beat 1
    Then from measure 3 beat 1 on, the output's left side sounds 3.0 times its right side, in time to the sample, within 1% RMS

  # Play waits while a plugin loads a profile (Woodshed Guitar says so as a VST3 plugin restoring its state in the
  # background does), for 5 seconds at most, so the first note already has the amp tone: here the song is played as soon
  # as it is opened again, while track 1's profile, which takes a while to load, still loads
  Scenario: played live, a reopened song waits for a capture that takes long to load and plays it from its first note
    Given there is a NAM capture "Large 3.nam" that multiplies the signal by 3.0, with 40 MB of notes in it
    And track 1 has a quarter note on every beat
    And I have added a guitar track on a channel of its own
    And track 2 has a quarter note on every beat
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And I have set the Volume of track 1's channel to 64
    And I have set the Balance of track 1's channel to 0
    And I have set the Volume of track 2's channel to 64
    And I have set the Balance of track 2's channel to 127
    And Woodshed Guitar of track 1 has loaded the NAM capture "Large 3.nam"
    And I have saved the song as a TuxGuitar file
    When I open it again
    And I play the song live from measure 1 beat 1
    Then from measure 1 beat 1 on, the output's left side sounds 3.0 times its right side, sample for sample within 0.001

  # A1 and A2 captures (README: the test captures). What they sound like is no number the scenario knows: they play
  # when the plugin takes them, sound, and sound other than the dry voice of track 2, shifted or not (both are made at
  # 48 kHz, so track 1 reports the resampling's latency)

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
    And track 1 sounds other than track 2 at every lag up to 64 samples, by more than 10% RMS

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
    And I have set "Quality" of the VST3 instrument of track 2 to 1.0
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

  # the song plays the profile it was saved with, its own copy, whatever became of the file since: a song sounds the
  # same each time it opens
  Scenario: a reopened song plays its own copy of the profile when the NAM capture's file has changed
    Given I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    And I have saved the song as a TuxGuitar file
    And the NAM capture "Gain 3.nam" has been changed to multiply the signal by 2.0
    When I open it again
    And I play the song from measure 1 beat 1
    Then track 1 sounds 3.0 times what track 2 sounds, sample for sample within 0.001
    And Woodshed Guitar of track 1 plays the profile "Gain 3.nam"

  # the file at the profile's path, when it is there and no longer what the song holds, is told in the editor, which
  # offers to load it as it is now
  Scenario Outline: Woodshed Guitar's editor says when the profile's file is no longer what the song holds
    Given I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    And I have saved the song as a TuxGuitar file
    And <what became of the file>
    And I have opened it again
    When I open the editor of the source of track 1
    Then Woodshed Guitar's editor of track 1 shows "Profile: Gain 3.nam" and ""
    And Woodshed Guitar's editor of track 1 says "<notice>", offering "<button>"

    Examples:
      | what became of the file                                                       | notice                                             | button           |
      | the NAM capture "Gain 3.nam" has been changed to multiply the signal by 2.0  | The profile file has changed since it was loaded.  | Update from file |
      | the NAM capture "Gain 3.nam" has been moved away                             |                                                    |                  |
      | the NAM capture "Gain 3.nam" has been left as it was                         |                                                    |                  |

  Scenario: Update from file plays the NAM capture's file as it is now, and modifies the song
    Given I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    And I have saved the song as a TuxGuitar file
    And the NAM capture "Gain 3.nam" has been changed to multiply the signal by 2.0
    And I have opened it again
    And I have opened the editor of the source of track 1
    When I click "Update from file" in Woodshed Guitar's editor of track 1
    And I play the song from measure 1 beat 1
    Then track 1 sounds 2.0 times what track 2 sounds, sample for sample within 0.001
    And Woodshed Guitar's editor of track 1 says "", offering ""
    And the song is modified

  Scenario: Update from file is one undo step
    Given I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    And I have saved the song as a TuxGuitar file
    And the NAM capture "Gain 3.nam" has been changed to multiply the signal by 2.0
    And I have opened it again
    And I have opened the editor of the source of track 1
    And I have clicked "Update from file" in Woodshed Guitar's editor of track 1
    When I undo
    And I play the song from measure 1 beat 1
    Then track 1 sounds 3.0 times what track 2 sounds, sample for sample within 0.001
    And Woodshed Guitar of track 1 says "The profile file has changed since it was loaded."

  # a capture is stored once in the song however many tracks play it: the song grows by one profile, not one per track.
  # (Woodshed Guitar's own state stays whole wherever TuxGuitar does not keep its profile apart: in another program it
  # holds the capture's contents, as before)
  Scenario: a capture that several tracks play is stored once in the song
    Given there is the test NAM capture "A1 standard test.nam"
    And I have added a guitar track on a channel of its own
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And Woodshed Guitar of track 1 has loaded the NAM capture "A1 standard test.nam"
    And I have saved the song as a TuxGuitar file
    When Woodshed Guitar of track 2 loads the NAM capture "A1 standard test.nam"
    Then the song saved now is larger than the song saved before by less than a tenth of the size of "A1 standard test.nam"

  Scenario: tracks sharing a capture each play it again after saving and reopening, whatever became of the file
    Given there is a NAM capture "Gain 2.nam" that multiplies the signal by 2.0
    And I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 3
    And I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 4
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 3
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 4
    And I have set "Output level" of the VST3 instrument of track 2 to -20.0
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    And Woodshed Guitar of track 2 has loaded the NAM capture "Gain 3.nam"
    And Woodshed Guitar of track 3 has loaded the NAM capture "Gain 2.nam"
    And I have saved the song as a TuxGuitar file
    And the NAM capture "Gain 3.nam" has been moved away
    And the NAM capture "Gain 2.nam" has been moved away
    When I open it again
    And I play the song from measure 1 beat 1
    Then track 1 sounds 3.0 times what track 4 sounds, sample for sample within 0.001
    And track 2 sounds 0.3 times what track 4 sounds, sample for sample within 0.001
    And track 3 sounds 2.0 times what track 4 sounds, sample for sample within 0.001
    And the state of every VST3 plugin is byte for byte what the plugin gave when the song was saved
    And a reader of file format 2.0 opens it with 4 measures

  # a song saved before captures were stored once holds the capture in its Woodshed Guitar's own state
  Scenario: a song whose Woodshed Guitar holds its capture in its own state still plays it
    Given I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And I have saved the song as a TuxGuitar file, Woodshed Guitar of track 1 holding "Gain 3.nam" in its own state
    And the NAM capture "Gain 3.nam" has been moved away
    When I open it again
    And I play the song from measure 1 beat 1
    Then track 1 sounds 3.0 times what track 2 sounds, sample for sample within 0.001
    And Woodshed Guitar of track 1 plays the profile "Gain 3.nam"

  # offline (an export), the plugin opened as the render starts waits for the profile it is given back, however long it
  # takes to load
  Scenario: played offline, a reopened song plays a capture that takes long to load from its first sample
    Given there is a NAM capture "Large 3.nam" that multiplies the signal by 3.0, with 5 MB of notes in it
    And I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And Woodshed Guitar of track 1 has loaded the NAM capture "Large 3.nam"
    And I have saved the song as a TuxGuitar file
    When I open it again
    And I play the song from measure 1 beat 1
    Then track 1 sounds 3.0 times what track 2 sounds, sample for sample within 0.001

  # a song whose capture cannot be read (its file damaged) opens with the dry voice, saying why
  Scenario: a song whose Woodshed Guitar holds a capture it cannot read plays no profile, saying why
    Given there is a text file "notes.txt"
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have saved the song as a TuxGuitar file, Woodshed Guitar of track 1 holding "notes.txt" in its own state
    When I open it again
    Then Woodshed Guitar of track 1 plays no profile
    And Woodshed Guitar of track 1 refuses it, saying something starting "notes.txt could not be loaded: it is not the JSON of a .nam file"

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

  # Its editor: Load profile... (a file dialog for .nam files), the profile's name, why the last file was refused, and
  # its parameters (TuxGuitar shows its parameter window only for plugins without an editor of their own). How it looks
  # and its file dialog are checked by hand (README); what it shows is read from its window, and a control is moved as
  # the user moves it (the control set, then told to its window as the user's move is)

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

  Scenario Outline: a control moved in Woodshed Guitar's editor changes the sound
    Given I have entered fret 5 on string 1
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And Woodshed Guitar of track 1 has loaded the NAM capture "Gain 3.nam"
    And I have opened the editor of the source of track 1
    When I set "<parameter>" to <value> in Woodshed Guitar's editor of track 1
    And I play the song from measure 1 beat 1
    Then track 1 sounds <times> times what track 2 sounds, sample for sample within 0.001

    Examples:
      | parameter    | value | times |
      | Input gain   | -20.0 | 0.3   |
      | Output level | -20.0 | 0.3   |

  Scenario: a control moved in Woodshed Guitar's editor modifies the song, one undo step
    Given I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have saved the song as a TuxGuitar file
    And I have opened the editor of the source of track 1
    When I set "Quality" to 1 in Woodshed Guitar's editor of track 1
    Then the song is modified
    And Woodshed Guitar of track 1 has "Quality" at 1.0
    When I undo
    Then Woodshed Guitar of track 1 has "Quality" at 0.0

  Scenario: Woodshed Guitar's editor shows its parameters as they are
    Given I have chosen the VST3 instrument "Woodshed Guitar" for track 1
    And I have set "Output level" of the VST3 instrument of track 1 to -6.0
    And I have set "Quality" of the VST3 instrument of track 1 to 1.0
    When I open the editor of the source of track 1
    Then Woodshed Guitar's editor of track 1 shows the controls "Input gain 0.0 dB, Output level -6.0 dB, Quality Light"
