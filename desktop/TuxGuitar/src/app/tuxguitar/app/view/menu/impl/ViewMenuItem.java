package app.tuxguitar.app.view.menu.impl;


import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.action.impl.layout.TGSetChordDiagramEnabledAction;
import app.tuxguitar.app.action.impl.layout.TGSetChordNameEnabledAction;
import app.tuxguitar.app.action.impl.layout.TGSetCompactViewAction;
import app.tuxguitar.app.action.impl.layout.TGSetLayoutScaleDecrementAction;
import app.tuxguitar.app.action.impl.layout.TGSetLayoutScaleIncrementAction;
import app.tuxguitar.app.action.impl.layout.TGSetLayoutScaleResetAction;
import app.tuxguitar.app.action.impl.layout.TGSetLinearLayoutAction;
import app.tuxguitar.app.action.impl.layout.TGSetMultitrackViewAction;
import app.tuxguitar.app.action.impl.layout.TGSetPageLayoutAction;
import app.tuxguitar.app.action.impl.layout.TGSetScoreEnabledAction;
import app.tuxguitar.app.action.impl.layout.TGSetTablatureEnabledAction;
import app.tuxguitar.app.action.impl.view.TGOpenMainToolBarSettingsDialogAction;
import app.tuxguitar.app.action.impl.view.TGToggleChannelsDialogAction;
import app.tuxguitar.app.action.impl.view.TGToggleEditToolbarAction;
import app.tuxguitar.app.action.impl.view.TGToggleFretBoardEditorAction;
import app.tuxguitar.app.action.impl.view.TGToggleMainToolbarAction;
import app.tuxguitar.app.action.impl.view.TGToggleMatrixEditorAction;
import app.tuxguitar.app.action.impl.view.TGTogglePianoEditorAction;
import app.tuxguitar.app.action.impl.view.TGToggleTableViewerAction;
import app.tuxguitar.app.action.impl.view.TGToggleTransportDialogAction;
import app.tuxguitar.app.system.icons.TGIconManager;
import app.tuxguitar.app.view.component.tab.Tablature;
import app.tuxguitar.app.view.component.tab.TablatureEditor;
import app.tuxguitar.app.view.component.table.TGTableViewer;
import app.tuxguitar.app.view.dialog.transport.TGTransportDialog;
import app.tuxguitar.app.view.menu.TGMenuItem;
import app.tuxguitar.app.view.toolbar.edit.TGEditToolBar;
import app.tuxguitar.app.view.toolbar.main.TGMainToolBar;
import app.tuxguitar.graphics.control.TGLayout;
import app.tuxguitar.graphics.control.TGLayoutHorizontal;
import app.tuxguitar.graphics.control.TGLayoutVertical;
import app.tuxguitar.ui.menu.UIMenu;
import app.tuxguitar.ui.menu.UIMenuActionItem;
import app.tuxguitar.ui.menu.UIMenuCheckableItem;
import app.tuxguitar.ui.menu.UIMenuSubMenuItem;

public class ViewMenuItem extends TGMenuItem {

	private UIMenuSubMenuItem layoutMenuItem;
	private UIMenuCheckableItem showMainToolbar;
	private UIMenuActionItem editMainToolbar;
	private UIMenuCheckableItem showEditToolbar;
	private UIMenuCheckableItem showTableViewer;
	private UIMenuCheckableItem showFretBoard;
	private UIMenuCheckableItem showInstruments;
	private UIMenuCheckableItem showTransport;
	private UIMenuCheckableItem showPiano;
	private UIMenuCheckableItem showMatrix;
	private UIMenuCheckableItem pageLayout;
	private UIMenuCheckableItem linearLayout;
	private UIMenuCheckableItem multitrack;
	private UIMenuCheckableItem scoreEnabled;
	private UIMenuCheckableItem tablatureEnabled;
	private UIMenuCheckableItem compact;
	private UIMenuActionItem zoomIn;
	private UIMenuActionItem zoomOut;
	private UIMenuActionItem zoomReset;

