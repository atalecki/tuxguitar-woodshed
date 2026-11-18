package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static app.tuxguitar.editor.action.SongActionFixture.reads;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import app.tuxguitar.editor.action.SongActionFixture;
import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.midi.synth.impl.GervillProcessorFactory;
import app.tuxguitar.song.models.TGAudioInput;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.vst3.TGVst3Element;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * What an app shows of a channel's sound chain to edit it: a row per element (the source, then the effects in
 * processing order, a disabled one in brackets) and, for the row picked (-1: none), what can be done with it. The
 * guitar's chain: a source "drone" (no VST3 plugin; elements other than VST3 plugins and the General MIDI program are
 * read by their type), the VST3 effect "Test Gain", then "echo", an effect of another kind, disabled.
 */
class TestSoundChainRows {

	private static final TGVst3Plugin GAIN = new TGVst3Plugin("gain.vst3", "gain", "Test Gain", false);
	private static final TGVst3Plugin SINE = new TGVst3Plugin("sine.vst3", "sine", "Test Sine", true);

	private final SongActionFixture fixture = new SongActionFixture();
	private final TGChannel guitar = this.fixture.songManager.getChannel(this.fixture.song, this.fixture.song.getTrack(0).getChannelId());

	TestSoundChainRows() {
		TGProgram chain = TGSoundChain.get(this.guitar);
		chain.setReceiver(TGSoundChain.source("drone"));
		chain.addOutput(TGVst3Element.create("effect-1", GAIN));
		chain.addOutput(TGSoundChain.source("echo"));
		chain.getOutput(1).setId("effect-2");
		chain.getOutput(1).setEnabled(false);
		TGSoundChain.save(this.guitar, chain, this.fixture.songManager.getFactory());
	}

	// for each row from -1: "<row> <what can be done>"
	private static String can(TGSoundChainRows rows) {
		return IntStream.rangeClosed(-1, 2).mapToObj(row -> row + (rows.isEffect(row) ? " effect " + TGSoundChainRows.effect(row) : " -") + (rows.canMoveUp(row) ? " up" : "") + (rows.canEdit(row) ? " edit" : "") + " " + rows.toggleKey(row).replace("audio.sound-chain.", "")).collect(Collectors.joining(" | "));
	}

	@Test
	void theSourceThenEachEffectInProcessingOrder() {
		assertEquals("Source: drone | 1. Test Gain | 2. [echo]", String.join(" | ", TGSoundChainRows.of(this.guitar).getRows("Source")));
	}

	// the source is not moved, removed or disabled; the first effect is not moved up; only a VST3 plugin has an editor
	@Test
	void whatCanBeDoneWithTheRowPicked() {
		assertEquals("-1 - disable | 0 - disable | 1 effect 1 edit disable | 2 effect 2 up enable", can(TGSoundChainRows.of(this.guitar)));
	}

	// "Keep in song": only a default source that plays an instrument (known to: read with what the channel plays) can be
	// kept; General MIDI playing, or an instrument of the track's own, cannot
	@Test
	void onlyADefaultSourcePlayingAnInstrumentCanBeKept() {
		TGDefaultPlay plays = this.defaultSourcePlayingSine();
		TGDefaultPlay generalMidi = new TGDefaultPlay(TGInstrumentKind.GUITAR, null, null, TGDefaultPlay.Is.NO_DEFAULT);
		TGProgram chain = TGSoundChain.get(this.guitar);
		String byDefault = TGSoundChainRows.of(this.guitar, plays).canKeep() + " " + TGSoundChainRows.of(this.guitar).canKeep() + " " + TGSoundChainRows.of(this.guitar, generalMidi).canKeep();
		chain.setReceiver(TGVst3Element.create("source", SINE));
		TGSoundChain.save(this.guitar, chain, this.fixture.songManager.getFactory());
		assertEquals("true false false false", byDefault + " " + TGSoundChainRows.of(this.guitar, plays).canKeep());
	}

	@Test
	void aVst3InstrumentSourceHasAnEditor() {
		TGProgram chain = TGSoundChain.get(this.guitar);
		chain.setReceiver(TGVst3Element.create("source", SINE));
		TGSoundChain.save(this.guitar, chain, this.fixture.songManager.getFactory());
		assertEquals("Source: Test Sine true", TGSoundChainRows.of(this.guitar).getRows("Source").get(0) + " " + TGSoundChainRows.of(this.guitar).canEdit(0));
	}

