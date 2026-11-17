package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.song.models.TGVelocities;
import app.tuxguitar.util.TGContext;

/**
 * Pins the count down played before the song (MidiPlayerCountDown.start): on the percussion channel, a side stick
 * (key 37, default velocity: the player's click sound until changed) per beat of the time signature of the measure being played (or the chosen number of ticks), at its tempo
 * scaled by the tempo percent; the first tick at once, and the count down lasts until the beat after the last tick.
 * It stops as soon as the player stops; without a measure being played, it plays nothing. Without a percussion channel
 * it ticks on the channel the player opens for the clicks.
 *
 * Time is virtual: the clock only moves while the count down waits. The song has 2 measures, 4/4 at 120;
 * the player is running at the start of the song. The result is the side stick's "on"/"off" with their time in ms,
 * then the time the count down ended at.
 */
public class TestMidiPlayerCountDown {

	private final TGContext context = new TGContext();
	private final MidiPlayer player = MidiPlayer.getInstance(context);
	private final TGSongManager songManager = TGDocumentManager.getInstance(context).getSongManager();
	private final TGSong song = TGDocumentManager.getInstance(context).getSong();
	private final VirtualCountDown countDown = new VirtualCountDown();
	private final List<String> log = new ArrayList<String>();
	// what the synthesizer's channels were sent
	private final List<String> received = new ArrayList<String>();

	private class VirtualCountDown extends MidiPlayerCountDown {
		private long now = 0;
		private long stopAt = Long.MAX_VALUE;

		VirtualCountDown() {
			super(player);
		}

		@Override
		protected long currentTime() {
			return this.now;
		}

		@Override
		protected void pause(long millis) {
			this.now += millis;
			if (this.now >= this.stopAt) {
				player.setRunning(false);
			}
		}
	}

	private class SideStickChannel implements MidiChannel {
		public void sendNoteOn(int key, int velocity, int voice, boolean bendMode) { log.add(note("on", key, velocity, voice, bendMode)); }
		public void sendNoteOff(int key, int velocity, int voice, boolean bendMode) { log.add(note("off", key, velocity, voice, bendMode)); }
		public void sendPitchBend(int value, int voice, boolean bendMode) { }
		public void sendProgramChange(int value) { }
		public void sendControlChange(int controller, int value) { }
		public void sendParameter(String key, String value) { }
		public void sendAllNotesOff() { }
	}

	// only the side stick's key and time, when sent at the default velocity, on no voice, not in bend mode
	private String note(String kind, int key, int velocity, int voice, boolean bendMode) {
		String sent = " v" + velocity + " s" + voice + " b" + bendMode;
		return kind + " " + key + " @" + countDown.now + (sent.equals(" v" + TGVelocities.DEFAULT + " s-1 bfalse") ? "" : sent);
	}

	public TestMidiPlayerCountDown() {
		songManager.addNewMeasureBeforeEnd(song);
		song.getMeasureHeader(0).getTempo().setQuarterValue(120);
		player.setRunning(true);
		countDown.setEnabled(true);
	}

	// a percussion channel as a song file may have it: with id 0
	private void withPercussion() {
		TGChannel channel = songManager.createChannel();
		channel.setChannelId(0);
		channel.setBank(TGChannel.DEFAULT_PERCUSSION_BANK);
		song.addChannel(channel);
		player.getChannelRouter().addMidiChannel(0, new SideStickChannel());
	}

	private String start() {
		player.updateLoop(true);
		countDown.start();
		return String.join(", ", log) + " -> end @" + countDown.now + (countDown.isRunning() ? " running" : "");
	}

	@Test
	public void ticksEveryBeatOfTheMeasure() {
		withPercussion();

		assertEquals("on 37 @0, off 37 @1, on 37 @501, off 37 @502, on 37 @1002, off 37 @1003, on 37 @1503, off 37 @1504"
			+ " -> end @2014", start());
	}

