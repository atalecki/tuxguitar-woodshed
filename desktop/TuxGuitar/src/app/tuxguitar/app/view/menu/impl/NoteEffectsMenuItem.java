package app.tuxguitar.app.view.menu.impl;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.action.impl.effects.TGOpenBendDialogAction;
import app.tuxguitar.app.action.impl.effects.TGOpenGraceDialogAction;
import app.tuxguitar.app.action.impl.effects.TGOpenHarmonicDialogAction;
import app.tuxguitar.app.action.impl.effects.TGOpenTremoloBarDialogAction;
import app.tuxguitar.app.action.impl.effects.TGOpenTremoloPickingDialogAction;
import app.tuxguitar.app.action.impl.effects.TGOpenTrillDialogAction;
import app.tuxguitar.app.system.icons.TGIconManager;
import app.tuxguitar.app.view.component.tab.TablatureEditor;
import app.tuxguitar.app.view.menu.TGMenuItem;
import app.tuxguitar.editor.action.effect.TGChangeAccentuatedNoteAction;
import app.tuxguitar.editor.action.effect.TGChangeDeadNoteAction;
import app.tuxguitar.editor.action.effect.TGChangeFadeInAction;
import app.tuxguitar.editor.action.effect.TGChangeGhostNoteAction;
import app.tuxguitar.editor.action.effect.TGChangeHammerNoteAction;
import app.tuxguitar.editor.action.effect.TGChangeHeavyAccentuatedNoteAction;
import app.tuxguitar.editor.action.effect.TGChangeLetRingAction;
import app.tuxguitar.editor.action.effect.TGChangePalmMuteAction;
import app.tuxguitar.editor.action.effect.TGChangePoppingAction;
import app.tuxguitar.editor.action.effect.TGChangeSlappingAction;
import app.tuxguitar.editor.action.effect.TGChangeSlideNoteAction;
import app.tuxguitar.editor.action.effect.TGChangeStaccatoAction;
import app.tuxguitar.editor.action.effect.TGChangeTappingAction;
import app.tuxguitar.editor.action.effect.TGChangeVibratoNoteAction;
import app.tuxguitar.ui.menu.UIMenu;
import app.tuxguitar.ui.menu.UIMenuCheckableItem;
import app.tuxguitar.ui.menu.UIMenuSubMenuItem;
import app.tuxguitar.util.TGNoteRange;

public class NoteEffectsMenuItem extends TGMenuItem {

	private UIMenuSubMenuItem noteEffectsMenuItem;
	private UIMenuCheckableItem vibrato;
	private UIMenuCheckableItem bend;
	private UIMenuCheckableItem tremoloBar;
	private UIMenuCheckableItem deadNote;
	private UIMenuCheckableItem slide;
	private UIMenuCheckableItem hammer;
	private UIMenuCheckableItem ghostNote;
	private UIMenuCheckableItem accentuatedNote;
	private UIMenuCheckableItem heavyAccentuatedNote;
	private UIMenuCheckableItem letRing;
	private UIMenuCheckableItem harmonicNote;
	private UIMenuCheckableItem graceNote;
	private UIMenuCheckableItem trill;
	private UIMenuCheckableItem tremoloPicking;
	private UIMenuCheckableItem palmMute;
	private UIMenuCheckableItem staccato;
	private UIMenuCheckableItem tapping;
	private UIMenuCheckableItem slapping;
	private UIMenuCheckableItem popping;
	private UIMenuCheckableItem fadeIn;

	public NoteEffectsMenuItem(UIMenuSubMenuItem noteEffectsMenuItem) {
		this.noteEffectsMenuItem = this.named(noteEffectsMenuItem, "effects", null, null);
	}

	public NoteEffectsMenuItem(UIMenu parent) {
		this(parent.createSubMenuItem());
	}