	// an audio channel's source is its takes: no instrument nor General MIDI program is chosen for it; a plugin picked
	// is added or chosen only as what it is
	@Test
	void whatCanBeAddedOrChosen() {
		TGChannel audio = this.fixture.songManager.getChannel(this.fixture.song, this.fixture.songManager.addAudioTrack(this.fixture.song, TGAudioInput.mono(1)).getChannelId());
		TGSoundChainRows guitar = TGSoundChainRows.of(this.guitar);
		TGSoundChainRows takes = TGSoundChainRows.of(audio);
		assertEquals("guitar: source true, add true false false, choose false true false | audio: source false, choose false",
			"guitar: source " + guitar.canChooseSource() + ", add " + guitar.canAdd(GAIN) + " " + guitar.canAdd(SINE) + " " + guitar.canAdd(null) + ", choose " + guitar.canChoose(GAIN) + " " + guitar.canChoose(SINE) + " " + guitar.canChoose(null)
			+ " | audio: source " + takes.canChooseSource() + ", choose " + takes.canChoose(SINE));
	}

	// the controls an app's editor enables for the row picked, the plugins picked in its drop-downs given
	private static String enabled(TGSoundChainRows rows, int row, TGVst3Plugin effect, TGVst3Plugin instrument) {
		return Arrays.stream(TGSoundChainRows.Control.values()).filter(control -> rows.isEnabled(control, row, effect, instrument)).map(Enum::name).collect(Collectors.joining(" "));
	}

	// each control enabled exactly as the dialog's buttons were: what can be added or chosen whatever the row, what the
	// row allows, an audio channel's source chosen by no control, and nothing at all without a track
	@TestFactory
	Stream<DynamicTest> theControlsEnabledForEachKindOfRowPicked() {
		TGChannel audio = this.fixture.songManager.getChannel(this.fixture.song, this.fixture.songManager.addAudioTrack(this.fixture.song, TGAudioInput.mono(1)).getChannelId());
		TGSoundChainRows guitar = TGSoundChainRows.of(this.guitar);
		return Stream.of(
			reads("none picked", "ADD_EFFECT USE_AS_SOURCE DEFAULT_INSTRUMENT", () -> enabled(guitar, -1, GAIN, SINE)),
			reads("the source", "ADD_EFFECT USE_AS_SOURCE DEFAULT_INSTRUMENT", () -> enabled(guitar, 0, GAIN, SINE)),
			reads("the first effect", "ADD_EFFECT USE_AS_SOURCE DEFAULT_INSTRUMENT REMOVE TOGGLE EDIT", () -> enabled(guitar, 1, GAIN, SINE)),
			reads("a later effect, disabled", "ADD_EFFECT USE_AS_SOURCE DEFAULT_INSTRUMENT MOVE_UP REMOVE TOGGLE", () -> enabled(guitar, 2, GAIN, SINE)),
			reads("plugins picked of the other kind", "DEFAULT_INSTRUMENT", () -> enabled(guitar, 0, SINE, GAIN)),
			reads("no plugins to pick", "DEFAULT_INSTRUMENT", () -> enabled(guitar, 0, null, null)),
			reads("an audio channel's takes", "ADD_EFFECT", () -> enabled(TGSoundChainRows.of(audio), 0, GAIN, SINE)),
			reads("no track", "", () -> enabled(TGSoundChainRows.NONE, 0, GAIN, SINE)));
	}

	// a default source that plays an instrument can be kept in the song, whatever row is picked
	@Test
	void keepIsEnabledWhileTheDefaultInstrumentPlays() {
		assertEquals("DEFAULT_INSTRUMENT KEEP", enabled(TGSoundChainRows.of(this.guitar, this.defaultSourcePlayingSine()), -1, null, null));
	}

	// the guitar's source made the default instrument (stored as General MIDI), which plays Test Sine
	private TGDefaultPlay defaultSourcePlayingSine() {
		TGProgram chain = TGSoundChain.get(this.guitar);
		chain.setReceiver(TGSoundChain.source(GervillProcessorFactory.TYPE));
		TGSoundChain.save(this.guitar, chain, this.fixture.songManager.getFactory());
		return new TGDefaultPlay(TGInstrumentKind.GUITAR, TGDefaultInstrument.parse("7A1C0008544753494E45000000000008", "Test Sine").get(),
			TGVst3Element.create("default-guitar", SINE), TGDefaultPlay.Is.PLAYED);
	}

	// no track: no rows
	@Test
	void noTrackHasNoRows() {
		assertEquals("[]", TGSoundChainRows.NONE.getRows("Source").toString());
	}

	@Test
	void thePluginsOfferedAsEffectsAndAsInstrumentsAndTheRowOfNoneIsMinusOne() {
		List<TGVst3Plugin> plugins = Arrays.asList(SINE, GAIN);
		assertEquals("[Test Gain] [Test Sine] -1 2", names(TGSoundChainRows.effects(plugins)) + " " + names(TGSoundChainRows.instruments(plugins)) + " " + TGSoundChainRows.row(null) + " " + TGSoundChainRows.row(2));
	}

	private static List<String> names(List<TGVst3Plugin> plugins) {
		return plugins.stream().map(TGVst3Plugin::getName).collect(Collectors.toList());
	}
}
