---
title: Texts through translations
description: The "still loading" hint, the refusals of interleaved practice and click-up, and the audio, VST3 and NAM texts (the VST3 host's failure reasons and the export dialog's format words too) go through TuxGuitar's translation files and Woodshed Guitar's editor through a string table of its own; English reads as before, and other languages fall back to English until translated.
sidebar:
  order: 17
---

## What it tests

Commits `7816968c5` (the loading hint as TuxGuitar messages) and `f27f9054f` (Woodshed Guitar's string table) on
`feature/countin-downbeat-i18n`. Every text shown to the user now goes through a translation mechanism. Only English
is filled in: in English nothing should read differently, and in another language the new texts show in English
(nothing blank, no raw keys such as `audio.loading-hint` or `profile.none`).

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `f27f9054f`
or later.

- The setup of **Woodshed Guitar, slice 4**: the TuxGuitar Sequencer and Synthesizer with an audio device, a capture
  **Huge** that takes more than 5 seconds to load, a capture **Lead** that loads at once, and `slice4.tg` (track 1 on
  Woodshed Guitar with **Huge** loaded).
- A file that is not a capture, such as a `notes.txt`.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Settings > Language: English. Open `slice4.tg` and press Play at once. | After about 5 seconds the song plays and the line at the foot of the window reads "Woodshed Guitar is still loading Huge.nam", exactly as before. It goes once the capture has loaded. | not run |
| 2 | Stop. Open track 1's Woodshed Guitar editor. | It reads as before: "Load profile...", "Profile: Huge.nam", "Input gain", "Output level", "Quality" with "Full" and "Light". | not run |
| 3 | In the editor, Load profile... | The file dialog is titled "Load profile", its file types read "NAM captures (*.nam)" and "All files (*.*)", and choosing the first lists only `.nam` files. | not run |
| 4 | In the dialog choose "All files (*.*)", then `notes.txt`. | The editor reads "Not loaded: notes.txt is not a NAM capture: Woodshed Guitar plays .nam files", and keeps "Profile: Huge.nam". | not run |
| 5 | Settings > Sound: the click sound's note label. | It reads "Drum note (other MIDI Ports)". | not run |
| 6 | Settings > Language: Deutsch (or any language other than English), OK, restart TuxGuitar if asked. Open `slice4.tg` and press Play at once. | TuxGuitar's menus are in that language; the line at the foot of the window still reads "Woodshed Guitar is still loading Huge.nam" in English (no translation yet), never `audio.loading-hint`. | not run |
| 7 | Stop. Settings > Sound. | The click sound's note label reads "Drum note (other MIDI Ports)" in English (not translated yet), not `midi.click.note`. | not run |
| 8 | Open track 1's Woodshed Guitar editor. | Its texts are English as in step 2 (the plugin ships English only, whatever the system's language), none blank and no keys such as `editor.load`. | not run |
| 9 | Settings > Language back to the one you use. | TuxGuitar reads as before. | not run |

## Notes

- The tests already cover: the hint said through the translation files (a made-up German in the features), English
  where a language lacks it, the English texts of the hint and of Woodshed Guitar's editor and refusals unchanged
  (the Woodshed Guitar features), and how the plugin chooses a text (its own check program: a language's text, English
  for a missing language or text, arguments filled once). What they cannot check is the running app in another
  language and the file dialog.
- Woodshed Guitar picks its language from Windows' display language, not from TuxGuitar's Language setting. With only
  English shipped there is no difference to see yet.
- Woodshed Guitar's parameter names in a host's own parameter list ("Input gain", "Output level", "Quality") stay
  English on purpose.

## Practice refusals (feature/practice-texts-i18n)

### What it tests

