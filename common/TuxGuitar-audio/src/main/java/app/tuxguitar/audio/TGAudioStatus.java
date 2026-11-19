package app.tuxguitar.audio;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.practice.TGPracticeUnderWay;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.util.TGContext;

/**
 * What an app shows of its audio, read at one moment (an app polls it): whether a recording is under way (its state,
 * a message key), whether Record can start one (a device, an armed track, no recording nor practice session under way;
 * the MIDI Port does not grey it out, the record action refuses then: TGRecordRefusal) and the input meter
 * of each armed track of the song open, in track order. Without a device nothing is metered.
 */
public final class TGAudioStatus {

	public static final String NO_DEVICE = "audio.status.no-device";
	public static final String NOT_RECORDING = "audio.status.not-recording";
	public static final String RECORDING = "audio.status.recording";

	private final String state;
	private final boolean canRecord;
	private final List<Meter> meters;

	private TGAudioStatus(String state, boolean canRecord, List<Meter> meters) {
		this.state = state;
		this.canRecord = canRecord;
		this.meters = meters;
	}

	public static TGAudioStatus of(TGContext context) {
		TGAudioSession session = TGAudioSession.getInstance(context);
		if (session.getDevice() == null) {
			return new TGAudioStatus(NO_DEVICE, false, Collections.<Meter>emptyList());
		}
		List<Meter> meters = session.getArmedTracks(TGDocumentManager.getInstance(context).getSong()).stream().map(track -> new Meter(track, session.getInputLevel(track))).collect(Collectors.toList());
		boolean recording = session.isRecording();
		return new TGAudioStatus(recording ? RECORDING : NOT_RECORDING, !recording && !meters.isEmpty() && TGPracticeUnderWay.getInstance(context).getCourse() == null, meters);
	}

	/**
	 * Whether a track has audio track settings (monitoring, input channels) an app can show: an audio track does, other
	 * tracks do not, nor does no track (an app's caret before a song is shown).
	 */
	public static boolean hasTrackSettings(TGTrack track) {
		return track != null && track.isAudio();
	}

	public String getState() {
		return this.state;
	}

	public boolean canRecord() {
		return this.canRecord;
	}

	public List<Meter> getMeters() {
		return this.meters;
	}

	/**
	 * An armed track's input level: how full its meter is (0 to 100), the level ("-6.0 dBFS", "-inf dBFS" for silence)
	 * and whether it has clipped since its clip indicator was reset.
	 */
	public static final class Meter {

		private final TGTrack track;
		private final int percent;
		private final String level;
		private final boolean clipped;
		private final TGInputLevel input;

		private Meter(TGTrack track, TGInputLevel meter) {
			double level = meter.getLevel();
			this.track = track;
			this.percent = TGMeterScale.percent(level);
			this.level = TGMeterScale.number(level) + " dBFS";
			this.clipped = meter.hasClipped();
			this.input = meter;
		}

		public TGTrack getTrack() {
			return this.track;
		}

		public int getPercent() {
			return this.percent;
		}

		public String getLevel() {
			return this.level;
		}

		public boolean hasClipped() {
			return this.clipped;
		}

		// the level itself, which a meter reads (TGTapMeter) as the Track panel's do
		public TGInputLevel getInput() {
			return this.input;
		}
	}
}
