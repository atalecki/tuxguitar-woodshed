# Built-in click sounds

The recordings Count In and the Metronome can click with on the TuxGuitar Synthesizer (see `MidiClickSound`,
`TGClickRecording`, `ClickProcessor`). The code-made "Click" is not a file.

## Source and licence

All seven come from the **Versilian Community Sample Library (VCSL)**, https://github.com/sgossner/VCSL,
commit `c1ea7bcc3c7309650ab0da9d15c9cd1fbc4a4c7e` (2026-01-14). Its `LICENSE` at that commit is
**Creative Commons CC0 1.0 Universal** (public domain dedication): no attribution is required; this note is kept
anyway.

## Processing

Each file was evened as TuxGuitar evens every click sound, own WAV files included (`TGClickRecording`): its channels
averaged to mono, at most 2 seconds kept, the silence after it trimmed (below a thousandth of its peak, -60 dB), its
peak brought to 0.5 (the code-made Click's level), then written as 16-bit PCM mono WAVE at its own rate, 44100 Hz.
Nothing else was changed (the silence before each sound is kept).

To make them again: `common/TuxGuitar-synth-core/tools/make-click-sounds.sh <a new, empty download folder>` downloads
the files at the commit above, checks the licence and each SHA-256 below, and writes this folder.
`TestClickSoundFiles` pins the shipped SHA-256s.

| Shipped file | VCSL file | Source SHA-256 | Shipped SHA-256 | Frames |
|---|---|---|---|---|
| wood-block.wav | Idiophones/Struck Idiophones/Woodblock/wood_click_ff.wav | 9a911c66212d50a09494821bf9af8ce6e007612e232b5905f838348cb1d63b75 | ad109957c89e3b27f8c6a76ca693c7f3d7e28e04ffebf895bdb16120b9c69947 | 19145 |
| cowbell.wav | Idiophones/Struck Idiophones/Cowbells/Cowbell1_Hit_v4_rr1_Mid.wav | e32a7a646bddfbc63595de9f64e3e435999c932ebadf0fe98759e754096002a0 | d1ad588e749d1109a9388b46c5fd0826582150491650a724b77b8818641b8225 | 31003 |
| snare.wav | Membranophones/Struck Membranophones/Snare Drum, Modern 1/Snare2_HitSN_v9_rr1_Mid.wav | ea0adea05227c09002fb7305613cb2877003dc3ba3467d3b2d60c20f5664d59a | 6035cc9c2b8f060d3e191fe158748822e814261dc55c61a12ec582668712a3ed | 59155 |
| side-stick.wav | Membranophones/Struck Membranophones/Snare Drum, Modern 1/Snare2_stick_v1_rr1_Mid.wav | 98258bc22a11aca0a8eb1d50a2e0a82cbe6357d30e0b55116db0b188015ff648 | 35e0450203ccdd832b0c70a16a0481e0c4b820eb44b74332f800fcbfc22137b3 | 33192 |
| claves.wav | Idiophones/Struck Idiophones/Claves/Claves1_Hit_v3_rr1_Mid.wav | 12285f65ed125f4140d1e8a86b2d6a08bf3283b7bf30c10d68290b8e8230a927 | 3a6fc1a4760b9b08c4ba9ca7d97990bfbe46902cc2f7d6a81c348c785c897f1b | 27662 |
| hi-hat.wav | Idiophones/Struck Idiophones/Hi-Hat Cymbal/HiHat_HitC_v4_rr1_Mid.wav | ccaf3b3a353c22d7aac00c261229c42a1ef083285069f4c76dbafba454b46edd | a8807f285c297c1ac0d767e9762d9a982898de6b4597fd2b32274fc546bcf93c | 27355 |
| clap.wav | Idiophones/Struck Idiophones/Claps/SoloClap_vl4.wav | 6611570b169578e3f6be2c253c4148420396e4f8ec1ef3fc3c2df158484ce769 | 033c6410f3508238ef1c32c14b6f4fb1b348ef79d5c61c2abd1239e7c7fd6b3c | 10006 |
