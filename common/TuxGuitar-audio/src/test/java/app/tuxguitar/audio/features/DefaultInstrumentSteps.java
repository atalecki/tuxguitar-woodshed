package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.tuxguitar.action.TGActionEvent;
import app.tuxguitar.action.TGActionManager;
import app.tuxguitar.audio.TGDefaultInstrument;
import app.tuxguitar.audio.TGDefaultInstrumentSource;
import app.tuxguitar.audio.TGLivePlugins;
import app.tuxguitar.audio.TGStatusHint;
import app.tuxguitar.event.TGEventListener;
import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.util.log.TGLogLevel;
import app.tuxguitar.util.log.TGLogRecorder;
import app.tuxguitar.vst3.TGVst3Hosting;
import app.tuxguitar.vst3.TGVst3Plugin;
import io.cucumber.java.After;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps on the default instruments of the kinds of tracks: set as the settings set them, on the default source the app
 * installs (TGLiveAudio.install), the plugins the VST3 scan found named by their class id.
 */
public class DefaultInstrumentSteps {

	// a class id no plugin has
	private static final String NOT_INSTALLED = "0123456789ABCDEF0123456789ABCDEF";

	private final AudioWorld world;
	private final PlaySteps play;
	// what TuxGuitar logged since a step started recording it, or null
	private TGLogRecorder log;
	// the actions the app ran taking what the plugins told it
	private final List<String> ran = new ArrayList<String>();

	public DefaultInstrumentSteps(AudioWorld world, PlaySteps play) {
		this.world = world;
		this.play = play;
	}

	@ParameterType("guitar|bass|drums|other")
	public TGInstrumentKind kind(String kind) {
		return TGInstrumentKind.valueOf(kind.toUpperCase(Locale.ROOT));
	}

	private TGDefaultInstrumentSource source() {
		return TGDefaultInstrumentSource.install(world.fixture().context);
	}

	private TGVst3Hosting hosting() {
		return TGVst3Hosting.getInstance(world.fixture().context);
	}

	@Given("the default instrument for {kind} tracks is {string}")
	public void theDefaultInstrumentIs(TGInstrumentKind kind, String name) {
		TGVst3Plugin plugin = hosting().find(name);
		assertNotNull(plugin, name);
		use(kind, plugin.getClassId(), plugin.getName());
	}

	private void use(TGInstrumentKind kind, String classId, String name) {
		source().use(source().getDefaults().with(kind, TGDefaultInstrument.parse(classId, name).get()));
	}

	// as settings made on another computer name it: by a class id no plugin scanned here has
	@Given("the default instrument for {kind} tracks is {string}, which is not installed on this computer")
	public void theDefaultInstrumentIsNotInstalled(TGInstrumentKind kind, String name) {
		use(kind, NOT_INSTALLED, name);
	}

	// after the scan, so the plugins found stay known: opening one fails, as when its host process cannot start
	@Given("the VST3 host cannot start")
	public void theVst3HostCannotStart() {
		hosting().configure(new File(PluginSteps.nativeBuild(), "no-host.exe"), hosting().getFolders());
	}

	// as the host process is again once installed, with no new scan
	@Given("the VST3 host can start again")
	public void theVst3HostCanStartAgain() {
		hosting().configure(PluginSteps.host(), hosting().getFolders());
	}

	// as the app does every 50 ms on its UI thread, here at each turn of the play loop, while the plugins still run; the
	// actions it runs meanwhile are kept (the desktop marks the song modified for each change of a sound chain it runs,
	// whether or not the change is refused)
	@When("I play the song live from {position}, the app taking what the plugins tell it as it plays")
	public void playLiveTakingWhatThePluginsTell(SongPosition from) throws Exception {
		world.synthesizer().addChannelListener(TGLivePlugins.getInstance(world.fixture().context));
		TGEventListener running = event -> this.ran.add(event.getAttribute(TGActionEvent.ATTRIBUTE_ACTION_ID));
		TGActionManager.getInstance(world.fixture().context).addPreExecutionListener(running);
		this.play.playLive(from, 1, TGLivePlugins.getInstance(world.fixture().context)::drain);
		TGActionManager.getInstance(world.fixture().context).removePreExecutionListener(running);
	}

	@Then("the app ran no action to take it")
	public void theAppRanNoAction() {
		assertEquals("[]", this.ran.toString());
	}

	@Then("the status line reads {string}")
	public void theStatusLineReads(String text) {
		assertEquals(text, TGStatusHint.getInstance(world.fixture().context).getText());
	}

	// what TuxGuitar logs from now on, warnings and worse
	@Given("TuxGuitar's log is recorded")
	public void theLogIsRecorded() {
		this.log = TGLogRecorder.attach(TGLogLevel.WARN);
	}

	@After
	public void stopRecordingTheLog() {
		if (this.log != null) {
			this.log.detach();
		}
	}

	@Then("the log has a warning starting {string}")
	public void theLogHasAWarningStarting(String start) {
		List<String> recorded = this.log.recorded();
		assertTrue(recorded.stream().anyMatch(line -> line.startsWith("WARN " + start)), recorded.toString());
	}

	@Then("the status line reads nothing")
	public void theStatusLineReadsNothing() {
		assertNull(TGStatusHint.getInstance(world.fixture().context).getText());
	}
}