	private UIMenuSubMenuItem chordMenuItem;
	private UIMenuCheckableItem chordName;
	private UIMenuCheckableItem chordDiagram;

	public ViewMenuItem(UIMenu parent) {
		this.layoutMenuItem = this.named(parent.createSubMenuItem(), "view", null, null);
	}

	public void showItems(){
		//--MAIN TOOLBAR--
		this.showMainToolbar = this.actionItem(this.layoutMenuItem.getMenu().createCheckItem(), "view.show-main-toolbar", TGToggleMainToolbarAction.NAME, null);
		this.editMainToolbar = this.actionItem(this.layoutMenuItem.getMenu().createActionItem(), "toolbar.settings", TGOpenMainToolBarSettingsDialogAction.NAME, TGIconManager.SETTINGS);

		//--EDIT TOOLBAR--
		this.showEditToolbar = this.named(this.layoutMenuItem.getMenu().createCheckItem(), "view.show-edit-toolbar", TGToggleMainToolbarAction.NAME, TGIconManager.TOOLBAR_EDIT);
		this.act(this.showEditToolbar, this.createActionProcessor(TGToggleEditToolbarAction.NAME));

		//--TRACKS TABLE--
		this.showTableViewer = this.actionItem(this.layoutMenuItem.getMenu().createCheckItem(), "view.show-table-viewer", TGToggleTableViewerAction.NAME, TGIconManager.TABLE_VIEWER);

		//--FRETBOARD--
		this.showFretBoard = this.actionItem(this.layoutMenuItem.getMenu().createCheckItem(), "view.show-fretboard", TGToggleFretBoardEditorAction.NAME, TGIconManager.FRETBOARD);

		this.layoutMenuItem.getMenu().createSeparator();

		//--INSTRUMENTS--
		this.showInstruments = this.actionItem(this.layoutMenuItem.getMenu().createCheckItem(), "view.show-instruments", TGToggleChannelsDialogAction.NAME, TGIconManager.INSTRUMENTS);

		//--TRANSPORT--
		this.showTransport = this.actionItem(this.layoutMenuItem.getMenu().createCheckItem(), "view.show-transport", TGToggleTransportDialogAction.NAME, TGIconManager.TRANSPORT);

		//--PIANO--
		this.showPiano = this.actionItem(this.layoutMenuItem.getMenu().createCheckItem(), "view.show-piano", TGTogglePianoEditorAction.NAME, TGIconManager.PIANO);

		//--MATRIX--
		this.showMatrix = this.actionItem(this.layoutMenuItem.getMenu().createCheckItem(), "view.show-matrix", TGToggleMatrixEditorAction.NAME, TGIconManager.MATRIX);

		this.layoutMenuItem.getMenu().createSeparator();

		//--PAGE LAYOUT--
		this.pageLayout = this.actionItem(this.layoutMenuItem.getMenu().createRadioItem(), "view.layout.page", TGSetPageLayoutAction.NAME, TGIconManager.LAYOUT_PAGE);

		//--LINEAR LAYOUT--
		this.linearLayout = this.actionItem(this.layoutMenuItem.getMenu().createRadioItem(), "view.layout.linear", TGSetLinearLayoutAction.NAME, TGIconManager.LAYOUT_LINEAR);

		//--MULTITRACK--
		this.multitrack = this.actionItem(this.layoutMenuItem.getMenu().createCheckItem(), "view.layout.multitrack", TGSetMultitrackViewAction.NAME, TGIconManager.LAYOUT_MULTITRACK);

		//--SCORE--
		this.scoreEnabled = this.actionItem(this.layoutMenuItem.getMenu().createCheckItem(), "view.layout.score-enabled", TGSetScoreEnabledAction.NAME, TGIconManager.LAYOUT_SCORE);

		//--TABLATURE--
		this.tablatureEnabled = this.actionItem(this.layoutMenuItem.getMenu().createCheckItem(), "view.layout.tablature-enabled", TGSetTablatureEnabledAction.NAME, TGIconManager.LAYOUT_TABLATURE);

		//--COMPACT--
		this.compact = this.actionItem(this.layoutMenuItem.getMenu().createCheckItem(), "view.layout.compact", TGSetCompactViewAction.NAME, TGIconManager.LAYOUT_COMPACT);

		this.layoutMenuItem.getMenu().createSeparator();

		//--CHORD STYLE--
		this.chordMenuItem = this.named(this.layoutMenuItem.getMenu().createSubMenuItem(), "view.layout.chord-style", null, null);

		this.chordName = this.actionItem(this.chordMenuItem.getMenu().createCheckItem(), "view.layout.chord-name", TGSetChordNameEnabledAction.NAME, null);

		this.chordDiagram = this.actionItem(this.chordMenuItem.getMenu().createCheckItem(), "view.layout.chord-diagram", TGSetChordDiagramEnabledAction.NAME, null);

		this.layoutMenuItem.getMenu().createSeparator();

		//--ZOOM IN--
		this.zoomIn = this.actionItem(this.layoutMenuItem.getMenu().createActionItem(), "view.zoom.in", TGSetLayoutScaleIncrementAction.NAME, TGIconManager.ZOOM_IN);

		//--ZOOM OUT--
		this.zoomOut = this.actionItem(this.layoutMenuItem.getMenu().createActionItem(), "view.zoom.out", TGSetLayoutScaleDecrementAction.NAME, TGIconManager.ZOOM_OUT);

		//--ZOOM RESET--
		this.zoomReset = this.actionItem(this.layoutMenuItem.getMenu().createActionItem(), "view.zoom.reset", TGSetLayoutScaleResetAction.NAME, TGIconManager.ZOOM_RESET);

		this.loadIcons();
		this.loadProperties();
	}

