package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

import app.tuxguitar.midi.synth.TGDefaultSource;
import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.midi.synth.TGProgramPropertiesUtil;
import app.tuxguitar.midi.synth.TGSynthChannel;
import app.tuxguitar.midi.synth.TGSynthChannelProperties;
import app.tuxguitar.midi.synth.impl.GervillProcessorFactory;
import app.tuxguitar.song.factory.TGFactory;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.song.models.TGChannelParameter;
import app.tuxguitar.util.TGMessagesManager;
import app.tuxguitar.vst3.TGVst3Element;

/**
 * What a channel sounds through: its source (the General MIDI program the channel plays, a VST3 instrument, or an audio
 * channel's takes), followed by its effects, in processing order, each enabled or disabled. It is the TuxGuitar
 * Synthesizer's program of the channel, kept in the channel's parameters under the sound chain's keys (TGSynthChannel
 * plays them); a channel without one sounds the custom program older versions saved, else its source alone. The reading
 * names the source and the effects joined by " > ", a disabled effect in brackets.
 */
public final class TGSoundChain {

	// the source type of an audio channel's chain: its takes, rendered by the audio engine
	public static final String TAKES = "takes";
	// the name of that source in TuxGuitar's language: "takes"
	private static final String TAKES_NAME = "audio.source.takes";
	// the id of the source of a track's sound set to MIDI (midi)
	private static final String MIDI_ID = "midi";
	// the keys of the instrument a track's sound set to MIDI replaced, kept while on MIDI (kept)
	private static final String KEPT_PREFIX = "sound.kept";

	private TGSoundChain() {
	}

	public static String read(TGChannel channel) {
		TGProgram chain = get(channel);
		StringJoiner reading = new StringJoiner(" > ");
		reading.add(name(chain.getReceiver(), channel));
		for (int i = 0; i < chain.countOutputs(); i++) {
			reading.add(effect(chain.getOutput(i), channel));
		}
		return reading.toString();
	}

	// an effect read by its name, a disabled one in brackets
	static String effect(TGProgramElement effect, TGChannel channel) {
		return (effect.isEnabled() ? name(effect, channel) : "[" + name(effect, channel) + "]");
	}

	// a General MIDI source is read by the channel's program, an audio channel's takes as TuxGuitar's language names them;
	// a VST3 plugin by its name; any other element (a VST3 element naming no plugin too) by its type
	static String name(TGProgramElement element, TGChannel channel) {
		if (names(element)) {
			return TGVst3Element.getName(element);
		}
		if (GervillProcessorFactory.TYPE.equals(element.getType())) {
			return TGMessagesManager.getMidiInstrument(channel.getProgram()).getName();
		}
		return (TAKES.equals(element.getType()) ? TGMessagesManager.getProperty(TAKES_NAME) : element.getType());
	}

	// a copy of the channel's chain
	public static TGProgram get(TGChannel channel) {
		TGSynthChannelProperties properties = properties(channel);
		TGProgram chain = TGProgramPropertiesUtil.getProgram(properties, TGSynthChannel.SOUND_CHAIN_PREFIX);
		if (chain == null) {
			chain = TGProgramPropertiesUtil.getProgram(properties, TGSynthChannel.CUSTOM_PROGRAM_PREFIX);
		}
		if (chain == null) {
			chain = new TGProgram();
			chain.setReceiver(source(channel.isAudioChannel() ? TAKES : GervillProcessorFactory.TYPE));
		}
		return chain;
	}

	// the element of the chain: an effect by its number from 1, or, without one, the source
	private static TGProgramElement at(TGProgram chain, Integer effect) {
		return (effect != null ? chain.getOutput(effect - 1) : chain.getReceiver());
	}

	// the VST3 plugin of the chain (an effect by its number, or the source); null when that is no VST3 plugin, or a VST3
	// element naming none (the Synthesizer dialog adds one by its type alone)
	public static TGProgramElement plugin(TGProgram chain, Integer effect) {
		TGProgramElement element = at(chain, effect);
		return (element != null && names(element) ? element : null);
	}

	// whether the element is a VST3 plugin: a VST3 element naming its plugin (module and class id)
	private static boolean names(TGProgramElement element) {
		return (TGVst3Element.isVst3(element) && TGVst3Element.plugin(element.getParameters(), false) != null);
	}