	@Test
	public void ticksEveryBeatOfTheTimeSignature() {
		withPercussion();
		TGMeasureHeader header = song.getMeasureHeader(0);
		header.getTimeSignature().setNumerator(3);
		header.getTimeSignature().getDenominator().setValue(TGDuration.EIGHTH);

		assertEquals("on 37 @0, off 37 @1, on 37 @251, off 37 @252, on 37 @502, off 37 @503 -> end @763", start());
	}

	@Test
	public void ticksTheChosenNumberOfTimesAtTheTempoPercent() {
		withPercussion();
		countDown.setTickCount(2);
		countDown.setTempoPercent(50);

		assertEquals("on 37 @0, off 37 @1, on 37 @1001, off 37 @1002 -> end @2012", start());
	}

	// 117 bpm at 80% is 93.6 bpm, a beat of 641.03 ms (not 93 bpm, 645.16 ms, as a whole bpm)
	@Test
	public void ticksAtTheExactTempoPercent() {
		withPercussion();
		song.getMeasureHeader(0).getTempo().setQuarterValue(117);
		countDown.setTickCount(2);
		countDown.setTempoPercent(80);

		assertEquals("on 37 @0, off 37 @1, on 37 @641, off 37 @642 -> end @1292", start());
	}

	@Test
	public void ticksTheClickSound() {
		withPercussion();
		countDown.setTickCount(1);
		player.setClick(new MidiClick(76, 127));

		assertEquals("on 76 @0 v127 s-1 bfalse, off 76 @1 v127 s-1 bfalse -> end @511", start());
	}

	// on a synthesizer that plays on a sample clock without drums of its own Count In is the same: on the percussion
	// channel at once, through its sound chain, nothing on the clock
	@Test
	public void onASynthesizerWithASampleClockItTicksOnThePercussionChannelAtOnce() {
		withPercussion();
		countDown.setTickCount(1);
		List<String> clocked = new ArrayList<String>();
		player.getSynthesizerProxy().setMidiSynthesizer(new ClockedSynthesizer(clocked));

		assertEquals("on 37 @0, off 37 @1 -> end @511 | []", start() + " | " + clocked);
	}

	@Test
	public void stopsWithThePlayer() {
		withPercussion();
		countDown.stopAt = 600;

		assertEquals("on 37 @0, off 37 @1, on 37 @501, off 37 @502 -> end @602", start());
	}

	// the measure that starts on the player's tick, not the one that ends there
	@Test
	public void findsTheMeasureFromItsFirstTick() {
		player.updateLoop(true);
		player.setTickPosition(song.getMeasureHeader(1).getStart());

		assertEquals(2, countDown.findMeasureHeader().getNumber());
	}

	@Test
	public void playsNothingWhenDisabled() {
		withPercussion();
		countDown.setEnabled(false);

		assertEquals(" -> end @0", start());
	}

	// the clicks a Count In has, on or off (a click-up counts in with Count In off): one per beat of the measure's time
	// signature, or the tick count set; before the measure, only with Count In on
	@Test
	public void aCountInHasAClickPerBeatOrTheTickCountWhetherItIsOnOrOff() {
		TGMeasureHeader header = song.getMeasureHeader(0);
		header.getTimeSignature().setNumerator(3);
		countDown.setEnabled(false);
		String beats = countDown.clicks(header) + " " + countDown.clicksBefore(header);
		countDown.setTickCount(2);
		String ticks = countDown.clicks(header) + " " + countDown.clicksBefore(header);
		countDown.setEnabled(true);

		assertEquals("3 0 | 2 0 | 2", beats + " | " + ticks + " | " + countDown.clicksBefore(header));
	}

	// on the channel the player opens for the clicks as playing starts: without a percussion channel, a percussion
	// channel of its own on a port (see TestMidiPlayerMetronome); the drums of a synthesizer that has them (the TuxGuitar
	// Synthesizer's), percussion channel or not
	private String ticksOn(MidiSynthesizer synthesizer) throws MidiPlayerException {
		countDown.setTickCount(1);
		player.getSynthesizerProxy().setMidiSynthesizer(synthesizer);
		player.updateChannels();
		received.clear();
		return start() + " | " + received;
	}

