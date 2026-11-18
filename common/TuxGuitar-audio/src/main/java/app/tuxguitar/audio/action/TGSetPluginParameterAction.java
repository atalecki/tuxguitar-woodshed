package app.tuxguitar.audio.action;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.audio.TGLivePlugins;
import app.tuxguitar.audio.TGSoundChain;
import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.vst3.TGVst3Element;

/**
 * Sets a parameter of a VST3 plugin of a channel's sound chain, by its plain value: of an effect (by its number), or,
 * without one, of the VST3 instrument that is the source, or of the default instrument played as the source while
 * TuxGuitar's parameter editor is open for it: the first value set there that it does not hold keeps it in the song with
 * that value, as the first change in a plugin's own editor does (TGLivePlugins.keepDefault), one change of the song.
 * Refused when that is not a VST3 plugin, or nothing is kept, or when TuxGuitar's parameter editor shows that value
 * already (TGLivePlugins.shows): nothing changes, so no undo step is added.
 */
public class TGSetPluginParameterAction extends TGSoundChainAction {

	public static final String NAME = "action.audio.sound-chain.set-plugin-parameter";

	// the parameter's id (Integer) and plain value (Double)
	public static final String ATTRIBUTE_PARAMETER = "pluginParameter";
	public static final String ATTRIBUTE_VALUE = "pluginParameterValue";

	public TGSetPluginParameterAction(TGContext context) {
		super(context, NAME);
	}

	protected boolean change(TGActionContext context, TGChannel channel, TGProgram chain) {
		Integer effect = context.getAttribute(TGEffectAction.ATTRIBUTE_EFFECT);
		int parameter = context.<Integer>getAttribute(ATTRIBUTE_PARAMETER);
		double value = context.<Double>getAttribute(ATTRIBUTE_VALUE);
		if (TGLivePlugins.getInstance(getContext()).shows(channel, effect, parameter, value)) {
			return false;
		}
		TGProgramElement plugin = TGSoundChain.plugin(chain, effect);
		if (plugin == null && effect == null) {
			plugin = this.keepDefault(channel, chain, parameter, value);
		}
		if (plugin == null) {
			return false;
		}
		TGVst3Element.setParameter(plugin, parameter, value);
		return true;
	}

	// the default instrument played as the source, kept in the song as the chain's source when the value changes it;
	// else null
	private TGProgramElement keepDefault(TGChannel channel, TGProgram chain, int parameter, double value) {
		TGProgramElement kept = TGLivePlugins.getInstance(getContext()).keepDefault(channel, parameter, value);
		if (kept != null) {
			chain.setReceiver(kept);
		}
		return kept;
	}
}
