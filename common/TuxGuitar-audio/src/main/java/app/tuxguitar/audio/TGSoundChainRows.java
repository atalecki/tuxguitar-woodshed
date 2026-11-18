package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.midi.synth.impl.GervillProcessorFactory;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * What an app shows of a channel's sound chain to edit it, read at one moment: a row per element (the source, then each
 * effect in processing order by its number, a disabled one in brackets) and, for the row picked (-1: none), what can be
 * done with it: an effect is removed, enabled or disabled, moved up unless it is the first; a VST3 plugin's editor
 * opens. A VST3 effect is added, a VST3 instrument or the default instrument of the channel's kind made the source,
 * except on an audio channel, whose source is its takes. A default source's row says what plays and why
 * (TGDefaultPlay).
 */
public final class TGSoundChainRows {

	// what an app's sound chain editor can be asked to do, each enabled or greyed for the row picked (the one place that
	// decides it): add the VST3 effect picked, make the VST3 instrument picked or the default instrument the source, keep
	// the default instrument in the song, move the picked effect up, remove it, enable or disable it, open its editor;
	// the Sound Chain panel opens the full dialog on its track
	public enum Control {
		ADD_EFFECT((rows, row, effect, instrument) -> rows.canAdd(effect)),
		USE_AS_SOURCE((rows, row, effect, instrument) -> rows.canChoose(instrument)),
		DEFAULT_INSTRUMENT((rows, row, effect, instrument) -> rows.canChooseSource()),
		KEEP((rows, row, effect, instrument) -> rows.canKeep()),
		MOVE_UP((rows, row, effect, instrument) -> rows.canMoveUp(row)),
		REMOVE((rows, row, effect, instrument) -> rows.isEffect(row)),
		TOGGLE((rows, row, effect, instrument) -> rows.isEffect(row)),
		EDIT((rows, row, effect, instrument) -> rows.canEdit(row)),
		OPEN_DIALOG((rows, row, effect, instrument) -> true);

		private final Rule rule;

		Control(Rule rule) {
			this.rule = rule;
		}
	}

	private interface Rule {
		boolean allows(TGSoundChainRows rows, int row, TGVst3Plugin effect, TGVst3Plugin instrument);
	}

	// no track (no song shown yet): no rows, every control greyed; nothing else is asked of it
	public static final TGSoundChainRows NONE = new TGSoundChainRows(null, null, null);

	// whether the control is enabled for the row picked (-1: none), the plugins picked in the app's drop-downs given
	// (null: none)
	public boolean isEnabled(Control control, int row, TGVst3Plugin effect, TGVst3Plugin instrument) {
		return this.chain != null && control.rule.allows(this, row, effect, instrument);
	}

	private final TGChannel channel;
	private final TGProgram chain;
	// what the channel plays without an instrument of its own, or null when that is not known
	private final TGDefaultPlay play;

	private TGSoundChainRows(TGChannel channel, TGProgram chain, TGDefaultPlay play) {
		this.channel = channel;
		this.chain = chain;
		this.play = play;
	}

	public static TGSoundChainRows of(TGChannel channel) {
		return of(channel, null);
	}

	// with what the channel plays as its default source (TGDefaultInstrumentSource.play), read at the same moment
	public static TGSoundChainRows of(TGChannel channel, TGDefaultPlay play) {
		return new TGSoundChainRows(channel, TGSoundChain.get(channel), play);
	}

	// source: what the app calls the source
	public List<String> getRows(String source) {
		List<String> rows = new ArrayList<String>();
		if (this.chain == null) {
			return rows;
		}
		rows.add(source + ": " + this.source());
		for (int i = 0; i < this.chain.countOutputs(); i++) {
			rows.add((i + 1) + ". " + TGSoundChain.effect(this.chain.getOutput(i), this.channel));
		}
		return rows;
	}

	// the source as the row names it: a default source says what plays and why, when that is known
	private String source() {
		String name = TGSoundChain.name(this.chain.getReceiver(), this.channel);
		return (this.play != null && this.isDefault() ? this.play.say(name) : name);
	}

	// whether the source is the default instrument of the channel's kind (stored as General MIDI)
	public boolean isDefault() {
		return GervillProcessorFactory.TYPE.equals(this.chain.getReceiver().getType());
	}

	// whether the default instrument plays as the source, so it can be kept in the song (TGKeepDefaultInstrumentAction)
	public boolean canKeep() {
		return (this.isDefault() && this.play != null && this.play.getElement() != null);
	}

	// the row picked: its index, or -1 for none
	public static int row(Integer picked) {
		return (picked != null ? picked : -1);
	}

	// the effect's number a row is (the plugin actions' effect), or null for the source
	public static Integer effect(int row) {
		return (row > 0 ? row : null);
	}

	public boolean isEffect(int row) {
		return row > 0;
	}

	public boolean canMoveUp(int row) {
		return row > 1;
	}

	public boolean canEdit(int row) {
		return row >= 0 && TGSoundChain.plugin(this.chain, effect(row)) != null;
	}

	// the source is always enabled
	public boolean isEnabled(int row) {
		return !this.isEffect(row) || this.chain.getOutput(row - 1).isEnabled();
	}

	// the message key of the enable/disable toggle for the row: what it would do
	public String toggleKey(int row) {
		return (this.isEnabled(row) ? "audio.sound-chain.disable" : "audio.sound-chain.enable");
	}

	public boolean canChooseSource() {
		return !this.channel.isAudioChannel();
	}

	// a plugin picked is added as an effect when it is one
	public boolean canAdd(TGVst3Plugin effect) {
		return effect != null && !effect.isInstrument();
	}

	public boolean canChoose(TGVst3Plugin instrument) {
		return instrument != null && instrument.isInstrument() && this.canChooseSource();
	}

	public static List<TGVst3Plugin> effects(List<TGVst3Plugin> plugins) {
		return plugins.stream().filter(plugin -> !plugin.isInstrument()).collect(Collectors.toList());
	}

	public static List<TGVst3Plugin> instruments(List<TGVst3Plugin> plugins) {
		return plugins.stream().filter(TGVst3Plugin::isInstrument).collect(Collectors.toList());
	}
}
