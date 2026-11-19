package app.tuxguitar.midi.synth;

import static app.tuxguitar.midi.synth.FakeProcessors.program;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.tuxguitar.midi.synth.TGLevels.Key;
import app.tuxguitar.player.base.MidiControllers;
import app.tuxguitar.player.base.MidiPlayerException;
import app.tuxguitar.util.TGContext;

/**
 * What a synthesizer's level taps measure: the sound after each element of a channel's chain, at the channel's output
 * and at the master, while someone watches.
 */
public class TestLevels {

	private static final int CHANNEL = 3;
	// the 16-bit samples between processors lose up to a few steps of 1/32767 each
	private static final float QUANTUM = 0.001f;

	private final TGContext context = new TGContext();
	private final TGSynthModel model = new TGSynthModel(this.context);
	private final TGLevels levels = this.model.getLevels();
	private TGSynthChannel channel;
	private int programs;

	@BeforeEach
	public void setUp() throws MidiPlayerException {
		TGSynthManager manager = TGSynthManager.getInstance(this.context);
		manager.addExtension(TGMidiProcessorFactory.class, new FakeProcessors.SourceFactory("source", 0.25f));
		manager.addExtension(TGMidiProcessorFactory.class, new FakeProcessors.SourceFactory("other", 0.125f));
		manager.addExtension(TGMidiProcessorFactory.class, new FakeProcessors.SourceFactory("float", 0.25f, FakeProcessors.FloatSource::new));
		manager.addExtension(TGAudioProcessorFactory.class, new FakeProcessors.EffectFactory("twice", sample -> sample * 2));
		manager.addExtension(TGAudioProcessorFactory.class, new FakeProcessors.EffectFactory("lift", sample -> sample + 0.1));
		manager.addExtension(TGAudioProcessorFactory.class, new FakeProcessors.EffectFactory("eightfold", sample -> sample * 8, true));
		manager.addExtension(TGAudioProcessorFactory.class, new FakeProcessors.EffectFactory("eighth", sample -> sample / 8, true));
		this.channel = this.open(CHANNEL);
	}

	private TGSynthChannel open(int id) throws MidiPlayerException {
		TGSynthChannel channel = (TGSynthChannel) this.model.openChannel(id);
		channel.sendControlChange(MidiControllers.BALANCE, 0);
		return channel;
	}

	// the channel plays the program, as a program number not played before (the same number again changes nothing)
	private void play(TGSynthChannel channel, TGProgram program) throws MidiPlayerException {
		this.programs ++;
		this.model.getProgram(0, this.programs).copyFrom(program);
		channel.sendProgramChange(this.programs);
	}

	private void play(TGProgram program) throws MidiPlayerException {
		this.play(this.channel, program);
	}

	private void render() {
		this.channel.fillBuffer(new TGAudioBuffer());
	}

	// the loudest sample of each side the tap measured so far
	private static float[] peaks(TGLevelTap tap) {
		float[] peak = new float[TGLevelTap.SIDES];
		tap.read(0, peak);
		return peak;
	}

	private float[] peaks(Key key) {
		return peaks(this.levels.tap(key));
	}

	@Test
	public void eachElementsTapMeasuresTheSoundAfterIt() throws MidiPlayerException {
		this.play(program("source", "twice", "lift"));
		this.levels.watch();
		this.render();
		assertArrayEquals(new float[] {0.25f, 0.25f}, peaks(Key.source(CHANNEL)), QUANTUM);
		assertArrayEquals(new float[] {0.5f, 0.5f}, peaks(Key.effect(CHANNEL, "twice")), QUANTUM);
		assertArrayEquals(new float[] {0.6f, 0.6f}, peaks(Key.effect(CHANNEL, "lift")), QUANTUM);
	}

	// Balance 0 is all left
	@Test
	public void theOutputsTapMeasuresAfterVolumeAndBalance() throws MidiPlayerException {
		this.play(program("source", "twice"));
		this.channel.sendControlChange(MidiControllers.VOLUME, 32);
		this.levels.watch();
		this.render();
		assertArrayEquals(new float[] {0.5f * 32 / 127, 0f}, peaks(Key.out(CHANNEL)), QUANTUM);
	}

	// the output is written in 16 bits, so it is cut at full scale: the tap reads full scale, and that clips
	@Test
	public void theOutputsTapIsHeldAtFullScaleAndClips() throws MidiPlayerException {
		this.play(program("source", "eightfold"));
		this.levels.watch();
		this.render();
		assertArrayEquals(new float[] {2f, 2f}, peaks(Key.effect(CHANNEL, "eightfold")), QUANTUM);
		assertEquals(1f, peaks(Key.out(CHANNEL))[0], 0f);
		assertEquals(1, this.levels.tap(Key.out(CHANNEL)).getClips());
	}

	@Test
	public void aDisabledEffectIsNotMeasured() throws MidiPlayerException {
		TGProgram program = program("source", "twice", "lift");
		program.getOutput(0).setEnabled(false);
		this.play(program);
		this.levels.watch();
		this.render();
		assertEquals(0, this.levels.tap(Key.effect(CHANNEL, "twice")).read(0, new float[TGLevelTap.SIDES]));
		assertArrayEquals(new float[] {0.35f, 0.35f}, peaks(Key.effect(CHANNEL, "lift")), QUANTUM);
	}

