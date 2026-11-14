package app.tuxguitar.vst3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.log.TGLogLevel;
import app.tuxguitar.util.log.TGLogRecorder;

/**
 * What VST3 hosting writes to TuxGuitar's log, with the real host process and test plugins: each host process started
 * (its pid), closed, stopped (its exit code) or ended for hanging, with what it printed to its standard error; each
 * plugin opened, refused and closed, and its editor, by the plugin's name; each scan's folders, modules and counts.
 */
public class TestVst3Log {

	private TGLogRecorder recorder;
	private TGVst3Host host;

	@BeforeEach
	public void record() {
		this.recorder = TGLogRecorder.attach(TGLogLevel.DEBUG);
	}

	@AfterEach
	public void stop() {
		if (this.host != null) {
			this.host.close();
		}
		this.recorder.detach();
	}

	private TGVst3Host start() throws TGVst3Exception {
		this.host = TGVst3Host.start(TestPlugins.host());
		return this.host;
	}

	private TGVst3Instance open(String name) throws TGVst3Exception {
		return start().open(this.host.scan(TestPlugins.bundle(name)).get(0), 44100f, 512);
	}

	private List<String> logged(String start) {
		return this.recorder.recorded().stream().filter(line -> line.startsWith(start)).collect(Collectors.toList());
	}

	// what is logged with the text, waiting up to 5 s for it (the host's standard error is read on a thread of its own)
	private List<String> awaitLogged(String text) throws InterruptedException {
		for (int i = 0; i < 100 && !this.recorder.text().contains(text); i++) {
			Thread.sleep(50);
		}
		return this.recorder.recorded().stream().filter(line -> line.contains(text)).collect(Collectors.toList());
	}

	@Test
	public void aHostStartedAndClosedIsLoggedByItsPid() throws TGVst3Exception {
		long pid = start().getPid();
		this.host.close();
		assertFalse(this.host.isRunning());

		assertEquals(Arrays.asList("INFO VST3 host started: pid " + pid + ", " + TestPlugins.host().getAbsolutePath()), logged("INFO VST3 host started"));
		assertEquals(Arrays.asList("INFO VST3 host pid " + pid + " closed (exit code 0)"), logged("INFO VST3 host pid " + pid + " closed"));
		assertEquals(Arrays.asList(), logged("WARN"), "a host TuxGuitar closed is no stop");
	}

	@Test
	public void aHostThatDoesNotStartIsLoggedWithWhy() {
		File missing = new File("target/no-such-vst3-host.exe");
		assertThrows(TGVst3Exception.class, () -> TGVst3Host.start(missing));

		List<String> logged = logged("WARN");
		assertEquals(1, logged.size(), logged.toString());
		assertTrue(logged.get(0).startsWith("WARN the VST3 host did not start: " + missing + " (java.io.IOException: "), logged.get(0));
	}

	@Test
	public void aPluginWhoseHostIsGoneIsClosedAndLoggedAtDebug() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		TestPlugins.kill(this.host.getPid());
		gain.close();

