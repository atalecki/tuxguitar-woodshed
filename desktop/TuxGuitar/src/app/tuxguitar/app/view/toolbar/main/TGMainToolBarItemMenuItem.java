package app.tuxguitar.app.view.toolbar.main;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.system.icons.TGIconManager;
import app.tuxguitar.ui.menu.UIMenuItem;
import app.tuxguitar.ui.widget.UIControl;
import app.tuxguitar.util.TGContext;

// a menu action (inside a menu, opened by a toolBar button)
public class TGMainToolBarItemMenuItem extends TGMainToolBarItem {

	private static final String SELECTED_CHAR = "\u2713";
	private UIMenuItem menuItem;
	private boolean checked;

	// build a menu item from a (checkable) button description, or a separator
	public TGMainToolBarItemMenuItem(TGMainToolBarItemConfig button) {
		super(button);
		for (String key : button.getAttributes().keySet()) {
			this.setAttribute(key, button.getAttributes().get(key));
		}
		this.menuItem = null;
	}

	public boolean isChecked() {
		return this.checked;
	}

	public void setMenuItem(UIMenuItem menuItem) {
		this.menuItem = menuItem;
	}

	@Override
	public void update(TGContext context, boolean running) {
		this.updateEnabled(context);
		if (this.updater != null) {
			this.checked = this.updater.checked(context, running);
			this.loadProperties();
		}
	}

	// greyed out while a practice session refuses its action
	private void updateEnabled(TGContext context) {
		if (this.menuItem != null) {
			this.menuItem.setEnabled(!this.greys(context));
		}
	}

	@Override
	public void loadProperties() {
		if (menuItem != null) {
			menuItem.setText((this.checked ? SELECTED_CHAR : "") + TuxGuitar.getProperty(this.getText()));
		}
	}

	@Override
	public void loadIcons(TGIconManager iconManager) {
		if (menuItem != null) {
			menuItem.setImage(iconManager.getImageByName(this.iconFileName));
		}
	}

	@Override
	public UIControl getControl() {
		return null;
	}

}