Commit `0a80dc603` on `feature/practice-texts-i18n` (its English pinned first in `00ce609bd`). The error messages of Interleaved Practice and Interleaved Click-Up (a
session or click-up that cannot be played as set, a flag the session cannot take, a sequencer that cannot play
practice, Record during practice) now come from the translation files, the measure, block or tempo they name too.
Only English is filled in: in English they read exactly as before; in another language they read in English, never as
a raw key such as `transport.practice.refusal.repeat`.

### Setup

Build and launch with the VS Code task **app: build + run** at commit `0a80dc603` or later. Settings >
Sound: the TuxGuitar Sequencer. A song of at least 8 measures (a new song with measures added is enough).

### Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Settings > Language: English. Transport > Interleaved Practice...: each turn plays a measure, pool from 5 to 5, 2 turns, "No consecutive repeats" on, Start. | An error message reads "Measure 5 would come twice in a row." and nothing plays. | not run |
| 2 | Interleaved Practice... again: pool from 0 to 2, Start. | The error reads "The pool starts before measure 1." | not run |
| 3 | Interleaved Practice... again: pool from 1 to 4, "Interval clock" on, 2 minutes per turn, session length 1 minute, Start. | The error reads "A session needs at least the minutes of one turn." | not run |
| 4 | Interleaved Practice... again: pool from 1 to 2, interval clock off, 6 turns, Start. During the first turn, flag measure 2 as 3x difficult. | The flag is refused: the error reads "Measure 2 comes up 3 times in a cycle of 4 turns and would come twice in a row.", and the session plays on. Stop. | not run |
| 5 | Only if Settings > Sound lists a sequencer other than the TuxGuitar Sequencer: choose it, then start Interleaved Click-Up... with its defaults. Then choose the TuxGuitar Sequencer again. | The error reads "Interleaved click-up needs a sequencer that takes turns added while it plays, and "(that sequencer's name)" does not: choose the TuxGuitar Sequencer in Settings > Sound." | not run |
| 6 | Settings > Language: Deutsch (or any language other than English), OK, restart TuxGuitar if asked. Repeat steps 1 and 2. | The menus and the dialog's labels are in that language (as far as translated); the two errors read in English exactly as in steps 1 and 2, never as `transport.practice.refusal.repeat` or with an empty name. | not run |
| 7 | Start a session that plays (pool from 1 to 4, 6 turns). | The turn shown under the Transport window's position reads as before ("Turn 1 of 6: Measure 3" or its translation). Stop. | not run |
| 8 | Settings > Language back to the one you use. | TuxGuitar reads as before. | not run |

### Notes

- The tests already cover: every refusal's English text, unchanged (the practice features, lib's and editor-utils'
  tests), for every kind of unit; and a made-up German said through the translation files for a session's refusal, a
  click-up's (the tempo in it too), a pool's (the unit counted in it too), the sequencer's (the practice named in it
  too) and Record's during a click-up. What they cannot check is the running app's error message.
- Most click-up refusals cannot be reached from its dialog (its fields stop values out of range), and Tools > Record is
  greyed out during a session, so its refusal is not shown there either.

## Audio, VST3 and NAM texts (feature/remaining-texts-i18n)

### What it tests

Commit `be6843271` on `feature/remaining-texts-i18n` (its English pinned first in `11c032929`). The texts of the audio
features now come from the translation files: the Sound Chain window's scan lines (effects and instruments found,
modules skipped, folders scanned, the NAM capture hint and the capture players it names), an audio track's source
("takes"), the line at the top of the Audio Device window, the error when a VST3 plugin or its editor cannot be opened,
and the error when a take inside a song file cannot be read. Only English is filled in: in English they read exactly
as before; in another language they read in English, never as a raw key such as `audio.scan.found`. The reasons these
texts quote from the plugin host, the audio library or the device (after a colon or in brackets) stay as those give
them.

### Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `be6843271` or
later. Settings > Sound: MIDI Port "TuxGuitar Synthesizer", and an audio device chosen in Tools > Audio Device....

