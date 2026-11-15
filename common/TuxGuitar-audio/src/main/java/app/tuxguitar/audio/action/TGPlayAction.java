package app.tuxguitar.audio.action;

import java.util.List;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.audio.TGAudioSession;
import app.tuxguitar.audio.TGCountIn;
import app.tuxguitar.audio.TGPlayedMeasure;
import app.tuxguitar.audio.TGRender;
import app.tuxguitar.audio.TGRenderer;
import app.tuxguitar.audio.TGSongTime;
import app.tuxguitar.audio.TGTimeline;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerException;
import app.tuxguitar.player.base.MidiPlayerMode;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.util.TGContext;

/**
 * Plays the song offline, on the audio device's clock, from a position until another one is reached in the last pass,
 * or until that pass ends, after the Count In when it is on; stopped at a sample of what plays (ATTRIBUTE_STOP_SAMPLE,
 * counted from where playing starts) it plays up to there. What it sounded is left in the context as
 * ATTRIBUTE_RENDER. What the inputs delivered meanwhile feeds the armed tracks' input meters, as when recording.
 */
public class TGPlayAction extends TGActionBase {

	public static final String NAME = "action.audio.play";

	public static final String ATTRIBUTE_START = TGRecordAction.ATTRIBUTE_START;
	public static final String ATTRIBUTE_STOP = TGRecordAction.ATTRIBUTE_STOP;
	public static final String ATTRIBUTE_PASSES = TGRecordAction.ATTRIBUTE_PASSES;
	public static final String ATTRIBUTE_STOP_SAMPLE = TGRecordAction.ATTRIBUTE_STOP_SAMPLE;
	public static final String ATTRIBUTE_RENDER = "render";

	public TGPlayAction(TGContext context) {
		this(context, NAME);
	}

	protected TGPlayAction(TGContext context, String name) {
		super(context, name);
	}

	protected void processAction(TGActionContext context) {
		TGRender render = render(getContext(), context, getSongManager(context), this.timeline(context), this.mode(), false);
		TGSong song = context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG);
		TGAudioSession.getInstance(getContext()).meterPlayed(getSongManager(context), song, render.getTimeline(), render.getLatency());
		context.setAttribute(ATTRIBUTE_RENDER, render);
		context.setAttribute(ATTRIBUTE_SUCCESS, Boolean.TRUE);
	}

	// what plays, before the stop sample: the song in song time from the context's positions
	protected TGTimeline timeline(TGActionContext context) {
		return songTime(getContext(), context);
	}

	// the Play Mode the MIDI sequence parser plays the song in
	protected MidiPlayerMode mode() {
		return MidiPlayer.getInstance(getContext()).getMode();
	}

	// what playing (or recording) from the context's positions sounds
	static TGRender play(TGContext context, TGActionContext actionContext, TGSongManager songManager, boolean recording) {
		return render(context, actionContext, songManager, songTime(context, actionContext), MidiPlayer.getInstance(context).getMode(), recording);
	}

	// from the start position until the stop position is reached in the last pass, after the Count In when it is on
	private static TGTimeline songTime(TGContext context, TGActionContext actionContext) {
		TGSong song = actionContext.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG);
		MidiPlayer player = MidiPlayer.getInstance(context);
		Long start = actionContext.getAttribute(ATTRIBUTE_START);
		Long stop = actionContext.getAttribute(ATTRIBUTE_STOP);
		Integer passes = actionContext.getAttribute(ATTRIBUTE_PASSES);
		int sampleRate = TGAudioSession.getInstance(context).getDevice().getSampleRate();
		List<TGPlayedMeasure> played = TGSongTime.play(song, player.getMode(), start, stop, passes);
		return new TGTimeline(song, played, sampleRate, TGCountIn.before(player.getCountDown(), played, sampleRate));
	}

	// what the timeline sounds up to the context's stop sample, its song played in the Play Mode
	private static TGRender render(TGContext context, TGActionContext actionContext, TGSongManager songManager, TGTimeline timeline, MidiPlayerMode mode, boolean recording) {
		TGSong song = actionContext.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG);
		TGAudioSession session = TGAudioSession.getInstance(context);
		Long stopSample = actionContext.getAttribute(ATTRIBUTE_STOP_SAMPLE);
		TGRenderer renderer = new TGRenderer(context, songManager, song, session);
		try {
			return renderer.render(timeline.until(stopSample != null ? stopSample : Long.MAX_VALUE), mode, recording,
				MidiPlayer.getInstance(context).isOutputPortOpen(TGAudioSession.SYNTHESIZER_PORT_KEY));
		} catch (MidiPlayerException e) {
			throw new IllegalStateException(e);
		}
	}
}
