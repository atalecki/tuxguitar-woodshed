package app.tuxguitar.audio;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where each plugin's editor window of its own was last left, by the plugin's class id, as an app keeps it (the desktop:
 * in TuxGuitar's config), so it opens there again (moved onto a visible screen by the host): its frame on the screen as
 * text, "left,top,right,bottom".
 */
public interface TGEditorFrames {

	// the frame kept for the plugin, or null
	String get(String classId);

	void put(String classId, String frame);

	// kept in memory only (until an app gives TGLivePlugins its own)
	static TGEditorFrames inMemory() {
		Map<String, String> kept = new ConcurrentHashMap<String, String>();
		return new TGEditorFrames() {
			public String get(String classId) {
				return kept.get(classId);
			}

			public void put(String classId, String frame) {
				kept.put(classId, frame);
			}
		};
	}

	// the frame of a window as the host says it ("left,top right,bottom") as it is kept
	static String kept(String shown) {
		return shown.replace(' ', ',');
	}

	// the frame kept, as the host takes it (left, top, right, bottom); null for none, or for what is no frame
	static int[] frame(String kept) {
		try {
			int[] frame = Arrays.stream(String.valueOf(kept).split(",")).mapToInt(Integer::parseInt).toArray();
			return (frame.length == 4 ? frame : null);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
