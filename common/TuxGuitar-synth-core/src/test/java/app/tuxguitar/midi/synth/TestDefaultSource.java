package app.tuxguitar.midi.synth;

import static app.tuxguitar.midi.synth.FakeProcessors.element;
import static app.tuxguitar.midi.synth.FakeProcessors.program;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.tuxguitar.midi.synth.impl.GervillProcessorFactory;
import app.tuxguitar.player.base.MidiControllers;
import app.tuxguitar.player.base.MidiParameters;
import app.tuxguitar.player.base.MidiPlayerException;
import app.tuxguitar.util.TGContext;

/**
 * A channel without an instrument of its own plays the default instrument of its kind (TGDefaultSource, which an app
 * installs on the synthesizer's manager): with no sound chain, in place of the program of its Bank and Program; with a
 * chain whose source is General MIDI, as that source, the effects kept. A chain with an instrument of its own plays it.
 * Without a default for its kind, the channel plays General MIDI as before. Here the guitars' default is the source
 * "guitar", the drums' "drums", the other kinds have none; General MIDI is the source "gervill" (each a fake source of
 * its own level).
 */
public class TestDefaultSource {

	private static final float GERVILL = 0.5f;
	private static final float TABLE = 0.25f;
	private static final float GUITAR = 0.125f;
	private static final float DRUMS = 0.375f;
	private static final float OWN = 0.03125f;
	private static final float BROKEN = 0.75f;

	private final TGContext context = new TGContext();
	private final TGSynthModel model = new TGSynthModel(this.context);
	private final FakeProcessors.SourceFactory gervill = new FakeProcessors.SourceFactory(GervillProcessorFactory.TYPE, GERVILL);
	private final FakeProcessors.SourceFactory table = new FakeProcessors.SourceFactory("table", TABLE);
	private final FakeProcessors.SourceFactory guitar = new FakeProcessors.SourceFactory("guitar", GUITAR);
	private final FakeProcessors.SourceFactory drums = new FakeProcessors.SourceFactory("drums", DRUMS);
	private final FakeProcessors.SourceFactory own = new FakeProcessors.SourceFactory("own", OWN);
	// a source that closes once it is given its parameters, as a VST3 plugin that cannot be opened
	private final FakeProcessors.SourceFactory broken = new FakeProcessors.SourceFactory("broken", BROKEN, Broken::new);
	private final Defaults defaults = new Defaults();
	private TGSynthChannel channel;

	@BeforeEach
	public void setUp() throws MidiPlayerException {
		TGSynthManager manager = TGSynthManager.getInstance(this.context);
		for (FakeProcessors.SourceFactory source : new FakeProcessors.SourceFactory[] {this.gervill, this.table, this.guitar, this.drums, this.own, this.broken}) {
			manager.addExtension(TGMidiProcessorFactory.class, source);
		}
		manager.addExtension(TGAudioProcessorFactory.class, new FakeProcessors.EffectFactory("lift", sample -> sample + 0.1));
		manager.addExtension(TGDefaultSource.class, this.defaults);
		this.fillTable();
		this.channel = (TGSynthChannel) this.model.openChannel(3);
		this.channel.sendControlChange(MidiControllers.BALANCE, 0);
	}

	// the synthesizer's program of every program of bank 0: the source "table"
	private void fillTable() {
		for (int program = 0; program < TGSynthModel.PROGRAMS_LENGTH; program++) {
			this.model.getProgram(0, program).copyFrom(program("table"));
		}
	}

	// the guitars' default and the drums' (each an element of its own id), unless a test sets others; it notes each that
	// did not open
	private static final class Defaults implements TGDefaultSource {
		private final Map<TGInstrumentKind, String> types = new EnumMap<TGInstrumentKind, String>(TGInstrumentKind.class);
		private final List<String> notOpened = new ArrayList<String>();

		Defaults() {
			this.types.put(TGInstrumentKind.GUITAR, "guitar");
			this.types.put(TGInstrumentKind.DRUMS, "drums");
		}

		public TGProgramElement sourceFor(int bank, int program) {
			String type = this.types.get(TGInstrumentKind.of(bank, program));
			return (type != null ? element("default-" + type, type) : null);
		}

		public void notOpened(TGProgramElement source, TGMidiProcessor processor) {
			this.notOpened.add(source.getId() + (processor != null ? " open " + processor.isOpen() : " none"));
		}
	}

	private static final class Broken extends FakeProcessors.TimedSource {

		Broken(float level) {
			super(level);
		}

		public void restoreParameters(Map<String, String> parameters) {
			super.restoreParameters(parameters);
			this.close();
		}
	}

