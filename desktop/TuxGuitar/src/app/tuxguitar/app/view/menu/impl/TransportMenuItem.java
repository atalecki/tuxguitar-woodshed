package app.tuxguitar.app.view.menu.impl;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.action.impl.transport.TGOpenPracticeDialogAction;
import app.tuxguitar.app.action.impl.transport.TGOpenTransportModeDialogAction;
import app.tuxguitar.app.action.impl.transport.TGTransportPlayPauseAction;
import app.tuxguitar.app.action.impl.transport.TGTransportSetLoopEHeaderAction;
import app.tuxguitar.app.action.impl.transport.TGTransportSetLoopSHeaderAction;
import app.tuxguitar.app.action.impl.transport.TGTransportStopAction;
import app.tuxguitar.app.system.icons.TGIconManager;
import app.tuxguitar.app.view.component.tab.Tablature;
import app.tuxguitar.app.view.component.tab.TablatureEditor;
import app.tuxguitar.editor.action.transport.TGTransportCountDownAction;
import app.tuxguitar.editor.action.transport.TGTransportMetronomeAction;
import app.tuxguitar.graphics.control.TGLayout;
import app.tuxguitar.app.action.impl.layout.TGToggleContinuousScrollingAction;
import app.tuxguitar.app.action.impl.layout.TGToggleHighlightPlayedBeatAction;
import app.tuxguitar.app.view.menu.TGMenuItem;
import app.tuxguitar.player.base.MidiPlayerMode;
import app.tuxguitar.song.models.TGMeasure;
import app.tuxguitar.ui.menu.UIMenu;
import app.tuxguitar.ui.menu.UIMenuActionItem;
import app.tuxguitar.ui.menu.UIMenuCheckableItem;
import app.tuxguitar.ui.menu.UIMenuSubMenuItem;

public class TransportMenuItem extends TGMenuItem {

	private UIMenuSubMenuItem transportMenuItem;

	private UIMenuActionItem play;
	private UIMenuActionItem stop;
	private UIMenuCheckableItem metronome;
	private UIMenuCheckableItem countDown;
	private UIMenuActionItem mode;
	private UIMenuActionItem practice;
	private UIMenuCheckableItem loopSHeader;
	private UIMenuCheckableItem loopEHeader;
	private UIMenuCheckableItem highlightPlayedBeat;
	private UIMenuCheckableItem continuousScrolling;

	private boolean isRunning;

	public TransportMenuItem(UIMenu parent) {
		this.transportMenuItem = parent.createSubMenuItem();
	}

	public void showItems(){
		this.named(this.transportMenuItem, "transport", null, null);
		//--PLAY--
		this.play = this.named(this.transportMenuItem.getMenu().createActionItem(), "transport.start", TGTransportPlayPauseAction.NAME, null);
		this.play.addSelectionListener(this.createActionProcessor(TGTransportPlayPauseAction.NAME));

		//--STOP--
		this.stop = this.named(this.transportMenuItem.getMenu().createActionItem(), "transport.stop", TGTransportStopAction.NAME, null);
		this.stop.addSelectionListener(this.createActionProcessor(TGTransportStopAction.NAME));

		//--SEPARATOR--
		this.transportMenuItem.getMenu().createSeparator();

		//--METRONOME--
		this.metronome = this.named(this.transportMenuItem.getMenu().createCheckItem(), "transport.metronome", TGTransportMetronomeAction.NAME, null);
		this.metronome.addSelectionListener(this.createActionProcessor(TGTransportMetronomeAction.NAME));

		//--COUNTDOWN--
		this.countDown = this.named(this.transportMenuItem.getMenu().createCheckItem(), "transport.count-down", TGTransportCountDownAction.NAME, null);
		this.countDown.addSelectionListener(this.createActionProcessor(TGTransportCountDownAction.NAME));

		//--MODE--
		this.mode = this.named(this.transportMenuItem.getMenu().createActionItem(), "transport.mode", TGOpenTransportModeDialogAction.NAME, null);
		this.mode.addSelectionListener(this.createActionProcessor(TGOpenTransportModeDialogAction.NAME));

		//--INTERLEAVED PRACTICE--
		this.practice = this.named(this.transportMenuItem.getMenu().createActionItem(), "transport.practice", TGOpenPracticeDialogAction.NAME, null);
		this.practice.addSelectionListener(this.createActionProcessor(TGOpenPracticeDialogAction.NAME));

		//--SEPARATOR--
		this.transportMenuItem.getMenu().createSeparator();

		//--LOOP START--
		this.loopSHeader = this.named(this.transportMenuItem.getMenu().createCheckItem(), "transport.set-loop-start", TGTransportSetLoopSHeaderAction.NAME, null);
		this.loopSHeader.addSelectionListener(this.createActionProcessor(TGTransportSetLoopSHeaderAction.NAME));

		//--LOOP END--
		this.loopEHeader = this.named(this.transportMenuItem.getMenu().createCheckItem(), "transport.set-loop-end", TGTransportSetLoopEHeaderAction.NAME, null);
		this.loopEHeader.addSelectionListener(this.createActionProcessor(TGTransportSetLoopEHeaderAction.NAME));

		//--SEPARATOR--
		this.transportMenuItem.getMenu().createSeparator();

		//--HIGHLIGHT PLAYED BEAT, CONTINOUS SCROLLING--
		this.highlightPlayedBeat = this.named(this.transportMenuItem.getMenu().createCheckItem(), "transport.highlight-played-beat", TGToggleHighlightPlayedBeatAction.NAME, null);
		this.highlightPlayedBeat.addSelectionListener(this.createActionProcessor(TGToggleHighlightPlayedBeatAction.NAME));
		this.continuousScrolling = this.named(this.transportMenuItem.getMenu().createCheckItem(), "transport.continuous-scrolling", TGToggleContinuousScrollingAction.NAME, null);
		this.continuousScrolling.addSelectionListener(this.createActionProcessor(TGToggleContinuousScrollingAction.NAME));

		this.isRunning = false;
		this.loadIcons();
		this.loadProperties();
	}