	public void showItems(){
		//--VIBRATO--
		this.vibrato = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.vibrato", TGChangeVibratoNoteAction.NAME, TGIconManager.EFFECT_VIBRATO);

		//--BEND--
		this.bend = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.bend", TGOpenBendDialogAction.NAME, TGIconManager.EFFECT_BEND);

		//--BEND--
		this.tremoloBar = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.tremolo-bar", TGOpenTremoloBarDialogAction.NAME, TGIconManager.EFFECT_TREMOLO_BAR);

		//--SLIDE--
		this.slide = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.slide", TGChangeSlideNoteAction.NAME, TGIconManager.EFFECT_SLIDE);

		//--SLIDE--
		this.deadNote = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.deadnote", TGChangeDeadNoteAction.NAME, TGIconManager.EFFECT_DEAD);

		//--HAMMER--
		this.hammer = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.hammer", TGChangeHammerNoteAction.NAME, TGIconManager.EFFECT_HAMMER);

		//--SEPARATOR--
		this.noteEffectsMenuItem.getMenu().createSeparator();

		//--GHOST NOTE--
		this.ghostNote = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.ghostnote", TGChangeGhostNoteAction.NAME, TGIconManager.EFFECT_GHOST);

		//--ACCENTUATED NOTE--
		this.accentuatedNote = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.accentuatednote", TGChangeAccentuatedNoteAction.NAME, TGIconManager.EFFECT_ACCENTUATED);

		//--HEAVY ACCENTUATED NOTE--
		this.heavyAccentuatedNote = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.heavyaccentuatednote", TGChangeHeavyAccentuatedNoteAction.NAME, TGIconManager.EFFECT_HEAVY_ACCENTUATED);

		//--LET RING--
		this.letRing = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.let-ring", TGChangeLetRingAction.NAME, TGIconManager.EFFECT_LET_RING);

		//--HARMONIC NOTE--
		this.harmonicNote = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.harmonic", TGOpenHarmonicDialogAction.NAME, TGIconManager.EFFECT_HARMONIC);

		//--GRACE NOTE--
		this.graceNote = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.grace", TGOpenGraceDialogAction.NAME, TGIconManager.EFFECT_GRACE);

		//--SEPARATOR--
		this.noteEffectsMenuItem.getMenu().createSeparator();

		//--TRILL--
		this.trill = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.trill", TGOpenTrillDialogAction.NAME, TGIconManager.EFFECT_TRILL);

		//--TREMOLO PICKING--
		this.tremoloPicking = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.tremolo-picking", TGOpenTremoloPickingDialogAction.NAME, TGIconManager.EFFECT_TREMOLO_PICKING);

		//--PALM MUTE--
		this.palmMute = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.palm-mute", TGChangePalmMuteAction.NAME, TGIconManager.EFFECT_PALM_MUTE);

		//--STACCATO--
		this.staccato = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.staccato", TGChangeStaccatoAction.NAME, TGIconManager.EFFECT_STACCATO);

		//--SEPARATOR--
		this.noteEffectsMenuItem.getMenu().createSeparator();

		//--TAPPING--
		this.tapping = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.tapping", TGChangeTappingAction.NAME, TGIconManager.EFFECT_TAPPING);

		//--SLAPPING--
		this.slapping = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.slapping", TGChangeSlappingAction.NAME, TGIconManager.EFFECT_SLAPPING);

		//--POPPING--
		this.popping = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.popping", TGChangePoppingAction.NAME, TGIconManager.EFFECT_POPPING);

		//--SEPARATOR--
		this.noteEffectsMenuItem.getMenu().createSeparator();

		//--FADE IN--
		this.fadeIn = this.actionItem(this.noteEffectsMenuItem.getMenu().createCheckItem(), "effects.fade-in", TGChangeFadeInAction.NAME, TGIconManager.EFFECT_FADE_IN);

		this.loadIcons();
		this.loadProperties();
	}

	public void setEnabled(boolean enabled) {
		this.noteEffectsMenuItem.setEnabled(enabled);
	}

	public void update(){
		boolean running = TuxGuitar.getInstance().getPlayer().isRunning();
		TGNoteRange noteRange = TablatureEditor.getInstance(this.findContext()).getTablature().getCurrentNoteRange();
		boolean isPercussion = TablatureEditor.getInstance(this.findContext()).getTablature().getCaret().getTrack().isPercussion();

		this.vibrato.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isVibrato()));
		this.vibrato.setEnabled(!running && !noteRange.isEmpty() && !isPercussion);
		this.bend.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isBend()));
		this.bend.setEnabled(!running && !noteRange.isEmpty() && !isPercussion);
		this.tremoloBar.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isTremoloBar()));
		this.tremoloBar.setEnabled(!running && !noteRange.isEmpty() && !isPercussion);
		this.deadNote.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isDeadNote()));
		this.deadNote.setEnabled(!running && !isPercussion);
		this.slide.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isSlide()));
		this.slide.setEnabled(!running && !noteRange.isEmpty() && !isPercussion);
		this.hammer.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isHammer()));
		this.hammer.setEnabled(!running && !noteRange.isEmpty() && !isPercussion);
		this.ghostNote.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isGhostNote()));
		this.ghostNote.setEnabled(!running && !noteRange.isEmpty());
		this.accentuatedNote.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isAccentuatedNote()));
		this.accentuatedNote.setEnabled(!running && !noteRange.isEmpty());
		this.heavyAccentuatedNote.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isHeavyAccentuatedNote()));
		this.heavyAccentuatedNote.setEnabled(!running && !noteRange.isEmpty());
		this.letRing.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isLetRing()));
		this.letRing.setEnabled(!running && !noteRange.isEmpty() && !isPercussion);
		this.harmonicNote.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isHarmonic()));
		this.harmonicNote.setEnabled(!running && !noteRange.isEmpty() && !isPercussion);
		this.graceNote.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isGrace()));
		this.graceNote.setEnabled(!running && !noteRange.isEmpty());
		this.trill.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isTrill()));
		this.trill.setEnabled(!running && !noteRange.isEmpty() && !isPercussion);
		this.tremoloPicking.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isTremoloPicking()));
		this.tremoloPicking.setEnabled(!running && !noteRange.isEmpty());
		this.palmMute.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isPalmMute()));
		this.palmMute.setEnabled(!running && !noteRange.isEmpty() && !isPercussion);
		this.staccato.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isStaccato()));
		this.staccato.setEnabled(!running && !noteRange.isEmpty() && !isPercussion);
		this.tapping.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isTapping()));
		this.tapping.setEnabled(!running && !noteRange.isEmpty() && !isPercussion);
		this.slapping.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isSlapping()));
		this.slapping.setEnabled(!running && !noteRange.isEmpty() && !isPercussion);
		this.popping.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isPopping()));
		this.popping.setEnabled(!running && !noteRange.isEmpty() && !isPercussion);
		this.fadeIn.setChecked(!noteRange.isEmpty() && noteRange.getNotes().stream().allMatch(n -> n.getEffect().isFadeIn()));
		this.fadeIn.setEnabled(!running && !noteRange.isEmpty() && !isPercussion);
	}

	public void loadProperties(){
		this.loadNames();
	}

	public void loadIcons(){
		this.loadImages();
	}
}
