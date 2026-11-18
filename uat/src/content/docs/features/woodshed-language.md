---
title: Plugin editors inside the app, 4 - Woodshed Guitar speaks TuxGuitar's language
description: Woodshed Guitar's editor says its texts in the language TuxGuitar shows, German included, and changes at once when that language changes.
sidebar:
  order: 35
---

## What it tests

Branch `feature/embedded-editors`, unit 4k (the commit "woodshed, audio: Woodshed Guitar speaks the language TuxGuitar
shows", and later). Q9: Woodshed Guitar follows Settings > Language, and ships German.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at that commit or later.
MIDI Port: the TuxGuitar Synthesizer. Settings > Sound, Default instruments: Guitar tracks **Woodshed Guitar**. View >
Show Sound Chain on (F8).

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | File > New. Look at Woodshed Guitar's editor in the Sound Chain panel. | Its texts are in the language TuxGuitar shows (English: "Load profile...", "Input gain", "Output level", "Quality", "Full"). | not run |
| 2 | Tools > Settings > Language: **Deutsch**; OK. | TuxGuitar's menus turn German, and at once Woodshed Guitar's editor too, while it stays open: "Profil laden...", "Eingangspegel", "Ausgangspegel", "Qualität", "Voll"; its profile line "Profil: RiffSmith Clean.nam". Every label holds its whole text; the sliders get shorter if needed. | not run |
| 3 | Press **Profil laden...**. | The file dialog's title is "Profil laden", its filters "NAM-Captures (*.nam)" and "Alle Dateien (*.*)". Cancel. | not run |
| 4 | Pick a language Woodshed Guitar has no texts in (Português, for example); OK. | Woodshed Guitar's editor speaks English again. | not run |
| 5 | Back to English; OK. Close TuxGuitar and start it again. | Woodshed Guitar's editor opens in English straight away. | not run |
| 6 | Open a third-party plugin's editor. | It keeps its own language (VST3 has no way to tell it). | not run |

## Notes

- The tests cover: woodshed-guitar-text-test (a language by its tag, by the part before "_", English for one without
  texts, the German shipped as TuxGuitar told), nam_guitar_instrument.feature (the editor speaks German as TuxGuitar
  shows it, English for a language without texts, and changes while open).
