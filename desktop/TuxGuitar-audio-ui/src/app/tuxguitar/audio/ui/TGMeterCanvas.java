package app.tuxguitar.audio.ui;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.audio.TGMeterLook;
import app.tuxguitar.audio.TGMeterLook.Role;
import app.tuxguitar.audio.TGMeterLook.Shape;
import app.tuxguitar.audio.TGMeterReading;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.appearance.UIAppearance;
import app.tuxguitar.ui.appearance.UIColorAppearance;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.resource.UIColor;
import app.tuxguitar.ui.resource.UIPainter;
import app.tuxguitar.ui.resource.UIRectangle;
import app.tuxguitar.ui.widget.UICanvas;
import app.tuxguitar.ui.widget.UIContainer;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UIPanel;

/**
 * A level meter on screen: it fills the shapes TGMeterLook gives for its reading, each role in a system colour (so a
 * Windows high-contrast theme recolours it); a click on it runs what clears its lamp; its
 * tooltip says what it read. A meter of two bars has its held peak written under it. Glue only; on the UI thread.
 */
final class TGMeterCanvas {

	// a bar's height: a meter of one bar is as high, one of two bars twice as high plus a gap
	static final float BAR = 6f;
	static final float STEREO = 2 * BAR + 1f;

	private static final Map<Role, UIColorAppearance> COLOURS = new EnumMap<Role, UIColorAppearance>(Role.class);

	static {
		COLOURS.put(Role.TRACK, UIColorAppearance.InputBackground);
		COLOURS.put(Role.BAR, UIColorAppearance.InputSelectedBackground);
		COLOURS.put(Role.OVER, UIColorAppearance.InputSelectedBackground);
		COLOURS.put(Role.STRIPE, UIColorAppearance.WidgetForeground);
		COLOURS.put(Role.ZERO, UIColorAppearance.WidgetForeground);
		COLOURS.put(Role.HOLD, UIColorAppearance.WidgetForeground);
		COLOURS.put(Role.LAMP, UIColorAppearance.WidgetForeground);
		COLOURS.put(Role.HOLE, UIColorAppearance.InputBackground);
		COLOURS.put(Role.BANG, UIColorAppearance.InputBackground);
		COLOURS.put(Role.GREYED, UIColorAppearance.WidgetHighlightBackground);
	}

	private final UICanvas canvas;
	private final boolean stereo;
	private final Consumer<String> number;
	private final Map<Role, UIColor> colours = new EnumMap<Role, UIColor>(Role.class);
	private TGMeterReading reading = TGMeterReading.OFF;

	private TGMeterCanvas(UIFactory factory, UIAppearance appearance, UIContainer parent, boolean stereo, Consumer<String> number, Runnable clicked) {
		this.canvas = factory.createCanvas(parent, false);
		this.stereo = stereo;
		this.number = number;
		COLOURS.forEach((role, colour) -> this.colours.put(role, factory.createColor(appearance.getColorModel(colour))));
		this.canvas.addPaintListener(event -> this.paint(event.getPainter()));
		this.canvas.addMouseUpListener(event -> clicked.run());
		this.canvas.addDisposeListener(event -> this.colours.values().forEach(UIColor::dispose));
	}

	// a meter of one bar (the louder side), in the group's cell, as wide as the cell
	static TGMeterCanvas mono(UIFactory factory, UIAppearance appearance, UIPanel group, int row, int column, Runnable clicked) {
		return one(factory, appearance, group, row, column, null, BAR, clicked);
	}

	// a meter of one bar (the louder side), as wide (null: the cell's width) and high as given
	static TGMeterCanvas one(UIFactory factory, UIAppearance appearance, UIPanel group, int row, int column, Float width, float height, Runnable clicked) {
		TGMeterCanvas meter = new TGMeterCanvas(factory, appearance, group, false, text -> { }, clicked);
		((UITableLayout) group.getLayout()).set(meter.canvas, row, column, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, false, 1, 1, width, height, 0f);
		return meter;
	}

	// a meter of two bars (left above right) as wide as given, its held peak written in the label
	static TGMeterCanvas stereo(UIFactory factory, UIAppearance appearance, UIPanel group, int row, int column, float width, UILabel number, Runnable clicked) {
		TGMeterCanvas meter = new TGMeterCanvas(factory, appearance, group, true, number::setText, clicked);
		((UITableLayout) group.getLayout()).set(meter.canvas, row, column, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_TOP, true, false, 1, 2, width, STEREO, 0f);
		return meter;
	}

	// the reading of the meter so named; drawn again only when it reads differently
	void show(TGMeterReading reading, String name) {
		this.canvas.setToolTipText(TGMeterLook.tip(name, reading, TuxGuitar.getProperty("audio.sound-chain.meter.read"), TuxGuitar.getProperty("audio.sound-chain.meter.off"), TuxGuitar.getProperty("audio.sound-chain.meter.clipped")));
		this.number.accept(TGMeterLook.number(reading));
		if (!reading.equals(this.reading)) {
			this.reading = reading;
			this.canvas.redraw();
		}
	}

	private void paint(UIPainter painter) {
		UIRectangle size = this.canvas.getBounds();
		TGMeterLook.shapes(this.reading, size.getWidth(), size.getHeight(), this.stereo).forEach(shape -> this.paint(painter, shape));
	}

	private void paint(UIPainter painter, Shape shape) {
		UIColor colour = this.colours.get(shape.getRole());
		painter.setBackground(colour);
		painter.initPath(UIPainter.PATH_FILL);
		painter.addRectangle(shape.getX(), shape.getY(), shape.getWidth(), shape.getHeight());
		painter.closePath();
	}
}
