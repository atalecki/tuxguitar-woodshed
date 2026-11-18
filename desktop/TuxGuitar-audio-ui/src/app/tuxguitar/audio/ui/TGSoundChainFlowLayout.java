package app.tuxguitar.audio.ui;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import app.tuxguitar.audio.TGSoundChainStrip;
import app.tuxguitar.ui.layout.UIAbstractLayout;
import app.tuxguitar.ui.resource.UIRectangle;
import app.tuxguitar.ui.resource.UISize;
import app.tuxguitar.ui.widget.UIControl;
import app.tuxguitar.ui.widget.UILayoutContainer;

/**
 * A line of the Sound Chain panel: its shown controls, in the order they were made, wrapped onto as few lines as fit
 * the width the panel was last given (TGSoundChainStrip.place decides where each goes). It asks for no width of its
 * own, so the window can be made narrower; when the width it is given needs other lines than it packed for, the
 * window is laid out again (grown). Glue only.
 */
final class TGSoundChainFlowLayout extends UIAbstractLayout {

	private static final float GAP = 4f;

	private final Runnable grown;
	// the width last given: until then, one line
	private float width = Float.MAX_VALUE;
	private float height;

	TGSoundChainFlowLayout(Runnable grown) {
		this.grown = grown;
	}

	// the children's sizes computed first (UIAbstractLayout)
	public UISize getComputedPackedSize(UILayoutContainer container) {
		this.height = place(shown(container), this.width).getHeight();
		return new UISize(0f, this.height);
	}

	public void setBounds(UILayoutContainer container, UIRectangle bounds) {
		List<UIControl> shown = shown(container);
		TGSoundChainStrip.Placement placed = place(shown, bounds.getWidth());
		IntStream.range(0, shown.size()).forEach(item -> shown.get(item).setBounds(new UIRectangle(bounds.getX() + placed.getX(item), bounds.getY() + placed.getY(item),
			shown.get(item).getPackedSize().getWidth(), shown.get(item).getPackedSize().getHeight())));
		this.width = bounds.getWidth();
		if (placed.getHeight() != this.height) {
			this.grown.run();
		}
	}

	private static List<UIControl> shown(UILayoutContainer container) {
		return container.getChildren().stream().filter(UIControl::isVisible).collect(Collectors.toList());
	}

	private static TGSoundChainStrip.Placement place(List<UIControl> shown, float width) {
		float[] widths = new float[shown.size()];
		float[] heights = new float[shown.size()];
		IntStream.range(0, shown.size()).forEach(item -> {
			widths[item] = shown.get(item).getPackedSize().getWidth();
			heights[item] = shown.get(item).getPackedSize().getHeight();
		});
		return TGSoundChainStrip.place(widths, heights, width, GAP);
	}
}