	// the mix of two channels at full volume, all left: each source at 0.25
	@Test
	public void theMastersTapMeasuresTheMix() throws MidiPlayerException {
		this.play(program("source"));
		this.play(this.open(4), program("source"));
		this.levels.watch();
		new TGAudioBufferProcessor(this.model).process();
		assertArrayEquals(new float[] {0.5f, 0f}, peaks(this.levels.master()), QUANTUM);
	}

	@Test
	public void nothingIsMeasuredWhileNobodyWatches() throws MidiPlayerException {
		this.play(program("source", "twice"));
		this.levels.watch();
		this.levels.unwatch();
		new TGAudioBufferProcessor(this.model).process();
		assertFalse(this.levels.isOn());
		assertEquals(0, this.levels.tap(Key.source(CHANNEL)).read(0, new float[TGLevelTap.SIDES]));
		assertEquals(0, this.levels.tap(Key.effect(CHANNEL, "twice")).read(0, new float[TGLevelTap.SIDES]));
		assertEquals(0, this.levels.tap(Key.out(CHANNEL)).read(0, new float[TGLevelTap.SIDES]));
		assertEquals(0, this.levels.master().read(0, new float[TGLevelTap.SIDES]));
	}

	// watching is counted: the levels are on while anyone still watches
	@Test
	public void theLevelsAreOnWhileAnyoneWatches() {
		this.levels.watch();
		this.levels.watch();
		this.levels.unwatch();
		assertTrue(this.levels.isOn());
		this.levels.unwatch();
		this.levels.unwatch();
		this.levels.watch();
		assertTrue(this.levels.isOn());
	}

	// another source in the chain (the default instrument, the source kept in its place) is the same point
	@Test
	public void theSourceIsTappedByItsRole() throws MidiPlayerException {
		this.play(program("source", "twice"));
		TGLevelTap tap = this.levels.tap(Key.source(CHANNEL));
		this.play(program("other", "twice"));
		this.levels.watch();
		this.render();
		assertSame(tap, this.levels.tap(Key.source(CHANNEL)));
		assertArrayEquals(new float[] {0.125f, 0.125f}, peaks(tap), QUANTUM);
	}

	@Test
	public void aMovedEffectKeepsItsTap() throws MidiPlayerException {
		this.play(program("source", "twice", "lift"));
		TGLevelTap tap = this.levels.tap(Key.effect(CHANNEL, "twice"));
		TGLevelTap lift = this.levels.tap(Key.effect(CHANNEL, "lift"));
		this.play(program("source", "lift", "twice"));
		this.levels.watch();
		this.render();
		assertSame(tap, this.levels.tap(Key.effect(CHANNEL, "twice")));
		assertSame(lift, this.levels.tap(Key.effect(CHANNEL, "lift")));
		assertArrayEquals(new float[] {0.7f, 0.7f}, peaks(tap), QUANTUM);
	}

	@Test
	public void aRemovedEffectsTapGoesWhenItsChainOpensAgain() throws MidiPlayerException {
		this.play(program("source", "twice", "lift"));
		this.play(program("source", "lift"));
		assertNull(this.levels.tap(Key.effect(CHANNEL, "twice")));
		assertNotNull(this.levels.tap(Key.effect(CHANNEL, "lift")));
	}

	// another channel's effect of the same id is another point
	@Test
	public void eachChannelHasItsOwnTaps() throws MidiPlayerException {
		this.play(program("source", "twice"));
		this.play(this.open(4), program("source", "lift"));
		assertNotNull(this.levels.tap(Key.effect(CHANNEL, "twice")));
		assertNull(this.levels.tap(Key.effect(4, "twice")));
		assertNotEquals(Key.out(CHANNEL), Key.out(4));
		assertNotEquals(Key.source(CHANNEL), Key.out(CHANNEL));
		assertNotEquals(Key.effect(CHANNEL, "twice"), Key.effect(CHANNEL, "lift"));
		assertEquals(Key.effect(CHANNEL, "twice"), Key.effect(CHANNEL, "twice"));
		assertNotEquals(Key.source(CHANNEL), null);
		assertNotEquals(Key.source(CHANNEL), "source");
	}

	// a chain that reopens (Undo, a program change) keeps its clip counts
	@Test
	public void tapsOutliveTheChannelsClosing() throws MidiPlayerException {
		this.play(program("source", "eightfold"));
		this.levels.watch();
		this.render();
		this.model.closeChannels();
		assertEquals(1, this.levels.tap(Key.effect(CHANNEL, "eightfold")).getClips());
	}

	@Test
	public void aChannelNeverOpenedHasNoTaps() {
		assertNull(this.levels.tap(Key.source(9)));
		assertNull(this.levels.tap(Key.out(9)));
		assertNotNull(this.levels.master());
	}

	// the audio thread must not allocate per block: 1000 metered blocks of a float chain allocate nothing
	@Test
	public void aMeteredBlockAllocatesNothing() throws MidiPlayerException {
		this.play(program("float", "eightfold", "eighth"));
		TGAudioBufferProcessor blocks = new TGAudioBufferProcessor(this.model);
		this.levels.watch();
		blocks.process();
		com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
		long before = threads.getThreadAllocatedBytes(Thread.currentThread().getId());
		for (int b = 0; b < 1000; b++) {
			blocks.process();
		}
		long allocated = threads.getThreadAllocatedBytes(Thread.currentThread().getId()) - before;
		assertTrue(allocated < 1024, allocated + " bytes allocated by 1000 blocks");
		assertEquals(1001, this.levels.master().read(0, new float[TGLevelTap.SIDES]));
	}
}
