---
title: Texts through translations
description: The "still loading" hint goes through TuxGuitar's translation files and Woodshed Guitar's editor through a string table of its own; English reads as before, and other languages fall back to English until translated.
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
