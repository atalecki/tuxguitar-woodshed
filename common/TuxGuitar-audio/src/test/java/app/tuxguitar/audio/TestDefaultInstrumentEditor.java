package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.audio.action.TGKeepDefaultInstrumentAction;
import app.tuxguitar.audio.action.TGSetPluginStateAction;
import app.tuxguitar.audio.features.EditorSteps;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.SongActionFixture;
import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.song.models.TGAudioInput;
import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.midi.synth.TGSynthChannel;
import app.tuxguitar.midi.synth.TGSynthModel;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.vst3.TGVst3Element;
import app.tuxguitar.vst3.TGVst3Exception;
import app.tuxguitar.vst3.TGVst3Hosting;
import app.tuxguitar.vst3.TGVst3Instance;
import app.tuxguitar.vst3.TGVst3InstrumentProcessor;
import app.tuxguitar.vst3.TGVst3PluginProcessor;

/**
 * The editor of the default instrument a track plays as its source (Edit on a default source), with the real host
 * process and test plugins: the guitar's default is Test Sine, the guitar's chain's source General MIDI. The first change
 * made in that editor keeps the default in the song with that change (the Sound Chain dialog's Keep in song, one change
 * of the song), and the editor goes on editing it there; opening and closing it without a change keeps nothing, and a
 * change heard after the default itself changed (another default in Settings) keeps nothing either. A capture loaded in
 * the plugin's editor saying so is the change (Test Sine has no parameters).
 */
public class TestDefaultInstrumentEditor {

	private final SongActionFixture fixture = SongActionFixture.onDocument();
	private final TGLivePlugins live = TGLivePlugins.getInstance(this.fixture.context);
	private final TGChannel guitar = this.fixture.songManager.getChannel(this.fixture.song, this.fixture.song.getTrack(0).getChannelId());
	// the changes of the song, in order: the action and the element
	private final List<String> changes = new ArrayList<String>();
	private TGVst3Hosting hosting;
	// after each test: the plugins kept open, then the host processes
	@AutoClose
	private final AutoCloseable plugins = TestPluginHosting.closing(this.fixture.context);

	@BeforeEach
	public void setUp() throws TGVst3Exception {
		this.hosting = TestPluginHosting.host(this.fixture.context);
		this.guitar.setProgram((short) 25);
		this.useDefault("Test Sine");
		this.fixture.map(new TGSetPluginStateAction(this.fixture.context) {
			protected boolean change(TGActionContext context, TGChannel channel, TGProgram chain) {
				changes.add("state " + context.getAttribute(ATTRIBUTE_ELEMENT));
				return super.change(context, channel, chain);
			}
		}, new TGKeepDefaultInstrumentAction(this.fixture.context) {
			protected boolean change(TGActionContext context, TGChannel channel, TGProgram chain) {
				changes.add("keep");
				return super.change(context, channel, chain);
			}
		});
	}

	private void useDefault(String plugin) {
		TGDefaultInstrument instrument = TGDefaultInstrument.parse(this.hosting.find(plugin).getClassId(), plugin).get();
		TGDefaultInstrumentSource.install(this.fixture.context).use(TGDefaultInstruments.NONE.with(TGInstrumentKind.GUITAR, instrument));
	}

	// Edit on the guitar's source: its editor, as TGOpenPluginEditorAction opens it
	private TGPluginEditor edit() throws TGVst3Exception {
		return this.live.openEditor(this.guitar.getChannelId(), "default-guitar", 0, "");
	}

	private TGVst3Instance plugin(String element) throws TGVst3Exception {
		return this.live.get(this.guitar.getChannelId(), element);
	}

	// a capture loaded in the plugin's own editor, saying so; then the app drains, as its UI thread does
	private void load(TGVst3Instance plugin, long capture) throws TGVst3Exception {
		plugin.message("TG.load", Map.of("capture", capture, "dirty", 1L), Collections.<String, byte[]>emptyMap());
		this.live.drain();
	}

	private void closeBox(TGVst3Instance plugin) throws TGVst3Exception {
		EditorSteps.closeBox(plugin);
		this.live.drain();
	}

	// the guitar's source as the song has it (a VST3 instrument with the capture a plugin opened afresh from it says),
	// else "General MIDI"
	private String song() throws TGVst3Exception {
		TGProgramElement source = TGSoundChain.plugin(TGSoundChain.get(this.guitar), null);
		return (source != null ? TGVst3Element.getName(source) + " capture " + capture(source) : "General MIDI");
	}

