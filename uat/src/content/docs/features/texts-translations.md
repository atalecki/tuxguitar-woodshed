---
title: Texts through translations
description: The "still loading" hint and the refusals of interleaved practice and click-up go through TuxGuitar's translation files and Woodshed Guitar's editor through a string table of its own; English reads as before, and other languages fall back to English until translated.
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
