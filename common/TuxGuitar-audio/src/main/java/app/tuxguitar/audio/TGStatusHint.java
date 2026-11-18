package app.tuxguitar.audio;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonUtil;

/**
 * The one quiet hint an app shows on its status line (the line under the fretboard): why Record was just refused (the
 * last time it was pressed, for REFUSAL_MILLIS after), else the hint of a plugin Play stopped waiting for
 * (TGLoadingHint). An app reads it as often as it likes, off its UI thread; null for none.
 */
public final class TGStatusHint {

	// how long the user is told why Record was refused
	static final long REFUSAL_MILLIS = 5000;

	private final Supplier<String> loading;
	// now, in milliseconds
	private final LongSupplier clock;
	// Record's last refusal, and when; null when it was not refused
	private volatile Refused refused;

	TGStatusHint(Supplier<String> loading, LongSupplier clock) {
		this.loading = loading;
		this.clock = clock;
	}

	public static TGStatusHint getInstance(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGStatusHint.class.getName(),
			key -> new TGStatusHint(TGLoadingHint.install(key)::getText, () -> TimeUnit.NANOSECONDS.toMillis(System.nanoTime())));
	}

	// Record was pressed: it was refused for that reason, or it records (null)
	public void recordPressed(TGRecordRefusal refusal) {
		this.refused = (refusal != null ? new Refused(refusal, this.clock.getAsLong()) : null);
	}

	public String getText() {
		Refused refused = this.refused;
		return (refused != null && this.clock.getAsLong() - refused.at < REFUSAL_MILLIS ? refused.why.say() : this.loading.get());
	}

	private static final class Refused {
		private final TGRecordRefusal why;
		private final long at;

		Refused(TGRecordRefusal why, long at) {
			this.why = why;
			this.at = at;
		}
	}
}
