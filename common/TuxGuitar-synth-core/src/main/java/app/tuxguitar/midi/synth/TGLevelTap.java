package app.tuxguitar.midi.synth;

/**
 * One point the sound passes through (after an element of a sound chain, a track's output, the master), measured a block
 * at a time. The audio thread is its only writer; any thread reads it without a lock. It keeps a ring of the last blocks'
 * peaks, per side, and counts the blocks measured and those that clipped (reached -0.1 dBFS). A reader keeps its own
 * cursor and the clip count it has seen: nothing here is ever reset, so there is no second writer.
 */
public final class TGLevelTap {

	// -0.1 dBFS, as a sample: at or above it the sound clips
	public static final float CLIPPING = (float) Math.pow(10, -0.1 / 20);
	// no ceiling: a point inside the chain keeps what goes above full scale
	public static final float UNLIMITED = Float.MAX_VALUE;

	// the blocks kept (about 0.74 s of 512 frames)
	static final int BLOCKS = 64;
	// how far behind the block being written a reader stays, so the oldest block it reads is not being overwritten
	static final int MARGIN = 8;
	// the newest blocks a reader reads at most
	public static final int NEWEST = BLOCKS - MARGIN;
	// left and right: what read() fills
	public static final int SIDES = 2;

	private final float[][] peaks = new float[SIDES][BLOCKS];
	// written last by measure (a volatile store), so the peaks before it are visible to a reader that reads it first
	private volatile long written;
	// single writer: read, incremented, stored
	private volatile long clips;

	// a block of samples, one array per side (a single array counts for both), measured with no ceiling
	public void measure(float[][] block) {
		this.measure(block, UNLIMITED);
	}

	// a block of samples, each side's peak held at the ceiling (where the sound is saturated after this point)
	public void measure(float[][] block, float ceiling) {
		long next = this.written;
		int slot = (int) (next % BLOCKS);
		boolean clipped = false;
		for (int side = 0; side < SIDES; side++) {
			float peak = Math.min(peak(block[side % block.length]), ceiling);
			this.peaks[side][slot] = peak;
			clipped |= (peak >= CLIPPING);
		}
		if (clipped) {
			this.clips = this.clips + 1;
		}
		this.written = next + 1;
	}

	private static float peak(float[] samples) {
		float peak = 0f;
		for (float sample : samples) {
			peak = Math.max(peak, Math.abs(sample));
		}
		return peak;
	}

	// the loudest sample of each side over the blocks measured since the cursor (the newest BLOCKS - MARGIN at most), into
	// the peak (one value per side); the cursor to read from next
	public long read(long from, float[] peak) {
		long to = this.written;
		java.util.Arrays.fill(peak, 0f);
		for (long block = Math.max(from, to - NEWEST); block < to; block++) {
			int slot = (int) (block % BLOCKS);
			for (int side = 0; side < SIDES; side++) {
				peak[side] = Math.max(peak[side], this.peaks[side][slot]);
			}
		}
		return to;
	}

	// how many blocks clipped, since the tap was made
	public long getClips() {
		return this.clips;
	}
}
