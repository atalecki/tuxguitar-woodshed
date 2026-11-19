package app.tuxguitar.audio;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonUtil;

/**
 * The one quiet hint an app shows on its status line (the line under the fretboard): why Record was just refused (the
 * last time it was pressed, for REFUSAL_MILLIS after or until its cause no longer holds, whichever comes first), else
 * the hint of a plugin Play stopped waiting for (TGLoadingHint). An app reads it as often as it likes, off its UI
 * thread; null for none.
 */
public final class TGStatusHint {

	// how long the user is told why Record was refused
	static final long REFUSAL_MILLIS = 5000;

	private final Supplier<String> loading;
	// what Record needs, as it is now
	private final Supplier<TGRecordConditions> conditions;
	// now, in milliseconds
	private final LongSupplier clock;
	// Record's last refusal, and when; null when it was not refused
	private volatile Refused refused;

	TGStatusHint(Supplier<String> loading, Supplier<TGRecordConditions> conditions, LongSupplier clock) {
		this.loading = loading;
		this.conditions = conditions;
		this.clock = clock;
	}

	public static TGStatusHint getInstance(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGStatusHint.class.getName(),
			key -> new TGStatusHint(TGLoadingHint.install(key)::getText, () -> TGRecordConditions.of(key, TGDocumentManager.getInstance(key).getSong()),
				() -> TimeUnit.NANOSECONDS.toMillis(System.nanoTime())));
	}

	// Record was pressed: it was refused for that reason, or it records (null)
	public void recordPressed(TGRecordRefusal refusal) {
		this.refused = (refusal != null ? new Refused(refusal, this.clock.getAsLong()) : null);
	}

	public String getText() {
		Refused refused = this.refused;
		String told = (refused != null ? refused.told(this.clock.getAsLong(), this.conditions) : null);
		return (told != null ? told : this.loading.get());
	}

	private static final class Refused {
		private final TGRecordRefusal why;
		private final long at;

		Refused(TGRecordRefusal why, long at) {
			this.why = why;
			this.at = at;
		}

		// what the user is told at the time: why, for REFUSAL_MILLIS after, while its cause holds in what Record needs
		// now (read only then); else null
		String told(long time, Supplier<TGRecordConditions> conditions) {
			if (time - this.at >= REFUSAL_MILLIS) {
				return null;
			}
			TGRecordConditions now = conditions.get();
			return (this.why.holds(now) ? this.why.say(now) : null);
		}
	}
}
