Feature: The Track panel's editor follows the song opened
  The Track panel shows the editor of the plugin the caret's track plays (its editor area). Opening a song stops the
  plugins of the song before, which closes their editors; the panel then shows what the new song's track plays. A track
  that plays a MIDI instrument (General MIDI, e.g. Flute: no default instrument for its kind) has no VST3 plugin, so no
  editor: the panel opens none, and asks for none, so the app shows no error message. A track that plays the same
  plugin as before shows its editor again.
  (The panel's glue is the desktop's; here the editor area is its state machine, TGEditorDock, run as the panel runs
  it: what the plugins did reaches it before the panel follows the new song, as on TuxGuitar's UI thread.)

  Background:
    Given a new song with one guitar track and 4 measures of 4/4 at 120 bpm
    And I have set the MIDI Port to "TuxGuitar Synthesizer"
    And I have set the audio device to "Test Interface"
    And the VST3 folders hold the test plugins
    And I have set the General MIDI program of track 1's channel to "Distortion Electric Guitar"
    And the default instrument for guitar tracks is "Test Sine"

  Scenario: opening a song whose track plays a MIDI instrument opens no editor and shows no error
    Given I have set the General MIDI program of track 1's channel to "Flute"
    And I have saved the song as a TuxGuitar file
    And I have set the General MIDI program of track 1's channel to "Distortion Electric Guitar"
    And the TuxGuitar Synthesizer runs the song's sound chains, each change sent to it as the app sends it
    And the Track panel shows the editor of the source of track 1, "Test Sine"
    When I open it again, the Track panel following it
    Then the app has shown no error message
    And the Track panel's editor area shows nothing

  Scenario: opening a song whose track plays the same plugin shows its editor again
    Given I have saved the song as a TuxGuitar file
    And the TuxGuitar Synthesizer runs the song's sound chains, each change sent to it as the app sends it
    And the Track panel shows the editor of the source of track 1, "Test Sine"
    When I open it again, the Track panel following it
    Then the app has shown no error message
    And the Track panel's editor area shows the editor of "Test Sine"
