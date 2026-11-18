package app.tuxguitar.audio.action;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionException;
import app.tuxguitar.audio.TGDefaultInstrumentSource;
import app.tuxguitar.audio.TGLivePlugins;
import app.tuxguitar.audio.TGSoundChain;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.TGMessagesManager;
import app.tuxguitar.vst3.TGEditorTarget;
import app.tuxguitar.vst3.TGVst3Element;
import app.tuxguitar.vst3.TGVst3Exception;

/**
 * Opens the editor of a VST3 plugin of the context's channel's sound chain: an effect by its number
 * (TGEffectAction.ATTRIBUTE_EFFECT), or, without one, the VST3 instrument that is the source, or the default instrument
 * the channel plays as its source (TGDefaultInstrumentSource.edited: the first change made there keeps it in the song,
 * see TGLivePlugins). It changes nothing in the song. Refused, saying why in TuxGuitar's language (a TGActionException, whose message an app shows), when that is no
 * VST3 plugin (NO_EDITOR), or the plugin or its editor cannot be opened (TGLivePlugins): the message names it. The plugin's own editor goes where the
 * target given says (ATTRIBUTE_TARGET: floating in a window TuxGuitar's main window owns, or inside an area of that
 * window), or, without one, in a window of its own; its caption names the plugin and the context's track
 * (TGDocumentContextAttributes.ATTRIBUTE_TRACK), when there is one.
 */
public class TGOpenPluginEditorAction extends TGActionBase {

	public static final String NAME = "action.audio.sound-chain.open-plugin-editor";
	// where the plugin's own editor goes (a TGEditorTarget); without one, in a window of its own that no window owns
	public static final String ATTRIBUTE_TARGET = "editorTarget";
	// the refusal of what is no VST3 plugin: {0} its name
	private static final String NO_EDITOR = "audio.plugin.refusal.no-editor";

	public TGOpenPluginEditorAction(TGContext context) {
		super(context, NAME);
	}

	protected void processAction(TGActionContext context) {
		TGChannel channel = context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_CHANNEL);
		Integer effect = context.getAttribute(TGEffectAction.ATTRIBUTE_EFFECT);
		TGProgramElement plugin = TGDefaultInstrumentSource.install(getContext()).edited(channel, effect);
		if (plugin == null) {
			throw new TGActionException(TGMessagesManager.getProperty(NO_EDITOR, new Object[] {TGSoundChain.name(TGSoundChain.get(channel), effect, channel)}));
		}
		String title = title(plugin, context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK));
		try {
			TGLivePlugins.getInstance(getContext()).openEditor(channel.getChannelId(), plugin.getId(), target(context), title);
			context.setAttribute(ATTRIBUTE_SUCCESS, Boolean.TRUE);
		} catch (TGVst3Exception e) {
			throw new TGActionException(e.getMessage(), e);
		}
	}

	// where the context says the plugin's own editor goes; floating, owned by none, when it says nothing
	public static TGEditorTarget target(TGActionContext context) {
		TGEditorTarget target = context.getAttribute(ATTRIBUTE_TARGET);
		return (target != null ? target : TGEditorTarget.floating(0));
	}

	// the caption of the editor's window: the plugin's name, and the track's when the editor is opened for one
	private static String title(TGProgramElement plugin, TGTrack track) {
		return TGVst3Element.getName(plugin) + (track != null ? " – " + track.getName() : "");
	}
}
