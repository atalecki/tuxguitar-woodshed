package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.Map;

import app.tuxguitar.midi.synth.TGAudioProcessor;
import app.tuxguitar.midi.synth.TGAudioProcessorFactory;
import app.tuxguitar.midi.synth.TGMidiProcessorFactory;
import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.midi.synth.TGSynthManager;
import app.tuxguitar.song.factory.TGFactory;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.TGMessagesManager;
import app.tuxguitar.util.log.TGLog;
import app.tuxguitar.vst3.TGVst3Element;
import app.tuxguitar.vst3.TGVst3Exception;
import app.tuxguitar.vst3.TGVst3Instance;
import app.tuxguitar.vst3.TGVst3PluginProcessor;
import app.tuxguitar.vst3.TGVst3State;

/**
 * Before a song is saved, each VST3 plugin of each sound chain gives its state, which the chain keeps in place of the
 * parameters set since the last one: saved with the song, it comes back byte for byte. An enabled plugin that runs (see
 * TGLivePlugins: one kept open only while it follows the song's element) gives the state it has as it runs, and is the
 * song's from then on; any other opens for it in a helper of its own, with what the chain holds, and closes again (a
 * disabled effect gives its state too). A channel without VST3 plugins is left as it is.
 */
final class TGPluginStates {

	private static final TGLog LOG = TGLog.get(TGPluginStates.class);
	// the refusal to open a plugin, in TuxGuitar's language: {0} the plugin, {1} the host's reason
	private static final String NOT_OPENED = "audio.plugin.refusal.not-opened";

	private TGPluginStates() {
	}

	static void store(TGContext context, TGSong song, TGFactory factory, float sampleRate) {
		Iterator<TGChannel> channels = song.getChannels();
		while (channels.hasNext()) {
			TGChannel channel = channels.next();
			TGProgram chain = TGSoundChain.get(channel);
			List<TGProgramElement> plugins = plugins(chain);
			Map<String, TGVst3Instance> gave = new HashMap<String, TGVst3Instance>();
			for (TGProgramElement plugin : plugins) {
				store(context, channel.getChannelId(), chain, plugin, sampleRate, gave);
			}
			if (!plugins.isEmpty()) {
				TGSoundChain.save(channel, chain, factory);
			}
			gave.forEach((element, plugin) -> TGLivePlugins.getInstance(context).took(channel.getChannelId(), element, plugin));
		}
	}

	// from the plugin as it runs when it is enabled (it gave the state, by element), else (or when it is gone) from a
	// fresh one
	private static void store(TGContext context, int channel, TGProgram chain, TGProgramElement plugin, float sampleRate, Map<String, TGVst3Instance> gave) {
		TGVst3Instance running = (plugin.isEnabled() ? TGLivePlugins.getInstance(context).find(channel, plugin.getId()) : null);
		if (running != null && store(running, plugin)) {
			gave.put(plugin.getId(), running);
		} else {
			store(processor(context, plugin, chain), plugin, sampleRate);
		}
	}

	// false when the plugin is gone
	private static boolean store(TGVst3Instance running, TGProgramElement plugin) {
		try {
			TGVst3State state = running.getState();
			TGVst3Element.setState(plugin, state);
			running.agree(state);
			return true;
		} catch (TGVst3Exception e) {
			LOG.debug("the running VST3 plugin of " + plugin.getId() + " is gone, its state is read from a fresh one: " + e.getMessage());
			return false;
		}
	}

	// the chain's VST3 elements: its source, then its effects
	private static List<TGProgramElement> plugins(TGProgram chain) {
		List<TGProgramElement> plugins = new ArrayList<TGProgramElement>();
		plugins.add(chain.getReceiver());
		for (int i = 0; i < chain.countOutputs(); i++) {
			plugins.add(chain.getOutput(i));
		}
		plugins.removeIf(element -> !TGVst3Element.isVst3(element));
		return plugins;
	}

	// a new processor of the element: the source's from the MIDI processor factories, an effect's from the audio ones
	static TGAudioProcessor processor(TGContext context, TGProgramElement plugin, TGProgram chain) {
		TGSynthManager manager = TGSynthManager.getInstance(context);
		if (plugin == chain.getReceiver()) {
			TGMidiProcessorFactory factory = manager.findMidiProcessorFactory(plugin.getType());
			return factory.createProcessor();
		}
		TGAudioProcessorFactory factory = manager.findAudioProcessorFactory(plugin.getType());
		return factory.createProcessor();
	}

	// a new processor of the element as the TuxGuitar Synthesizer opens it, at the audio device's rate, its plugin open;
	// refused naming the plugin and the host's reason (the processor closed)
	static TGAudioProcessor opened(TGContext context, TGProgramElement element, TGProgram chain) throws TGVst3Exception {
		TGAudioProcessor processor = processor(context, element, chain);
		processor.setSampleRate(TGAudioSaving.sampleRate(context));
		processor.restoreParameters(element.getParameters());
		TGVst3PluginProcessor plugin = (TGVst3PluginProcessor) processor;
		if (plugin.getInstance() == null) {
			processor.close();
			throw new TGVst3Exception(TGMessagesManager.getProperty(NOT_OPENED, new Object[] {TGVst3Element.getName(element), plugin.getProblem()}));
		}
		return processor;
	}

	// an instrument's element (one without a state of its own, say a default instrument's) as a song keeps it as a
	// chain's source: opened in a helper of its own, its state once it has restored (a profile loading), then closed
	static TGProgramElement kept(TGContext context, TGProgramElement element) throws TGVst3Exception {
		TGAudioProcessor processor = opened(context, element, alone(element));
		try {
			restored(((TGVst3PluginProcessor) processor).getInstance());
			TGProgramElement kept = TGSoundChain.source(element.getType());
			processor.storeParameters(kept.getParameters());
			return kept;
		} finally {
			processor.close();
		}
	}

	// an instrument's element as a song keeps it as a chain's source, with the state given (what the plugin holds)
	static TGProgramElement kept(TGProgramElement element, TGVst3State state) {
		TGProgramElement kept = TGVst3Element.create(TGSoundChain.source(element.getType()).getId(), TGVst3Element.plugin(element.getParameters(), true));
		TGVst3Element.setState(kept, state);
		return kept;
	}

	// a chain whose source is the instrument's element, alone
	static TGProgram alone(TGProgramElement element) {
		TGProgram chain = new TGProgram();
		chain.setReceiver(element);
		return chain;
	}

	// once the plugin no longer restores its state in the background (for a while at most, see TGVst3Instance)
	static void restored(TGVst3Instance instance) {
		while (instance.isRestoring()) {
			LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
		}
	}

	private static void store(TGAudioProcessor processor, TGProgramElement plugin, float sampleRate) {
		try {
			processor.setSampleRate(sampleRate);
			processor.restoreParameters(plugin.getParameters());
			processor.storeParameters(plugin.getParameters());
		} finally {
			processor.close();
		}
	}
}
