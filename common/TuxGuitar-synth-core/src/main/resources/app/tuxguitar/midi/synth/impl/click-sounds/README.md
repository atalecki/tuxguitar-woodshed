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
averaged to mono, the silence before it trimmed so that it sounds on the beat (it starts 1 ms before its first sample
at a hundredth of its peak, -40 dB, faded in over that millisecond), at most 2 seconds kept from there, the silence
after it trimmed (below a thousandth of its peak, -60 dB), its peak brought to 0.5 (the code-made Click's level), then
written as 16-bit PCM mono WAVE at its own rate, 44100 Hz. Nothing else was changed.

To make them again: `common/TuxGuitar-synth-core/tools/make-click-sounds.sh <a new, empty download folder>` downloads
the files at the commit above, checks the licence and each SHA-256 below, and writes this folder.
`TestClickSoundFiles` pins the shipped SHA-256s.

| Shipped file | VCSL file | Source SHA-256 | Shipped SHA-256 | Frames |
|---|---|---|---|---|
| wood-block.wav | Idiophones/Struck Idiophones/Woodblock/wood_click_ff.wav | 9a911c66212d50a09494821bf9af8ce6e007612e232b5905f838348cb1d63b75 | 2f827c865a3e57082c03651d5cd5e91aa223125e08ef8ca3f8e3571348d2dda5 | 18515 |
| cowbell.wav | Idiophones/Struck Idiophones/Cowbells/Cowbell1_Hit_v4_rr1_Mid.wav | e32a7a646bddfbc63595de9f64e3e435999c932ebadf0fe98759e754096002a0 | 31ffeb1a49f59c7fdbbc60552187bcfc7951f51b90b977be196e254ba1bf5206 | 30587 |
| snare.wav | Membranophones/Struck Membranophones/Snare Drum, Modern 1/Snare2_HitSN_v9_rr1_Mid.wav | ea0adea05227c09002fb7305613cb2877003dc3ba3467d3b2d60c20f5664d59a | d1aa619fdf5aabcdeb27702056d2705721b63d348f80f62d483a764f2e96727c | 58790 |
| side-stick.wav | Membranophones/Struck Membranophones/Snare Drum, Modern 1/Snare2_stick_v1_rr1_Mid.wav | 98258bc22a11aca0a8eb1d50a2e0a82cbe6357d30e0b55116db0b188015ff648 | b529eb7b1a24ffb06689417491a18775ca84fc0d8f0aae4ba7065d6c1522da41 | 32864 |
| claves.wav | Idiophones/Struck Idiophones/Claves/Claves1_Hit_v3_rr1_Mid.wav | 12285f65ed125f4140d1e8a86b2d6a08bf3283b7bf30c10d68290b8e8230a927 | 529d8d6465b0608ff4154014838f5e09e8fe160c88a02153730d82a71b363df2 | 27330 |
| hi-hat.wav | Idiophones/Struck Idiophones/Hi-Hat Cymbal/HiHat_HitC_v4_rr1_Mid.wav | ccaf3b3a353c22d7aac00c261229c42a1ef083285069f4c76dbafba454b46edd | 503362c73f37d0a339a13d12d0b1f3c6b85fdab32091b90c18a6132b160468f6 | 27031 |
| clap.wav | Idiophones/Struck Idiophones/Claps/SoloClap_vl4.wav | 6611570b169578e3f6be2c253c4148420396e4f8ec1ef3fc3c2df158484ce769 | 84ea7879b02d28e5e8e3f6a9785f68a7923e534ab288f6204db3db99d4a35658 | 8120 |
