package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.util.TGMessagesManager;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * What the settings offer to choose the default instruments (TGDefaultInstruments) from, and what the user chose, from
 * the defaults in use and the VST3 instruments the last scan found. Each kind of track chooses General MIDI or one of
 * those instruments (the one in use, when the scan did not find it, stays offered as "<name> (not installed)", so
 * opening the settings never loses it); guitar and bass, while Woodshed Guitar is their choice, a profile: a .nam file,
 * or none, the one TuxGuitar ships for the kind. Only what can be played is offered: no other instrument, and no
 * profile for another one; nothing while the MIDI Port chosen is not the TuxGuitar Synthesizer, the only one they play
 * on, and a line says why. One writer (the app's UI thread).
 */
public final class TGDefaultInstrumentChoices {

	private static final String GENERAL_MIDI = "audio.default-instrument.general-midi";
	private static final String NOT_INSTALLED = "audio.default-instrument.not-installed-choice";
	private static final String SHIPPED = "audio.default-instrument.shipped-profile";
	private static final String SYNTHESIZER_ONLY = "audio.default-instrument.synthesizer-only";

	private final TGDefaultInstruments current;
	private final List<TGVst3Plugin> scanned;
	private TGDefaultInstruments chosen;
	// the MIDI Port chosen is the TuxGuitar Synthesizer's
	private boolean onSynthesizer = true;

	public TGDefaultInstrumentChoices(TGDefaultInstruments current, List<TGVst3Plugin> scanned) {
		this.current = current;
		this.scanned = scanned;
		this.chosen = current;
	}

	// one choice of a kind: General MIDI (no instrument), or an instrument, as the user reads it; the same choice as
	// another of the same instrument (class id)
	public static final class Choice {
		private final TGDefaultInstrument instrument;
		private final String label;

		Choice(TGDefaultInstrument instrument, String label) {
			this.instrument = instrument;
			this.label = label;
		}

		public String getLabel() {
			return this.label;
		}

		private String classId() {
			return (this.instrument != null ? this.instrument.getClassId() : "");
		}

		public boolean equals(Object other) {
			return (other instanceof Choice && ((Choice) other).classId().equals(this.classId()));
		}

		public int hashCode() {
			return this.classId().hashCode();
		}
	}

	// General MIDI, the instruments the scan found in its order, then the one in use for the kind when the scan did not
	// find it
	public List<Choice> choices(TGInstrumentKind kind) {
		List<Choice> choices = new ArrayList<Choice>();
		choices.add(new Choice(null, TGMessagesManager.getProperty(GENERAL_MIDI)));
		this.scanned.stream().filter(TGVst3Plugin::isInstrument).forEach(plugin -> choices.add(found(plugin)));
		this.current.get(kind).map(TGDefaultInstrumentChoices::notInstalled).filter(choice -> !choices.contains(choice)).ifPresent(choices::add);
		return choices;
	}

	private static Choice found(TGVst3Plugin plugin) {
		return new Choice(new TGDefaultInstrument(plugin.getClassId().toUpperCase(Locale.ROOT), plugin.getName()), plugin.getName());
	}

	private static Choice notInstalled(TGDefaultInstrument instrument) {
		return new Choice(instrument, TGMessagesManager.getProperty(NOT_INSTALLED, new Object[] {instrument.getName()}));
	}

	public Choice chosen(TGInstrumentKind kind) {
		return this.chosen.get(kind).map(instrument -> new Choice(instrument, instrument.getName())).orElse(new Choice(null, TGMessagesManager.getProperty(GENERAL_MIDI)));
	}

	public void choose(TGInstrumentKind kind, Choice choice) {
		this.chosen = (choice.instrument != null ? this.chosen.with(kind, choice.instrument) : this.chosen.withGeneralMidi(kind));
	}

	// guitar and bass have a profile
	public static boolean hasProfile(TGInstrumentKind kind) {
		return TGDefaultInstruments.hasProfile(kind);
	}

	// whether the kind's choice plays a profile: Woodshed Guitar for guitar or bass
	public boolean takesProfile(TGInstrumentKind kind) {
		return (hasProfile(kind) && this.chosen.get(kind).filter(TGDefaultInstruments.WOODSHED_GUITAR::equals).isPresent());
	}

	// the kind's profile as the user reads it: the file chosen, else the one TuxGuitar ships
	public String profile(TGInstrumentKind kind) {
		return this.chosen.getProfile(kind).orElse(TGMessagesManager.getProperty(SHIPPED, new Object[] {TGDefaultInstrumentSource.shipped(kind).replaceFirst("\\.nam$", "")}));
	}

	public boolean isShipped(TGInstrumentKind kind) {
		return !this.chosen.getProfile(kind).isPresent();
	}

	// a .nam file (its path), or null: the one TuxGuitar ships
	public void chooseProfile(TGInstrumentKind kind, String path) {
		this.chosen = this.chosen.withProfile(kind, path);
	}

	public TGDefaultInstruments getChosen() {
		return this.chosen;
	}

	// the MIDI Port chosen on the page (its key; null for none): the defaults are chosen only while it is the TuxGuitar
	// Synthesizer's, the only one they play on
	public void choosePort(String portKey) {
		this.onSynthesizer = TGAudioSession.SYNTHESIZER_PORT_KEY.equals(portKey);
	}

	// whether anything can be chosen: until a port is told, it can
	public boolean isEditable() {
		return this.onSynthesizer;
	}

	// why nothing can be chosen; "" while it can
	public String getNotice() {
		return (this.onSynthesizer ? "" : TGMessagesManager.getProperty(SYNTHESIZER_ONLY));
	}

	// a file can be chosen as the kind's profile: Woodshed Guitar is its choice and the defaults can be chosen
	public boolean canChooseProfile(TGInstrumentKind kind) {
		return (this.onSynthesizer && this.takesProfile(kind));
	}

	// the kind can go back to the profile TuxGuitar ships: a file can be chosen, and one is
	public boolean canGoBackToShipped(TGInstrumentKind kind) {
		return (this.canChooseProfile(kind) && !this.isShipped(kind));
	}
}
