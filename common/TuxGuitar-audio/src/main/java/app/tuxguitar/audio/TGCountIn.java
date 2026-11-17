package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import app.tuxguitar.player.base.MidiClick;
import app.tuxguitar.player.base.MidiPlayerCountDown;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGTimeSignature;

/**
 * Count In: clicks one beat apart before the first measure plays, the first one where playing starts and the last one
 * a beat before the first measure. One per beat of the measure playing starts in (or as many as the Count In ticks
 * set), a beat being the unit of its time signature at its tempo and the first pass's tempo scale, exactly; accented
 * as live (MidiClick.isCountInAccented: on the bar lines before where the first measure starts playing).
 * Positions are samples from where playing starts. Stopped early, it lasts until the stop and keeps the clicks before
 * it. Immutable.
 */
public final class TGCountIn {

	public static final TGCountIn NONE = new TGCountIn(new ArrayList<Boolean>(), 0, 0);

	// whether each click is accented, one per click
	private final List<Boolean> accents;
	// samples per beat, not rounded
	private final double beat;
	// samples before the first measure
	private final long length;

	private TGCountIn(List<Boolean> accents, double beat, long length) {
		this.accents = accents;
		this.beat = beat;
		this.length = length;
	}

	// the Count In before the measures played, with Count In on (none with it off, or when nothing plays)
	public static TGCountIn before(MidiPlayerCountDown countDown, List<TGPlayedMeasure> played, int sampleRate) {
		return (played.isEmpty() ? NONE : of(countDown::clicksBefore, played, sampleRate));
	}

	// as many clicks before the measures played (some) as the function of their first measure's header says (0 clicks
	// last no time, as none)
	public static TGCountIn of(ToIntFunction<TGMeasureHeader> clicksBefore, List<TGPlayedMeasure> played, int sampleRate) {
		TGPlayedMeasure first = played.get(0);
		int clicks = clicksBefore.applyAsInt(first.getHeader());
		TGTimeSignature signature = first.getHeader().getTimeSignature();
		double quarters = signature.getDenominator().getTime() / (double) TGDuration.QUARTER_TIME;
		double beat = quarters * 60.0 * sampleRate / first.getTempo();
		List<Boolean> accents = IntStream.range(0, clicks).mapToObj(click -> MidiClick.isCountInAccented(click, clicks, signature, first.getFrom())).collect(Collectors.toList());
		return new TGCountIn(accents, beat, Math.round(clicks * beat));
	}

	// samples before the first measure
	public long length() {
		return this.length;
	}

	// its clicks, from the sample it starts on: a beat apart; those after where it stopped left out
	public TGClicks clicks(long start) {
		List<Integer> clicks = IntStream.range(0, this.accents.size()).filter(click -> Math.round(click * this.beat) < this.length).boxed().collect(Collectors.toList());
		return new TGClicks(clicks.stream().map(click -> start + Math.round(click * this.beat)).collect(Collectors.toList()),
			clicks.stream().map(this.accents::get).collect(Collectors.toList()));
	}

	// stopped at the sample
	public TGCountIn until(long sample) {
		return new TGCountIn(this.accents, this.beat, Math.min(this.length, sample));
	}
}