	@Test
	public void withoutAPercussionChannelItTicksOnThePlayersPercussionChannelOnAPort() throws MidiPlayerException {
		assertEquals(" -> end @511 | [-3 on 37 95, -3 off 37 95]", ticksOn(new PortSynthesizer(received)));
	}

	@Test
	public void withoutAPercussionChannelItTicksOnTheDrumsOfASynthesizerThatHasThem() throws MidiPlayerException {
		assertEquals(" -> end @511 | [-2 on 77 95, -2 off 77 95]", ticksOn(new ClockedSynthesizer(received, true)));
	}

	// with one too: the TuxGuitar Synthesizer's Count In clicks on its drums (the built-in click), as offline, not
	// through the percussion channel's sound chain
	@Test
	public void withAPercussionChannelItTicksOnTheDrumsOfASynthesizerThatHasThem() throws MidiPlayerException {
		withPercussion();

		assertEquals(" -> end @511 | [-2 on 77 95, -2 off 77 95]", ticksOn(new ClockedSynthesizer(received, true)));
	}

	// the notes Count In sends the drums (the built-in click), so many ticks before the player's tick
	private String accentsBefore(long tick, int ticks) throws MidiPlayerException {
		countDown.setTickCount(ticks);
		player.getSynthesizerProxy().setMidiSynthesizer(new ClockedSynthesizer(received, true));
		player.updateChannels();
		player.setTickPosition(tick);
		// as Play does before Count In
		player.changeTickPosition();
		received.clear();
		start();
		return received.stream().filter(line -> line.startsWith("-2 on ")).map(line -> line.split(" ")[2]).collect(java.util.stream.Collectors.joining(" "));
	}

	// on the drums (the built-in click) the clicks on beat 1 are accented, counted backwards from where playing starts
	// in the time signature of its measure (MidiClick.isCountInAccented): 7 clicks before 3/4 are "3 | 1 2 3 | 1 2 3";
	// on a port every click is the drum note (ticksEveryBeatOfTheMeasure)
	@Test
	public void onTheDrumsTheClicksOnTheBarLinesBeforeTheStartAreAccented() throws MidiPlayerException {
		song.getMeasureHeader(0).getTimeSignature().setNumerator(3);

		assertEquals("77 76 77 77 76 77 77", accentsBefore(song.getMeasureHeader(0).getStart(), 7));
	}

	// from beat 3 of 4/4, 4 clicks are "3 4 | 1 2"
	@Test
	public void beforeAStartOnABeatTheClicksLeadIntoThatBeat() throws MidiPlayerException {
		assertEquals("77 77 76 77", accentsBefore(song.getMeasureHeader(0).getStart() + 2 * TGDuration.QUARTER_TIME, 4));
	}

	// from half way through beat 2 no click falls on a beat of the song: the first is accented, as before
	@Test
	public void beforeAStartBetweenTwoBeatsTheFirstClickIsAccented() throws MidiPlayerException {
		assertEquals("76 77 77 77 76 77", accentsBefore(song.getMeasureHeader(0).getStart() + 3 * TGDuration.QUARTER_TIME / 2, 6));
	}

	// in the second measure, it is where playing starts in that measure that counts
	@Test
	public void theStartIsCountedFromItsOwnMeasure() throws MidiPlayerException {
		assertEquals("77 77 76", accentsBefore(song.getMeasureHeader(1).getStart() + TGDuration.QUARTER_TIME, 3));
	}

	@Test
	public void playsNothingOutsideTheLoop() {
		withPercussion();
		player.getMode().setLoop(true);
		player.getMode().setLoopSHeader(2);
		player.getMode().setLoopEHeader(2);

		assertEquals(" -> end @0", start());
	}
}
