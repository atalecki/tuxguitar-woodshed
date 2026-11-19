package app.tuxguitar.midi.synth;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;

import org.junit.jupiter.api.Test;

/**
 * A level tap: what a meter reads from one point of the sound, a block at a time.
 */
public class TestLevelTap {

	private static final int FRAMES = FakeProcessors.FRAMES;

	private final TGLevelTap tap = new TGLevelTap();

	// a block whose sides hold one sample each at the levels (silence elsewhere); one level makes a mono block
	private static float[][] block(float... levels) {
		float[][] block = new float[levels.length][FRAMES];
		for (int side = 0; side < levels.length; side++) {
			block[side][FRAMES / 2] = levels[side];
		}
		return block;
	}

	// the peaks read from the cursor
	private float[] read(long from) {
		float[] peak = new float[TGLevelTap.SIDES];
		this.tap.read(from, peak);
		return peak;
	}

	@Test
	public void aBlockGivesTheLoudestSampleOfEachSide() {
		this.tap.measure(block(0.5f, -0.25f));
		assertArrayEquals(new float[] {0.5f, 0.25f}, read(0));
	}

	@Test
	public void theLoudestOfSeveralBlocksIsRead() {
		this.tap.measure(block(0.5f, 0.125f));
		this.tap.measure(block(0.25f, 0.75f));
		assertArrayEquals(new float[] {0.5f, 0.75f}, read(0));
	}

	@Test
	public void aMonoBlockCountsForBothSides() {
		this.tap.measure(block(-0.5f));
		assertArrayEquals(new float[] {0.5f, 0.5f}, read(0));
	}

	// a track's output is saturated at full scale; inside the chain the sound keeps what goes above it
	@Test
	public void aCeilingHoldsThePeakWhereTheSoundIsSaturatedWithoutOneItGoesAboveFullScale() {
		TGLevelTap inside = new TGLevelTap();
		inside.measure(block(1.5f, 0.5f));
		this.tap.measure(block(1.5f, 0.5f), 1f);
		assertArrayEquals(new float[] {1f, 0.5f}, read(0));
		float[] peak = new float[TGLevelTap.SIDES];
		inside.read(0, peak);
		assertArrayEquals(new float[] {1.5f, 0.5f}, peak);
	}

	@Test
	public void aBlockAtMinusPointOneDecibelsClipsAndOneJustUnderDoesNot() {
		this.tap.measure(block(TGLevelTap.CLIPPING, 0f));
		this.tap.measure(block(0f, Math.nextDown(TGLevelTap.CLIPPING)));
		this.tap.measure(block(0f, -TGLevelTap.CLIPPING));
		assertEquals(2, this.tap.getClips());
	}

	@Test
	public void aBlockClippingOnBothSidesCountsOnce() {
		this.tap.measure(block(1f, 1f));
		assertEquals(1, this.tap.getClips());
	}

	@Test
	public void aCeilingAtFullScaleStillClips() {
		this.tap.measure(block(2f, 0f), 1f);
		assertEquals(1, this.tap.getClips());
	}

	@Test
	public void theClippingLevelIsMinusPointOneDecibels() {
		assertEquals(-0.1, 20 * Math.log10(TGLevelTap.CLIPPING), 1e-6);
	}

	@Test
	public void aReaderResumesFromItsCursor() {
		this.tap.measure(block(0.5f, 0.5f));
		float[] peak = new float[TGLevelTap.SIDES];
		long cursor = this.tap.read(0, peak);
		assertEquals(1, cursor);
		this.tap.measure(block(0.25f, 0.125f));
		assertArrayEquals(new float[] {0.25f, 0.125f}, read(cursor));
	}

	@Test
	public void nothingNewSinceTheCursorReadsSilence() {
		this.tap.measure(block(0.5f, 0.5f));
		long cursor = this.tap.read(0, new float[TGLevelTap.SIDES]);
		float[] peak = {0.75f, 0.75f};
		assertEquals(cursor, this.tap.read(cursor, peak));
		assertArrayEquals(new float[] {0f, 0f}, peak);
	}

	// the ring wraps: a reader that lags takes the newest blocks only, those not about to be overwritten. A loud block,
	// then quieter ones: what a reader from the start reads
	private float[] afterALoudBlockAndQuieterOnes(int quieter) {
		this.tap.measure(block(0.75f, 0.75f));
		for (int b = 0; b < quieter; b++) {
			this.tap.measure(block(0.25f, 0.125f));
		}
		return read(0);
	}

	@Test
	public void aLaggingReaderGetsTheNewestBlocksOnly() {
		int newest = TGLevelTap.BLOCKS - TGLevelTap.MARGIN;
		assertArrayEquals(new float[] {0.25f, 0.125f}, afterALoudBlockAndQuieterOnes(newest));
	}

	@Test
	public void aReaderJustInsideTheNewestBlocksGetsTheOldestOfThem() {
		int newest = TGLevelTap.BLOCKS - TGLevelTap.MARGIN;
		assertArrayEquals(new float[] {0.75f, 0.75f}, afterALoudBlockAndQuieterOnes(newest - 1));
	}

	@Test
	public void theRingWrapsAroundManyTimes() {
		for (int b = 0; b < 5 * TGLevelTap.BLOCKS; b++) {
			this.tap.measure(block(0.25f, 0.25f));
		}
		long cursor = this.tap.read(0, new float[TGLevelTap.SIDES]);
		this.tap.measure(block(0.5f, 0.125f));
		assertEquals(5 * TGLevelTap.BLOCKS, cursor);
		assertArrayEquals(new float[] {0.5f, 0.125f}, read(cursor));
	}

	// the audio thread must not allocate per block
	@Test
	public void measuringAllocatesNothing() {
		com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
		float[][] block = block(0.5f, 1f);
		this.tap.measure(block);
		long before = threads.getThreadAllocatedBytes(Thread.currentThread().getId());
		for (int b = 0; b < 1000; b++) {
			this.tap.measure(block);
		}
		long allocated = threads.getThreadAllocatedBytes(Thread.currentThread().getId()) - before;
		assertTrue(allocated < 1024, allocated + " bytes allocated by 1000 blocks");
		assertEquals(1001, this.tap.getClips());
	}
}
