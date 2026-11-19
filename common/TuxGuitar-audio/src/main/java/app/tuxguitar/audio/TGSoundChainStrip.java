package app.tuxguitar.audio;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The Track panel's chain list: each element of the chain is a link (links), in the order the sound flows: its row
 * (the panel picks it by it), its text, and whether an arrow leads into it from the one before (every one after the
 * source).
 */
public final class TGSoundChainStrip {

	private TGSoundChainStrip() {
	}

	// the chain list's links, one per element in the order the sound flows (TGSoundChainRows.getElements): an arrow
	// leads into each after the first
	public static List<Link> links(List<String> elements) {
		return IntStream.range(0, elements.size()).mapToObj(row -> new Link(row, elements.get(row))).collect(Collectors.toList());
	}

	// an element of the chain list: its row, its text, whether an arrow leads into it
	public static final class Link {

		private final int row;
		private final String text;

		Link(int row, String text) {
			this.row = row;
			this.text = text;
		}

		public int getRow() {
			return this.row;
		}

		public String getText() {
			return this.text;
		}

		public boolean isArrowed() {
			return this.row > 0;
		}
	}
}
