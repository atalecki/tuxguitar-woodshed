package app.tuxguitar.vst3;

/**
 * Hears a plugin's edits from its own editor as gestures: a gesture runs from the first begin edit to the end edit that
 * closes it (a preset chosen begins the preset and every parameter it sets, then ends them all), and when it ends the
 * plugin holds what the gesture did. A change outside any gesture (the plugin dirty: a capture loaded), or its editor
 * hidden (it may have changed the plugin without saying so), ends a change too; during a gesture, the gesture's end
 * covers it.
 */
public final class TGVst3Gestures implements TGVst3Listener {

	private final Runnable ended;
	// the edits begun and not ended yet
	private int open;

	// ended: run when a gesture, or a change outside one, ends
	public TGVst3Gestures(Runnable ended) {
		this.ended = ended;
	}

	public void notice(TGVst3Notice notice) {
		TGVst3Notice.Kind kind = notice.getKind();
		if (kind == TGVst3Notice.Kind.BEGIN_EDIT) {
			this.open++;
		} else if (kind == TGVst3Notice.Kind.END_EDIT) {
			this.end();
		} else if (this.open == 0 && outsideEdits(kind)) {
			this.ended.run();
		}
	}

	private void end() {
		if (--this.open == 0) {
			this.ended.run();
		}
	}

	private static boolean outsideEdits(TGVst3Notice.Kind kind) {
		return (kind == TGVst3Notice.Kind.DIRTY || kind == TGVst3Notice.Kind.EDITOR_HIDDEN);
	}
}
