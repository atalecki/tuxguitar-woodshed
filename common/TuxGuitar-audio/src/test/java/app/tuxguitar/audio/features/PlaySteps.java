package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;

import app.tuxguitar.audio.LivePlay;
import app.tuxguitar.audio.TGRender;
import app.tuxguitar.audio.action.TGPlayAction;
import app.tuxguitar.audio.device.TGAudioDeviceException;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerException;
import app.tuxguitar.player.impl.sequencer.MidiSequencerImpl;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

/**
 * Steps that play the song offline, on the Test Interface's clock, and keep what it sounded for the listening steps.
 */
public class PlaySteps {

	private final AudioWorld world;
	// where playing under way started (the When that happens while it plays plays it, see playUnderWay)
	private SongPosition playingFrom;
	// the sample of a change made while the song played live (see playLiveChanging)
	private long change;

	public PlaySteps(AudioWorld world) {
		this.world = world;
	}

	@Given("I am playing the song from {position}")
	public void playingFrom(SongPosition from) {
		this.playingFrom = from;
	}

	// the playing under way, until the song ends, offline: what happens while it plays is scheduled before
	void playUnderWay() {
		playFrom(this.playingFrom);
	}

	@When("I play the song from {position}( until the song ends)")
	public void playFrom(SongPosition from) {
		play(from, Long.MAX_VALUE, 1);
	}

	// stopped there: what was sounding stops with it
	@Given("I have played the song from {position} and stopped at {position}")
	@When("I play the song from {position} to {position}")
	public void playTo(SongPosition from, SongPosition to) {
		play(from, to.tick(world.song()), 1);
	}

	@When("I play the song from {position} for {int} passes")
	public void playPasses(SongPosition from, int passes) {
		play(from, Long.MAX_VALUE, passes);
	}

	// live, as the player plays the song on the TuxGuitar Synthesizer's sample clock (see LivePlay), not offline: what the
	// output sounded is kept (see LiveSteps); then the synthesizer's channels close
	@When("I play the song live from {position}")
	public void playLive(SongPosition from) throws MidiPlayerException, TGAudioDeviceException {
		playLive(from, 1);
	}

	// with Play looped on, the passes: the last one ends the song (see turn)
	@When("I play the song live from {position} for {int} passes")
	public void playLive(SongPosition from, int passes) throws MidiPlayerException, TGAudioDeviceException {
		playLive(from, passes, () -> { });
	}

	// the action taken before each turn of the play loop, as the app's other threads act while the song plays
	void playLive(SongPosition from, int passes, Runnable eachTurn) throws MidiPlayerException, TGAudioDeviceException {
		playLive(from, passes, () -> { }, eachTurn);
	}

	// and the action taken as Play starts: the song's chains run, Play has not yet waited for them to be ready
	void playLive(SongPosition from, int passes, Runnable asPlayStarts, Runnable eachTurn) throws MidiPlayerException, TGAudioDeviceException {
		MidiPlayer player = MidiPlayer.getInstance(world.fixture().context);
		player.loadSequencer(new MidiSequencerImpl(world.fixture().context));
		player.setTickPosition(from.tick(world.song()));
		player.play();
		asPlayStarts.run();
		world.threads().runStartHere();
		world.setLive(LivePlay.run(world.fixture().context, world.synthesizer(), world.backend(), () -> { }, block -> {
			eachTurn.run();
			return turn(player, passes);
		}));
		player.closeChannels();
	}

	// a turn of the play loop, as it plays the passes: in the last one Play looped goes off, so the song ends where that
	// pass ends, as when I turn it off while it plays. Whether playing goes on
	static boolean turn(MidiPlayer player, int passes) {
		try {
			player.getMode().setLoop(player.getMode().isLoop() && player.getSequencer().getPasses() < passes - 1);
		} catch (MidiPlayerException e) {
			throw new IllegalStateException(e);
		}
		return player.runPlayLoopProcess();
	}

	@When("I play the song live from {position} and press Stop {int} second(s) in")
	public void playLiveStopping(SongPosition from, int seconds) throws MidiPlayerException, TGAudioDeviceException {
		playLiveChanging(from, seconds, MidiPlayer::stop);
	}

	// as the app moves it while the song plays (TGTransport: the song's tick, here with no repeats)
	@When("I play the song live from {position}, moving the position to {position} {int} second(s) in")
	public void playLiveMoving(SongPosition from, SongPosition to, int seconds) throws MidiPlayerException, TGAudioDeviceException {
		playLiveChanging(from, seconds, player -> player.setTickPosition(to.tick(world.song())));
	}

	// live, the change made between two blocks (as the app's other threads act) once the device has played the block of
	// the second's sample: its sample, the first of the next block; the device then runs on for another second, the play
	// loop taking its turns, whether or not playing goes on
	private void playLiveChanging(SongPosition from, int seconds, Consumer<MidiPlayer> changing) throws MidiPlayerException, TGAudioDeviceException {
		MidiPlayer player = MidiPlayer.getInstance(world.fixture().context);
		player.loadSequencer(new MidiSequencerImpl(world.fixture().context));
		player.setTickPosition(from.tick(world.song()));
		player.play();
		world.threads().runStartHere();
		int at = seconds * TestInterface.SAMPLE_RATE / LivePractisingSteps.BLOCK;
		int last = at + TestInterface.SAMPLE_RATE / LivePractisingSteps.BLOCK;
		this.change = (at + 1L) * LivePractisingSteps.BLOCK;
		world.setLive(LivePlay.run(world.fixture().context, world.synthesizer(), world.backend(), () -> { }, block -> {
			if (block == at) {
				changing.accept(player);
			}
			player.runPlayLoopProcess();
			return block < last;
		}));
		player.stop();
		player.closeChannels();
	}

	long change() {
		return this.change;
	}

	// live, TuxGuitar stopped while the audio device runs (see LivePlay); then the synthesizer's channels close
	@When("the input has delivered {int} second(s) live")
	public void theInputHasDeliveredLive(int seconds) throws MidiPlayerException, TGAudioDeviceException {
		world.setLive(LivePlay.listen(world.fixture().context, world.synthesizer(), world.backend(), seconds * world.device().getSampleRate()));
		MidiPlayer.getInstance(world.fixture().context).closeChannels();
	}

	private void play(SongPosition from, long stop, int passes) {
		world.playAlong(from.tick(world.song()), stop, passes);
		world.fixture().actionContext.setAttribute(TGPlayAction.ATTRIBUTE_START, from.tick(world.song()));
		world.fixture().actionContext.setAttribute(TGPlayAction.ATTRIBUTE_STOP, stop);
		world.fixture().actionContext.setAttribute(TGPlayAction.ATTRIBUTE_PASSES, passes);
		world.fixture().actionContext.setAttribute(TGPlayAction.ATTRIBUTE_STOP_SAMPLE, null);
		world.execute(new TGPlayAction(world.fixture().context));
		assertTrue(world.succeeded());
	}

	static Heard heard(AudioWorld world) {
		TGRender render = world.fixture().actionContext.getAttribute(TGPlayAction.ATTRIBUTE_RENDER);
		return new Heard(render, world.song());
	}
}
