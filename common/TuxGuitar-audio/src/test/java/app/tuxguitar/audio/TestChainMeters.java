package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import app.tuxguitar.midi.synth.TGLevelTap;
import app.tuxguitar.midi.synth.TGLevels;
import app.tuxguitar.midi.synth.TGLevels.Key;

/**
 * The meters of a sound chain: a reading per row followed, then the master, from the live levels' taps. Each case runs
 * a script of steps on fresh levels, one tick of 33 ms per reading:
 * - "src3=0.5": the point measures a block whose loudest sample is 0.5 (src, gain, out of channel 3 or 4; master)
 * - "guitar", "bass": the panel follows track 1's rows (source, effect gain, an effect switched off, out; channel 3)
 *   or track 2's (source, out; channel 4)
 * - "next" and "play": a reading, stopped or playing; "clear:1": a click on row 1's meter
 * - "nolive": no live levels from now on (another MIDI Port); "again": another synthesizer's levels, its out4 opened
 * and gives the last reading: each meter's louder peak in dB, rounded ("-inf": silence), "!" when its lamp is lit,
 * "off" when it measures nothing.
 */
public class TestChainMeters {

	private static final List<Key> GUITAR = Arrays.asList(Key.source(3), Key.effect(3, "gain"), null, Key.out(3));
	private static final List<Key> BASS = Arrays.asList(Key.source(4), Key.out(4));

	// each case: what it shows, its script, the last reading
	private static final String[][] CASES = {
		{"each row reads its point, then the master", "src3=0.5 gain3=0.25 out3=0.125 master=0.0625 guitar next", "-6 -12 off -18 -24"},
		{"an effect switched off, or a point that never ran, reads off", "src3=0.5 guitar next", "-6 off off off -inf"},
		{"without live levels (another MIDI Port) every meter is off", "bass nolive next", "off off off"},
		{"following nothing leaves the master", "master=1 next", "0!"},
		{"a meter falls when nothing new arrives: only the blocks since are new", "bass src4=0.5 next next", "-7 off -inf"},
		{"another track's clips show when it is picked", "out4=1 guitar src3=0 next bass next", "off 0! -inf"},
		{"a click clears its row's lamp", "src4=1 out4=1 bass next clear:1 next", "-1! -1 -inf"},
		{"cleared on one track, it stays cleared after another was shown", "out4=1 out3=0 bass next clear:1 guitar next bass next", "off -1 -inf"},
		{"a click on a row without a tap does nothing", "master=1 guitar next clear:2 clear:9 clear:-1 next", "off off off off -1!"},
		{"Play starting clears every lamp, of tracks not shown since too", "out3=1 out4=1 guitar next play bass play", "off 0 -inf"},
		{"a clip while playing lights again", "out4=1 bass next play out4=1 play", "off 0! -inf"},
		{"shown while Play is under way, no Play started: the lamps stay", "out4=1 bass play", "off 0! -inf"},
		{"another synthesizer's taps are new: their meters start afresh", "out4=1 bass next again next", "off -inf -inf"},
	};

	@Test
	void eachCaseEndsOnItsReading() {
		String expected = Arrays.stream(CASES).map(row -> row[0] + ": " + row[2]).collect(Collectors.joining("\n"));
		String read = Arrays.stream(CASES).map(row -> row[0] + ": " + new Script().run(row[1])).collect(Collectors.joining("\n"));
		assertEquals(expected, read);
	}

	private static final class Script {
		private final TGLevels levels = new TGLevels();
		private final TGChainMeters meters = new TGChainMeters();
		private final Map<String, Supplier<TGLevelTap>> points = new HashMap<String, Supplier<TGLevelTap>>();
		private TGLevels live = this.levels;
		private long now;

		Script() {
			for (int channel = 3; channel <= 4; channel++) {
				Key source = Key.source(channel);
				Key gain = Key.effect(channel, "gain");
				Key out = Key.out(channel);
				this.points.put("src" + channel, () -> this.levels.open(source));
				this.points.put("gain" + channel, () -> this.levels.open(gain));
				this.points.put("out" + channel, () -> this.levels.open(out));
			}
			this.points.put("master", this.levels::master);
		}

		String run(String script) {
			String said = "";
			for (String step : script.split(" ")) {
				said = this.step(step, said);
			}
			return said;
		}

		private String step(String step, String said) {
			String[] measured = step.split("[=:]");
			if (step.contains("=")) {
				this.points.get(measured[0]).get().measure(new float[][] {{Float.parseFloat(measured[1]), 0f}});
			} else if (step.contains(":")) {
				this.meters.clear(Integer.parseInt(measured[1]));
			} else if (step.equals("guitar") || step.equals("bass")) {
				this.meters.follow(step.equals("guitar") ? GUITAR : BASS);
			} else if (step.equals("nolive") || step.equals("again")) {
				this.live = (step.equals("nolive") ? null : new TGLevels());
				Optional.ofNullable(this.live).ifPresent(levels -> levels.open(Key.out(4)));
			} else {
				return this.next(step.equals("play"));
			}
			return said;
		}

		private String next(boolean playing) {
			this.now += 33;
			return this.meters.next(this.live, this.now, playing).stream().map(Script::say).collect(Collectors.joining(" "));
		}

		private static String say(TGMeterReading reading) {
			String peak = (Double.isInfinite(reading.getPeak()) ? "-inf" : Long.toString(Math.round(reading.getPeak())));
			return (reading.isOn() ? peak + (reading.isClipped() ? "!" : "") : "off");
		}
	}
}