		List<String> logged = logged("DEBUG VST3 plugin Test Gain");
		assertEquals(1, logged.size(), this.recorder.recorded().toString());
		assertTrue(logged.get(0).startsWith("DEBUG VST3 plugin Test Gain: not closed in its host, which is gone: the VST3 host stopped"), logged.get(0));
	}

	@Test
	public void aPluginOpenedAndClosedIsLoggedByItsName() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		gain.close();

		assertEquals(Arrays.asList("INFO VST3 plugin Test Gain opened in host pid " + this.host.getPid(), "INFO VST3 plugin Test Gain closed"),
				logged("INFO VST3 plugin"));
	}

	@Test
	public void aPluginTheHostRefusesIsLoggedWithTheReason() throws TGVst3Exception {
		TGVst3Plugin missing = new TGVst3Plugin(new File("Missing Amp.vst3").getPath(), "0123456789ABCDEF0123456789ABCDEF", "Missing Amp", false);
		TGVst3Exception refused = assertThrows(TGVst3Exception.class, () -> start().open(missing, 44100f, 512));

		assertEquals(Arrays.asList("WARN VST3 plugin Missing Amp did not open in host pid " + this.host.getPid() + ": " + refused.getMessage()), logged("WARN VST3 plugin"));
	}

	@Test
	public void aPluginsEditorIsLoggedAtDebug() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		gain.openEditor(0, "");
		TGVst3Instance owned = this.host.open(this.host.scan(TestPlugins.bundle("Test Gain")).get(0), 44100f, 512);
		owned.openEditor(gain.getEditorWindow().getWindow(), "");
		gain.closeEditor();
		TGVst3Instance clip = this.host.open(this.host.scan(TestPlugins.bundle("Test Clip 0.5")).get(0), 44100f, 512);
		clip.openEditor(0, "");

		assertEquals(Arrays.asList("DEBUG VST3 plugin Test Gain: editor opened, unowned", "DEBUG VST3 plugin Test Gain: editor opened, owned by TuxGuitar's window",
				"DEBUG VST3 plugin Test Gain: editor closed", "DEBUG VST3 plugin Test Clip 0.5: no editor of its own"), logged("DEBUG VST3 plugin"));
	}

	@Test
	public void aHostThatCrashesIsLoggedWithItsExitCodeAndWhatItPrinted() throws TGVst3Exception, InterruptedException {
		TGVst3Instance gain = open("Test Gain");
		long pid = this.host.getPid();
		assertThrows(TGVst3Exception.class, () -> gain.message("TG.crash", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap()));

		assertEquals(Arrays.asList("INFO VST3 host pid " + pid + ": Test plugin: crashing as asked"), awaitLogged("crashing as asked"));
		assertEquals(Arrays.asList("WARN VST3 host pid " + pid + ": the VST3 host stopped (exit code 70)"), logged("WARN VST3 host"));
	}

	@Test
	public void aHostThatStopsBetweenCallsIsLoggedOnce() throws TGVst3Exception {
		long pid = start().getPid();
		TestPlugins.kill(pid);
		assertFalse(this.host.isRunning());
		assertFalse(this.host.isRunning());

		assertEquals(1, logged("WARN VST3 host pid " + pid + ": the VST3 host stopped (exit code ").size(), this.recorder.recorded().toString());
	}

	@Test
	public void aHostEndedForHangingIsLogged() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			TGVst3Instance hang = open("Test Hang");
			hang.setParameter(hang.findParameter("Hang at"), 0);
			hang.setParameter(hang.findParameter("Hang for"), 1e9);
			float[][] block = new float[2][512];
			hang.process(block, 512, TGVst3Transport.STOPPED, Collections.<TGVst3NoteEvent>emptyList());
			Thread.sleep(2100);
			assertThrows(TGVst3Exception.class, () -> hang.process(block, 512, TGVst3Transport.STOPPED, Collections.<TGVst3NoteEvent>emptyList()));

			assertEquals(Arrays.asList("WARN VST3 host pid " + this.host.getPid() + ": the VST3 host did not answer for 2 s, ended"), logged("WARN VST3 host"));
		});
	}

	@Test
	public void aScanLogsItsFoldersEachModuleWhatItSkippedAndWhy(@TempDir File folder) throws TGVst3Exception {
		File refused = new File(folder, "NamModels.vst3");
		refused.mkdir();
		TGVst3Hosting hosting = TGVst3Hosting.getInstance(new TGContext());
		hosting.configure(TestPlugins.host(), Arrays.asList(folder, TestPlugins.folder()));
		try {
			hosting.scan();
		} finally {
			hosting.close();
		}
		TGVst3SkippedModule skipped = hosting.getSkipped().get(0);

		assertEquals(Arrays.asList("INFO VST3 scan of 2 folders: " + folder + ", " + TestPlugins.folder()), logged("INFO VST3 scan of"));
		assertEquals(Arrays.asList("DEBUG VST3 module " + TestPlugins.bundle("Test Gain") + ": Test Gain"), logged("DEBUG VST3 module " + TestPlugins.bundle("Test Gain")));
		assertEquals(Arrays.asList("INFO VST3 module skipped: " + refused + ": " + skipped.getReason()), logged("INFO VST3 module skipped"));
		List<String> done = logged("INFO VST3 scan done");
		assertEquals(1, done.size());
		assertTrue(done.get(0).matches("INFO VST3 scan done: 10 plugins in 10 modules, 1 skipped, in \\d{1,5} ms"), done.get(0));
	}
}
