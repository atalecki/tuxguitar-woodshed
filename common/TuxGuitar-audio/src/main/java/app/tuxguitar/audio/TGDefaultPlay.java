package app.tuxguitar.audio;

import java.util.Locale;

import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.util.TGMessagesManager;

/**
 * What a channel without an instrument of its own plays, and why, as TGDefaultInstrumentSource decides it: the default
 * instrument of its kind, found in the scan and not known to fail; else General MIDI, because its kind has no default,
 * the scan is not done yet, the scan did not find the default, or the default could not be loaded playing this song.
 * Said in TuxGuitar's language, as the Sound Chain dialog's source row reads it.
 */
public final class TGDefaultPlay {

	public enum Is {
		// the default instrument plays
		PLAYED("audio.default-instrument.source.played"),
		// General MIDI plays, because...
		NO_DEFAULT("audio.default-instrument.source.no-default"), NOT_SCANNED("audio.default-instrument.source.not-scanned"),
		NOT_INSTALLED("audio.default-instrument.source.not-installed"), NOT_LOADED("audio.default-instrument.source.not-loaded");

		// "{0} (...{1}...{2})": {0} what plays (the instrument, or General MIDI's program), {1} the kind's tracks, {2} the
		// default instrument
		private final String message;

		Is(String message) {
			this.message = message;
		}
	}

	private final TGInstrumentKind kind;
	private final TGDefaultInstrument instrument;
	private final TGProgramElement element;
	private final Is is;

	TGDefaultPlay(TGInstrumentKind kind, TGDefaultInstrument instrument, TGProgramElement element, Is is) {
		this.kind = kind;
		this.instrument = instrument;
		this.element = element;
		this.is = is;
	}

	public Is getIs() {
		return this.is;
	}

	TGInstrumentKind getKind() {
		return this.kind;
	}

	// the kind's default; null when it has none
	TGDefaultInstrument getInstrument() {
		return this.instrument;
	}

	// the default instrument's element when it plays, else null (General MIDI)
	public TGProgramElement getElement() {
		return (this.is == Is.PLAYED ? this.element : null);
	}

	// "Woodshed Guitar (default for guitar tracks)", "General MIDI, Steel String Acoustic Guitar (no default for guitar
	// tracks)": program, General MIDI's program the channel plays otherwise
	public String say(String program) {
		String tracks = TGMessagesManager.getProperty("audio.default-instrument.kind." + this.kind.name().toLowerCase(Locale.ROOT));
		String name = (this.instrument != null ? this.instrument.getName() : "");
		return TGMessagesManager.getProperty(this.is.message, new Object[] {(this.is == Is.PLAYED ? name : program), tracks, name});
	}
}
