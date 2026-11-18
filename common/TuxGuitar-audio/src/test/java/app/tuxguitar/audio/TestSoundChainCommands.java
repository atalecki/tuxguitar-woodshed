package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionException;
import app.tuxguitar.action.TGActionManager;
import app.tuxguitar.audio.action.TGAddVst3EffectAction;
import app.tuxguitar.audio.action.TGChooseGeneralMidiAction;
import app.tuxguitar.audio.action.TGChooseVst3InstrumentAction;
import app.tuxguitar.audio.action.TGEffectAction;
import app.tuxguitar.audio.action.TGKeepDefaultInstrumentAction;
import app.tuxguitar.audio.action.TGMoveEffectUpAction;
import app.tuxguitar.audio.action.TGOpenPluginEditorAction;
import app.tuxguitar.audio.action.TGRemoveEffectAction;
import app.tuxguitar.audio.action.TGSetEffectEnabledAction;
import app.tuxguitar.audio.action.TGSetPluginParameterAction;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.SongActionFixture;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.thread.TGThreadHandler;
import app.tuxguitar.thread.TGThreadLoop;
import app.tuxguitar.thread.TGThreadPriority;
import app.tuxguitar.vst3.TGVst3Element;
import app.tuxguitar.vst3.TGVst3Exception;
import app.tuxguitar.vst3.TGVst3Hosting;

/**
 * What a sound chain editor (the Sound Chain dialog, the Sound Chain panel) asks of a track's sound chain, run as the
 * app runs actions (by name, on a thread the app starts: here at once), each on the channel the track has in the song at
 * that moment; each tells the editor once it is done, so it reads the chain again. The plugins are the test VST3
 * plugins, run by the native host.
 */
class TestSoundChainCommands {

	private final SongActionFixture fixture = SongActionFixture.onDocument();
	private final List<String> told = new ArrayList<String>();
	private TGTrack track = this.fixture.song.getTrack(0);
	private final TGSoundChainCommands commands = new TGSoundChainCommands(this.fixture.context, () -> this.track, () -> this.told.add("done"));
	private TGVst3Hosting hosting;

	@BeforeEach
	void setUp() throws TGVst3Exception {
		this.hosting = TestPluginHosting.host(this.fixture.context);
		TGActionManager.getInstance(this.fixture.context).setActionContextFactory(this::actionContext);
		TGThreadManagerAtOnce.install(this.fixture.context);
		this.fixture.map(new TGAddVst3EffectAction(this.fixture.context), new TGChooseVst3InstrumentAction(this.fixture.context), new TGChooseGeneralMidiAction(this.fixture.context),
			new TGKeepDefaultInstrumentAction(this.fixture.context), new TGMoveEffectUpAction(this.fixture.context), new TGRemoveEffectAction(this.fixture.context),
			new TGSetEffectEnabledAction(this.fixture.context), new TGOpenPluginEditorAction(this.fixture.context),
			new TGSetPluginParameterAction(this.fixture.context));
		TGProgram chain = TGSoundChain.get(this.channel(this.track));
		chain.setReceiver(TGSoundChain.source("drone"));
		TGSoundChain.save(this.channel(this.track), chain, this.fixture.songManager.getFactory());
	}

	@AfterEach
	void tearDown() {
		TGLivePlugins.getInstance(this.fixture.context).close();
		this.hosting.close();
	}

