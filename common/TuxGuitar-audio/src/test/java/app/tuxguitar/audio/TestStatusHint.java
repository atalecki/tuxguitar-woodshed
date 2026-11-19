package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import app.tuxguitar.editor.TuxGuitarMessages;

/**
 * The quiet hint an app shows on its status line: why Record was just refused, for 5 seconds after it was pressed or
 * until its cause no longer holds, whichever comes first, else the loading hint ("Woodshed Guitar is still loading
 * Clean.nam" here); read with TuxGuitar's English messages, on a clock the test moves, with what Record needs as the
 * test sets it (at first: interleaved practice under way, nothing armed, another MIDI Port, no device; every refusal
 * holds).
 */
class TestStatusHint {

	private static final String PRACTICE = "Interleaved practice";
	private static final TGRecordConditions NOTHING = new TGRecordConditions(PRACTICE, false, false, false);

	private long now = 1000;
	private TGRecordConditions conditions = NOTHING;
	private final TGStatusHint hint = new TGStatusHint(() -> "Woodshed Guitar is still loading Clean.nam", () -> this.conditions, () -> this.now);

	// what the hint reads at each of the times, in milliseconds after the start
	private String readAt(long... times) {
		long start = this.now;
		List<String> read = new ArrayList<String>();
		for (long time : times) {
			this.now = start + time;
			read.add(this.hint.getText());
		}
		return String.join(" | ", read);
	}

	private String english(Consumer<TGStatusHint> pressing, long... times) {
		return TuxGuitarMessages.english(() -> {
			pressing.accept(this.hint);
			return this.readAt(times);
		});
	}

	@Test
	void withoutARefusalItIsTheLoadingHint() {
		assertEquals("Woodshed Guitar is still loading Clean.nam", this.english(hint -> {
		}, 0));
	}

	@Test
	void aRefusalIsToldForFiveSecondsThenTheLoadingHintAgain() {
		assertEquals("Arm a track to record | Arm a track to record | Woodshed Guitar is still loading Clean.nam",
			this.english(hint -> hint.recordPressed(TGRecordRefusal.NO_ARMED_TRACK), 0, 4999, 5000));
	}

	// the last time Record was pressed: a later refusal from then on, none once it records
	@Test
	void theLastPressIsTold() {
		assertEquals("Choose an audio device to record | Woodshed Guitar is still loading Clean.nam", this.english(hint -> {
			hint.recordPressed(TGRecordRefusal.NO_ARMED_TRACK);
			hint.recordPressed(TGRecordRefusal.NO_DEVICE);
		}, 0) + " | " + this.english(hint -> hint.recordPressed(null), 0));
	}

	// Record refused, then, a second later, what Record needs as it is then: what the hint reads before and after
	private String refusedThen(TGRecordRefusal refusal, TGRecordConditions then) {
		String before = this.english(hint -> hint.recordPressed(refusal), 0);
		this.conditions = then;
		return before + " | " + this.english(hint -> {
		}, 1000);
	}

	// as soon as its cause no longer holds, though another refusal does
	@Test
	void aRefusalIsToldUntilItsCauseNoLongerHolds() {
		String ended = this.refusedThen(TGRecordRefusal.PRACTICE, new TGRecordConditions(null, false, false, false));
		String armed = this.refusedThen(TGRecordRefusal.NO_ARMED_TRACK, new TGRecordConditions(PRACTICE, true, false, false));
		String port = this.refusedThen(TGRecordRefusal.MIDI_PORT, new TGRecordConditions(PRACTICE, true, true, false));
		String device = this.refusedThen(TGRecordRefusal.NO_DEVICE, new TGRecordConditions(PRACTICE, false, false, true));
		assertEquals("Interleaved practice is under way: stop it to record. | Woodshed Guitar is still loading Clean.nam"
			+ " | Arm a track to record | Woodshed Guitar is still loading Clean.nam"
			+ " | Choose the TuxGuitar Synthesizer as the MIDI Port to record | Woodshed Guitar is still loading Clean.nam"
			+ " | Choose an audio device to record | Woodshed Guitar is still loading Clean.nam", String.join(" | ", ended, armed, port, device));
	}

	// while its cause holds, though another no longer does
	@Test
	void aRefusalWhoseCauseHoldsIsToldOn() {
		assertEquals("Choose the TuxGuitar Synthesizer as the MIDI Port to record | Choose the TuxGuitar Synthesizer as the MIDI Port to record",
			this.refusedThen(TGRecordRefusal.MIDI_PORT, new TGRecordConditions(null, true, false, true)));
	}
}
