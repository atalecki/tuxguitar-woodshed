---
title: Woodshed Guitar evens out each profile's loudness
description: Every NAM profile that stores its loudness plays at the same level, as in NAM's own plugin, so changing profile (or going from a guitar track to a bass track) does not jump the volume.
sidebar:
  order: 26
---

## What it tests

Commit `1592faa30` on `feature/default-instrument-follow-ups`. Woodshed Guitar now evens out every profile's loudness, the
shipped ones and those you choose: a `.nam` file stores how loud the capture plays a standard input (its loudness), and
Woodshed Guitar brings it to -18 dB, the level NAM's own plugin brings captures to in its default "Normalized" output
mode. A profile that stores no loudness (older or hand-made captures) plays as before. **Output level** still works on
top. Songs saved before open and play as before, only at the evened-out level.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit `1592faa30` or
later on the branch.

- Start TuxGuitar with the TuxGuitar Synthesizer as the MIDI Port and an audio device. In Settings > Sound, Default
  instruments: Guitar tracks **Woodshed Guitar** with the shipped profile (**RiffSmith Clean (shipped with
  TuxGuitar)**), Bass tracks **Woodshed Guitar** with the shipped profile (**RiffSmith Bass Clean**); OK.
- Have two `.nam` captures of your own at hand that were trained by NAM's trainer (any capture downloaded from
  TONE3000 is) and that sound at clearly different levels in a plugin with normalisation turned off (say a quiet clean
  one and a loud high-gain one).
- Optional, for step 6: a song saved with Woodshed Guitar on a track before this commit.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | File > New. On track 1 (guitar) enter a few notes and a chord. Add a second track, set its instrument to **Fingered Electric Bass**, and enter the same notes. Play each track soloed, one after the other. | Both sound at about the same loudness. Before this change the bass was clearly softer (about 6 dB); now it is about as loud as the guitar, or a little louder on the same notes. Neither distorts or crackles. | not run |
| 2 | In Tools > Sound Chain for track 1, press **Keep in song**, then open Woodshed Guitar's editor and **Load profile...** your quiet capture. Play track 1. Then load your loud capture and play again. | The two captures sound at about the same loudness; only the tone changes. | not run |
| 3 | In Woodshed Guitar's editor, set **Output level** to -12 dB, then to +6 dB, playing after each. | Each move changes the level as before: -12 dB clearly quieter, +6 dB louder than at 0 dB. | not run |
| 4 | Save the song, close it and open it again. Play track 1. | It plays at the same level as before saving, with the profile loaded. | not run |
| 5 | Load a capture that stores no loudness (for example one made by hand, or a very old A1 capture), if you have one. Play. | It plays at its own level, as before this change (it may be louder or softer than the others). | not run |
| 6 | Optional: open a song saved before this commit with Woodshed Guitar and a profile. Play it. | It opens without any message and plays its profile; the level may differ from before (it is now evened out). | not run |

## Notes

- Measured with `woodshed-guitar-time` (the same six-string chord each second, 30 s, 44.1 kHz, Full): before, RiffSmith
  Clean peaked at 0.833 (RMS 0.205) and RiffSmith Bass Clean at 0.454 (RMS 0.155); after, 0.950 (RMS 0.234) and 1.388
  (RMS 0.475). The bass capture stores a loudness of -27.7 dB and the guitar one -19.1 dB, so the bass is raised more.
  From that six-string guitar chord the bass capture now peaks above 1.0 (bass tracks playing bass notes were not
  measured). If step 1 crackles or sounds distorted, report it: it is an open question for the product owner.
- The scenarios cover the level arithmetic exactly (nam_guitar_instrument.feature, "Evening out the loudness"), and
  `woodshed-guitar-level-test` checks it in the plugin's own code; this page checks what is heard.
