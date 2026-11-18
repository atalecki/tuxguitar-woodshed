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
	
	// the type of each kind's default; the other kinds have none
	private static final Map<TGInstrumentKind, String> DEFAULTS = new EnumMap<TGInstrumentKind, String>(TGInstrumentKind.class);

	static {
		DEFAULTS.put(TGInstrumentKind.GUITAR, "guitar");
		DEFAULTS.put(TGInstrumentKind.DRUMS, "drums");
	}

	private final TGContext context = new TGContext();
	private final TGSynthModel model = new TGSynthModel(this.context);
	private final FakeProcessors.SourceFactory gervill = new FakeProcessors.SourceFactory(GervillProcessorFactory.TYPE, GERVILL);
	private final FakeProcessors.SourceFactory table = new FakeProcessors.SourceFactory("table", TABLE);
	private final FakeProcessors.SourceFactory guitar = new FakeProcessors.SourceFactory("guitar", GUITAR);
	private final FakeProcessors.SourceFactory drums = new FakeProcessors.SourceFactory("drums", DRUMS);
	private final FakeProcessors.SourceFactory own = new FakeProcessors.SourceFactory("own", OWN);
	private TGSynthChannel channel;

	@BeforeEach
	public void setUp() throws MidiPlayerException {
		TGSynthManager manager = TGSynthManager.getInstance(this.context);
		for (FakeProcessors.SourceFactory source : new FakeProcessors.SourceFactory[] {this.gervill, this.table, this.guitar, this.drums, this.own}) {
			manager.addExtension(TGMidiProcessorFactory.class, source);
		}
		manager.addExtension(TGAudioProcessorFactory.class, new FakeProcessors.EffectFactory("lift", sample -> sample + 0.1));
		manager.addExtension(TGDefaultSource.class, TestDefaultSource::byKind);
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

	// the guitars' default and the drums', each an element of its own id
	private static TGProgramElement byKind(int bank, int program) {
		String type = DEFAULTS.get(TGInstrumentKind.of(bank, program));
		return (type != null ? element("default-" + type, type) : null);
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
}