- A `.nam` capture (any, such as **Lead.nam**) copied into one of the VST3 folders the scan lists, and no NAM capture
  player (Gateway, the TONE3000 plugin or the Neural Amp Modeler plugin) installed. If one is installed, step 2 names it
  instead.
- A song with an audio track (Tools > Add Audio Track).

### Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Settings > Language: English. Select track 1, Tools > Sound Chain..., and press Scan again. | When the scan ends, the text under the lists reads "VST3 effects found: N. VST3 instruments found: M." (your counts), each module skipped as "Skipped [module]: [why]", and last "Folders scanned: " with the folders, as before. | not run |
| 2 | Read the second line. | It reads "NAM captures found: 1. A .nam capture is a model of an amp or pedal, not a plugin: a NAM capture player plays it, and the scan recognised none. Install Gateway (the official Neural Amp Modeler plugin, free from the Neural Amp Modeler website) or the TONE3000 plugin (free from tone3000.com); A2 captures need one of these two. Add the player to the track as a VST3 effect after the instrument, then load the capture in its editor." (with a player installed: "... not a plugin: [player] plays it. Add [player] to the track ..."). | not run |
| 3 | Close it. Select the audio track, Tools > Sound Chain.... | The first row reads "Source: takes". Close it. | not run |
| 4 | Tools > Audio Device.... | The top line reads "TuxGuitar audio: [output device] at 44100 Hz, latency in [n] out [n] frames" (your device and latencies), as before. Cancel. | not run |
| 5 | Settings > Language: Deutsch (or any language other than English), OK, restart TuxGuitar if asked. Repeat steps 1 to 4. | The windows' own labels are in that language as far as translated; every text checked in steps 1 to 4 reads in English exactly as there, none blank and none a key such as `audio.scan.found`, `audio.capture-player.gateway`, `audio.source.takes` or `audio.device.open`. | not run |
| 6 | Settings > Language back to the one you use. | TuxGuitar reads as before. | not run |

### The VST3 host's failure reasons and the export formats

Commits on `feature/remaining-texts-i18n`: `8038b63df` (the three reasons the VST3 host failed, said through the
translation files and logged in English; their English pinned first in `be54bdb4e`), `79be324ad` and `a7812c9e7` (the
export dialog shows each format by a label, chosen by its name; the words "16-bit", "24-bit" and "32-bit float" said
through the translation files, the types WAVE, AIFF and AU as they are) and `faae7b847` (an unused message deleted).
Build and launch with the VS Code task **app: build + run** at `faae7b847` or later, as in the setup above.

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Settings > Language: English. File > Export, choose the audio file export. | The Options dialog's Audio Format list reads "WAVE 16-bit", "WAVE 24-bit", "WAVE 32-bit float", "AIFF 16-bit", "AU 16-bit", the first chosen, as before. | not run |
| 2 | Choose "WAVE 24-bit", OK, and save the file. | The file is written; a player or the file's properties show a 24-bit WAVE file at 44100 Hz. | not run |
| 3 | Close TuxGuitar. In the folder TuxGuitar runs from, rename `tuxguitar-vst3-host.exe` (in the folder of the native libraries, next to the audio library) to `tuxguitar-vst3-host.off`. Launch TuxGuitar, select track 1, Tools > Sound Chain.... | The text under the lists reads "The VST3 folders could not be scanned: the VST3 host did not start: [the program's path]". | not run |
| 4 | Open the log file (the WARN lines of today's log in TuxGuitar's log folder). | It holds "the VST3 host did not start: [the program's path]" in English, with the system's reason. | not run |
| 5 | Settings > Language: Deutsch (or any language other than English), OK, restart TuxGuitar if asked. Repeat steps 1 and 3. | The dialog's and window's own labels are in that language as far as translated; the formats and the scan's line read in English exactly as in steps 1 and 3 (no translation yet), none blank and none a key such as `audio.export.format.pcm-16` or `audio.vst3-host.not-started`. Choosing a format and exporting works as in step 2. | not run |
| 6 | Close TuxGuitar, rename `tuxguitar-vst3-host.off` back to `tuxguitar-vst3-host.exe`, Settings > Language back to the one you use. | The scan finds your plugins again; TuxGuitar reads as before. | not run |

