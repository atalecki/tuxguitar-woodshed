package app.tuxguitar.audio;

import static app.tuxguitar.editor.action.SongActionFixture.reads;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import app.tuxguitar.audio.TGSoundChainFollow.Key;
import app.tuxguitar.editor.TuxGuitarMessages;
import app.tuxguitar.editor.action.SongActionFixture;
import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.midi.synth.impl.GervillProcessorFactory;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.song.models.TGTrack;

/**
 * A sound chain editor that follows the caret's track (the Sound Chain panel) reads the rows again, which asks what the
 * default instrument plays (it touches the disk), only when what it shows changed: another song or track, the track's
 * name, its channel, the channel's instrument (its kind decides the default), the chain, or the default instruments'
 * stamp; not on each move of the caret. Another track picks its source; the same track keeps the row picked while it is
 * there. The song's tracks play a source "drone" (General MIDI's name needs the app's messages).
 */
class TestSoundChainFollow {

	private final SongActionFixture fixture = new SongActionFixture();
	private final TGTrack guitar = this.fixture.song.getTrack(0);
	private final TGTrack other = this.fixture.songManager.addTrack(this.fixture.song);

	TestSoundChainFollow() {
		this.change(this.guitar, chain -> chain.setReceiver(TGSoundChain.source("drone")));
		this.change(this.other, chain -> chain.setReceiver(TGSoundChain.source("drone")));
	}

	private TGChannel channel(TGTrack track) {
		return this.fixture.songManager.getChannel(this.fixture.song, track.getChannelId());
	}

	private void change(TGTrack track, Consumer<TGProgram> change) {
		TGProgram chain = TGSoundChain.get(this.channel(track));
		change.accept(chain);
		TGSoundChain.save(this.channel(track), chain, this.fixture.songManager.getFactory());
	}

	private Key key(TGTrack track) {
		return TGSoundChainFollow.key(track, this.channel(track), 1);
	}

	// whether the editor reads the rows again after the change, from the guitar's key
	private String changedAfter(Runnable change) {
		Key shown = this.key(this.guitar);
		change.run();
		return String.valueOf(TGSoundChainFollow.changed(shown, this.key(this.guitar)));
	}

	@Test
	void nothingIsReadAgainWhileTheSameThingIsShown() {
		assertEquals("false false", TGSoundChainFollow.changed(this.key(this.guitar), this.key(this.guitar)) + " " + TGSoundChainFollow.changed(Key.NONE, TGSoundChainFollow.key(this.fixture.context, null)));
	}

	@TestFactory
	Stream<DynamicTest> theRowsAreReadAgainWhenWhatIsShownChanged() {
		return Stream.of(
			reads("another track", "true", () -> String.valueOf(TGSoundChainFollow.changed(this.key(this.guitar), this.key(this.other)))),
			reads("no track", "true", () -> String.valueOf(TGSoundChainFollow.changed(this.key(this.guitar), Key.NONE))),
			reads("another song", "true", () -> String.valueOf(TGSoundChainFollow.changed(this.key(this.guitar), this.key(new SongActionFixture().song.getTrack(0))))),
			reads("the default instruments' stamp", "true", () -> String.valueOf(TGSoundChainFollow.changed(this.key(this.guitar), TGSoundChainFollow.key(this.guitar, this.channel(this.guitar), 2)))),
			reads("an effect added", "true", () -> this.changedAfter(() -> this.change(this.guitar, chain -> chain.addOutput(TGSoundChain.source("echo"))))),
			reads("the source", "true", () -> this.changedAfter(() -> this.change(this.guitar, chain -> chain.setReceiver(TGSoundChain.source("organ"))))),
			reads("the track renamed", "true", () -> this.changedAfter(() -> this.guitar.setName("Lead"))),
			reads("the track's colour", "true", () -> this.changedAfter(() -> this.guitar.getColor().setG(7))),
			reads("the track's sound, its chain read the same", "true", () -> TuxGuitarMessages.english(this::switchedToMidi)),
			reads("the channel's instrument", "true", () -> this.changedAfter(() -> this.channel(this.guitar).setProgram((short) 33))),
			reads("the channel's bank", "true", () -> this.changedAfter(() -> this.channel(this.guitar).setBank((short) 128))),
			reads("another channel", "true", () -> this.changedAfter(() -> this.guitar.setChannelId(this.other.getChannelId()))),
			reads("the track's position", "true", () -> this.changedAfter(() -> this.guitar.setNumber(5))),
			reads("a note entered", "false", () -> this.changedAfter(() -> this.fixture.note(this.fixture.beat(this.guitar.getMeasure(0), 0), 1, 3))));
	}

