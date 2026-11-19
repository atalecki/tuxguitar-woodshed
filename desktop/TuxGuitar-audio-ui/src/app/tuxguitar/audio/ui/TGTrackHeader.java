package app.tuxguitar.audio.ui;

import java.util.Optional;
import java.util.stream.Stream;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.view.main.TGWindow;
import app.tuxguitar.audio.TGSoundChainCommands;
import app.tuxguitar.song.models.TGColor;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.chooser.UIColorChooser;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.resource.UIColor;
import app.tuxguitar.ui.resource.UIColorModel;
import app.tuxguitar.ui.resource.UIKey;
import app.tuxguitar.ui.resource.UIKeyCombination;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.ui.widget.UITextField;
import app.tuxguitar.util.TGContext;

/**
 * The Track panel's header: the track's colour, a swatch a click on which opens the colour chooser, and its name, which
 * Enter or leaving the field gives the track; each one undoable change of the track's info, as Track Properties makes it
 * (TGSoundChainCommands). Without a track, both are greyed, the field saying so. Glue only; on the UI thread.
 */
final class TGTrackHeader {

	private final TGContext context;
	private final UIFactory factory;
	private final TGSoundChainCommands commands;
	private final UIButton swatch;
	private final UITextField name;
	// the swatch's colour, the track's
	private UIColor colour;

	TGTrackHeader(TGContext context, UIFactory factory, UIPanel parent, int row, TGSoundChainCommands commands) {
		this.context = context;
		this.factory = factory;
		this.commands = commands;
		UIPanel header = factory.createPanel(parent, false);
		header.setLayout(new UITableLayout(0f));
		this.swatch = factory.createButton(header);
		this.swatch.addSelectionListener(event -> this.choose());
		this.swatch.addDisposeListener(event -> this.dispose());
		this.name = factory.createTextField(header);
		this.name.addKeyPressedListener(event -> this.entered(event.getKeyCombination()));
		this.name.addFocusLostListener(event -> this.rename());
		UITableLayout layout = (UITableLayout) header.getLayout();
		layout.set(this.swatch, 1, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, false, false);
		layout.set(this.name, 1, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, false);
		((UITableLayout) parent.getLayout()).set(header, row, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_TOP, true, false);
	}

	// the texts in the language chosen
	void retell() {
		this.swatch.setText(TuxGuitar.getProperty("audio.track.swatch"));
		this.swatch.setToolTipText(TuxGuitar.getProperty("audio.track.colour"));
	}

	// the track shown (null: none, titled as given)
	void show(TGTrack track, String title) {
		this.name.setText(title);
		this.name.setEnabled(track != null);
		this.swatch.setEnabled(track != null);
		Optional.ofNullable(track).map(TGTrack::getColor).ifPresent(this::paint);
	}

	private void paint(TGColor color) {
		this.dispose();
		this.colour = this.factory.createColor(color.getR(), color.getG(), color.getB());
		this.swatch.setFgColor(this.colour);
	}

	private void dispose() {
		Optional.ofNullable(this.colour).ifPresent(UIColor::dispose);
		this.colour = null;
	}

	private void entered(UIKeyCombination keys) {
		if (Stream.of(UIKey.ENTER, UIKey.NUMPAD_ENTER).anyMatch(keys::contains)) {
			this.rename();
		}
	}

	// the name typed (the name the track has already: nothing)
	private void rename() {
		this.commands.rename(this.name.getText());
	}

	private void choose() {
		UIColorChooser chooser = this.factory.createColorChooser(TGWindow.getInstance(this.context).getWindow());
		chooser.setDefaultModel(new UIColorModel(this.colour.getRed(), this.colour.getGreen(), this.colour.getBlue()));
		chooser.setText(TuxGuitar.getProperty("choose-color"));
		chooser.choose(chosen -> Optional.ofNullable(chosen).map(TGTrackHeader::color).ifPresent(this.commands::recolor));
	}

	private static TGColor color(UIColorModel chosen) {
		return TGColor.newColor(chosen.getRed(), chosen.getGreen(), chosen.getBlue());
	}
}
