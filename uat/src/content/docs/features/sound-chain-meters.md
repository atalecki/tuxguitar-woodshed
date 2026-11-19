---
title: Sound Chain panel, level meters
description: A meter under every button of the chain, then Track out and Master; peaks, a hold line, clip lamps you click to clear.
sidebar:
  order: 38
---

## What it tests

Commit `34798b37d` on `feature/sound-chain-meters`. The Sound Chain panel (F8) now shows how loud the sound is at each
point of the track's chain: a thin meter under each button (the sound after that instrument or effect), then
**Track out** (after the track's Volume and Balance, left and right, with its held peak in dB) and **Master** (the whole
mix, the click included). A bar jumps up with each note and falls back smoothly; a line stays at the highest point for
2 seconds. A small box at a meter's right end fills with a "!" once the sound reached the top (-0.1 dB) and stays so
until you click that meter or press Play again.

Commit `f22ae0cf1` draws every bar in a thin outline (1 pixel at 100% scaling) in the text colour (black on the default
theme), so an empty meter shows against the panel's grey, also in a high-contrast theme. The bars inside keep their
height; each meter grew by the outline (2 pixels per bar).

## Setup

Build and launch with the VS Code task **app: build + run**. In **Tools > Settings > Sound**, set the MIDI Port to
**TuxGuitar Synthesizer**. Open a song with a guitar track that has notes in its first measures (any song from the
examples). Press **F8** if the Sound Chain panel is hidden.

## Steps

| #   | Do | Expect | Result |
| --- | --- | --- | --- |
| 1   | Click a note of the guitar track. Look at the Sound Chain panel. | Under the source's button there is a thin bar, as wide as the button. After the last button: "→ Track out" with a number and two bars, then "Master" with a number and two bars. Each meter has a small square box at its right end. Every bar sits in a thin outline in the text colour (black), the box's outline the same colour. With nothing playing the bars are empty, white inside their outline, easy to find on the panel's grey, and the numbers read "-inf dB". | not run |
| 2   | Press Play. | The bars move with the music: each jumps up with a note and falls back smoothly. A thin line stays at the highest point for about 2 seconds, then falls. The numbers beside Track out and Master follow the highest point, such as "-6.3 dB". | not run |
| 3   | While it plays, hover the source's meter, then Track out's. | A tooltip such as "After Source: ...: peak -9.1 dB, held -6.0 dB", and "Track out: peak ..., held ...". | not run |
| 4   | Stop. Watch the bars. | They fall to empty in about 3 seconds. The lamps that were lit stay lit. | not run |
| 5   | Add an effect to the track (VST3 effect drop-down, **Add**), such as TONE3000 or any gain plugin. Play. | A meter appears under the new button. It moves with the sound that leaves that effect: turning the effect's output up in its editor makes its bar longer, and Track out's too. | not run |
| 6   | Pick the effect's button and press **Disable**. Play. | The effect's meter is empty and greyed inside its outline, like its button. The source's and Track out's meters still move. | not run |
| 7   | Enable the effect again. In the track's mixer (or **Track > Properties**), set Volume to about a quarter. Play. | Track out's bars are much shorter than the effect's bar: Track out shows the level after Volume. | not run |
| 8   | Set Balance all to the left. Play. | Track out's top bar (left) moves, its bottom bar (right) stays empty. Master shows the same. Set Balance back to the middle. | not run |
| 9   | Turn the effect's output (or gain) far up so the sound distorts. Play. | Where the bar goes past the 0 dB mark (a thin line about nine tenths of the way along), the rest of the bar is striped. The meter's box fills and shows a "!". Track out's box fills too once the track is cut at full scale. | not run |
| 10  | Hover a meter with a filled box. | The tooltip ends with "Clipped: click the meter to clear." | not run |
| 11  | Click that meter. | Its box goes back to an empty outline. The other boxes stay as they were. | not run |
| 12  | Leave the effect loud so boxes light. Stop, then press Play again. | Every box goes out when Play starts, then lights again only if the sound clips again. | not run |
| 13  | Pick a bass track that uses the default bass (no instrument of its own). Play. | The source's box may fill: the default bass goes a little over the top inside the chain. This is expected (the owner chose to be told). Lowering the default bass's Output level in its editor stops it. | not run |
| 14  | With the song stopped, enter a note with the keyboard or a click on the fretboard. | The source's bar and Track out's jump up for the note, then fall back. | not run |
| 15  | Make the Sound Chain panel narrow (shrink TuxGuitar's window) so the chain wraps onto two lines. | Each meter stays under its own button. Track out and Master wrap like the buttons. | not run |
| 16  | Set the MIDI Port to another port (not the TuxGuitar Synthesizer). | All meters are empty and greyed inside their outlines. The panel's quiet note says sound chains are heard only with the TuxGuitar Synthesizer. Set it back: the meters work again. | not run |
| 17  | Open Task Manager, note TuxGuitar's CPU while playing with the panel shown. Press F8 to hide the panel; keep playing. | The panel and its meters are hidden. CPU drops by about as much as the meters cost (it should be small either way). Press F8 again: the meters start from empty; lit boxes are still lit. | not run |
| 18  | Pick another track while the first one's Track out box is lit, then pick the first track again. | The first track's box is still lit (its clips are kept while the meters show). | not run |
| 19  | Repeat steps 1, 2 and 9 with Windows display scaling at 100%, 150% and 175%, and drag TuxGuitar to another monitor with a different scaling. | The meters scale with the text: thin bars, a box as high as the meter, readable "!", nothing cut off. | not run |
| 20  | Turn on a Windows high-contrast theme (Settings > Accessibility > Contrast themes), then restart TuxGuitar. Play a loud chain. | The meters use the theme's colours: the track in the window background colour, the bar in the highlight colour, the marks, stripes, boxes and each bar's outline in the text colour. Everything stays visible; an empty meter shows as its outline. | not run |
| 21  | Turn on the grayscale colour filter (Settings > Accessibility > Colour filters > Grayscale). Play a loud chain. | Without colour you can still tell the bar, the striped part above 0 dB, the hold line and a lit box (filled, with "!"). | not run |

## Notes

- An empty meter is a white track in an outline in the text colour, with a thin 0 dB mark and an outlined box. The
  outline is new (commit `f22ae0cf1`): before it, an empty meter was faint on the panel's grey. Say so in the result if
  the outline is cut off on any side or the meter is still hard to find.
- Nothing is measured while the panel is hidden, or while the MIDI Port is another one, or during an export.
- Two tracks on the same channel share one sound chain, so they share its meters.
- A theme change takes effect after TuxGuitar restarts (the app reads the system colours when it starts).
- The levels each meter shows, the clip rules, Play clearing the boxes and "nothing measured while hidden" are covered
  by the tests (`plugins/sound_chain_levels.feature`); this page checks what only a person can see.
