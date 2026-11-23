package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionErrorEvent;
import app.tuxguitar.action.TGActionManager;
import app.tuxguitar.audio.TGEditorDock;
import app.tuxguitar.audio.TGEditorDock.Element;
import app.tuxguitar.audio.TGEditorDock.Shows;
import app.tuxguitar.audio.TGEditorDock.Step;
import app.tuxguitar.audio.TGEditorPlacement;
import app.tuxguitar.audio.TGLivePlugins;
import app.tuxguitar.audio.TGPluginEditor;
import app.tuxguitar.audio.TGSoundChainCommands;
import app.tuxguitar.audio.action.TGOpenPluginEditorAction;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.util.TGContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * The Track panel's editor area run as the desktop's glue runs it (TGSoundChainEditorArea), on the state machine
 * (TGEditorDock) and the commands (TGSoundChainCommands) the app uses: each step's view closed, its editor opened
 * (here in a window of its own, owned by none); what the plugins did with the editors opened kept until the UI thread
 * gets there, then the panel following the caret's track. The app's error messages are the actions' failures.
 */
public class TrackPanelSteps {

	// what the area shows while an editor of the element is shown, inside it or in a window of its own
	private static final Set<Shows> SHOWING = EnumSet.of(Shows.OWN_EDITOR, Shows.PARAMETERS, Shows.OWN_WINDOW);
	private static final TGEditorPlacement PLACEMENT = TGEditorPlacement.floating(() -> 0L);

	private final AudioWorld world;
	private final TGEditorDock dock = new TGEditorDock(Collections.<String>emptySet(), false);
	private final Map<Element, TGPluginEditor> editors = new HashMap<Element, TGPluginEditor>();
	// what reaches TuxGuitar's UI thread later (TGSynchronizer), in order
	private final List<Runnable> later = new ArrayList<Runnable>();
	private final List<String> errors = Collections.synchronizedList(new ArrayList<String>());
	private TGTrack shown;
	private TGSoundChainCommands commands;

	public TrackPanelSteps(AudioWorld world) {
		this.world = world;
	}

	private TGContext context() {
		return world.fixture().context;
	}

	@Given("the Track panel shows the editor of the source of track {int}, {string}")
	public void thePanelShowsTheSourcesEditor(int track, String plugin) {
		TGActionManager actions = TGActionManager.getInstance(context());
		actions.setActionContextFactory(this::actionContext);
		actions.addErrorListener(event -> this.errors.add(event.<Throwable>getAttribute(TGActionErrorEvent.PROPERTY_ACTION_ERROR).getMessage()));
		world.fixture().map(new TGOpenPluginEditorAction(context()));
		this.commands = new TGSoundChainCommands(context(), () -> this.shown, () -> { });
		this.follow(track);
		this.theAreaShowsTheEditorOf(plugin);
	}

	// as the app makes one for each action: on the document's song
	private TGActionContext actionContext() {
		TGActionContext context = new TGActionContext() {
		};
		context.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG_MANAGER, world.fixture().songManager);
		context.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG, world.song());
		return context;
	}

	// what the plugins did while the song was read reaches the area first, then the panel follows the caret's track
	@When("I open it again, the Track panel following it")
	public void openAgainThePanelFollowing() {
		world.open(world.saved());
		this.settle();
		this.follow(1);
	}

	@Then("the app has shown no error message")
	public void noErrorMessage() {
		assertEquals("[]", this.errors.toString());
	}

	@Then("the Track panel's editor area shows nothing")
	public void theAreaShowsNothing() {
		assertEquals("null NOTHING", this.dock.element() + " " + this.dock.shows());
	}

	@Then("the Track panel's editor area shows the editor of {string}")
	public void theAreaShowsTheEditorOf(String plugin) {
		Shows shows = this.dock.shows();
		assertEquals(plugin + " showing", this.dock.element() + " " + (SHOWING.contains(shows) ? "showing" : shows));
	}

	// the panel shows the track (its source picked), and gets there
	private void follow(int track) {
		this.shown = world.track(track);
		this.run(this.dock.picked(this.commands.element(0)));
		this.settle();
	}

	// the actions started run, and what they posted to the UI thread, until nothing is left
	private void settle() {
		List<Runnable> posted = this.posted();
		while (!posted.isEmpty() || this.ranActions()) {
			posted.forEach(Runnable::run);
			posted = this.posted();
		}
	}

	private boolean ranActions() {
		List<Thread> threads = world.threads().run();
		threads.forEach(TrackPanelSteps::join);
		return !threads.isEmpty();
	}

	private static void join(Thread thread) {
		try {
			thread.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	private synchronized List<Runnable> posted() {
		List<Runnable> posted = new ArrayList<Runnable>(this.later);
		this.later.clear();
		return posted;
	}

	private synchronized void post(Runnable runnable) {
		this.later.add(runnable);
	}

	// as TGSoundChainEditorArea.run: the view closed, the editor opened where the step says
	private void run(Step step) {
		Optional.ofNullable(this.editors.remove(step.getClose())).ifPresent(TGLivePlugins.getInstance(context())::close);
		Optional.ofNullable(step.getOpen()).ifPresent(this::open);
	}

	// as TGSoundChainEditorArea.open: only an element the track shown still has, else forgotten
	private void open(Element element) {
		if (!this.commands.open(element, PLACEMENT, editor -> this.post(() -> this.opened(element, editor)))) {
			this.run(this.dock.gone(element));
		}
	}

	private void opened(Element element, TGPluginEditor editor) {
		this.editors.put(element, editor);
		editor.watch(() -> this.post(() -> this.run(this.dock.followed(element, editor, this.editors.get(element) == editor))));
		this.run(this.dock.opened(element, editor.getOpened()));
	}
}