	// the name of the channel's chain's element (an effect by its number, or the source), as the reading names it; an
	// effect not there by its number
	public static String name(TGProgram chain, Integer effect, TGChannel channel) {
		TGProgramElement element = at(chain, effect);
		return (element != null ? name(element, channel) : "effect " + effect);
	}

	// the element of the chain with the id, or null
	public static TGProgramElement element(TGProgram chain, String id) {
		if (chain.getReceiver().getId().equals(id)) {
			return chain.getReceiver();
		}
		for (int i = 0; i < chain.countOutputs(); i++) {
			if (chain.getOutput(i).getId().equals(id)) {
				return chain.getOutput(i);
			}
		}
		return null;
	}

	// the source the type names (a General MIDI program follows the channel's Bank and Program)
	public static TGProgramElement source(String type) {
		TGProgramElement source = new TGProgramElement();
		source.setId("source");
		source.setType(type);
		return source;
	}

	// the source of a track's sound set to MIDI: General MIDI marked plain, so no default plays in its place; of an id of
	// its own, apart from the VST3 instrument's ("source") the song keeps meanwhile (kept), as elements are known by id
	// (the live plugins, the editor area)
	public static TGProgramElement midi() {
		TGProgramElement midi = source(GervillProcessorFactory.TYPE);
		midi.setId(MIDI_ID);
		midi.setParameter(TGDefaultSource.PLAIN_GENERAL_MIDI, Boolean.TRUE.toString());
		return midi;
	}

	// the instrument a track's sound set to MIDI replaced, kept while on MIDI; null for none
	public static TGProgramElement kept(TGChannel channel) {
		return TGProgramPropertiesUtil.getElement(properties(channel), KEPT_PREFIX);
	}

	// the chain into the channel's parameters, under the sound chain's keys; the custom program's are dropped
	public static void save(TGChannel channel, TGProgram chain, TGFactory factory) {
		save(channel, chain, kept(channel), factory);
	}

	// with the instrument the song keeps while on MIDI (null: none), saved only when the chain's source is MIDI's
	public static void save(TGChannel channel, TGProgram chain, TGProgramElement instrument, TGFactory factory) {
		List<TGChannelParameter> parameters = parameters(channel, chain, instrument, factory);
		channel.removeParameters();
		parameters.forEach(channel::addParameter);
	}

	private static List<TGChannelParameter> parameters(TGChannel channel, TGProgram chain, TGProgramElement instrument, TGFactory factory) {
		List<TGChannelParameter> parameters = new ArrayList<TGChannelParameter>();
		Iterator<TGChannelParameter> kept = channel.getParameters();
		while (kept.hasNext()) {
			TGChannelParameter parameter = kept.next();
			if (!isChainKey(parameter.getKey())) {
				parameters.add(parameter.clone(factory));
			}
		}
		TGSynthChannelProperties saved = new TGSynthChannelProperties();
		TGProgramPropertiesUtil.setProgram(saved, TGSynthChannel.SOUND_CHAIN_PREFIX, chain);
		if (instrument != null && TGSoundMode.isMidi(chain.getReceiver())) {
			TGProgramPropertiesUtil.setElement(saved, KEPT_PREFIX, instrument);
		}
		saved.getProperties().forEach((key, value) -> {
			TGChannelParameter parameter = factory.newChannelParameter();
			parameter.setKey(key);
			parameter.setValue(value);
			parameters.add(parameter);
		});
		return parameters;
	}

	private static boolean isChainKey(String key) {
		return (key.startsWith(TGSynthChannel.SOUND_CHAIN_PREFIX + ".") || key.startsWith(TGSynthChannel.CUSTOM_PROGRAM_PREFIX + ".") || key.startsWith(KEPT_PREFIX + "."));
	}

	private static TGSynthChannelProperties properties(TGChannel channel) {
		TGSynthChannelProperties properties = new TGSynthChannelProperties();
		Iterator<TGChannelParameter> parameters = channel.getParameters();
		while (parameters.hasNext()) {
			TGChannelParameter parameter = parameters.next();
			properties.setValue(parameter.getKey(), parameter.getValue());
		}
		return properties;
	}
}
