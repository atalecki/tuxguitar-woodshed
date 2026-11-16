package app.tuxguitar.practice;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * The words the app shows for practice, looked up in its messages (keys and their arguments) so that it words them in
 * its own language. A turn under the Transport window's position: of interleaved practice, "Turn 3 of 12: Line 5"
 * (transport.practice.turn), with the interval clock "Turn 3: Line 5, 1:42 left" (transport.practice.timed-turn) or in
 * a break "Break, 0:12 left. Next: Line 7" (transport.practice.break), a beat named by its measure and beat
 * (transport.practice.beat-turn, beat-timed-turn, beat-break); a click-up step, "Step 7 of 23: lines 2-3 at 84 bpm"
 * (transport.click-up.step), its sections one alone (transport.click-up.section.&lt;kind&gt;: "line 3", "measure 2 beat
 * 3") or a run (transport.click-up.sections.&lt;kind&gt;), its tempo by transport.click-up.tempo.&lt;mode&gt;. And the
 * label of a field of a range of units: as it is, or, when the range is given in another kind (beats in measures), in
 * that kind (transport.practice.range.&lt;kind&gt;: "Pool from (measures)").
 */
public final class TGPracticeWording {

	// the app's messages: the text of the key, the arguments in place of {0}, {1}...
	public interface Messages {
		String get(String key, String[] arguments);
	}

	private static final String[] NONE = new String[0];

	private TGPracticeWording() {
	}

	public static String turn(TGPracticeTurn turn, Messages messages) {
		return turn.getStep().map(step -> step(turn, step, messages)).orElseGet(() -> practice(turn, messages));
	}

	// the field's label, in the kind the range of units of the kind is given in when that is another
	public static String rangeField(String key, TGPracticeUnitKind kind, Messages messages) {
		String label = messages.get(key, NONE);
		TGPracticeUnitKind ranged = kind.rangeKind();
		return (ranged == kind ? label : messages.get("transport.practice.range." + ranged.getName(), new String[] {label}));
	}

	private static String step(TGPracticeTurn turn, TGClickUpStep step, Messages messages) {
		String tempo = messages.get("transport.click-up.tempo." + step.getTempoMode().getName(), new String[] {String.valueOf(step.getRung())});
		return messages.get("transport.click-up.step", new String[] {String.valueOf(turn.getTurn()), String.valueOf(turn.getTurns()), sections(turn, messages), tempo});
	}

	// one section by its place ("line 3", "measure 2 beat 3"), a run by the places of its first and last
	private static String sections(TGPracticeTurn turn, Messages messages) {
		String[] from = place(turn.getFrom());
		String[] to = place(turn.getTo());
		String kind = turn.getFrom().getKind().getName();
		return (Arrays.equals(from, to) ? messages.get("transport.click-up.section." + kind, from)
			: messages.get("transport.click-up.sections." + kind, Stream.concat(Stream.of(from), Stream.of(to)).toArray(String[]::new)));
	}

	// a beat's measure and its beat in it; any other unit's number
	private static String[] place(TGPracticeUnit unit) {
		if (unit.getKind() == TGPracticeUnitKind.BEAT) {
			return new String[] {String.valueOf(unit.getFirst()), String.valueOf(unit.getNumber())};
		}
		return new String[] {String.valueOf(unit.getNumber())};
	}

	// a turn of interleaved practice, counted or timed: its unit named by its kind's word and number, a beat by its
	// measure and beat (the keys' beat- forms)
	private static String practice(TGPracticeTurn turn, Messages messages) {
		TGPracticeUnit unit = turn.getFrom();
		boolean beat = (unit.getKind() == TGPracticeUnitKind.BEAT);
		String key = "transport.practice." + (beat ? "beat-" : "");
		String[] named = (beat ? place(unit) : new String[] {messages.get("transport.practice.turn." + unit.getKind().getName(), NONE), String.valueOf(unit.getNumber())});
		if (!turn.isTimed()) {
			return messages.get(key + "turn", new String[] {String.valueOf(turn.getTurn()), String.valueOf(turn.getTurns()), named[0], named[1]});
		}
		String left = String.format("%d:%02d", turn.getSecondsLeft() / 60, turn.getSecondsLeft() % 60);
		return messages.get(key + (turn.isBreak() ? "break" : "timed-turn"), new String[] {String.valueOf(turn.getTurn()), named[0], named[1], left});
	}
}