	// the first sample of the chain, before Volume and Balance, to a thousandth (the 16-bit samples between processors
	// lose a few steps of 1/32767)
	private String sounds() {
		float[][] block = new float[TGAudioBuffer.CHANNELS][FakeProcessors.FRAMES];
		this.channel.fillChain(block);
		return level(block[0][0]);
	}

	private static String level(float level) {
		return String.format(Locale.ROOT, "%.3f", level);
	}

	// the parameters, as the player sends them: the sound chain's source and effects (none: no chain)
	private void sendChain(String source, String... effects) throws MidiPlayerException {
		this.channel.sendParameter(MidiParameters.SENDING_PARAMS, Boolean.TRUE.toString());
		if (source != null) {
			send(TGSynthChannel.SOUND_CHAIN_PREFIX, "source", source);
			sendEffects(effects);
		}
		this.channel.sendParameter(MidiParameters.SENDING_PARAMS, Boolean.FALSE.toString());
	}

	private void sendEffects(String... effects) throws MidiPlayerException {
		for (int i = 0; i < effects.length; i++) {
			send(TGSynthChannel.SOUND_CHAIN_PREFIX + ".output." + i, effects[i], effects[i]);
		}
		this.channel.sendParameter(TGSynthChannel.SOUND_CHAIN_PREFIX + ".output.count", Integer.toString(effects.length));
	}

	private void send(String prefix, String id, String type) throws MidiPlayerException {
		this.channel.sendParameter(prefix + ".id", id);
		this.channel.sendParameter(prefix + ".type", type);
	}

	// a chain whose source is General MIDI marked as the source's parameter says ("true": plain General MIDI, a track's
	// sound set to MIDI), with its effects
	private void sendMarkedChain(String mark, String... effects) throws MidiPlayerException {
		this.channel.sendParameter(MidiParameters.SENDING_PARAMS, Boolean.TRUE.toString());
		send(TGSynthChannel.SOUND_CHAIN_PREFIX, "midi", GervillProcessorFactory.TYPE);
		this.channel.sendParameter(TGSynthChannel.SOUND_CHAIN_PREFIX + ".param.count", "1");
		this.channel.sendParameter(TGSynthChannel.SOUND_CHAIN_PREFIX + ".param.0.name", TGDefaultSource.PLAIN_GENERAL_MIDI);
		this.channel.sendParameter(TGSynthChannel.SOUND_CHAIN_PREFIX + ".param.0.value", mark);
		sendEffects(effects);
		this.channel.sendParameter(MidiParameters.SENDING_PARAMS, Boolean.FALSE.toString());
	}

	// a track's sound set to MIDI: its program plays, never the default of its kind (none is opened), its effects kept
	@Test
	public void aChainWhoseSourceIsPlainGeneralMidiPlaysItsProgramNotTheDefault() throws MidiPlayerException {
		sendMarkedChain(Boolean.TRUE.toString(), "lift");
		sendBankAndProgram(0, 25);

		assertEquals(level(GERVILL + 0.1f) + " 0", sounds() + " " + this.guitar.created.size());
	}

	// a mark that does not say "true" marks nothing: the default plays
	@Test
	public void aGeneralMidiSourceMarkedOtherThanTrueTakesTheDefault() throws MidiPlayerException {
		sendMarkedChain(Boolean.FALSE.toString());
		sendBankAndProgram(0, 25);

		assertEquals(level(GUITAR), sounds());
	}

	// a source of the same id ("source") but of another type in place of the one playing, on a kind without a default:
	// the processor that played closes and one of the new type opens and plays, either way (before, the processor of the
	// old type was kept and given the new source's parameters: silence with a real plugin). Read as "<the old processor
	// open> <processors of the new type made> <what sounds>"
	@Test
	public void aSourceOfTheSameIdAndAnotherTypeClosesTheOldProcessorAndPlays() throws MidiPlayerException {
		sendChain("own");
		sendBankAndProgram(0, 5);
		String toGeneralMidi = this.switched(GervillProcessorFactory.TYPE, this.gervill);
		String toOwn = this.switched("own", this.own);

		assertEquals("false 1 " + level(GERVILL) + " | false 1 " + level(OWN), toGeneralMidi + " | " + toOwn);
	}

	// the source playing switched to the type given, its factory given
	private String switched(String then, FakeProcessors.SourceFactory made) throws MidiPlayerException {
		TGMidiProcessor played = this.channel.getProcessor().getProcessor();
		int before = made.created.size();
		sendChain(then);
		return played.isOpen() + " " + (made.created.size() - before) + " " + sounds();
	}

