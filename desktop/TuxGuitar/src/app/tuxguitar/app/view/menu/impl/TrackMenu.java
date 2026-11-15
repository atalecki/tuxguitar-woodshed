package app.tuxguitar.app.view.menu.impl;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.view.menu.TGMenuItem;
import app.tuxguitar.app.action.impl.track.TGGoFirstTrackAction;
import app.tuxguitar.app.action.impl.track.TGGoPreviousTrackAction;
import app.tuxguitar.app.action.impl.track.TGGoNextTrackAction;
import app.tuxguitar.app.action.impl.track.TGGoLastTrackAction;
import app.tuxguitar.editor.action.track.TGAddNewTrackAction;
import app.tuxguitar.editor.action.track.TGCloneTrackAction;
import app.tuxguitar.editor.action.track.TGRemoveTrackAction;
import app.tuxguitar.editor.action.track.TGMoveTrackUpAction;
import app.tuxguitar.editor.action.track.TGMoveTrackDownAction;
import app.tuxguitar.editor.action.track.TGChangeTrackSoloAction;
import app.tuxguitar.editor.action.track.TGChangeTrackMuteAction;
import app.tuxguitar.app.action.impl.track.TGToggleLyricEditorAction;
import app.tuxguitar.app.system.icons.TGIconManager;
import app.tuxguitar.app.action.impl.track.TGOpenTrackPropertiesDialogAction;
import app.tuxguitar.graphics.control.TGTrackImpl;
import app.tuxguitar.ui.menu.UIMenu;
import app.tuxguitar.ui.menu.UIMenuActionItem;
import app.tuxguitar.ui.menu.UIMenuCheckableItem;

public class TrackMenu extends TGMenuItem {

	private UIMenu menu;
	private UIMenuActionItem first;
	private UIMenuActionItem previous;
	private UIMenuActionItem next;
	private UIMenuActionItem last;
	private UIMenuActionItem addTrack;
	private UIMenuActionItem cloneTrack;
	private UIMenuActionItem removeTrack;
	private UIMenuActionItem moveUp;
	private UIMenuActionItem moveDown;
	private UIMenuCheckableItem changeSolo;
	private UIMenuCheckableItem changeMute;
	private UIMenuActionItem lyrics;
	private UIMenuActionItem properties;

	public TrackMenu(UIMenu menu) {
		this.menu = menu;
	}

	public UIMenu getMenu() {
		return this.menu;
	}

	public void showItems(){
		if(!isDisposed()){
			//--FIRST--
			this.first = this.actionItem(this.menu.createActionItem(), "track.first", TGGoFirstTrackAction.NAME, TGIconManager.TRACK_FIRST);

			//--PREVIOUS--
			this.previous = this.actionItem(this.menu.createActionItem(), "track.previous", TGGoPreviousTrackAction.NAME, TGIconManager.TRACK_PREVIOUS);

			//--NEXT--
			this.next = this.actionItem(this.menu.createActionItem(), "track.next", TGGoNextTrackAction.NAME, TGIconManager.TRACK_NEXT);

			//--LAST--
			this.last = this.actionItem(this.menu.createActionItem(), "track.last", TGGoLastTrackAction.NAME, TGIconManager.TRACK_LAST);

			//--SEPARATOR--
			this.menu.createSeparator();

			//--ADD TRACK--
			this.addTrack = this.actionItem(this.menu.createActionItem(), "track.add", TGAddNewTrackAction.NAME, TGIconManager.TRACK_ADD);

			//--CLONE TRACK--
			this.cloneTrack = this.actionItem(this.menu.createActionItem(), "track.clone", TGCloneTrackAction.NAME, TGIconManager.TRACK_CLONE);

			//--REMOVE TRACK--
			this.removeTrack = this.actionItem(this.menu.createActionItem(), "track.remove", TGRemoveTrackAction.NAME, TGIconManager.TRACK_REMOVE);

			//--SEPARATOR--
			this.menu.createSeparator();

			//--MOVE UP--
			this.moveUp = this.actionItem(this.menu.createActionItem(), "track.move-up", TGMoveTrackUpAction.NAME, TGIconManager.ARROW_UP);

			//--MOVE DOWN--
			this.moveDown = this.actionItem(this.menu.createActionItem(), "track.move-down", TGMoveTrackDownAction.NAME, TGIconManager.ARROW_DOWN);

			//--SEPARATOR--
			this.menu.createSeparator();

			//--SOLO--
			this.changeSolo = this.actionItem(this.menu.createCheckItem(), "track.solo", TGChangeTrackSoloAction.NAME, TGIconManager.TRACK_SOLO);

			//--MUTE--
			this.changeMute = this.actionItem(this.menu.createCheckItem(), "track.mute", TGChangeTrackMuteAction.NAME, TGIconManager.TRACK_MUTE);

			//--SEPARATOR--
			this.menu.createSeparator();

			//--LYRICS--
			this.lyrics = this.actionItem(this.menu.createActionItem(), "track.lyrics", TGToggleLyricEditorAction.NAME, null);

			//--PROPERTIES--
			this.properties = this.actionItem(this.menu.createActionItem(), "track.properties", TGOpenTrackPropertiesDialogAction.NAME, null);

			this.loadIcons();
			this.loadProperties();
		}
	}

	public void loadProperties(){
		if(!isDisposed()){
			this.loadNames();
		}
	}

	public void update(){
		if(!isDisposed()){
			TGTrackImpl track = TuxGuitar.getInstance().getTablatureEditor().getTablature().getCaret().getTrack();
			int tracks = track.getSong().countTracks();
			boolean isFirst = (track.getNumber() == 1);
			boolean isLast = (track.getNumber() == tracks);
			boolean running = TuxGuitar.getInstance().getPlayer().isRunning();
			this.first.setEnabled(!isFirst);
			this.previous.setEnabled(!isFirst);
			this.next.setEnabled(!isLast);
			this.last.setEnabled(!isLast);
			this.addTrack.setEnabled(!running);
			this.cloneTrack.setEnabled(!running);
			this.removeTrack.setEnabled(!running);
			this.moveUp.setEnabled(!running && tracks > 1);
			this.moveDown.setEnabled(!running && tracks > 1);
			this.changeSolo.setChecked(track.isSolo());
			this.changeMute.setChecked(track.isMute());
			this.properties.setEnabled(!running);
		}
	}

	public void loadIcons(){
		this.loadImages();
	}

	public boolean isDisposed(){
		return (this.menu == null || this.menu.isDisposed());
	}

	public void dispose(){
		if(!isDisposed()){
			this.menu.dispose();
		}
	}
}
