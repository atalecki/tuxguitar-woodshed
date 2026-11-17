---
title: Woodshed Guitar, slice 4 (the "still loading" hint)
description: When Play stops waiting for a capture that is still loading, a quiet line at the foot of the window says which plugin is still loading what, until it has loaded.
sidebar:
  order: 16
---

## What it tests

Commits `f721b5642` (plugins say what they load) and `33dbb5a34` (the hint) on `feature/nam-guitar-instrument`.
A follow-up to **Woodshed Guitar, slice 3**:

- Play waits up to 5 seconds for a capture that is still loading. If the capture is still loading when Play starts
  anyway, a quiet line appears at the foot of the main window, under the fretboard: "Woodshed Guitar is still loading
  <file name>". It goes away by itself once that capture has loaded, whether or not the song is still playing.
- There is no popup, and no line at all when Play's wait was enough (the capture loaded within the 5 seconds), or when
  nothing was loading.

## Setup

Close any running TuxGuitar first. Build and launch with the VS Code task **app: build + run** at commit
`33dbb5a34` or later on the branch.

- In Settings > Sound, choose the TuxGuitar Sequencer and the TuxGuitar Synthesizer, and an audio device.
- You need a capture that takes **more than 5 seconds** to load (**Huge**). If none of yours does, make one: copy a small
  A1 `.nam`, open the copy in a text editor, and before the last `}` add
  `, "metadata": {"notes": "..."}` with a few hundred MB of text between the quotes (or ask for a script that does it).
  Also have a small capture (**Lead**) that loads in a moment.
- File > New. Enter a few bars of notes on track 1. Choose Woodshed Guitar as track 1's instrument (Sound Chain) and
  Load profile... **Huge**; wait until its editor no longer says "Loading". Save the song as `slice4.tg`.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Close `slice4.tg`, open it again, and press Play at once. | For about 5 seconds nothing plays (Play waits). Then the song starts, and a line at the foot of the window, under the fretboard, reads "Woodshed Guitar is still loading Huge.nam" (your file's name). No dialog opens. | not run |
| 2 | Keep the song playing until **Huge** has loaded (the notes take on the amp tone). | Within about a quarter of a second of the tone coming in, the line disappears and the window's layout closes up again (the space it took is given back). | not run |
| 3 | Repeat step 1, but press Stop as soon as the line appears. | The line stays while the capture loads, though nothing plays, and disappears once it has loaded (open the editor: when it no longer says "Loading", the line has gone). | not run |
| 4 | Load profile... **Lead** in track 1's editor and press Play straight away. | Play may hold for a moment; no line appears at any point. | not run |
| 5 | With the song loaded and nothing loading, press Play and Stop a few times. | No line appears. | not run |
| 6 | Hide the fretboard (View menu, untick the fretboard) and repeat step 1. | The line still appears at the foot of the window, and goes once the capture has loaded. | not run |

## Notes

- The tests already cover: the hint appears only when Play stopped waiting while a plugin was still loading, names
  the plugin and what it loads ("its sound" when a plugin does not say), goes once that plugin has finished, and does
  not appear when Play's wait was enough; Woodshed Guitar says the file it loads, and the newest one when a load is
  given while another is under way. This page checks what a person sees: where the line is, that it is quiet, and that
  it goes in time.
- The line is checked four times a second, so it may appear or go up to a quarter of a second after the moment.
- Any VST3 plugin that announces a background state restoration gets the same hint, with its own name.
