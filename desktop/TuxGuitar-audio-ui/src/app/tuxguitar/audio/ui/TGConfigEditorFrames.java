package app.tuxguitar.audio.ui;

import app.tuxguitar.audio.TGEditorFrames;
import app.tuxguitar.util.configuration.TGConfigManager;

/**
 * Where each plugin's editor window of its own was last left, kept in TuxGuitar audio's config at once, by the plugin's
 * class id. Glue only.
 */
final class TGConfigEditorFrames implements TGEditorFrames {

	private static final String KEY = "editor-frame.";

	private final TGConfigManager config;

	TGConfigEditorFrames(TGConfigManager config) {
		this.config = config;
	}

	public String get(String classId) {
		return this.config.getStringValue(KEY + classId, null);
	}

	public void put(String classId, String frame) {
		this.config.setValue(KEY + classId, frame);
		this.config.save();
	}
}
