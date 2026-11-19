package app.tuxguitar.audio.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import app.tuxguitar.action.TGActionException;
import app.tuxguitar.audio.TGSoundChain;
import app.tuxguitar.audio.TGSoundChainRows;
import app.tuxguitar.audio.TGSoundMode;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.TuxGuitarMessages;
import app.tuxguitar.editor.action.SongActionFixture;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.midi.synth.TGSynthChannel;
import app.tuxguitar.song.models.TGAudioInput;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.song.models.TGChannelParameter;
import app.tuxguitar.vst3.TGVst3Element;
import app.tuxguitar.vst3.TGVst3Plugin;
import app.tuxguitar.vst3.TGVst3Processors;
import app.tuxguitar.vst3.TGVst3State;

/**
 * The sound chain actions on a channel, without a plugin host: what they refuse, and how the chain is kept in the
 * channel's parameters. A result reads as the chain, then "success" when the action reported it.
 */
public class TestSoundChainActions {

	private static final TGVst3Plugin GAIN = new TGVst3Plugin("gain.vst3", "1", "Gain", false);
	private static final TGVst3Plugin SYNTH = new TGVst3Plugin("synth.vst3", "2", "Synth", true);

	private final SongActionFixture fixture = SongActionFixture.onDocument();
	private final TGChannel guitar = fixture.songManager.getChannel(fixture.song, fixture.song.getTrack(0).getChannelId());

