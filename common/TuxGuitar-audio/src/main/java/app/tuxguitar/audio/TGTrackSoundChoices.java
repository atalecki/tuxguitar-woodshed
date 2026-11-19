package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import app.tuxguitar.midi.synth.TGDefaultSource;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.player.base.MidiInstrument;
import app.tuxguitar.util.TGMessagesManager;
import app.tuxguitar.vst3.TGVst3Element;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * What the Track panel offers in its drop-downs, and which is picked. On VST3: the default instrument of the track's
 * kind first (no plugin: the panel names it by what it plays), then each VST3 instrument the scan found, then the
 * track's own instrument when the scan did not find it (so the one picked shows). On MIDI: General MIDI's 128 programs
 * in TuxGuitar's language, a percussion channel's numbered (as the channel settings name them). A name too long for the
 * panel (as measured there) is cut, its last character "…".
 */
public final class TGTrackSoundChoices {

	private static final int PROGRAMS = 128;
	private static final String ELLIPSIS = "…";

	private TGTrackSoundChoices() {
	}

	// the VST3 instrument drop-down for the source: null (the default) first
	public static List<TGVst3Plugin> instruments(TGProgramElement source, List<TGVst3Plugin> scanned) {
		List<TGVst3Plugin> choices = new ArrayList<TGVst3Plugin>();
		choices.add(null);
		choices.addAll(scanned);
		TGVst3Plugin own = own(source);
		if (own != null && find(choices, own) == -1) {
			choices.add(own);
		}
		return choices;
	}

	// the choice picked for the source: the default (0) for a default source, the instrument of its class id for a VST3
	// instrument; -1 for none (MIDI, another source)
	public static int picked(TGProgramElement source, List<TGVst3Plugin> choices) {
		if (TGDefaultSource.givesWay(source)) {
			return 0;
		}
		TGVst3Plugin own = own(source);
		return (own != null ? find(choices, own) : -1);
	}

	// the source's VST3 instrument, or null
	private static TGVst3Plugin own(TGProgramElement source) {
		return (TGVst3Element.isVst3(source) ? TGVst3Element.plugin(source.getParameters(), true) : null);
	}

	private static int find(List<TGVst3Plugin> choices, TGVst3Plugin own) {
		return IntStream.range(1, choices.size()).filter(choice -> choices.get(choice).getClassId().equalsIgnoreCase(own.getClassId())).findFirst().orElse(-1);
	}

	// General MIDI's programs by name in TuxGuitar's language; numbered after the prefix ("Program #0") on a percussion
	// channel, or when the language names fewer
	public static List<String> programs(boolean percussion, String prefix) {
		MidiInstrument[] named = TGMessagesManager.getInstruments();
		if (percussion || named == null || named.length < PROGRAMS) {
			return IntStream.range(0, PROGRAMS).mapToObj(program -> prefix + " #" + program).collect(Collectors.toList());
		}
		return IntStream.range(0, PROGRAMS).mapToObj(program -> named[program].getName()).collect(Collectors.toList());
	}

	// the text as it is when it fits (the panel measures), else the longest start of it that fits with "…" after it
	public static String shorten(String text, Predicate<String> fits) {
		if (fits.test(text)) {
			return text;
		}
		int fitting = 0;
		int over = text.length();
		while (over - fitting > 1) {
			int middle = (fitting + over) / 2;
			if (fits.test(cut(text, middle))) {
				fitting = middle;
			} else {
				over = middle;
			}
		}
		return cut(text, fitting);
	}

	// the start of the text, its length given, then "…" (no space before it)
	private static String cut(String text, int length) {
		return text.substring(0, length).replaceFirst("\\s+$", "") + ELLIPSIS;
	}
}
