package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import app.tuxguitar.audio.TGEditorDock.Element;
import app.tuxguitar.vst3.TGVst3EditorOpened.Kind;

/**
 * The Sound Chain panel's editor area as a state machine: for each event, what the area does (the step: the view it
 * closes, the editor it opens and where) and what it then shows, with the line it says instead of an editor.
 */
public class TestEditorDock {

	private static final Element GAIN = new Element(1, "effect-1", "GAIN", "Test Gain");
	private static final Element WOODSHED = new Element(1, "source", "WOODSHED", "Woodshed Guitar");
	private static final Element OTHER_TRACK = new Element(2, "source", "WOODSHED", "Woodshed Guitar");

	private TGEditorDock dock = new TGEditorDock(Collections.<String>emptySet(), false);
	private final List<String> seen = new ArrayList<String>();

	// the step of each event, then what the area shows: "step / shows line"
	private TestEditorDock does(Supplier<TGEditorDock.Step> event) {
		TGEditorDock.Step step = event.get();
		TGEditorDock.Line line = this.dock.line();
		this.seen.add(step + " / " + this.dock.shows() + (line != null ? " " + line : ""));
		return this;
	}

	private void sees(String... expected) {
		assertEquals(Arrays.asList(expected), this.seen);
	}

	// a chip picked: its plugin's editor opens inside the area; once the host put it there, the area shows it
	@Test
	public void aPickedPluginsEditorOpensInsideTheArea() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, Kind.EMBEDDED));
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR");
	}

	// another chip, or another track (its source picked): the view shown closes, the other's opens; the same chip again
	// changes nothing; a source that is no VST3 plugin shows nothing
	@Test
	public void anotherChipOrTrackClosesTheViewShownAndOpensItsOwn() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, Kind.EMBEDDED)).does(() -> dock.picked(GAIN)).does(() -> dock.picked(WOODSHED))
			.does(() -> dock.opened(WOODSHED, Kind.EMBEDDED)).does(() -> dock.picked(OTHER_TRACK)).does(() -> dock.picked(null));
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR", "nothing / OWN_EDITOR", "close Test Gain, open Woodshed Guitar inside / OPENING", "nothing / OWN_EDITOR",
			"close Woodshed Guitar, open Woodshed Guitar inside / OPENING", "close Woodshed Guitar / NOTHING");
	}

	// too large for the panel: it opens in a window of its own, and the area says so with Show, which brings that window
	// to the front; the window opening keeps the line
	@Test
	public void anEditorTooLargeForThePanelOpensInAWindowOfItsOwn() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, Kind.TOO_LARGE)).does(() -> dock.opened(GAIN, Kind.OWNED)).does(() -> dock.showWindow());
		sees("open Test Gain inside / OPENING", "open Test Gain in a window / TOO_LARGE audio.sound-chain.editor.too-large(Test Gain) [audio.sound-chain.editor.show]",
			"nothing / TOO_LARGE audio.sound-chain.editor.too-large(Test Gain) [audio.sound-chain.editor.show]",
			"open Test Gain in a window / TOO_LARGE audio.sound-chain.editor.too-large(Test Gain) [audio.sound-chain.editor.show]");
	}

	// a plugin without an editor of its own: TuxGuitar's parameters show in the area, and close as a view does
	@Test
	public void aPluginWithoutAnEditorOfItsOwnShowsItsParameters() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, Kind.NONE)).does(() -> dock.picked(WOODSHED));
		sees("open Test Gain inside / OPENING", "nothing / PARAMETERS", "close Test Gain, open Woodshed Guitar inside / OPENING");
	}

	// Hide editor gives the room back (the view closes) until Show editor, whatever is picked meanwhile; remembered
	@Test
	public void hideEditorClosesTheViewUntilShowEditor() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, Kind.EMBEDDED)).does(() -> dock.visibility(true, true)).does(() -> dock.picked(WOODSHED)).does(() -> dock.visibility(false, true));
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR", "close Test Gain / HIDDEN", "nothing / HIDDEN", "open Woodshed Guitar inside / OPENING");
		assertEquals(true, new TGEditorDock(Collections.<String>emptySet(), true).picked(GAIN).toString().equals("nothing"));
	}

	// the panel hidden (F8): the view closes; shown again, the picked plugin's editor opens again
	@Test
	public void thePanelHiddenClosesTheViewAndShownOpensItAgain() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, Kind.EMBEDDED)).does(() -> dock.visibility(false, false)).does(() -> dock.visibility(false, true));
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR", "close Test Gain / HIDDEN", "open Test Gain inside / OPENING");
	}

	// Open in window: the editor goes to a window of its own (the host closes the view inside), remembered for the
	// plugin; Put back in the panel forgets it and puts it inside again
	@Test
	public void openInWindowIsRememberedForThePluginUntilPutBack() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, Kind.EMBEDDED)).does(() -> dock.popOut());
		String pinned = dock.getPinned().toString();
		does(() -> dock.opened(GAIN, Kind.OWNED)).does(() -> dock.putBack());
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR", "open Test Gain in a window / OWN_WINDOW audio.sound-chain.editor.in-window(Test Gain) [audio.sound-chain.editor.put-back]",
			"nothing / OWN_WINDOW audio.sound-chain.editor.in-window(Test Gain) [audio.sound-chain.editor.put-back]", "open Test Gain inside / OPENING");
		assertEquals("[GAIN] []", pinned + " " + dock.getPinned());
	}

	// a plugin remembered in a window of its own opens there when picked
	@Test
	public void aPluginRememberedInAWindowOfItsOwnOpensThere() {
		this.dock = new TGEditorDock(Collections.singleton("GAIN"), false);
		does(() -> dock.picked(GAIN)).does(() -> dock.picked(WOODSHED));
		sees("open Test Gain in a window / OWN_WINDOW audio.sound-chain.editor.in-window(Test Gain) [audio.sound-chain.editor.put-back]", "open Woodshed Guitar inside / OPENING");
	}

	// the close box of a window it was put in puts the editor back in the panel; the close box of a too large one, or of
	// another plugin's window, changes nothing
	@Test
	public void theCloseBoxOfItsWindowPutsTheEditorBackInThePanel() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, Kind.EMBEDDED)).does(() -> dock.popOut()).does(() -> dock.floatingClosed(WOODSHED)).does(() -> dock.floatingClosed(GAIN))
			.does(() -> dock.opened(GAIN, Kind.TOO_LARGE)).does(() -> dock.floatingClosed(GAIN));
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR", "open Test Gain in a window / OWN_WINDOW audio.sound-chain.editor.in-window(Test Gain) [audio.sound-chain.editor.put-back]",
			"nothing / OWN_WINDOW audio.sound-chain.editor.in-window(Test Gain) [audio.sound-chain.editor.put-back]", "open Test Gain inside / OPENING",
			"open Test Gain in a window / TOO_LARGE audio.sound-chain.editor.too-large(Test Gain) [audio.sound-chain.editor.show]",
			"nothing / TOO_LARGE audio.sound-chain.editor.too-large(Test Gain) [audio.sound-chain.editor.show]");
		assertEquals("[]", dock.getPinned().toString());
	}

	// the Sound Chain dialog's Edit opened the plugin's editor in a window of its own (a plugin has one view at a time):
	// the area says so, with Put back in the panel; not remembered
	@Test
	public void anEditorOpenedElsewhereLeavesTheAreaSayingSo() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, Kind.EMBEDDED)).does(() -> dock.moved(WOODSHED)).does(() -> dock.moved(GAIN)).does(() -> dock.putBack());
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR", "nothing / OWN_EDITOR",
			"nothing / OWN_WINDOW audio.sound-chain.editor.in-window(Test Gain) [audio.sound-chain.editor.put-back]", "open Test Gain inside / OPENING");
		assertEquals("[]", dock.getPinned().toString());
	}

	// what the host did for an editor no longer wanted there (another chip picked, the editor hidden meanwhile): a view
	// inside, or TuxGuitar's parameters, close; a window of its own stays
	@Test
	public void anEditorOpenedWhenNoLongerWantedThereCloses() {
		does(() -> dock.picked(GAIN)).does(() -> dock.picked(WOODSHED)).does(() -> dock.opened(GAIN, Kind.EMBEDDED)).does(() -> dock.opened(GAIN, Kind.NONE))
			.does(() -> dock.opened(GAIN, Kind.OWNED)).does(() -> dock.visibility(true, true)).does(() -> dock.opened(WOODSHED, Kind.EMBEDDED));
		sees("open Test Gain inside / OPENING", "close Test Gain, open Woodshed Guitar inside / OPENING", "close Test Gain / OPENING", "close Test Gain / OPENING", "nothing / OPENING",
			"close Woodshed Guitar / HIDDEN", "close Woodshed Guitar / HIDDEN");
	}

	// the plugin stopped (it hung, or its host crashed) and was started again: the area says so, with Show editor, which
	// opens it again; stopped again, it stays silent; another plugin's stop changes nothing
	@Test
	public void aStoppedPluginIsToldWithShowEditorWhenStartedAgain() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, Kind.EMBEDDED)).does(() -> dock.stopped(WOODSHED, true, "", true)).does(() -> dock.stopped(GAIN, true, "", true))
			.does(() -> dock.again()).does(() -> dock.stopped(GAIN, false, "exit code 70", true)).does(() -> dock.stopped(GAIN, false, "exit code 70", false));
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR", "nothing / OWN_EDITOR",
			"nothing / STOPPED audio.sound-chain.editor.hung-restarted(Test Gain) [audio.sound-chain.editor.show-editor]", "open Test Gain inside / OPENING",
			"nothing / STOPPED audio.sound-chain.editor.stopped-restarted(Test Gain, exit code 70) [audio.sound-chain.editor.show-editor]",
			"nothing / STOPPED audio.sound-chain.editor.stopped-silent(Test Gain, exit code 70)");
	}

	// a plugin that hung and was not started again says it hung
	@Test
	public void aHungPluginNotStartedAgainSaysItStaysSilent() {
		does(() -> dock.picked(GAIN)).does(() -> dock.stopped(GAIN, true, "", false));
		sees("open Test Gain inside / OPENING", "nothing / STOPPED audio.sound-chain.editor.hung-silent(Test Gain)");
	}

	// its editor could not be opened (the plugin did not load): the area says why
	@Test
	public void anEditorThatCouldNotOpenSaysWhy() {
		does(() -> dock.picked(GAIN)).does(() -> dock.refused(WOODSHED, "other")).does(() -> dock.refused(GAIN, "Test Gain did not open: gone"));
		sees("open Test Gain inside / OPENING", "nothing / OPENING", "nothing / REFUSED Test Gain did not open: gone");
	}

	// a plugin asking to show its editor (VST3 requestOpenEditor): shown, it stays; the picked one stopped or in a window
	// of its own opens again where it goes; another, or the editor hidden, opens in a window of its own
	@Test
	public void aPluginAskingToShowItsEditorIsShownWhereItGoes() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, Kind.EMBEDDED)).does(() -> dock.requested(GAIN)).does(() -> dock.requested(WOODSHED))
			.does(() -> dock.stopped(GAIN, true, "", true)).does(() -> dock.requested(GAIN)).does(() -> dock.visibility(true, true)).does(() -> dock.requested(GAIN));
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR", "nothing / OWN_EDITOR", "open Woodshed Guitar in a window / OWN_EDITOR",
			"nothing / STOPPED audio.sound-chain.editor.hung-restarted(Test Gain) [audio.sound-chain.editor.show-editor]", "open Test Gain inside / OPENING", "close Test Gain / HIDDEN",
			"open Test Gain in a window / HIDDEN");
	}

	// what the area reads to execute a step and say its line
	@Test
	public void theAreaReadsAStepALineAndTheElementShown() {
		TGEditorDock.Step step = dock.picked(GAIN);
		dock.opened(GAIN, Kind.EMBEDDED);
		TGEditorDock.Step moved = dock.picked(WOODSHED);
		dock.opened(WOODSHED, Kind.TOO_LARGE);
		TGEditorDock.Line line = dock.line();
		Element shown = dock.element();
		assertEquals("null Test Gain INSIDE | Test Gain Woodshed Guitar INSIDE | audio.sound-chain.editor.too-large [Woodshed Guitar] audio.sound-chain.editor.show | 1 source WOODSHED false",
			step.getClose() + " " + step.getOpen() + " " + step.getHow() + " | " + moved.getClose() + " " + moved.getOpen() + " " + moved.getHow() + " | " + line.getKey() + " "
				+ Arrays.toString(line.getArguments()) + " " + line.getButton() + " | " + shown.getChannel() + " " + shown.getId() + " " + shown.getClassId() + " " + dock.isHidden());
	}

	@Test
	public void elementsAreEqualByChannelElementAndPlugin() {
		assertEquals("true false false false", GAIN.equals(new Element(1, "effect-1", "GAIN", "renamed")) + " " + GAIN.equals(new Element(2, "effect-1", "GAIN", "Test Gain")) + " "
			+ GAIN.equals(new Element(1, "effect-2", "GAIN", "Test Gain")) + " " + GAIN.equals(new Element(1, "effect-1", "CLIP", "Test Gain")));
		assertEquals(GAIN.hashCode(), new Element(1, "effect-1", "GAIN", "renamed").hashCode());
	}
}