	public void update() {
		Tablature tablature = TablatureEditor.getInstance(this.findContext()).getTablature();
		int style = tablature.getViewLayout().getStyle();
		this.showMainToolbar.setChecked(TGMainToolBar.getInstance(this.findContext()).isVisible());
		this.showEditToolbar.setChecked(TGEditToolBar.getInstance(this.findContext()).isVisible());
		this.showTableViewer.setChecked(TGTableViewer.getInstance(this.findContext()).isVisible());
		this.showFretBoard.setChecked(TuxGuitar.getInstance().getFretBoardEditor().isVisible());
		this.showInstruments.setChecked(!TuxGuitar.getInstance().getChannelManager().isDisposed());
		this.showTransport.setChecked(!TGTransportDialog.getInstance(this.findContext()).isDisposed());
		this.showPiano.setChecked(!TuxGuitar.getInstance().getPianoEditor().isDisposed());
		this.showMatrix.setChecked(!TuxGuitar.getInstance().getMatrixEditor().isDisposed());
		this.pageLayout.setChecked(tablature.getViewLayout() instanceof TGLayoutVertical);
		this.linearLayout.setChecked(tablature.getViewLayout() instanceof TGLayoutHorizontal);
		this.multitrack.setChecked( (style & TGLayout.DISPLAY_MULTITRACK) != 0 );
		this.scoreEnabled.setChecked( (style & TGLayout.DISPLAY_SCORE) != 0 );
		this.tablatureEnabled.setChecked( (style & TGLayout.DISPLAY_TABLATURE) != 0 );
		this.compact.setEnabled((style & TGLayout.DISPLAY_MULTITRACK) == 0 || tablature.getViewLayout().getSong().countTracks() == 1);
		this.compact.setChecked( this.compact.isEnabled() && ((style & TGLayout.DISPLAY_COMPACT) != 0) );
		this.chordName.setChecked( (style & TGLayout.DISPLAY_CHORD_NAME) != 0 );
		this.chordDiagram.setChecked( (style & TGLayout.DISPLAY_CHORD_DIAGRAM) != 0 );
		this.zoomReset.setEnabled(!Tablature.DEFAULT_SCALE.equals(tablature.getScale()));
	}

	public void loadProperties(){
		this.loadNames();
	}

	public void loadIcons(){
		this.loadImages();
		this.showMainToolbar.setImage(TuxGuitar.getInstance().getIconManager().getToolbarMain());
	}
}
