package app.tuxguitar.app.view.menu.impl;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.action.TGActionProcessorListener;
import app.tuxguitar.app.system.icons.TGIconManager;
import app.tuxguitar.app.view.menu.TGMenuItem;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.note.TGChangeVelocityAction;
import app.tuxguitar.song.models.TGNote;
import app.tuxguitar.song.models.TGVelocities;
import app.tuxguitar.ui.menu.UIMenu;
import app.tuxguitar.ui.menu.UIMenuCheckableItem;
import app.tuxguitar.ui.menu.UIMenuSubMenuItem;

public class DynamicMenuItem extends TGMenuItem {

	private UIMenuSubMenuItem dynamicMenuItem;
	private UIMenuCheckableItem pianoPianissimo;
	private UIMenuCheckableItem pianissimo;
	private UIMenuCheckableItem piano;
	private UIMenuCheckableItem mezzoPiano;
	private UIMenuCheckableItem mezzoForte;
	private UIMenuCheckableItem forte;
	private UIMenuCheckableItem fortissimo;
	private UIMenuCheckableItem forteFortissimo;

	public DynamicMenuItem(UIMenuSubMenuItem dynamicMenuItem) {
		this.dynamicMenuItem = this.named(dynamicMenuItem, "dynamic", null, TGIconManager.DYNAMIC);
	}

	public DynamicMenuItem(UIMenu parent) {
		this(parent.createSubMenuItem());
	}

	public void showItems(){
		//--PPP--
		this.pianoPianissimo = this.named(this.dynamicMenuItem.getMenu().createCheckItem(), "dynamic.piano-pianissimo", null, TGIconManager.DYNAMIC_PPP);
		this.act(this.pianoPianissimo, this.createChangeVelocityAction(TGVelocities.PIANO_PIANISSIMO));

		//--PP--
		this.pianissimo = this.named(this.dynamicMenuItem.getMenu().createCheckItem(), "dynamic.pianissimo", null, TGIconManager.DYNAMIC_PP);
		this.act(this.pianissimo, this.createChangeVelocityAction(TGVelocities.PIANISSIMO));

		//--P--
		this.piano = this.named(this.dynamicMenuItem.getMenu().createCheckItem(), "dynamic.piano", null, TGIconManager.DYNAMIC_P);
		this.act(this.piano, this.createChangeVelocityAction(TGVelocities.PIANO));

		//--MP--
		this.mezzoPiano = this.named(this.dynamicMenuItem.getMenu().createCheckItem(), "dynamic.mezzo-piano", null, TGIconManager.DYNAMIC_MP);
		this.act(this.mezzoPiano, this.createChangeVelocityAction(TGVelocities.MEZZO_PIANO));

		//--MF--
		this.mezzoForte = this.named(this.dynamicMenuItem.getMenu().createCheckItem(), "dynamic.mezzo-forte", null, TGIconManager.DYNAMIC_MF);
		this.act(this.mezzoForte, this.createChangeVelocityAction(TGVelocities.MEZZO_FORTE));

		//--F--
		this.forte = this.named(this.dynamicMenuItem.getMenu().createCheckItem(), "dynamic.forte", null, TGIconManager.DYNAMIC_F);
		this.act(this.forte, this.createChangeVelocityAction(TGVelocities.FORTE));

		//--FF--
		this.fortissimo = this.named(this.dynamicMenuItem.getMenu().createCheckItem(), "dynamic.fortissimo", null, TGIconManager.DYNAMIC_FF);
		this.act(this.fortissimo, this.createChangeVelocityAction(TGVelocities.FORTISSIMO));

		//--FFF--
		this.forteFortissimo = this.named(this.dynamicMenuItem.getMenu().createCheckItem(), "dynamic.forte-fortissimo", null, TGIconManager.DYNAMIC_FFF);
		this.act(this.forteFortissimo, this.createChangeVelocityAction(TGVelocities.FORTE_FORTISSIMO));

		this.loadIcons();
		this.loadProperties();
	}

	public void update(){
		TGNote note = TuxGuitar.getInstance().getTablatureEditor().getTablature().getCaret().getSelectedNote();
		int velocity = ((note != null)?note.getVelocity():TuxGuitar.getInstance().getTablatureEditor().getTablature().getCaret().getVelocity());
		boolean running = TuxGuitar.getInstance().getPlayer().isRunning();
		this.pianoPianissimo.setChecked(velocity == TGVelocities.PIANO_PIANISSIMO);
		this.pianoPianissimo.setEnabled(!running);
		this.pianissimo.setChecked(velocity == TGVelocities.PIANISSIMO);
		this.pianissimo.setEnabled(!running);
		this.piano.setChecked(velocity == TGVelocities.PIANO);
		this.piano.setEnabled(!running);
		this.mezzoPiano.setChecked(velocity == TGVelocities.MEZZO_PIANO);
		this.mezzoPiano.setEnabled(!running);
		this.mezzoForte.setChecked(velocity == TGVelocities.MEZZO_FORTE);
		this.mezzoForte.setEnabled(!running);
		this.forte.setChecked(velocity == TGVelocities.FORTE);
		this.forte.setEnabled(!running);
		this.fortissimo.setChecked(velocity == TGVelocities.FORTISSIMO);
		this.fortissimo.setEnabled(!running);
		this.forteFortissimo.setChecked(velocity == TGVelocities.FORTE_FORTISSIMO);
		this.forteFortissimo.setEnabled(!running);
	}

	public void loadProperties(){
		this.loadNames();
	}

	public void loadIcons(){
		this.loadImages();
	}

	public TGActionProcessorListener createChangeVelocityAction(Integer velocity) {
		return this.createActionProcessor(TGChangeVelocityAction.NAME, TGDocumentContextAttributes.ATTRIBUTE_VELOCITY, velocity);
	}
}
