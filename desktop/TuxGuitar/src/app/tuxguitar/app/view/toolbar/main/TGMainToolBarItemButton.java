package app.tuxguitar.app.view.toolbar.main;

import java.util.Optional;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.ui.toolbar.UIToolActionItem;
import app.tuxguitar.ui.toolbar.UIToolCheckableItem;
import app.tuxguitar.ui.toolbar.UIToolItem;
import app.tuxguitar.ui.widget.UIControl;
import app.tuxguitar.util.TGContext;

// basic button, checkable button
public class TGMainToolBarItemButton extends TGMainToolBarItem {

	protected UIToolItem toolItem;
	private boolean checked;

	public TGMainToolBarItemButton(TGMainToolBarItemConfig config, UIToolItem toolItem) {
		super(config);
		this.toolItem = toolItem;
		this.showIconOn(toolItem, UIToolItem::setImage);
	}

	@Override
	public UIControl getControl() {
		// unused, control is stored in toolBar section
		return null;
	}

	@Override
	public void update(TGContext context, boolean running) {
		this.updateEnabled(context, running);
		if (this.updater != null) {
			this.updateChecked(updater.checked(context, running));
			Optional.ofNullable(updater.getIconName(context, running)).filter(name -> !name.equals(this.iconFileName)).ifPresent(this::updateIcon);
			Optional.ofNullable(updater.getText(context, running)).filter(text -> !text.equals(this.text)).ifPresent(this::updateText);
		}
	}

	private void updateEnabled(TGContext context, boolean running) {
		if (this.toolItem != null) {
			((UIToolActionItem) toolItem).setEnabled(this.enabled(context, running));
		}
	}

	private void updateChecked(boolean checked) {
		this.checked = checked;
		if (toolItem instanceof UIToolCheckableItem) {
			((UIToolCheckableItem) toolItem).setChecked(this.checked);
		}
	}

	private void updateIcon(String newIconName) {
		this.iconFileName = newIconName;
		this.loadIcons(TuxGuitar.getInstance().getIconManager());
	}

	private void updateText(String newText) {
		this.text = newText;
		this.loadProperties();
	}

	// as its updater says, unless a practice session refuses its action
	private boolean enabled(TGContext context, boolean running) {
		return (this.updaterEnables(context, running) && !this.greys(context));
	}

	// enabled without an updater
	private boolean updaterEnables(TGContext context, boolean running) {
		return (this.updater == null || this.updater.enabled(context, running));
	}

	@Override
	public void loadProperties() {
		if (toolItem != null) {
			toolItem.setToolTipText(TuxGuitar.getProperty(this.text));
		}
	}


}