	private String run(TGChannel channel, TGActionBase action) {
		fixture.actionContext.setAttribute(TGActionBase.ATTRIBUTE_SUCCESS, null);
		fixture.actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_CHANNEL, channel);
		fixture.execute(action);
		return read(channel) + (Boolean.TRUE.equals(fixture.actionContext.getAttribute(TGActionBase.ATTRIBUTE_SUCCESS)) ? " success" : "");
	}

	// the chain's elements by type, a VST3 plugin by name (the General MIDI names need the app's messages)
	private static String read(TGChannel channel) {
		TGProgram chain = TGSoundChain.get(channel);
		StringJoiner reading = new StringJoiner(" > ");
		reading.add(name(chain.getReceiver()));
		for (int i = 0; i < chain.countOutputs(); i++) {
			reading.add(name(chain.getOutput(i)));
		}
		return reading.toString();
	}

	private static String name(TGProgramElement element) {
		return (TGVst3Element.isVst3(element) ? TGVst3Element.getName(element) : element.getType());
	}

	private String add(TGVst3Plugin plugin) {
		fixture.actionContext.setAttribute(TGAddVst3EffectAction.ATTRIBUTE_PLUGIN, plugin);
		return run(guitar, new TGAddVst3EffectAction(fixture.context));
	}

	private String onEffect(int effect, TGEffectAction action) {
		fixture.actionContext.setAttribute(TGEffectAction.ATTRIBUTE_EFFECT, effect);
		return run(guitar, action);
	}

	private String ids() {
		TGProgram chain = TGSoundChain.get(guitar);
		List<String> ids = new ArrayList<String>();
		for (int i = 0; i < chain.countOutputs(); i++) {
			ids.add(chain.getOutput(i).getId());
		}
		return String.join(" ", ids);
	}

	@Test
	public void eachEffectAddedGetsAnIdNoOtherEffectHas() {
		add(GAIN);
		add(GAIN);
		onEffect(1, new TGRemoveEffectAction(fixture.context));
		assertEquals("gervill > Gain > Gain success", add(GAIN));
		assertEquals("effect-2 effect-3", ids());
	}

	@Test
	public void changesToAnEffectNotThereAreRefused() {
		add(GAIN);
		assertEquals("gervill > Gain", onEffect(1, new TGMoveEffectUpAction(fixture.context)));
		assertEquals("gervill > Gain", onEffect(2, new TGRemoveEffectAction(fixture.context)));
		assertEquals("gervill > Gain", onEffect(0, new TGRemoveEffectAction(fixture.context)));
	}

	@Test
	public void aParameterIsSetOnlyOnAVst3Plugin() {
		add(GAIN);
		fixture.actionContext.setAttribute(TGSetPluginParameterAction.ATTRIBUTE_PARAMETER, 1);
		fixture.actionContext.setAttribute(TGSetPluginParameterAction.ATTRIBUTE_VALUE, 0.5);
		fixture.actionContext.setAttribute(TGEffectAction.ATTRIBUTE_EFFECT, null);
		assertEquals("gervill > Gain", run(guitar, new TGSetPluginParameterAction(fixture.context)));
		fixture.actionContext.setAttribute(TGEffectAction.ATTRIBUTE_EFFECT, 2);
		assertEquals("gervill > Gain", run(guitar, new TGSetPluginParameterAction(fixture.context)));
		fixture.actionContext.setAttribute(TGEffectAction.ATTRIBUTE_EFFECT, 1);
		assertEquals("gervill > Gain success", run(guitar, new TGSetPluginParameterAction(fixture.context)));
		assertEquals("0.5", TGSoundChain.get(guitar).getOutput(0).getParameters().get("vst3.parameter.1"));
	}

	// a plugin's state is kept on its element, found by id: an effect or the source, only when it is a VST3 plugin
	@Test
	public void aPluginsStateIsKeptOnTheVst3ElementWithTheId() {
		add(GAIN);
		String general = setState("source", 0) + " | " + setState("effect-1", 1);
		fixture.actionContext.setAttribute(TGAddVst3EffectAction.ATTRIBUTE_PLUGIN, SYNTH);
		run(guitar, new TGChooseVst3InstrumentAction(fixture.context));
		String instrument = setState("source", 2) + " | " + setState("effect-2", 3);
		TGProgram chain = TGSoundChain.get(guitar);
		assertEquals("gervill > Gain | gervill > Gain success | Synth > Gain success | Synth > Gain | AQ== Ag==",
			general + " | " + instrument + " | " + chain.getOutput(0).getParameters().get("vst3.state") + " " + chain.getReceiver().getParameters().get("vst3.state"));
	}

	// a state of one byte, kept on the element with the id
	private String setState(String element, int value) {
		fixture.actionContext.setAttribute(TGSetPluginStateAction.ATTRIBUTE_ELEMENT, element);
		fixture.actionContext.setAttribute(TGSetPluginStateAction.ATTRIBUTE_STATE, new TGVst3State(new byte[] {(byte) value}, new byte[0]));
		return run(guitar, new TGSetPluginStateAction(fixture.context));
	}

	// what the refusal to open the effect's editor says to an English user
	private String editorRefusal(Integer effect) {
		fixture.actionContext.setAttribute(TGEffectAction.ATTRIBUTE_EFFECT, effect);
		return TuxGuitarMessages.english(() -> assertThrows(TGActionException.class, () -> run(guitar, new TGOpenPluginEditorAction(fixture.context))).getMessage());
	}

	// the editor of an effect not there, or of a plugin that cannot be opened (no host here), is refused saying why,
	// naming it, and nothing changes (the General MIDI source's name needs the app's messages: the features name it)
	@Test
	public void theEditorOfNoPluginOrOfOneThatCannotOpenIsRefusedSayingWhy() {
		TGVst3Processors.install(fixture.context);
		add(GAIN);
		assertEquals("effect 2 is not a VST3 plugin: it has no editor to open | The VST3 plugin Gain could not be opened: no VST3 host is configured | gervill > Gain",
			editorRefusal(2) + " | " + editorRefusal(1) + " | " + read(guitar));
	}

	@Test
	public void anAudioChannelKeepsItsTakesAsTheSource() {
		TGChannel audio = fixture.songManager.getChannel(fixture.song, fixture.songManager.addAudioTrack(fixture.song, TGAudioInput.mono(1)).getChannelId());
		fixture.actionContext.setAttribute(TGAddVst3EffectAction.ATTRIBUTE_PLUGIN, SYNTH);
		assertEquals("takes", run(audio, new TGChooseVst3InstrumentAction(fixture.context)));
		assertEquals("takes", run(audio, new TGChooseGeneralMidiAction(fixture.context)));
		assertEquals("Synth success", run(guitar, new TGChooseVst3InstrumentAction(fixture.context)));
		assertEquals("gervill success", run(guitar, new TGChooseGeneralMidiAction(fixture.context)));
		assertEquals("gervill", run(guitar, new TGChooseTakesAction(fixture.context)));
		assertEquals("takes success", run(audio, new TGChooseTakesAction(fixture.context)));
	}

	// a track's sound switched: the chain as run reads it, then the sound the track has after
	private String toMidi(TGChannel channel) {
		return run(channel, new TGSetMidiSoundAction(fixture.context)) + " " + TGSoundMode.of(channel);
	}

	private String toVst3(TGChannel channel) {
		return run(channel, new TGSetVst3SoundAction(fixture.context)) + " " + TGSoundMode.of(channel);
	}

	private String choose(TGVst3Plugin instrument) {
		fixture.actionContext.setAttribute(TGAddVst3EffectAction.ATTRIBUTE_PLUGIN, instrument);
		return run(guitar, new TGChooseVst3InstrumentAction(fixture.context));
	}

	// MIDI plays the General MIDI program in place of the VST3 instrument, which the song keeps, its state and id with
	// it, while on MIDI; the switch back restores it. The effects stay in both
	@Test
	public void theVst3InstrumentMidiReplacesIsKeptAndTheSwitchBackRestoresIt() {
		choose(SYNTH);
		setState("source", 7);
		add(GAIN);
		String midi = toMidi(guitar);
		String back = toVst3(guitar);
		TGProgramElement restored = TGSoundChain.get(guitar).getReceiver();
		assertEquals("gervill > Gain success MIDI | Synth > Gain success VST3 | source Bw==",
			midi + " | " + back + " | " + restored.getId() + " " + restored.getParameters().get("vst3.state"));
	}

	// the default source (no instrument of the track's own) switched to MIDI keeps nothing: back, it is the default again
	@Test
	public void theDefaultSourceSwitchedToMidiAndBackIsTheDefaultAgain() {
		assertEquals("gervill success MIDI | gervill success VST3 | default source", toMidi(guitar) + " | " + toVst3(guitar) + " | "
			+ (TGSoundChainRows.of(guitar).isDefault() ? "default " : "") + TGSoundChain.get(guitar).getReceiver().getId());
	}

	// a switch to the sound the track has is refused; an audio channel has neither (its source is its takes)
	@Test
	public void aSwitchToTheSoundTheTrackHasOrOnAnAudioChannelIsRefused() {
		TGChannel audio = fixture.songManager.getChannel(fixture.song, fixture.songManager.addAudioTrack(fixture.song, TGAudioInput.mono(1)).getChannelId());
		assertEquals("gervill VST3 | gervill success MIDI | gervill MIDI | takes TAKES | takes TAKES",
			toVst3(guitar) + " | " + toMidi(guitar) + " | " + toMidi(guitar) + " | " + toMidi(audio) + " | " + toVst3(audio));
	}

	// the instrument kept is the song's only while on MIDI: changes of the effects keep it, another source chosen drops
	// it (back from MIDI then, nothing is restored but the default)
	@Test
	public void theInstrumentKeptStaysThroughEffectChangesAndGoesWithAnotherSource() {
		choose(SYNTH);
		toMidi(guitar);
		add(GAIN);
		String throughEffects = toVst3(guitar);
		toMidi(guitar);
		run(guitar, new TGChooseGeneralMidiAction(fixture.context));
		toMidi(guitar);
		assertEquals("Synth > Gain success VST3 | gervill > Gain success VST3", throughEffects + " | " + toVst3(guitar));
	}

	// the instrument kept is in the channel's parameters under keys of its own, only while on MIDI
	@Test
	public void theInstrumentKeptHasKeysOfItsOwnOnlyWhileOnMidi() {
		choose(SYNTH);
		toMidi(guitar);
		String midi = kept();
		toVst3(guitar);
		assertEquals("[sound.kept.enabled, sound.kept.id, sound.kept.param.0.name, sound.kept.param.0.value, sound.kept.param.1.name, "
			+ "sound.kept.param.1.value, sound.kept.param.2.name, sound.kept.param.2.value, sound.kept.param.count, sound.kept.type] | []", midi + " | " + kept());
	}

	private String kept() {
		return keys().stream().filter(key -> key.startsWith("sound.kept.")).collect(Collectors.toList()).toString();
	}

	private void parameter(String key, String value) {
		TGChannelParameter parameter = fixture.songManager.getFactory().newChannelParameter();
		parameter.setKey(key);
		parameter.setValue(value);
		guitar.addParameter(parameter);
	}

	private List<String> keys() {
		List<String> keys = new ArrayList<String>();
		Iterator<TGChannelParameter> parameters = guitar.getParameters();
		parameters.forEachRemaining(parameter -> keys.add(parameter.getKey()));
		keys.sort(null);
		return keys;
	}

	// a song of an older version keeps its sound chain as a custom program; changed, it is kept apart from it
	@Test
	public void aCustomProgramOfAnOlderVersionIsTheChainAndIsKeptApartOnceChanged() {
		parameter(TGSynthChannel.CUSTOM_PROGRAM_PREFIX + ".id", "custom");
		parameter(TGSynthChannel.CUSTOM_PROGRAM_PREFIX + ".type", "lv2");
		parameter("other.key", "kept");
		assertEquals("lv2", read(guitar));
		assertEquals("lv2 > Gain success", add(GAIN));
		assertEquals("[other.key, sound.chain.enabled, sound.chain.id, sound.chain.output.0.enabled, sound.chain.output.0.id, "
			+ "sound.chain.output.0.param.0.name, sound.chain.output.0.param.0.value, sound.chain.output.0.param.1.name, "
			+ "sound.chain.output.0.param.1.value, sound.chain.output.0.param.2.name, sound.chain.output.0.param.2.value, "
			+ "sound.chain.output.0.param.count, sound.chain.output.0.type, sound.chain.output.count, sound.chain.param.count, "
			+ "sound.chain.type]", keys().toString());
	}
}