	private void sendBankAndProgram(int bank, int program) throws MidiPlayerException {
		this.channel.sendControlChange(MidiControllers.BANK_SELECT, bank);
		this.channel.sendProgramChange(program);
	}

	@Test
	public void aChannelWithoutAChainPlaysTheDefaultOfItsKindElseItsProgram() throws MidiPlayerException {
		sendChain(null);
		sendBankAndProgram(0, 25);
		String guitar = sounds();
		sendBankAndProgram(0, 5);
		String other = sounds();
		sendBankAndProgram(128, 0);

		assertEquals(level(GUITAR) + " " + level(TABLE) + " " + level(DRUMS), guitar + " " + other + " " + sounds());
	}

	@Test
	public void aChainWhoseSourceIsGeneralMidiPlaysTheDefaultOfItsKindWithItsEffects() throws MidiPlayerException {
		sendChain(GervillProcessorFactory.TYPE, "lift");
		sendBankAndProgram(0, 25);
		String guitar = sounds();
		sendBankAndProgram(0, 5);

		assertEquals(level(GUITAR + 0.1f) + " " + level(GERVILL + 0.1f), guitar + " " + sounds());
	}

	@Test
	public void aChainWithAnInstrumentOfItsOwnPlaysItWhateverItsKind() throws MidiPlayerException {
		sendChain("own");
		sendBankAndProgram(0, 25);

		assertEquals(level(OWN) + " 0", sounds() + " " + this.guitar.created.size());
	}

	// a channel's kind is known once it has a program: its General MIDI source plays until then, and the default of no
	// other kind is opened on the way
	@Test
	public void theDefaultOpensOnceTheChannelHasAProgram() throws MidiPlayerException {
		sendChain(GervillProcessorFactory.TYPE);
		String before = sounds();
		sendBankAndProgram(0, 25);

		assertEquals(level(GERVILL) + " " + level(GUITAR) + " 1", before + " " + sounds() + " " + this.guitar.created.size());
	}

	// a program of the same kind keeps the default as it runs (nothing hears its chain change), its source told the
	// program as a General MIDI source is
	@Test
	public void anotherProgramOfTheSameKindKeepsTheDefault() throws MidiPlayerException {
		sendChain(GervillProcessorFactory.TYPE);
		sendBankAndProgram(0, 25);
		List<Integer> changes = new ArrayList<Integer>();
		this.model.addChannelListener(changed -> changes.add(changed.getId()));
		sendBankAndProgram(0, 27);

		assertEquals("1 [program 0:25, program 0:27] []", this.guitar.created.size() + " " + this.guitar.last().received + " " + changes);
	}

	// the percussion bank is drums whatever the program, so a bank select alone changes the kind of a chain
	@Test
	public void aBankSelectToThePercussionBankPlaysTheDrumsDefault() throws MidiPlayerException {
		sendChain(GervillProcessorFactory.TYPE);
		sendBankAndProgram(0, 0);
		String other = sounds();
		this.channel.sendControlChange(MidiControllers.BANK_SELECT, 128);

		assertEquals(level(GERVILL) + " " + level(DRUMS), other + " " + sounds());
	}

	// the parameters sent again (each Play) find the kind the channel has
	@Test
	public void theParametersSentAgainPlayTheDefaultOfTheChannelsKind() throws MidiPlayerException {
		sendChain(null);
		sendBankAndProgram(0, 25);
		sendChain(GervillProcessorFactory.TYPE, "lift");

		assertEquals(level(GUITAR + 0.1f), sounds());
	}

	// a default that does not open (a plugin that cannot be opened) is told its source, and General MIDI plays: without a
	// chain the program of its bank and program, with one its General MIDI source, the effects kept
	@Test
	public void aDefaultThatDoesNotOpenPlaysGeneralMidiItsSourceTold() throws MidiPlayerException {
		this.defaults.types.put(TGInstrumentKind.GUITAR, "broken");
		sendChain(null);
		sendBankAndProgram(0, 25);
		String without = sounds();
		sendChain(GervillProcessorFactory.TYPE, "lift");

		assertEquals(level(TABLE) + " " + level(GERVILL + 0.1f) + " [default-broken open false, default-broken open false]", without + " " + sounds() + " " + this.defaults.notOpened);
	}

	// no processor of its type: the plugin's host is not there
	@Test
	public void aDefaultOfATypeNoFactoryMakesPlaysGeneralMidi() throws MidiPlayerException {
		this.defaults.types.put(TGInstrumentKind.GUITAR, "missing");
		sendChain(GervillProcessorFactory.TYPE);
		sendBankAndProgram(0, 25);

		assertEquals(level(GERVILL) + " [default-missing none]", sounds() + " " + this.defaults.notOpened);
	}

