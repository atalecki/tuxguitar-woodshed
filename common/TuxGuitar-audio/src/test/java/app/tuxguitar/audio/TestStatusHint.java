package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import app.tuxguitar.editor.TuxGuitarMessages;

/**
 * The quiet hint an app shows on its status line: why Record was just refused, for 5 seconds after it was pressed, else
 * the loading hint ("Woodshed Guitar is still loading Clean.nam" here); read with TuxGuitar's English messages, on a
 * clock the test moves.
 */
class TestStatusHint {

	private long now = 1000;
	private final TGStatusHint hint = new TGStatusHint(() -> "Woodshed Guitar is still loading Clean.nam", () -> this.now);

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
}
