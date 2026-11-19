package app.tuxguitar.audio;

import static app.tuxguitar.editor.action.SongActionFixture.reads;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * The Track panel's chain list: the elements in the order the sound flows, each by its row, an arrow leading into each
 * after the source.
 */
class TestSoundChainStrip {

	// each link of the chain list: "<row>:<text>", "-> " before it when an arrow leads into it
	private static String links(String... elements) {
		return TGSoundChainStrip.links(Arrays.asList(elements)).stream().map(link -> (link.isArrowed() ? "-> " : "") + link.getRow() + ":" + link.getText())
			.collect(Collectors.joining(" "));
	}

	@TestFactory
	Stream<DynamicTest> anArrowLeadsIntoEachElementAfterTheSource() {
		return Stream.of(
			reads("a source and two effects", "0:Source: drone -> 1:Test Gain -> 2:[echo]", () -> links("Source: drone", "Test Gain", "[echo]")),
			reads("a source alone", "0:Source: takes", () -> links("Source: takes")),
			reads("no track", "", () -> links()));
	}
}