	// the default source switched to MIDI: both read as the General MIDI program (which needs the app's messages); the
	// source "drone" again after
	private String switchedToMidi() {
		this.change(this.guitar, chain -> chain.setReceiver(TGSoundChain.source(GervillProcessorFactory.TYPE)));
		String changed = this.changedAfter(() -> this.change(this.guitar, chain -> chain.setReceiver(TGSoundChain.midi())));
		this.change(this.guitar, chain -> chain.setReceiver(TGSoundChain.source("drone")));
		return changed;
	}

	// the row picked after reading again, as "<from> to <to>: <row>"
	private static String picked(String names, Key shown, Key now, Integer before, int rows) {
		return names + ": " + TGSoundChainFollow.picked(shown, now, before, rows);
	}

	@Test
	void anotherTrackPicksItsSourceTheSameTrackKeepsTheRowPickedWhileItIsThere() {
		Key guitar = this.key(this.guitar);
		Key other = this.key(this.other);
		assertEquals("first track shown: 0 | another track: 0 | another track, only its source: 0 | same track: 2 | same track, the row gone: -1 | same track, none picked: -1 | no track: -1",
			String.join(" | ", picked("first track shown", Key.NONE, guitar, null, 3), picked("another track", guitar, other, 2, 3), picked("another track, only its source", guitar, other, 2, 1), picked("same track", guitar, guitar, 2, 3),
				picked("same track, the row gone", guitar, guitar, 2, 2), picked("same track, none picked", guitar, guitar, null, 3), picked("no track", guitar, Key.NONE, 1, 0)));
	}

	// what the editor shows for the caret's track, read from the app's song: none before a song is shown
	// the document's first track, its source "drone"
	private static TGTrack drone(SongActionFixture document) {
		TGTrack track = document.song.getTrack(0);
		TGChannel channel = document.songManager.getChannel(document.song, track.getChannelId());
		TGProgram chain = TGSoundChain.get(channel);
		chain.setReceiver(TGSoundChain.source("drone"));
		TGSoundChain.save(channel, chain, document.songManager.getFactory());
		return track;
	}

	@Test
	void theCaretsTrackIsShownWithItsNameAndRowsOrNoTrack() {
		SongActionFixture document = SongActionFixture.onDocument();
		TGTrack track = drone(document);
		Key shown = TGSoundChainFollow.key(document.context, track);
		Key none = TGSoundChainFollow.key(document.context, null);
		assertEquals(track.getName() + " " + track.getName() + " [Source: drone] | No track null [] true", shown.title("No track") + " " + shown.getTrack().getName() + " " + shown.rows(document.context).getRows("Source") + " | " + none.title("No track") + " " + none.getTrack() + " "
			+ none.rows(document.context).getRows("Source") + " " + (none.rows(document.context) == TGSoundChainRows.NONE));
	}

	// the default instruments' stamp is read with the key: a change of them is a change of what is shown
	@Test
	void theKeyHoldsTheDefaultInstrumentsStamp() {
		SongActionFixture document = SongActionFixture.onDocument();
		TGTrack track = drone(document);
		Key before = TGSoundChainFollow.key(document.context, track);
		TGDefaultInstrumentSource.install(document.context).use(TGDefaultInstruments.NONE);
		assertEquals("true", String.valueOf(TGSoundChainFollow.changed(before, TGSoundChainFollow.key(document.context, track))));
	}

	// with another MIDI Port than the TuxGuitar Synthesizer (here none is open) the chain is still edited, saved in the
	// song, but not heard: the editor says so in one quiet line, in the user's language; with it, nothing is said
	@Test
	void anEditorSaysSoundChainsAreHeardOnlyWithTheTuxGuitarSynthesizer() {
		Map<String, String> madeUp = Map.of("audio.sound-chain.synthesizer-only", "Klangketten nur mit dem Synthesizer");
		assertEquals("Sound chains are heard only with the TuxGuitar Synthesizer as MIDI Port | Klangketten nur mit dem Synthesizer | []",
			TuxGuitarMessages.english(() -> TGSoundChainFollow.note(this.fixture.context)) + " | " + TuxGuitarMessages.said(madeUp, () -> TGSoundChainFollow.note(false)) + " | ["
				+ TuxGuitarMessages.said(madeUp, () -> TGSoundChainFollow.note(true)) + "]");
	}
}