- The tests cover the three reasons in English and in a made-up German where the user reads them (an effect's
  problem, a scan that failed, the editor refusal), the English log under another language, and the export labels in
  English and German with the names unchanged. A host that stops or stops answering is hard to bring about by hand (a
  plugin that crashes or hangs); the tests use test plugins that do.
- The reasons the host process itself gives ("not a VST3 module: ...") and the audio library's stay as they give them.

### A take that cannot be read, and an unconfigured VST3 host (fix/record-refusal-feedback)

Commits on `fix/record-refusal-feedback`: `3bfaa5e3e` (opening a song whose take cannot be read says so, with the
reason: "A take in this song could not be read: Not a wave file", through the translation files; before, the error read
the bare reason) and `38f287dac` ("no VST3 host is configured" said through the translation files, logged in English).
Build and launch with the VS Code task **app: build + run** at `cdaa0ffaa` or later, as in the setup above. You need an
archive tool that can replace a file inside a zip (such as 7-Zip) and a short text file (any `.txt`).

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Settings > Language: English. Arm the audio track, record a few seconds (Tools > Record, then Stop) and File > Save As a TuxGuitar file, `damaged.tg`. Close the song. | Saved as usual. | not run |
| 2 | Copy `damaged.tg` to `damaged.zip` and open it in the archive tool. In its `takes` folder, replace the wave file (such as `1.wav`) with your text file renamed to that name. Save the archive and rename it back to `damaged.tg` (over the first). | The archive tool shows `version.txt`, `content.xml` and the folder `takes` holding a wave file per take. | not run |
| 3 | File > Open `damaged.tg`. | The song is not opened; the error message reads "A take in this song could not be read: Not a wave file", no Java class names. | not run |
| 4 | Settings > Language: Deutsch (or any language other than English), OK, restart TuxGuitar if asked. Repeat step 3. | The message reads in English exactly as in step 3 (no translation yet), never `audio.take.refusal.unreadable` nor `audio.take.refusal.not-wave`. | not run |
| 5 | Settings > Language back to the one you use. Delete `damaged.tg` and `damaged.zip`. | TuxGuitar reads as before. | not run |

- "no VST3 host is configured" cannot be brought about in the desktop app: when its audio starts, TuxGuitar always says
  where the host program is (next to the audio library), and a missing program reads "the VST3 host did not start:
  ..." instead (step 3 of the previous table). The tests cover it in English, in a made-up German, and in English in
  the log.
- The tests cover the damaged take's message in English and a made-up German, opening a song file whose takes are no
  wave files, and a take codec that refuses.

### Notes

- The tests already cover: every one of these texts in English, unchanged (TestPluginScan, TestLiveAudio, TestTakeWave,
  TestSoundChainActions and the plugin features); and a made-up German said through the translation files for the
  scan's lines and capture hint (a scan made earlier is said in the language shown when it is read), the device's
  description, a take file's three refusals, and the refusals to open the editor of an audio track's source, a plugin
  that cannot be opened and the editor of a plugin whose host stopped. What they cannot check is the running app's
  windows and error messages.
- The errors when a plugin or its editor cannot be opened, and when a take cannot be read, are hard to bring about by
  hand (a plugin uninstalled after a song was saved with it; a damaged song file); the tests cover them. The error
  opening a song whose take is damaged said the bare reason ("Not a wave file"), with no Java class names; since
  `3bfaa5e3e` it says which part of the song failed: see the steps just above.
- The Sound Chain window's Edit... is greyed out for a source that is no VST3 plugin, so its refusal ("takes is not a
  VST3 plugin: it has no editor to open") is not shown there.
