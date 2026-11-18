package app.tuxguitar.audio;

import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.function.Supplier;

import app.tuxguitar.audio.action.TGAddVst3EffectAction;
import app.tuxguitar.audio.action.TGChooseGeneralMidiAction;
import app.tuxguitar.audio.action.TGChooseVst3InstrumentAction;
import app.tuxguitar.audio.action.TGEffectAction;
import app.tuxguitar.audio.action.TGKeepDefaultInstrumentAction;
import app.tuxguitar.audio.action.TGMoveEffectUpAction;
import app.tuxguitar.audio.action.TGOpenPluginEditorAction;
import app.tuxguitar.audio.action.TGRemoveEffectAction;
import app.tuxguitar.audio.action.TGSetEffectEnabledAction;
import app.tuxguitar.audio.action.TGSetPluginParameterAction;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.editor.action.TGActionProcessor;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.vst3.TGVst3Element;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * What a sound chain editor (the Sound Chain dialog, the Sound Chain panel) asks of a track's sound chain: each a sound
 * chain action, run as the app runs actions (TGActionProcessor: on a thread of its own), on the channel the track has in
 * the song at that moment (the track asked for then; an undo may have replaced its channel), a row being the editor's
 * (TGSoundChainRows: 0 the source, then each effect). Once an action is done the editor is told (finished, on the
 * action's thread), so it reads the chain again; a refused one tells nothing (the app shows why). Edit opens the row's
 * plugin's own editor where the placement says, or TuxGuitar's parameter editor, then handed to the editor (to show
 * TuxGuitar's, or follow what becomes of the plugin's own), as the action opens it.
 */
public final class TGSoundChainCommands {

	private final TGContext context;
	private final Supplier<TGTrack> track;
	private final Runnable finished;

	public TGSoundChainCommands(TGContext context, Supplier<TGTrack> track, Runnable finished) {
		this.context = context;
		this.track = track;
		this.finished = finished;
	}

	public void addEffect(TGVst3Plugin effect) {
		this.run(this.withPlugin(TGAddVst3EffectAction.NAME, effect));
	}

	public void useAsSource(TGVst3Plugin instrument) {
		this.run(this.withPlugin(TGChooseVst3InstrumentAction.NAME, instrument));
	}

	// the default instrument of the track's kind becomes the source
	public void useDefault() {
		this.run(new TGActionProcessor(this.context, TGChooseGeneralMidiAction.NAME));
	}

	// the default instrument that plays is kept in the song as the track's own
	public void keep() {
		this.run(new TGActionProcessor(this.context, TGKeepDefaultInstrumentAction.NAME));
	}

	public void moveUp(int row) {
		this.run(this.onRow(TGMoveEffectUpAction.NAME, row));
	}

	public void remove(int row) {
		this.run(this.onRow(TGRemoveEffectAction.NAME, row));
	}

	// the row's effect enabled when it is disabled, else disabled
	public void toggle(int row) {
		TGActionProcessor processor = this.onRow(TGSetEffectEnabledAction.NAME, row);
		processor.setAttribute(TGSetEffectEnabledAction.ATTRIBUTE_ENABLED, !TGSoundChainRows.of(this.channel()).isEnabled(row));
		this.run(processor);
	}

	// the row's plugin's own editor opens where the placement says, its caption naming the track; or else TuxGuitar's
	// parameter editor; either is handed to opened once open, by the action (the app may run it later, on its UI thread)
	public void edit(int row, TGEditorPlacement placement, Consumer<TGPluginEditor> opened) {
		TGActionProcessor processor = this.onRow(TGOpenPluginEditorAction.NAME, row);
		placement.place(processor);
		processor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK, this.track.get());
		processor.setAttribute(TGOpenPluginEditorAction.ATTRIBUTE_OPENED, opened);
		this.run(processor);
	}

	// the row's plugin's parameter set to the plain value, one undoable change of the song: TuxGuitar's parameter rows
	// (the panel's, the Parameters window's); a default instrument played as the source, its parameter editor open, is
	// kept in the song by the first value it does not hold
	public void set(int row, int parameter, double plain) {
		TGActionProcessor processor = this.onRow(TGSetPluginParameterAction.NAME, row);
		processor.setAttribute(TGSetPluginParameterAction.ATTRIBUTE_PARAMETER, parameter);
		processor.setAttribute(TGSetPluginParameterAction.ATTRIBUTE_VALUE, plain);
		this.run(processor);
	}

	private TGActionProcessor withPlugin(String action, TGVst3Plugin plugin) {
		TGActionProcessor processor = new TGActionProcessor(this.context, action);
		processor.setAttribute(TGAddVst3EffectAction.ATTRIBUTE_PLUGIN, plugin);
		return processor;
	}

	// an action on the row's effect (the source: none)
	private TGActionProcessor onRow(String action, int row) {
		TGActionProcessor processor = new TGActionProcessor(this.context, action);
		processor.setAttribute(TGEffectAction.ATTRIBUTE_EFFECT, TGSoundChainRows.effect(row));
		return processor;
	}

	// the plugin of the row as the Sound Chain panel's editor area knows it (TGEditorDock.Element), or null when the row's
	// is no VST3 plugin (General MIDI without a default instrument, an audio track's takes), no row is picked (-1) or no
	// track is shown
	public TGEditorDock.Element element(int row) {
		if (row < 0 || this.track.get() == null) {
			return null;
		}
		TGChannel channel = this.channel();
		TGProgramElement plugin = TGDefaultInstrumentSource.install(this.context).edited(channel, TGSoundChainRows.effect(row));
		return (plugin != null ? new TGEditorDock.Element(channel.getChannelId(), plugin.getId(), TGVst3Element.getClassId(plugin), TGVst3Element.getName(plugin), row) : null);
	}

	// the row of the element of the channel's chain when the track has that channel (a plugin asking for its editor),
	// else -1
	public int row(int channel, String id) {
		TGTrack shown = this.track.get();
		int rows = (shown != null && shown.getChannelId() == channel ? TGSoundChain.get(this.channel()).countOutputs() + 1 : 0);
		return IntStream.range(0, rows).filter(row -> this.names(row, id)).findFirst().orElse(-1);
	}

	private boolean names(int row, String id) {
		TGEditorDock.Element element = this.element(row);
		return (element != null && element.getId().equals(id));
	}

	// the track's channel as the song has it now
	private TGChannel channel() {
		TGDocumentManager document = TGDocumentManager.getInstance(this.context);
		return document.getSongManager().getChannel(document.getSong(), this.track.get().getChannelId());
	}

	private void run(TGActionProcessor processor) {
		this.run(processor, this.finished);
	}

	private void run(TGActionProcessor processor, Runnable done) {
		processor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_CHANNEL, this.channel());
		processor.setOnFinish(done);
		processor.process();
	}
}
