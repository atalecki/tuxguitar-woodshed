---
title: NAM capture player keeps its capture
description: A capture loaded in TONE3000's own editor stays on Play and when another track's sound chain changes, and the song keeps it.
sidebar:
  order: 12
---

## What it tests

Commits `c07e1ff3b` and `7af8b9f3c` on `fix/nam-sound-chain-state`. Before this fix, a capture loaded in a capture
player's own editor (TONE3000, Gateway) went blank when you pressed Play. Setting up the player on a second track also
blanked the first. Now the running plugin keeps what its editor did when the song sends it the same sound chain again.
The song takes the capture as soon as the plugin says it changed, or at the latest when you close the plugin's window.
That change is one undo step and marks the song modified.

## Setup

Close any running TuxGuitar first: the build copies the app's jars, and a running TuxGuitar holds them locked. Then build
and launch with the VS Code task **app: build + run**, at commit `7af8b9f3c` or later on the branch. The VST3 host is
rebuilt with it (protocol version 4); an older host is refused.

- In Settings > Sound, choose the TuxGuitar Sequencer and the TuxGuitar Synthesizer.
- Install the TONE3000 plugin and download two or three different captures (`.nam` files), say **A**, **B** and **C**,
  so that each sounds clearly different (a clean amp, a high-gain one, a fuzz pedal).
- File > New, then add a second guitar track (Track > Add Track) so each track has a channel of its own. Enter a few
  notes in both tracks, so Play sounds both.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1 | Open track 1's sound chain, add the VST3 effect TONE3000 and click Edit. In TONE3000's window load capture **A**. Close the window by its close box. | Edit > Undo is now available (the song took the capture). | not run |
| 2 | Press Play, let it play a few seconds, then stop. Open track 1's TONE3000 editor again. | Track 1 sounds through capture **A** from the first note. The editor still shows **A**, not blank. | not run |
| 3 | Open track 2's sound chain, add TONE3000, load capture **B** in its editor and close the window. | Track 2's editor shows **B**. Open track 1's editor: it still shows **A**. | not run |
| 4 | Press Play. | Track 1 sounds through **A**, track 2 through **B**. Neither editor goes blank. | not run |
| 5 | Open track 1's editor and load capture **C**. Leave the window open and press Play. | Track 1 sounds through **C**; the editor keeps showing **C**. | not run |
| 6 | Stop. Close track 1's window by its close box. Then Edit > Undo once. | After the undo, track 1 goes back to **A**: its editor (open it again if needed) shows **A**, and Play sounds **A**. | not run |
| 7 | Edit > Redo. Save the song, close it and open it again. | Track 1's editor shows **C** and track 2's **B**, and Play sounds each. | not run |
| 8 | With track 1's TONE3000 window open, change nothing and close it by its close box. | Edit > Undo still offers the change before (the redo of step 7 or the save), not a new step: closing it changed nothing. | not run |

## Notes

- Whether TONE3000 tells its host when you load a capture (VST3 "dirty") is not known. If it does, Edit > Undo is
  available as soon as you load, before you close the window. If it does not, the song takes the capture when you close
  the window (or save). Until then the running plugin keeps it on Play. Either way it is fine; note which one you saw
  in step 1.
- Two tracks on one channel share one sound chain by design: adding TONE3000 "to the second track" then adds a second
  effect to the same chain. The setup gives each track its own channel.
- Covered by the tests, no need to check twice: the Test Gain plugin keeping an unrecorded change while the channel is
  sent again, the undo of a stored change, the protocol version check.