	public void update(){
		TGMeasure measure = TuxGuitar.getInstance().getTablatureEditor().getTablature().getCaret().getMeasure();
		MidiPlayerMode pm = TuxGuitar.getInstance().getPlayer().getMode();
		boolean running = TuxGuitar.getInstance().getPlayer().isRunning();
		this.stop.setEnabled(TuxGuitar.getInstance().getPlayer().isRunningOrPractising());
		this.metronome.setChecked(TuxGuitar.getInstance().getPlayer().isMetronomeEnabled());
		this.countDown.setChecked(TuxGuitar.getInstance().getPlayer().getCountDown().isEnabled());
		this.loopSHeader.setEnabled( pm.isLoop() );
		this.loopSHeader.setChecked( isMeasure(measure, pm.getLoopSHeader()) );
		this.loopEHeader.setEnabled( pm.isLoop() );
		this.loopEHeader.setChecked( isMeasure(measure, pm.getLoopEHeader()) );
		Tablature tablature = TablatureEditor.getInstance(this.findContext()).getTablature();
		int style = tablature.getViewLayout().getStyle();
		this.highlightPlayedBeat.setChecked( (style & TGLayout.HIGHLIGHT_PLAYED_BEAT) != 0 );
		this.continuousScrolling.setEnabled(!running);
		this.continuousScrolling.setChecked( (style & TGLayout.CONTINUOUS_SCROLL) != 0 );
		this.loadIcons(false);
		this.play.setText(TuxGuitar.getProperty(this.playKey()));
	}

	private static boolean isMeasure(TGMeasure measure, int number) {
		return (measure != null && measure.getNumber() == number);
	}

	private String playKey() {
		return (this.isRunning ? "transport.pause" : "transport.start");
	}

	public void loadProperties() {
		this.loadNames();
	}

	public void loadIcons(){
		this.loadIcons(true);
		this.metronome.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.TRANSPORT_METRONOME));
		this.countDown.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.TRANSPORT_COUNT_IN));
		this.mode.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.TRANSPORT_MODE));
		this.loopSHeader.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.TRANSPORT_LOOP_START));
		this.loopEHeader.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.TRANSPORT_LOOP_END));
		this.highlightPlayedBeat.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.TRANSPORT_HIGHLIGHT_PLAYED_BEAT));
		this.continuousScrolling.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.TRANSPORT_CONTINUOUS_SCROLLING));
	}

	public void loadIcons(boolean force){
		boolean lastStatusRunning = this.isRunning;

		this.isRunning = TuxGuitar.getInstance().getPlayer().isRunning();

		if(force || lastStatusRunning != isRunning){
			this.stop.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.TRANSPORT_ICON_STOP));
			if(this.isRunning){
				this.play.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.TRANSPORT_ICON_PAUSE));
			} else {
				this.play.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.TRANSPORT_ICON_PLAY));
			}
		}
	}
}
