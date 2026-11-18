package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Where a plugin's editor window of its own was last left, as an app keeps it: a frame as text.
 */
public class TestEditorFrames {

	@Test
	public void aWindowsFrameIsKeptAsTextAndReadBack() {
		String kept = TGEditorFrames.kept("10,-20 330,240");
		assertEquals("10,-20,330,240 [10, -20, 330, 240]", kept + " " + Arrays.toString(TGEditorFrames.frame(kept)));
	}

	// nothing kept, or what is no frame, is none: the window opens where it opens by itself
	@Test
	public void whatIsNoFrameIsNone() {
		assertEquals("null null null", TGEditorFrames.frame(null) + " " + TGEditorFrames.frame("1,2,3") + " " + TGEditorFrames.frame("1,2,x,4"));
	}

	@Test
	public void inMemoryTheFrameOfEachPluginIsKeptUntilAnother() {
		TGEditorFrames frames = TGEditorFrames.inMemory();
		frames.put("GAIN", "1,2,3,4");
		frames.put("CLIP", "5,6,7,8");
		frames.put("GAIN", "9,10,11,12");
		assertEquals("9,10,11,12 5,6,7,8 null", frames.get("GAIN") + " " + frames.get("CLIP") + " " + frames.get("ECHO"));
	}
}