	private int capture(TGProgramElement source) throws TGVst3Exception {
		TGVst3InstrumentProcessor fresh = new TGVst3InstrumentProcessor(this.hosting);
		try {
			fresh.restoreParameters(source.getParameters());
			return EditorSteps.capture(fresh.getInstance());
		} finally {
			fresh.close();
		}
	}

	// only a source stored as the default plays the default: an audio track's takes and an instrument of the track's own
	// do not, whatever their channel's program
	@Test
	public void theDefaultEditedIsTheOneASourceStoredAsTheDefaultPlays() {
		TGChannel audio = this.fixture.songManager.getChannel(this.fixture.song, this.fixture.songManager.addAudioTrack(this.fixture.song, TGAudioInput.mono(1)).getChannelId());
		audio.setProgram((short) 25);
		TGDefaultInstrumentSource defaults = TGDefaultInstrumentSource.install(this.fixture.context);
		String guitar = defaults.edited(this.guitar).getId();
		TGProgram chain = TGSoundChain.get(this.guitar);
		chain.setReceiver(TGVst3Element.create("source", this.hosting.find("Test Click")));
		TGSoundChain.save(this.guitar, chain, this.fixture.songManager.getFactory());
		assertEquals("default-guitar null null", guitar + " " + defaults.edited(audio) + " " + defaults.edited(this.guitar));
	}

	@Test
	public void theDefaultsOwnEditorOpensAndTheSongStaysAsItIs() throws TGVst3Exception {
		TGPluginEditor editor = this.edit();
		assertEquals("true Test Sine General MIDI []", editor.isOwn() + " " + plugin("default-guitar").getName() + " " + this.song() + " " + this.changes);
	}

	@Test
	public void theFirstChangeKeepsTheDefaultInTheSongWithItAndTheEditorGoesOnEditingItThere() throws TGVst3Exception {
		this.edit();
		TGVst3Instance edited = plugin("default-guitar");
		this.load(edited, 7);
		String first = this.song() + " " + this.changes + " " + (this.live.getEditor(this.guitar.getChannelId(), "source") != null) + " " + (plugin("source") == edited);
		this.load(edited, 9);
		assertEquals("Test Sine capture 7 [keep] true true | Test Sine capture 9 [keep, state source]", first + " | " + this.song() + " " + this.changes);
	}

	@Test
	public void openedAndClosedWithoutAChangeItKeepsNothing() throws TGVst3Exception {
		this.edit();
		this.closeBox(plugin("default-guitar"));
		assertEquals("General MIDI []", this.song() + " " + this.changes);
	}

	// another default chosen in Settings meanwhile: what the old one does is no change of this song
	@Test
	public void aChangeHeardOnceTheDefaultIsAnotherKeepsNothing() throws TGVst3Exception {
		this.edit();
		TGVst3Instance edited = plugin("default-guitar");
		this.useDefault("Test Click");
		this.load(edited, 7);
		assertEquals("General MIDI []", this.song() + " " + this.changes);
	}

	// the live synthesizer plays the default: once kept, it goes on playing the same plugin (the app sends the chain
	// again after the change), whose editor stays open
	@Test
	public void theDefaultTheSynthesizerPlaysKeptGoesOnPlayingWithItsEditorOpen() throws Exception {
		TGSynthModel synthesizer = LiveOutput.synthesizer(this.fixture.context, 44100);
		synthesizer.addChannelListener(this.live);
		TGSynthChannel channel = TestPluginHosting.play(synthesizer, this.fixture.songManager, this.fixture.song, this.guitar);
		channel.sendProgramChange(this.guitar.getProgram());
		this.edit();
		TGVst3Instance edited = plugin("default-guitar");
		boolean runs = (running(channel) == edited);
		this.load(edited, 7);
		TGMidiTaps.sendParameters(synthesizer, this.fixture.songManager, this.fixture.song, this.guitar.getChannelId());
		assertEquals("true | true true Test Sine capture 7", runs + " | " + (running(channel) == edited) + " " + (this.live.getEditor(this.guitar.getChannelId(), "source") != null) + " " + this.song());
	}

	private static TGVst3Instance running(TGSynthChannel channel) {
		return ((TGVst3PluginProcessor) channel.getProcessor().getProcessor()).getInstance();
	}
}
