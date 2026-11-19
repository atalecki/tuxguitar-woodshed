package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import app.tuxguitar.audio.TGChainMeters;
import app.tuxguitar.audio.TGLiveLevels;
import app.tuxguitar.audio.TGMeterReading;
import app.tuxguitar.audio.TGSoundChainRows;
import app.tuxguitar.audio.device.TGAudioDeviceException;
import app.tuxguitar.midi.synth.TGLevelTap;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerException;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps of the Sound Chain panel's level meters, read as the panel reads them (TGChainMeters over the live levels), on a
 * clock of the blocks played: about 30 times a second (every 3 blocks), and once more after Play stops, as the panel
 * goes on showing.
 */
public class MeterSteps {

	// the panel reads every 3 blocks of 512 frames
	private static final int BLOCKS_A_READING = 3;
	private static final double DB = 0.1;

	private final AudioWorld world;
	private final PlaySteps play;
	private final TGChainMeters meters = new TGChainMeters();
	private int rows;
	private boolean shown;
	private long blocks;
	// per meter (each row of the panel, then the master): the highest peak each side reached, and the last reading
	private final List<double[]> highest = new ArrayList<double[]>();
	private List<TGMeterReading> last = new ArrayList<TGMeterReading>();

	public MeterSteps(AudioWorld world, PlaySteps play) {
		this.world = world;
		this.play = play;
	}

	private TGLiveLevels live() {
		return TGLiveLevels.getInstance(world.fixture().context);
	}

	// as the app does once the TuxGuitar Synthesizer opened (TGLiveAudio.play), then as the panel does when it shows
	@Given("the Sound Chain panel shows the meters of track {int}")
	public void shows(int track) {
		this.live().play(world.synthesizer().getLevels());
		this.live().watch();
		this.shown = true;
		this.follow(track);
	}

	private void follow(int track) {
		List<app.tuxguitar.midi.synth.TGLevels.Key> rows = TGSoundChainRows.of(world.channel(world.track(track))).getMeters();
		this.rows = rows.size();
		this.meters.follow(rows);
	}

	@After
	public void hide() {
		if (this.shown) {
			this.live().unwatch();
			this.shown = false;
		}
	}

	@When("I play the song live from {position} with the meters showing")
	public void playLive(SongPosition from) throws MidiPlayerException, TGAudioDeviceException {
		MidiPlayer player = MidiPlayer.getInstance(world.fixture().context);
		this.play.playLive(from, 1, () -> this.tick(player));
		this.read(false);
	}

	// each block played; every 3 the panel reads, Play under way
	private void tick(MidiPlayer player) {
		this.blocks ++;
		if (this.blocks % BLOCKS_A_READING == 0) {
			this.read(player.isRunning());
		}
	}

	private void read(boolean playing) {
		long now = this.blocks * 512 * 1000 / TestInterface.SAMPLE_RATE;
		this.last = this.meters.next(this.live().get(), now, playing);
		while (this.highest.size() < this.last.size()) {
			this.highest.add(new double[] {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY});
		}
		for (int meter = 0; meter < this.last.size(); meter++) {
			for (int side = 0; side < TGLevelTap.SIDES; side++) {
				this.highest.get(meter)[side] = Math.max(this.highest.get(meter)[side], this.last.get(meter).getPeak(side));
			}
		}
	}

	// "source", "effect <n>", "Track out" or "Master": its index among the meters
	private int meter(String name) {
		if (name.equals("source")) {
			return 0;
		}
		if (name.startsWith("effect ")) {
			return Integer.parseInt(name.substring("effect ".length()));
		}
		return (name.equals("Track out") ? this.rows - 1 : this.rows);
	}

	@Then("the {string} meter peaked at {double} dB within 0.1 dB")
	public void peaked(String name, double decibels) {
		double[] highest = this.highest.get(this.meter(name));
		assertEquals(decibels, Math.max(highest[0], highest[1]), DB, name + " " + Arrays.toString(highest));
	}

	@Then("the {string} meter peaked at {double} dB on the left and {double} dB on the right, within 0.1 dB")
	public void peakedOnEachSide(String name, double left, double right) {
		double[] highest = this.highest.get(this.meter(name));
		assertEquals(left + " " + right, near(left, highest[0]) + " " + near(right, highest[1]));
	}

	// the expected level when within 0.1 dB of it, else what was read
	private static String near(double expected, double read) {
		return Double.toString(Math.abs(expected - read) <= DB ? expected : read);
	}

	@Then("the {string} meter was silent on the right")
	public void silentOnTheRight(String name) {
		assertEquals(Double.NEGATIVE_INFINITY, this.highest.get(this.meter(name))[1]);
	}

	@Then("the {string} meter peaked above {double} dB")
	public void peakedAbove(String name, double decibels) {
		double[] highest = this.highest.get(this.meter(name));
		assertTrue(Math.max(highest[0], highest[1]) > decibels, name + " " + Arrays.toString(highest));
	}

	@Then("the {string} meter's clip lamp is lit")
	public void lit(String name) {
		assertTrue(this.last.get(this.meter(name)).isClipped(), name);
	}

	@Then("the {string} meter's clip lamp is out")
	public void out(String name) {
		assertFalse(this.last.get(this.meter(name)).isClipped(), name);
	}

	// the meter is empty and greyed: it measures nothing
	@Then("the {string} meter is off")
	public void off(String name) {
		assertEquals(TGMeterReading.OFF, this.last.get(this.meter(name)));
	}

	@When("I click the {string} meter")
	public void click(String name) {
		this.meters.clear(this.meter(name));
		this.blocks += BLOCKS_A_READING;
		this.read(false);
	}

	// no tap of the TuxGuitar Synthesizer measured a block
	@Then("no level was measured")
	public void nothingMeasured() {
		for (TGLevelTap tap : world.synthesizer().getLevels().taps()) {
			assertEquals(0, tap.read(0, new float[TGLevelTap.SIDES]));
		}
	}
}
