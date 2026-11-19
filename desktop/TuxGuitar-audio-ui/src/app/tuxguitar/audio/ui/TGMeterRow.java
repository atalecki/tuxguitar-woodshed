package app.tuxguitar.audio.ui;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.audio.TGAudioSession;
import app.tuxguitar.audio.TGAudioStatus;
import app.tuxguitar.audio.TGInputLevel;
import app.tuxguitar.audio.TGTapMeter;
import app.tuxguitar.app.ui.TGApplication;
import app.tuxguitar.audio.action.TGResetClipIndicatorAction;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.TGActionProcessor;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.util.TGContext;

/**
 * An armed track's row of the Recording window: its name, its input meter (the Sound Chain panel's meter: a bar that
 * falls back, a hold line, a lamp lit while the input has clipped; a click on it resets the clip indicator), the level,
 * "CLIP" once it has clipped, and a button that resets the clip indicator.
 */
final class TGMeterRow {

	private final UILabel name;
	// how wide and high the meter is
	private static final float WIDTH = 200f;
	private static final float HEIGHT = 2 * TGMeterCanvas.BAR + 1f;

	private final TGMeterCanvas bar;
	private final TGTapMeter reader;
	private final UILabel level;
	private final UILabel clip;

	TGMeterRow(TGContext context, UIFactory factory, UIPanel panel, int row, TGTrack track) {
		UITableLayout layout = (UITableLayout) panel.getLayout();
		this.name = factory.createLabel(panel);
		layout.set(this.name, row, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, true, false, 1, 1, 120f, null, null);
		TGInputLevel input = TGAudioSession.getInstance(context).getInputLevel(track);
		this.reader = new TGTapMeter(input.getTap(), input::getClipsSinceReset);
		this.bar = TGMeterCanvas.one(factory, TGApplication.getInstance(context).getApplication().getAppearance(), panel, row, 2, WIDTH, HEIGHT, () -> resetClip(context, track));
		this.level = factory.createLabel(panel);
		layout.set(this.level, row, 3, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_CENTER, false, false, 1, 1, 80f, null, null);
		this.clip = factory.createLabel(panel);
		layout.set(this.clip, row, 4, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, false, false, 1, 1, 40f, null, null);
		UIButton reset = factory.createButton(panel);
		reset.setText(TuxGuitar.getProperty("audio.status.reset-clip"));
		reset.addSelectionListener(event -> resetClip(context, track));
		layout.set(reset, row, 5, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, false, false);
	}

	private static void resetClip(TGContext context, TGTrack track) {
		TGActionProcessor processor = new TGActionProcessor(context, TGResetClipIndicatorAction.NAME);
		processor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK, track);
		processor.process();
	}

	void show(TGAudioStatus.Meter meter) {
		this.name.setText(meter.getTrack().getName());
		this.bar.show(this.reader.read(System.currentTimeMillis()), meter.getTrack().getName());
		this.level.setText(meter.getLevel());
		this.clip.setText(meter.hasClipped() ? TuxGuitar.getProperty("audio.status.clip") : "");
	}
}
