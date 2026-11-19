package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import app.tuxguitar.audio.TGLivePlugins;
import app.tuxguitar.audio.TGLoadingHint;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.vst3.TGVst3Exception;
import app.tuxguitar.vst3.TGVst3Instance;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps on the hint an app shows while a plugin Play stopped waiting for still loads (TGLoadingHint): a test plugin
 * told to restore its state in the background as Play starts (the test plugins' message "TG.restoring", see README),
 * the hint read at each turn of the play loop, with the tick the song was at.
 */
public class LoadingHintSteps {

	private final AudioWorld world;
	private final PlaySteps play;
	// what the hint read at each turn of the play loop, by the tick the song was at
	private final List<Map.Entry<Long, String>> read = new ArrayList<Map.Entry<Long, String>>();

	public LoadingHintSteps(AudioWorld world, PlaySteps play) {
		this.world = world;
		this.play = play;
	}

	// and loading that again in the first turn that finds the song at the later position (Play does not wait for it)
	@When("I play the song live from {position}, track {int}'s plugin loading {string} as Play starts, until {position}, then {string} from {position}")
	public void playLoadingUntilThenAgain(SongPosition from, int track, String what, SongPosition until, String again, SongPosition later) throws Exception {
		Once finishing = new Once(until, () -> restoring(track, null, 0));
		Once loading = new Once(later, () -> restoring(track, again.getBytes(StandardCharsets.UTF_8), 1));
		this.playReading(from, () -> restoring(track, what.getBytes(StandardCharsets.UTF_8), 1), () -> {
			finishing.run();
			loading.run();
		});
		assertTrue(finishing.done && loading.done, "finished and loading again while the song played");
	}

	// the plugin finishes in the first turn that finds the song there, before the hint is read
	@When("I play the song live from {position}, track {int}'s plugin loading as Play starts, until {position}")
	public void playLoadingUntil(SongPosition from, int track, SongPosition until) throws Exception {
		Once finishing = new Once(until, () -> restoring(track, null, 0));
		this.playReading(from, () -> restoring(track, null, 1), finishing);
		assertTrue(finishing.done, "finished while the song played");
	}

	// an action taken once, in the first turn of the play loop that finds the song at the position
	private final class Once implements Runnable {

		private final long tick;
		private final Runnable action;
		private boolean done;

		private Once(SongPosition at, Runnable action) {
			this.tick = at.tick(world.song());
			this.action = action;
		}

		public void run() {
			if (this.isDue()) {
				this.done = true;
				this.action.run();
			}
		}

		private boolean isDue() {
			return (!this.done && MidiPlayer.getInstance(world.fixture().context).getTickPosition() >= this.tick);
		}
	}

	// the plugin finishes a second after Play starts, on a thread of its own, while Play waits for it; then it loads
	// again in the first turn that finds the song at the position, before the hint is read
	@When("I play the song live from {position}, track {int}'s plugin loading {string} as Play starts, for 1 second, then {string} from {position}")
	public void playLoadingFor(SongPosition from, int track, String what, String again, SongPosition later) throws Exception {
		Thread finishing = new Thread(() -> {
			sleep(TimeUnit.SECONDS.toMillis(1));
			restoring(track, null, 0);
		});
		Once loading = new Once(later, () -> restoring(track, again.getBytes(StandardCharsets.UTF_8), 1));
		this.playReading(from, () -> {
			restoring(track, what.getBytes(StandardCharsets.UTF_8), 1);
			finishing.start();
		}, loading);
		finishing.join();
		assertTrue(loading.done, "loading again while the song played");
	}

	// the app's hint is the one TuxGuitar's audio installs; read after the turn's action
	private void playReading(SongPosition from, Runnable asPlayStarts, Runnable eachTurn) throws Exception {
		world.synthesizer().addChannelListener(TGLivePlugins.getInstance(world.fixture().context));
		TGLoadingHint hint = TGLoadingHint.install(world.fixture().context);
		this.play.playLive(from, 1, asPlayStarts, () -> {
			eachTurn.run();
			this.read.add(Map.entry(MidiPlayer.getInstance(world.fixture().context).getTickPosition(), String.valueOf(hint.getText())));
		});
	}

	// "TG.restoring" to track's plugin as it runs live, with what it loads (none: null)
	private void restoring(int track, byte[] what, long underway) {
		try {
			running(track).message("TG.restoring", Collections.singletonMap("underway", underway), saying(what));
		} catch (TGVst3Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static Map<String, byte[]> saying(byte[] what) {
		return (what != null ? Collections.singletonMap("what", what) : Collections.<String, byte[]>emptyMap());
	}

	private TGVst3Instance running(int track) {
		return TGLivePlugins.getInstance(world.fixture().context).find(world.channel(world.track(track)).getChannelId(), "source");
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Then("until {position} the app's loading hint read {string}, and nothing from then on")
	public void theHintReadUntil(SongPosition until, String hint) {
		long tick = until.tick(world.song());
		assertFalse(this.read.isEmpty(), "the hint was read");
		this.read.forEach(turn -> assertEquals(turn.getKey() < tick ? hint : "null", turn.getValue(), "at tick " + turn.getKey()));
		assertTrue(this.read.stream().anyMatch(turn -> turn.getKey() < tick), "read before " + until);
		assertTrue(this.read.stream().anyMatch(turn -> turn.getKey() >= tick), "read from " + until);
	}

	@Then("the app's loading hint read nothing while the song played")
	public void theHintReadNothing() {
		assertFalse(this.read.isEmpty(), "the hint was read");
		this.read.forEach(turn -> assertEquals("null", turn.getValue(), "at tick " + turn.getKey()));
	}
}