	// an instrument of the track's own that does not open stays silent, as before: only a default falls back
	@Test
	public void anInstrumentOfItsOwnThatDoesNotOpenIsSilent() throws MidiPlayerException {
		sendChain("broken");
		sendBankAndProgram(0, 25);

		assertEquals(level(0f) + " []", sounds() + " " + this.defaults.notOpened);
	}

	// the default kept in the song as the channel's own instrument (a chain whose source is of the default's type, the
	// state it had) goes on playing on the processor the default played on, given the chain's source (a plugin's editor
	// open on it stays open)
	@Test
	public void aDefaultKeptAsTheChannelsOwnInstrumentGoesOnPlayingOnItsProcessor() throws MidiPlayerException {
		FakeProcessors.Source played = this.playTheGuitarsDefault();
		sendChain("guitar");

		assertEquals("1 true true source true " + level(GUITAR), this.guitar.created.size() + " " + (this.guitar.last() == played) + " " + played.isOpen() + " " + this.playing(played));
	}

	// the keep undone (General MIDI again): the default goes on playing on the processor once more, given the default's
	// element (an editor open on it stays open, the plugin as the default has it); kept again (a redo), it goes on too
	@Test
	public void aKeptDefaultGivenGeneralMidiAgainGoesOnPlayingOnItsProcessorAsTheDefault() throws MidiPlayerException {
		FakeProcessors.Source played = this.playTheGuitarsDefault();
		sendChain("guitar");
		sendChain(GervillProcessorFactory.TYPE);
		String undone = this.guitar.created.size() + " " + this.playing(played);
		sendChain("guitar");

		assertEquals("1 default-guitar true " + level(GUITAR) + " | 1 source true " + level(GUITAR), undone + " | " + this.guitar.created.size() + " " + this.playing(played));
	}

	// any instrument of the track's own of the default's type goes on as the default the same way: General MIDI chosen,
	// its processor is given the default's element
	@Test
	public void anInstrumentOfItsOwnOfTheDefaultsTypeGivenGeneralMidiGoesOnPlayingOnItsProcessorAsTheDefault() throws MidiPlayerException {
		sendChain("guitar");
		sendBankAndProgram(0, 25);
		FakeProcessors.Source own = this.guitar.last();
		sendChain(GervillProcessorFactory.TYPE);

		assertEquals("1 default-guitar true " + level(GUITAR), this.guitar.created.size() + " " + this.playing(own));
	}

	// the guitar's default played as the source of a chain whose source is General MIDI: its processor
	private FakeProcessors.Source playTheGuitarsDefault() throws MidiPlayerException {
		sendChain(GervillProcessorFactory.TYPE);
		sendBankAndProgram(0, 25);
		return this.guitar.last();
	}

	// what the channel plays the processor as: the id of the program's source, whether the processor is the one mapped to
	// it, what the chain sounds
	private String playing(FakeProcessors.Source processor) {
		TGProgramElement source = this.channel.getProgram().getReceiver();
		return source.getId() + " " + (this.channel.getProcessor().getAudioProcessor(source) == processor) + " " + sounds();
	}

	// a default closed meanwhile (a bank select without a chain) is no longer played: the track's own instrument opens
	@Test
	public void aDefaultClosedMeanwhileIsNotHandedOver() throws MidiPlayerException {
		sendChain(null);
		sendBankAndProgram(0, 25);
		this.channel.sendControlChange(MidiControllers.BANK_SELECT, 1);
		sendChain("guitar");

		assertEquals("2 " + level(GUITAR), this.guitar.created.size() + " " + sounds());
	}

	// only a default that plays is handed over: General MIDI played in place of one that did not open is not
	@Test
	public void theProgramPlayedInPlaceOfADefaultThatDidNotOpenIsNotHandedOver() throws MidiPlayerException {
		this.defaults.types.put(TGInstrumentKind.GUITAR, "broken");
		sendChain(null);
		sendBankAndProgram(0, 25);
		FakeProcessors.Source fellBackTo = this.table.last();
		sendChain("table");

		assertEquals("2 false", this.table.created.size() + " " + fellBackTo.isOpen());
	}

	// an instrument of the track's own of another type opens as before
	@Test
	public void anInstrumentOfItsOwnOfAnotherTypeThanTheDefaultOpensItsOwnProcessor() throws MidiPlayerException {
		sendChain(GervillProcessorFactory.TYPE);
		sendBankAndProgram(0, 25);
		FakeProcessors.Source played = this.guitar.last();
		sendChain("own");

		assertEquals("1 false " + level(OWN), this.own.created.size() + " " + played.isOpen() + " " + sounds());
	}
}
