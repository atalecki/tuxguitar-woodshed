package app.tuxguitar.audio.action;

import java.util.ArrayList;
import java.util.List;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.audio.TGAudioSession;
import app.tuxguitar.audio.TGCountIn;
import app.tuxguitar.audio.TGPlayedMeasure;
import app.tuxguitar.audio.TGSongTime;
import app.tuxguitar.audio.TGTimeline;
import app.tuxguitar.audio.TGTurn;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerMode;
import app.tuxguitar.practice.TGPracticeChooser;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeUnit;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.util.TGContext;

/**
 * Plays an interleaved practice session offline, as TGPlayAction plays the song: its turns, chosen from a seed, back to
 * back, each its Count In (when Count In is on, in its first measure's time signature and tempo) then its unit's
 * measures once each in score order (TGSongTime.unit), all at the Percentage of Tempo in effect when it starts (Training
 * Mode's start percentage with Training Mode on); Play looped, the Loop Range and Training Mode take no part and stay as
 * they were. It stops by itself after the last turn, or at ATTRIBUTE_STOP_SAMPLE; either way the session has ended.
 */
public class TGPractiseAction extends TGPlayAction {

	public static final String NAME = "action.audio.practise";

	public static final String ATTRIBUTE_SESSION = "practice session";
	// the seed of the random choice of the turns
	public static final String ATTRIBUTE_SEED = "seed";

	public TGPractiseAction(TGContext context) {
		super(context, NAME);
	}

	protected void processAction(TGActionContext context) {
		super.processAction(context);
		TGPracticeSession session = context.getAttribute(ATTRIBUTE_SESSION);
		session.end();
	}

	protected TGTimeline timeline(TGActionContext context) {
		TGSong song = context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG);
		return timeline(getContext(), song, context.getAttribute(ATTRIBUTE_SESSION), context.getAttribute(ATTRIBUTE_SEED));
	}

	// the song as written: the units' measures are where it plays them (see TGSongTime.unit)
	protected MidiPlayerMode mode() {
		return new MidiPlayerMode();
	}

	// what the session plays, with the turns the seed chooses, with Count In and the Percentage of Tempo as they are now
	public static TGTimeline timeline(TGContext context, TGSong song, TGPracticeSession session, long seed) {
		MidiPlayer player = MidiPlayer.getInstance(context);
		int sampleRate = TGAudioSession.getInstance(context).getDevice().getSampleRate();
		int percent = startPercent(player.getMode());
		TGPracticeChooser chooser = session.chooser(seed);
		List<TGTurn> turns = new ArrayList<TGTurn>();
		for (int turn = 0; turn < session.getTurns(); turn++) {
			TGPracticeUnit unit = chooser.next();
			List<TGPlayedMeasure> measures = TGSongTime.unit(song, unit.getFirst(), unit.getLast(), turn, percent);
			turns.add(new TGTurn(TGCountIn.before(player.getCountDown(), measures, sampleRate), measures));
		}
		return TGTimeline.of(song, turns, sampleRate);
	}

	// the Percentage of Tempo the Play Mode starts at: Training Mode's start percentage, not one it has raised to
	private static int startPercent(MidiPlayerMode mode) {
		MidiPlayerMode start = new MidiPlayerMode();
		start.copyFrom(mode);
		start.reset();
		return start.getCurrentPercent();
	}
}
