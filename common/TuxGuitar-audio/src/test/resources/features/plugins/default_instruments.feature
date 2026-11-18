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
    And the status line reads nothing

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

  # falling back to General MIDI: a default the scan did not find on this computer, or one it found that could not be
  # loaded, plays General MIDI instead (never silence), and the status line under the fretboard says so quietly, once
  # per song opened or change of the defaults, for 5 seconds or until its cause no longer holds; the log keeps the
  # reason in English. A kind without a default plays General MIDI with nothing said
  Scenario: a default instrument not installed on this computer plays General MIDI, and the status line says so
    Given the default instrument for guitar tracks is "Missing Piano", which is not installed on this computer
    And TuxGuitar's log is recorded
    When I play the song from measure 1 beat 1
    Then track 1 sounds, every sample a number, peaking above 0.001 in measure 1
    And the status line reads "Missing Piano is not installed: guitar tracks play General MIDI."
    And the log has a warning starting "the default instrument for guitar tracks, Missing Piano (0123456789ABCDEF0123456789ABCDEF) plays General MIDI instead: it is not installed"

  Scenario: a default instrument that cannot be loaded plays General MIDI, and the status line says so
    Given the default instrument for drums tracks is "Test Sine"
    And I make track 1's channel a percussion channel
    And the VST3 host cannot start
    And TuxGuitar's log is recorded
    When I play the song from measure 1 beat 1
    Then track 1 sounds, every sample a number, peaking above 0.001 in measure 1
    And the status line reads "Test Sine could not be loaded: drum tracks play General MIDI."
    And the log has a warning starting "the default instrument for drums tracks, Test Sine (7A1C0008544753494E45000000000008) plays General MIDI instead: it could not be loaded: the VST3 host did not start: "

  Scenario: a kind without a default instrument plays General MIDI with nothing said
    Given the default instrument for bass tracks is "Test Sine"
    When I play the song from measure 1 beat 1
    Then track 1 sounds, every sample a number, peaking above 0.001 in measure 1
    And the status line reads nothing

  # a default instrument that could not be loaded is remembered: each later Play plays General MIDI at once, without
  # trying it again, until the defaults change, the VST3 folders are scanned again or the song is opened again (General
  # MIDI's distortion guitar sounds harmonics; Test Sine none)
  Scenario: a default instrument that could not be loaded is not tried again on the next Play
    Given I have set the General MIDI program of track 1's channel to "Distortion Electric Guitar"
    And the default instrument for guitar tracks is "Test Sine"
    And the VST3 host cannot start
    And I have played the song from measure 1 beat 1 and stopped at measure 2 beat 1
    And the VST3 host can start again
    When I play the song from measure 1 beat 1
    Then track 1 sounds a 880 Hz tone above -60 dBFS in measure 1

  Scenario Outline: a default instrument that could not be loaded is tried again once <what>
    Given the default instrument for guitar tracks is "Test Sine"
    And I have saved the song as a TuxGuitar file
    And the VST3 host cannot start
    And I have played the song from measure 1 beat 1 and stopped at measure 2 beat 1
    And the VST3 host can start again
    When <change>
    And I play the song from measure 1 beat 1
    Then track 1 sounds a 440 Hz tone and no 880 Hz tone above -60 dBFS in measures 1

    Examples:
      | what                               | change                                                  |
      | the defaults change                | the default instrument for guitar tracks is "Test Sine" |
      | the VST3 folders are scanned again | I scan the VST3 folders                                 |
      | the song is opened again           | I open it again                                         |

  # an instrument the track chose itself is not a default: when it cannot be loaded the track stays silent, as before
  Scenario: an instrument of the track's own that cannot be loaded stays silent
    Given I have chosen the VST3 instrument "Test Sine" for track 1
    And the VST3 host cannot start
    When I play the song from measure 1 beat 1
    Then track 1 sounds nothing above -60 dBFS from measure 1 beat 1 to measure 2 beat 1
    And the status line reads nothing

  # every text TuxGuitar shows goes through its translations ("{0}" the instrument, "{1}" the kind's tracks)
  Scenario: the status line says a default instrument fell back as the language TuxGuitar shows says it
    Given TuxGuitar's language says "{0} ist nicht installiert: {1} spielen General MIDI." for the message "audio.default-instrument.not-installed"
    And TuxGuitar's language says "Gitarrenspuren" for the message "audio.default-instrument.kind.guitar"
    And the default instrument for guitar tracks is "Missing Piano", which is not installed on this computer
    When I play the song from measure 1 beat 1
    Then the status line reads "Missing Piano ist nicht installiert: Gitarrenspuren spielen General MIDI."

  # the log keeps the reason in English, whatever the language
  Scenario: a default instrument that could not be loaded is said as the language TuxGuitar shows says it
    Given TuxGuitar's language says "{0} ließ sich nicht laden: {1} spielen General MIDI." for the message "audio.default-instrument.not-loaded"
    And TuxGuitar's language says "Schlagzeugspuren" for the message "audio.default-instrument.kind.drums"
    And TuxGuitar's language says "der VST3-Host startete nicht: {0}" for the message "audio.vst3-host.not-started"
    And the default instrument for drums tracks is "Test Sine"
    And I make track 1's channel a percussion channel
    And the VST3 host cannot start
    And TuxGuitar's log is recorded
    When I play the song from measure 1 beat 1
    Then the status line reads "Test Sine ließ sich nicht laden: Schlagzeugspuren spielen General MIDI."
    And the log has a warning starting "the default instrument for drums tracks, Test Sine (7A1C0008544753494E45000000000008) plays General MIDI instead: it could not be loaded: the VST3 host did not start: "

  # Woodshed Guitar, TuxGuitar's own instrument, is the guitar and bass default until another (or General MIDI) is
  # chosen, playing the profile chosen for the kind in the settings, else the profile TuxGuitar ships for the kind next
  # to its own plugins folder ("RiffSmith Clean.nam" for guitar, "RiffSmith Bass Clean.nam" for bass: A2 captures of a
  # clean amp and of a clean bass rig).
  # Play waits while the profile loads, as for a profile loaded in its editor. A profile chosen whose file is not there
  # plays the profile TuxGuitar ships instead, and the status line says so. Track 2 plays Woodshed Guitar chosen as its own
  # instrument, to compare with
  Scenario Outline: a guitar or bass track without an instrument of its own plays Woodshed Guitar through the profile chosen
    Given the VST3 folders hold the test plugins and TuxGuitar's own plugins
    And there is a NAM capture "Gain 3.nam" that multiplies the signal by 3.0
    And I have set the General MIDI program of track 1's channel to "<program>"
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1 at dynamic fff
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And the default instrument for <kind> tracks is "Woodshed Guitar"
    And the <kind> profile in the settings is the NAM capture "Gain 3.nam"
    When I play the song from measure 1 beat 1
    Then track 1 sounds 3.0 times what track 2 sounds, sample for sample within 0.001

    Examples:
      | program                       | kind   |
      | Steel String Acoustic Guitar  | guitar |
      | Fingered Electric Bass        | bass   |

  Scenario Outline: with no profile chosen, Woodshed Guitar plays the profile TuxGuitar ships for the kind
    Given the VST3 folders hold the test plugins and TuxGuitar's own plugins
    And there is the NAM capture TuxGuitar ships, "<shipped>"
    And I have set the General MIDI program of track 1's channel to "<program>"
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1 at dynamic fff
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And Woodshed Guitar of track 2 has loaded the NAM capture "<shipped>"
    And the default instrument for <kind> tracks is "Woodshed Guitar"
    When I play the song from measure 1 beat 1
    Then track 1 sounds, every sample a number, peaking above 0.001 in measure 1
    And track 1 sounds 1.0 times what track 2 sounds, sample for sample within 0.001

    Examples:
      | program                       | kind   | shipped                  |
      | Steel String Acoustic Guitar  | guitar | RiffSmith Clean.nam      |
      | Fingered Electric Bass        | bass   | RiffSmith Bass Clean.nam |

  # the bass profile is its own: one chosen for guitar tracks is not played by bass tracks
  Scenario: a bass track plays the bass profile TuxGuitar ships, not the profile chosen for guitar tracks
    Given the VST3 folders hold the test plugins and TuxGuitar's own plugins
    And there is the NAM capture TuxGuitar ships, "RiffSmith Bass Clean.nam"
    And there is a NAM capture "Gain 3.nam" that multiplies the signal by 3.0
    And I have set the General MIDI program of track 1's channel to "Fingered Electric Bass"
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1 at dynamic fff
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And Woodshed Guitar of track 2 has loaded the NAM capture "RiffSmith Bass Clean.nam"
    And the default instrument for bass tracks is "Woodshed Guitar"
    And the guitar profile in the settings is the NAM capture "Gain 3.nam"
    When I play the song from measure 1 beat 1
    Then track 1 sounds 1.0 times what track 2 sounds, sample for sample within 0.001

  Scenario: a profile chosen whose file is not there plays the profile TuxGuitar ships instead, and the status line says so
    Given the VST3 folders hold the test plugins and TuxGuitar's own plugins
    And there is the NAM capture TuxGuitar ships, "RiffSmith Clean.nam"
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1 at dynamic fff
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And Woodshed Guitar of track 2 has loaded the NAM capture "RiffSmith Clean.nam"
    And the default instrument for guitar tracks is "Woodshed Guitar"
    And the guitar profile in the settings is "Gone.nam", whose file is not there
    When I play the song from measure 1 beat 1
    Then track 1 sounds, every sample a number, peaking above 0.001 in measure 1
    And track 1 sounds 1.0 times what track 2 sounds, sample for sample within 0.001
    And the status line reads "Gone.nam was not found: guitar tracks play RiffSmith Clean, the profile TuxGuitar ships."

  # "{2}" the profile TuxGuitar ships that plays instead
  Scenario: a profile not found is said as the language TuxGuitar shows says it
    Given TuxGuitar's language says "{0} wurde nicht gefunden: {1} spielen {2}, das mitgelieferte Profil." for the message "audio.default-instrument.profile-missing"
    And TuxGuitar's language says "Bassspuren" for the message "audio.default-instrument.kind.bass"
    And the VST3 folders hold the test plugins and TuxGuitar's own plugins
    And I have set the General MIDI program of track 1's channel to "Fingered Electric Bass"
    And the default instrument for bass tracks is "Woodshed Guitar"
    And the bass profile in the settings is "Gone.nam", whose file is not there
    When I play the song from measure 1 beat 1
    Then the status line reads "Gone.nam wurde nicht gefunden: Bassspuren spielen RiffSmith Bass Clean, das mitgelieferte Profil."

  # at a Volume of 32, so the profile's three times the fff note stays below full scale
  Scenario: played live, Play waits for the default instrument's profile to load and plays it from the first note
    Given the VST3 folders hold the test plugins and TuxGuitar's own plugins
    And there is a NAM capture "Large 3.nam" that multiplies the signal by 3.0, with 40 MB of notes in it
    And I have added a guitar track on a channel of its own
    And I have moved the caret to track 2
    And I have entered fret 5 on string 1 at dynamic fff
    And I have chosen the VST3 instrument "Woodshed Guitar" for track 2
    And I have set the Volume of track 1's channel to 32
    And I have set the Balance of track 1's channel to 0
    And I have set the Volume of track 2's channel to 32
    And I have set the Balance of track 2's channel to 127
    And the default instrument for guitar tracks is "Woodshed Guitar"
    And the guitar profile in the settings is the NAM capture "Large 3.nam"
    When I play the song live from measure 1 beat 1
    Then from measure 1 beat 1 on, the output's left side sounds 3.0 times its right side, sample for sample within 0.001
    And the status line reads nothing

  # the profile is the computer's setting, not the song's: Woodshed Guitar telling the app it changed (it does once a
  # profile has loaded) changes nothing in the song
  Scenario: the default instrument's profile loading live leaves the song as it was
    Given the VST3 folders hold the test plugins and TuxGuitar's own plugins
    And there is a NAM capture "Gain 3.nam" that multiplies the signal by 3.0
    And the default instrument for guitar tracks is "Woodshed Guitar"
    And the guitar profile in the settings is the NAM capture "Gain 3.nam"
    And I have saved the song as a TuxGuitar file
    When I play the song live from measure 1 beat 1, the app taking what the plugins tell it as it plays
    Then the song is not modified
    And the app ran no action to take it
