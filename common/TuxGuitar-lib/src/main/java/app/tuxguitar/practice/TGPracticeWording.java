package app.tuxguitar.practice;

import java.util.stream.Stream;

/**
 * The words the app shows for practice, looked up in its messages (keys and their arguments) so that it words them in
 * its own language. A turn under the Transport window's position: of interleaved practice, "Turn 3 of 12: Line 5"
 * (transport.practice.turn), with the interval clock "Turn 3: Line 5, 1:42 left" (transport.practice.timed-turn) or in
 * a break "Break, 0:12 left. Next: Line 7" (transport.practice.break), a beat named by its measure and beat
 * (transport.practice.beat-turn, beat-timed-turn, beat-break), a block of more than one unit by its first and last unit,
 * capitalised as a unit is, "Turn 3 of 12: Lines 4–5" (transport.practice.block-turn, block-timed-turn, block-break,
 * the run by transport.practice.block.&lt;kind&gt;: "Lines 4–5", "Measure 3 beat 4 to measure 4 beat 1"); a click-up
 * step, "Step 7 of 23: lines 2-3 at 84 bpm"
 * (transport.click-up.step), its sections one alone (transport.click-up.section.&lt;kind&gt;: "line 3", "measure 2 beat
 * 3") or a run (transport.click-up.sections.&lt;kind&gt;), its tempo by transport.click-up.tempo.&lt;mode&gt;. And the
 * label of a field of a range of units: as it is, or, when the range is given in another kind (beats in measures), in
 * that kind (transport.practice.range.&lt;kind&gt;: "Pool from (measures)"); the label of the units per block by the
 * kind (transport.practice.units-per-block.&lt;kind&gt;: "Measures per block").
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

	// the label of the units per block, by the kind of unit: "Measures per block"
	public static String blockField(TGPracticeUnitKind kind, Messages messages) {
		return messages.get("transport.practice.units-per-block." + kind.getName(), NONE);
	}

	private static String step(TGPracticeTurn turn, TGClickUpStep step, Messages messages) {
		String tempo = messages.get("transport.click-up.tempo." + step.getTempoMode().getName(), new String[] {String.valueOf(step.getRung())});
		return messages.get("transport.click-up.step", new String[] {String.valueOf(turn.getTurn()), String.valueOf(turn.getTurns()), sections(turn, messages), tempo});
	}

	// one section by its place ("line 3", "measure 2 beat 3"), a run by the places of its first and last ("lines 2–3")
	private static String sections(TGPracticeTurn turn, Messages messages) {
		if (isRun(turn)) {
			return run("transport.click-up.sections.", turn, messages);
		}
		return messages.get("transport.click-up.section." + turn.getFrom().getKind().getName(), place(turn.getFrom()));
	}

	// a run by the places of its first and last unit, in the key of the prefix and their kind
	private static String run(String prefix, TGPracticeTurn turn, Messages messages) {
		return messages.get(prefix + turn.getFrom().getKind().getName(), arguments(place(turn.getFrom()), place(turn.getTo()), NONE));
	}

	// a beat's measure and its beat in it; any other unit's number
	private static String[] place(TGPracticeUnit unit) {
		if (unit.getKind() == TGPracticeUnitKind.BEAT) {
			return new String[] {String.valueOf(unit.getFirst()), String.valueOf(unit.getNumber())};
		}
		return new String[] {String.valueOf(unit.getNumber())};
	}

	// a turn of interleaved practice, counted or timed: its block named as named says, in the keys of its form
	private static String practice(TGPracticeTurn turn, Messages messages) {
		String key = "transport.practice." + form(turn);
		String[] named = named(turn, messages);
		if (!turn.isTimed()) {
			return messages.get(key + "turn", arguments(new String[] {String.valueOf(turn.getTurn()), String.valueOf(turn.getTurns())}, named, NONE));
		}
		String left = String.format("%d:%02d", turn.getSecondsLeft() / 60, turn.getSecondsLeft() % 60);
		return messages.get(key + (turn.isBreak() ? "break" : "timed-turn"), arguments(new String[] {String.valueOf(turn.getTurn())}, named, new String[] {left}));
	}

	// whether the turn plays a run of more than one unit (a block, a step's sections): its first and last are not one unit
	private static boolean isRun(TGPracticeTurn turn) {
		return !turn.getFrom().equals(turn.getTo());
	}

	// the keys' form: block- for a block of more than one unit, beat- for a beat, none for any other unit
	private static String form(TGPracticeTurn turn) {
		if (isRun(turn)) {
			return "block-";
		}
		return (turn.getFrom().getKind() == TGPracticeUnitKind.BEAT ? "beat-" : "");
	}

	// a block of more than one unit by its run ("Lines 4–5"); a beat by its measure and beat; any other unit by its
	// kind's word and its number
	private static String[] named(TGPracticeTurn turn, Messages messages) {
		TGPracticeUnit unit = turn.getFrom();
		if (isRun(turn)) {
			return new String[] {run("transport.practice.block.", turn, messages)};
		}
		if (unit.getKind() == TGPracticeUnitKind.BEAT) {
			return place(unit);
		}
		return new String[] {messages.get("transport.practice.turn." + unit.getKind().getName(), NONE), String.valueOf(unit.getNumber())};
	}

	// the arguments before, in between and after, in that order
	private static String[] arguments(String[] before, String[] named, String[] after) {
		return Stream.of(before, named, after).flatMap(Stream::of).toArray(String[]::new);
	}
}
