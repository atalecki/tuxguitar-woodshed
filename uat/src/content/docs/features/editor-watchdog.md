---
title: Plugin editors inside the app, 2 - a plugin that stops responding
description: A plugin whose editor hangs is ended within about 2 seconds and plays again at once from the song; its editor is not reopened.
sidebar:
  order: 33
---

## What it tests

Branch `feature/embedded-editors`, unit 4g (the commit "vst3, audio: a plugin whose host stops by itself is started
again from its element", and later). A plugin whose editor's code hangs takes its whole host process with it;
TuxGuitar ends that process after about 2 s without an answer, starts the plugin again at once from what the song
holds, and says so in the Sound Chain panel. The same restart applies to a plugin whose process crashes, once until
the song or the chain changes.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at that commit or later.

- MIDI Port: the TuxGuitar Synthesizer, with an audio device. View > Show Sound Chain on (F8).
- TuxGuitar's test effect **Test Gain** (it has a test editor that can be told to hang). Copy
  `common/TuxGuitar-vst3/native-build/VST3/Release/Test Gain.vst3` (built by the audio tests) into a VST3 folder
  TuxGuitar scans, for example `%CommonProgramFiles%\VST3`, then press **Scan again** in the Sound Chain dialog.
- To make its editor hang, the steps use the debug console of the VS Code task (or any Java debugger) only to call
  `TGLivePlugins.getInstance(context).get(channel, element).message("TG.hang-view", Map.of("millis", 20000L), Map.of())`;
  without one, steps 3 to 6 can be checked with a real plugin that hangs, or skipped.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | File > New. Enter a few notes. In the Sound Chain panel add the VST3 effect **Test Gain** and pick its button. | Test Gain's small editor shows in the panel. | not run |
| 2 | Play the song; leave it looping (Player > Loop) while you do the next steps. | The notes sound through Test Gain (louder than without it). | not run |
| 3 | Make Test Gain's editor hang (setup). | For up to about 2 seconds TuxGuitar may not react to clicks. The track of Test Gain goes silent at once; any other track plays on. | not run |
| 4 | Wait 3 seconds. | TuxGuitar reacts again. Test Gain's editor is gone from the panel; the line reads "Test Gain stopped responding and was started again." with **Show editor**. The track sounds again through Test Gain, as loud as before. | not run |
| 5 | Press **Show editor**. | Test Gain's editor shows again in the panel. | not run |
| 6 | Make it hang a second time, wait 3 seconds. | The line reads "Test Gain stopped responding again: it stays silent until the song or its sound chain changes."; no Show editor. The track is silent through Test Gain; other tracks play on. | not run |
| 7 | Remove Test Gain and add it again (or reopen the song). | It plays again; a later hang restarts it once more. | not run |
| 8 | (A real plugin, optional) If a plugin of yours crashes while the song plays, watch the panel. | The plugin is heard again within about a second; with its editor shown, the line reads "<plugin> stopped (<why>) and was started again." | not run |

## Notes

- The tests cover it with the test plugins: TestVst3Host (a host whose editor hangs is ended within 2.5 s while
  nothing plays; a fit or close from TuxGuitar's window gives up within 2.5 s), TestVst3Processors (started again once,
  then left silent until given its element), hosting_vst3_plugins.feature (ended within 3 s, plays again from the song
  with the song's value, its editor not reopened). This page checks what is seen and heard in the running app.
- While a plugin's editor hangs, Windows may make TuxGuitar's window unresponsive too (the input of the two windows is
  tied); that is bounded by the 2 s.