	// as the app makes one for each action: on the document's song
	private TGActionContext actionContext() {
		TGActionContext context = new TGActionContext() {
		};
		context.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG_MANAGER, this.fixture.songManager);
		context.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG, this.fixture.song);
		return context;
	}

	private TGChannel channel(TGTrack of) {
		return this.fixture.songManager.getChannel(this.fixture.song, of.getChannelId());
	}

	// the track's chain as an editor shows it (its source "drone" until a command changes it: General MIDI's name needs
	// the app's messages), then what the editor was told
	private String read(TGTrack of) {
		return String.join(" | ", TGSoundChainRows.of(this.channel(of)).getRows("Source")) + " " + this.told;
	}


	@Test
	void effectsAreAddedMovedDisabledAndRemovedEachChangeTold() {
		this.commands.addEffect(this.hosting.find("Test Gain"));
		this.commands.addEffect(this.hosting.find("Test Clip 0.5"));
		this.commands.moveUp(2);
		String moved = this.read(this.track);
		this.commands.toggle(1);
		String disabled = this.read(this.track);
		this.commands.toggle(1);
		this.commands.remove(2);
		assertEquals("Source: drone | 1. Test Clip 0.5 | 2. Test Gain [done, done, done] | Source: drone | 1. [Test Clip 0.5] | 2. Test Gain [done, done, done, done] | Source: drone | 1. Test Clip 0.5 [done, done, done, done, done, done]",
			moved + " | " + disabled + " | " + this.read(this.track));
	}

	// "Default instrument" makes the default the source (stored as General MIDI); "Keep in song" makes the default
	// instrument that plays the track's own
	@Test
	void anInstrumentOrTheDefaultIsMadeTheSourceAndTheDefaultKept() {
		TGDefaultInstrumentSource.install(this.fixture.context).use(TGDefaultInstruments.NONE.with(TGInstrumentKind.GUITAR, TGDefaultInstrument.parse("7A1C0008544753494E45000000000008", "Test Sine").get()));
		this.commands.useAsSource(this.hosting.find("Test Sine"));
		String chosen = this.read(this.track);
		this.commands.useDefault();
		boolean byDefault = TGSoundChainRows.of(this.channel(this.track)).isDefault();
		this.commands.keep();
		assertEquals("Source: Test Sine [done] | true | Source: Test Sine [done, done, done] false", chosen + " | " + byDefault + " | " + this.read(this.track) + " " + TGSoundChainRows.of(this.channel(this.track)).isDefault());
	}

	// the panel follows the caret's track: each command acts on the track it is given then
	@Test
	void eachCommandActsOnTheTrackGivenAtThatMoment() {
		TGTrack first = this.track;
		this.track = this.fixture.songManager.addTrack(this.fixture.song);
		this.commands.addEffect(this.hosting.find("Test Gain"));
		assertEquals("first 0, other 1 [done]", "first " + this.countEffects(first) + ", other " + this.countEffects(this.track) + " " + this.told);
	}

	private int countEffects(TGTrack of) {
		return TGSoundChain.get(this.channel(of)).countOutputs();
	}

	// Edit asks the action to open the picked effect's editor, placed as the editor says (floating: owned by the window
	// given; embedded: inside the area given, at most the height given, the window given kept as its owner), titled with
	// the track
	@Test
	void editOpensThePickedPluginsEditorWhereThePlacementSays() {
		List<String> asked = new ArrayList<String>();
		this.fixture.map(new TGActionBase(this.fixture.context, TGOpenPluginEditorAction.NAME) {
			protected void processAction(TGActionContext context) {
				asked.add("effect " + context.getAttribute(TGEffectAction.ATTRIBUTE_EFFECT) + " " + TGOpenPluginEditorAction.target(context)
					+ " track " + context.<TGTrack>getAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK).getName()
					+ " channel " + context.<TGChannel>getAttribute(TGDocumentContextAttributes.ATTRIBUTE_CHANNEL).getChannelId());
			}
		});
		this.commands.edit(2, TGEditorPlacement.floating(() -> 42L), editor -> asked.add("parameters"));
		this.commands.edit(0, TGEditorPlacement.embedded(() -> 5L, () -> 7L), editor -> asked.add("parameters"));
		String track = " track " + this.track.getName() + " channel " + this.track.getChannelId();
		assertEquals("[effect 2 floating, owned by 42" + track + ", effect null inside 5, at most 0.5 of its window high, owned by 7" + track + "] [done, done]", asked + " " + this.told);
	}

	// the app runs the action later on its UI thread (its interceptor), after the command returned: the editor is handed
	// once the action has opened it, not before
	@Test
	void theEditorIsHandedOnceTheActionRanLater() throws Exception {
		List<Runnable> later = new ArrayList<Runnable>();
		boolean[] deferring = {true};
		TGActionManager actions = TGActionManager.getInstance(this.fixture.context);
		actions.addInterceptor((id, context) -> {
			boolean deferred = (deferring[0] && id.equals(TGOpenPluginEditorAction.NAME));
			if (deferred) {
				later.add(() -> this.runLater(actions, deferring, id, context));
			}
			return deferred;
		});
		this.commands.addEffect(this.hosting.find("Test Clip 0.5"));
		List<String> opened = new ArrayList<String>();
		this.commands.edit(1, TGEditorPlacement.floating(() -> 0L), editor -> opened.add("clip " + editor.getParameters()));
		String before = opened.toString();
		later.forEach(Runnable::run);
		assertEquals("[] [clip [Ceiling]]", before + " " + opened);
	}

	private void runLater(TGActionManager actions, boolean[] deferring, String id, TGActionContext context) {
		deferring[0] = false;
		try {
			actions.execute(id, context);
		} catch (TGActionException e) {
			throw new IllegalStateException(e);
		}
	}

	// a row's plugin as the editor area knows it: its channel, element, plugin, name and row; a row without a VST3 plugin
	// (General MIDI, no default instrument) is none
	@Test
	void aRowsPluginIsTheElementTheEditorAreaKnows() {
		this.commands.addEffect(this.hosting.find("Test Gain"));
		TGEditorDock.Element gain = this.commands.element(1);
		String element = gain.getChannel() + " " + gain.getId().equals(TGSoundChain.get(this.channel(this.track)).getOutput(0).getId()) + " "
			+ gain.getClassId().equals(this.hosting.find("Test Gain").getClassId()) + " " + gain.getName() + " " + gain.getRow();
		TGSoundChainCommands none = new TGSoundChainCommands(this.fixture.context, () -> null, () -> { });
		assertEquals(this.track.getChannelId() + " true true Test Gain 1 null null null", element + " " + this.commands.element(0) + " " + this.commands.element(-1) + " " + none.element(0));
	}

	// a plugin asking for its editor is found by its channel and element among the rows of the track shown; none on
	// another channel, nor one not in the chain
	@Test
	void aPluginAskingForItsEditorIsFoundAmongTheRows() {
		this.commands.addEffect(this.hosting.find("Test Gain"));
		String effect = TGSoundChain.get(this.channel(this.track)).getOutput(0).getId();
		int channel = this.track.getChannelId();
		assertEquals("1 -1 -1", this.commands.row(channel, effect) + " " + this.commands.row(channel + 1, effect) + " " + this.commands.row(channel, "nothing"));
	}

	// an action given no placement opens the editor in a window of its own, owned by none
	@Test
	void anEditorGivenNoPlacementFloatsOwnedByNone() {
		assertEquals("floating, owned by 0", TGOpenPluginEditorAction.target(new TGActionContext() {
		}).toString());
	}

	// the editor opened is handed to the editor once open: the plugin's own (as the host opened it), or, for a plugin
	// without one, TuxGuitar's parameter editor
	@Test
	void theEditorOpenedIsHandedOnceOpen() {
		List<String> opened = new ArrayList<String>();
		this.commands.addEffect(this.hosting.find("Test Gain"));
		this.commands.addEffect(this.hosting.find("Test Clip 0.5"));
		this.commands.edit(1, TGEditorPlacement.floating(() -> 0L), editor -> opened.add("gain own " + editor.isOwn() + " " + editor.getOpened().getKind()));
		this.commands.edit(2, TGEditorPlacement.floating(() -> 0L), editor -> opened.add("clip own " + editor.isOwn() + " " + editor.getRows().get(0).getTitle()));
		assertEquals("[gain own true OWN_WINDOW, clip own false Ceiling] [done, done, done, done]", opened + " " + this.told);
	}

	// so is the default instrument a source plays (General MIDI for a guitar track: its default) when it has no editor of
	// its own
	@Test
	void aDefaultSourceWithoutItsOwnEditorHandsTheParameterEditor() {
		this.channel(this.track).setProgram((short) 25);
		TGDefaultInstrument tone = TGDefaultInstrument.parse(this.hosting.find("Test Tone").getClassId(), "Test Tone").get();
		TGDefaultInstrumentSource.install(this.fixture.context).use(TGDefaultInstruments.NONE.with(TGInstrumentKind.GUITAR, tone));
		this.commands.useDefault();
		List<String> parameters = new ArrayList<String>();
		this.commands.edit(0, TGEditorPlacement.floating(() -> 0L), editor -> parameters.add("tone " + editor.getParameters()));
		assertEquals("[tone [Level]] [done, done]", parameters + " " + this.told);
	}

	// a value set in TuxGuitar's parameter rows of the default the source plays (its parameter editor open) keeps it in the
	// song with that value, as the Parameters window does: the source is the track's own Test Tone, Level 0.5 in the song
	@Test
	void aValueSetInTheParameterRowsOfTheDefaultKeepsItWithThatValue() {
		this.channel(this.track).setProgram((short) 25);
		TGDefaultInstrument tone = TGDefaultInstrument.parse(this.hosting.find("Test Tone").getClassId(), "Test Tone").get();
		TGDefaultInstrumentSource.install(this.fixture.context).use(TGDefaultInstruments.NONE.with(TGInstrumentKind.GUITAR, tone));
		this.commands.useDefault();
		List<TGPluginEditor> opened = new ArrayList<TGPluginEditor>();
		this.commands.edit(0, TGEditorPlacement.floating(() -> 0L), opened::add);
		TGPluginEditor editor = opened.get(0);
		this.commands.set(0, editor.getRows().get(0).getId(), 0.5);
		TGProgramElement source = TGSoundChain.plugin(TGSoundChain.get(this.channel(this.track)), null);
		String kept = (source != null ? TGVst3Element.getName(source) + " with 0.5 " + source.getParameters().containsValue("0.5") : "General MIDI");
		assertEquals("Test Tone with 0.5 true [done, done, done]", kept + " " + this.told);
	}

	// the threads the app starts, run at once on the caller's
	private static final class TGThreadManagerAtOnce implements TGThreadHandler {

		static void install(app.tuxguitar.util.TGContext context) {
			app.tuxguitar.thread.TGThreadManager.getInstance(context).setThreadHandler(new TGThreadManagerAtOnce());
		}

		public void start(TGThreadPriority priority, Runnable runnable) {
			runnable.run();
		}

		public void loop(TGThreadLoop loop) {
		}

		public void yield() {
		}

		public void dispose() {
		}

		public Object getThreadId() {
			return null;
		}
	}
}
