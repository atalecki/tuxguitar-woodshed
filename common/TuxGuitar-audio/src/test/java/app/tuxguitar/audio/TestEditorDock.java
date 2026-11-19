package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import app.tuxguitar.audio.TGEditorDock.Element;
import app.tuxguitar.vst3.TGVst3EditorOpened;
import app.tuxguitar.vst3.TGVst3EditorOpened.Kind;

/**
 * The Sound Chain panel's editor area as a state machine: for each event, what the area does (the step: the view it
 * closes, the editor it opens and where) and what it then shows, with the line it says instead of an editor.
 */
public class TestEditorDock {

	private static final Element GAIN = new Element(1, "effect-1", "GAIN", "Test Gain", 1);
	private static final Element WOODSHED = new Element(1, "source", "WOODSHED", "Woodshed Guitar", 0);
	private static final Element OTHER_TRACK = new Element(2, "source", "WOODSHED", "Woodshed Guitar", 0);

	private static final String INSIDE_GAIN = " audio.sound-chain.editor.inside(Test Gain) [audio.sound-chain.editor.open-in-window]";
	private static final String INSIDE_WOODSHED = " audio.sound-chain.editor.inside(Woodshed Guitar) [audio.sound-chain.editor.open-in-window]";

	private TGEditorDock dock = new TGEditorDock(Collections.<String>emptySet(), false);
	private final List<String> seen = new ArrayList<String>();

	// what the host did: an editor 200 by 100 pixels, in an area 800 pixels wide when it is inside
	private static TGVst3EditorOpened opened(Kind kind) {
		return TGVst3EditorOpened.of(kind, 200, 100, false, 800);
	}

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
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, opened(Kind.EMBEDDED)));
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR" + INSIDE_GAIN);
	}

	// another chip, or another track (its source picked): the view shown closes, the other's opens; the same chip again
	// changes nothing; a source that is no VST3 plugin shows nothing
	@Test
	public void anotherChipOrTrackClosesTheViewShownAndOpensItsOwn() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, opened(Kind.EMBEDDED))).does(() -> dock.picked(GAIN)).does(() -> dock.picked(WOODSHED))
			.does(() -> dock.opened(WOODSHED, opened(Kind.EMBEDDED))).does(() -> dock.picked(OTHER_TRACK)).does(() -> dock.picked(null));
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR" + INSIDE_GAIN, "nothing / OWN_EDITOR" + INSIDE_GAIN, "close Test Gain, open Woodshed Guitar inside / OPENING", "nothing / OWN_EDITOR" + INSIDE_WOODSHED,
			"close Woodshed Guitar, open Woodshed Guitar inside / OPENING", "close Woodshed Guitar / NOTHING");
	}

	// too large for the panel: it opens in a window of its own, and the area says so with Show, which brings that window
	// to the front; the window opening keeps the line
	@Test
	public void anEditorTooLargeForThePanelOpensInAWindowOfItsOwn() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, opened(Kind.TOO_LARGE))).does(() -> dock.opened(GAIN, opened(Kind.OWNED))).does(() -> dock.showWindow());
		sees("open Test Gain inside / OPENING", "open Test Gain in a window / TOO_LARGE audio.sound-chain.editor.too-large(Test Gain) [audio.sound-chain.editor.show]",
			"nothing / TOO_LARGE audio.sound-chain.editor.too-large(Test Gain) [audio.sound-chain.editor.show]",
			"open Test Gain in a window / TOO_LARGE audio.sound-chain.editor.too-large(Test Gain) [audio.sound-chain.editor.show]");
	}

	// the panel made narrower than the editor inside (fitted again, it is still wider than the area: it cannot shrink to
	// it): the view inside closes and it opens in a window of its own, the area saying so with Show, as for one too large
	// when it opened; an editor that shrank to the area's width stays, as does the one shown when another plugin's is
	// fitted; the panel made wider again leaves it in its window
	@Test
	public void anEditorStillWiderThanTheAreaOnceFittedGoesToAWindowOfItsOwn() {
		String tooLarge = " / TOO_LARGE audio.sound-chain.editor.too-large(Test Gain) [audio.sound-chain.editor.show]";
		TGVst3EditorOpened wider = TGVst3EditorOpened.of(Kind.EMBEDDED, 200, 100, false, 180);
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, opened(Kind.EMBEDDED))).does(() -> dock.fitted(GAIN, TGVst3EditorOpened.of(Kind.EMBEDDED, 280, 100, true, 280)))
			.does(() -> dock.fitted(WOODSHED, wider)).does(() -> dock.fitted(GAIN, wider)).does(() -> dock.opened(GAIN, opened(Kind.OWNED)))
			.does(() -> dock.fitted(GAIN, TGVst3EditorOpened.of(Kind.EMBEDDED, 200, 100, false, 900)));
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR" + INSIDE_GAIN, "nothing / OWN_EDITOR" + INSIDE_GAIN, "nothing / OWN_EDITOR" + INSIDE_GAIN,
			"close Test Gain, open Test Gain in a window" + tooLarge,
			"nothing" + tooLarge, "nothing" + tooLarge);
	}

	// Show on the too-large line: once the area is as wide as the editor (its width in the area's units: the pixels the
	// host said, over the area's pixels then, times its units then), the editor goes back inside, the host judging it
	// again (too large again, as for its height, its window opens again); narrower, or its width not known (the area's
	// never told), its window comes to the front. Here the screen scales 150 %: the area 300 units, 450 pixels; the editor
	// 600 pixels, so 400 units
	@Test
	public void showPutsAnEditorTooLargeBackInsideOnceTheAreaIsWideEnough() {
		String tooLarge = " / TOO_LARGE audio.sound-chain.editor.too-large(Test Gain) [audio.sound-chain.editor.show]";
		TGVst3EditorOpened large = TGVst3EditorOpened.of(Kind.TOO_LARGE, 600, 100, false, 450);
		does(() -> dock.press()).does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, large)).does(() -> dock.opened(GAIN, opened(Kind.OWNED))).does(() -> dock.press());
		dock.refits(300);
		does(() -> dock.picked(null)).does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, large)).does(() -> dock.opened(GAIN, opened(Kind.OWNED)));
		dock.refits(399);
		does(() -> dock.press());
		dock.refits(400);
		does(() -> dock.press()).does(() -> dock.opened(GAIN, large)).does(() -> dock.opened(GAIN, opened(Kind.OWNED)));
		dock.refits(600);
		does(() -> dock.press()).does(() -> dock.opened(GAIN, opened(Kind.EMBEDDED)));
		sees("nothing / NOTHING", "open Test Gain inside / OPENING", "open Test Gain in a window" + tooLarge, "nothing" + tooLarge, "open Test Gain in a window" + tooLarge,
			"nothing / NOTHING", "open Test Gain inside / OPENING", "open Test Gain in a window" + tooLarge, "nothing" + tooLarge,
			"open Test Gain in a window" + tooLarge,
			"open Test Gain inside / OPENING", "open Test Gain in a window" + tooLarge, "nothing" + tooLarge,
			"open Test Gain inside / OPENING", "nothing / OWN_EDITOR" + INSIDE_GAIN);
	}

	// an editor sent to its own window by the area made narrower: Show puts it back once the area is wide enough again
	@Test
	public void showPutsAnEditorTheNarrowerAreaSentToItsWindowBackOnceWideEnough() {
		dock.refits(300);
		dock.picked(GAIN);
		dock.opened(GAIN, opened(Kind.EMBEDDED));
		dock.refits(150);
		dock.fitted(GAIN, TGVst3EditorOpened.of(Kind.EMBEDDED, 200, 100, false, 150));
		does(() -> dock.press());
		dock.refits(200);
		does(() -> dock.press());
		sees("open Test Gain in a window / TOO_LARGE audio.sound-chain.editor.too-large(Test Gain) [audio.sound-chain.editor.show]", "open Test Gain inside / OPENING");
	}

	// a plugin without an editor of its own: TuxGuitar's parameters show in the area, and close as a view does
	@Test
	public void aPluginWithoutAnEditorOfItsOwnShowsItsParameters() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, opened(Kind.NONE))).does(() -> dock.picked(WOODSHED));
		sees("open Test Gain inside / OPENING", "nothing / PARAMETERS audio.sound-chain.editor.parameters(Test Gain)", "close Test Gain, open Woodshed Guitar inside / OPENING");
	}

	// Hide editor gives the room back (the view closes) until Show editor, whatever is picked meanwhile; remembered
	// the area shows the parameter rows of the plugin picked once its editor opened as TuxGuitar's: not of another, not of
	// one with its own editor, not once another is picked
	@Test
	public void theAreaShowsTheParameterRowsOfThePickedPluginWithoutAnEditor() {
		List<String> rows = new ArrayList<String>();
		dock.picked(GAIN);
		rows.add("opening " + dock.parameters(GAIN));
		dock.opened(GAIN, opened(Kind.NONE));
		rows.add("gain " + dock.parameters(GAIN) + " woodshed " + dock.parameters(WOODSHED));
		dock.picked(WOODSHED);
		dock.opened(WOODSHED, opened(Kind.EMBEDDED));
		rows.add("gain " + dock.parameters(GAIN) + " woodshed " + dock.parameters(WOODSHED));
		assertEquals("[opening false, gain true woodshed false, gain false woodshed false]", rows.toString());
	}

	@Test
	public void hideEditorClosesTheViewUntilShowEditor() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, opened(Kind.EMBEDDED))).does(() -> dock.visibility(true, true)).does(() -> dock.picked(WOODSHED)).does(() -> dock.visibility(false, true));
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR" + INSIDE_GAIN, "close Test Gain / HIDDEN", "nothing / HIDDEN", "open Woodshed Guitar inside / OPENING");
		assertEquals(true, new TGEditorDock(Collections.<String>emptySet(), true).picked(GAIN).toString().equals("nothing"));
	}

	// the panel hidden (F8): the view closes; shown again, the picked plugin's editor opens again
	@Test
	public void thePanelHiddenClosesTheViewAndShownOpensItAgain() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, opened(Kind.EMBEDDED))).does(() -> dock.visibility(false, false)).does(() -> dock.visibility(false, true));
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR" + INSIDE_GAIN, "close Test Gain / HIDDEN", "open Test Gain inside / OPENING");
	}

	// Open in window: the editor goes to a window of its own (the host closes the view inside), remembered for the
	// plugin; Put back in the panel forgets it and puts it inside again
	@Test
	public void openInWindowIsRememberedForThePluginUntilPutBack() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, opened(Kind.EMBEDDED))).does(() -> dock.popOut());
		String pinned = dock.getPinned().toString();
		does(() -> dock.opened(GAIN, opened(Kind.OWNED))).does(() -> dock.putBack());
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR" + INSIDE_GAIN, "open Test Gain in a window / OWN_WINDOW audio.sound-chain.editor.in-window(Test Gain) [audio.sound-chain.editor.put-back]",
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
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, opened(Kind.EMBEDDED))).does(() -> dock.popOut()).does(() -> dock.floatingClosed(WOODSHED)).does(() -> dock.floatingClosed(GAIN))
			.does(() -> dock.opened(GAIN, opened(Kind.TOO_LARGE))).does(() -> dock.floatingClosed(GAIN));
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR" + INSIDE_GAIN, "open Test Gain in a window / OWN_WINDOW audio.sound-chain.editor.in-window(Test Gain) [audio.sound-chain.editor.put-back]",
			"nothing / OWN_WINDOW audio.sound-chain.editor.in-window(Test Gain) [audio.sound-chain.editor.put-back]", "open Test Gain inside / OPENING",
			"open Test Gain in a window / TOO_LARGE audio.sound-chain.editor.too-large(Test Gain) [audio.sound-chain.editor.show]",
			"nothing / TOO_LARGE audio.sound-chain.editor.too-large(Test Gain) [audio.sound-chain.editor.show]");
		assertEquals("[]", dock.getPinned().toString());
	}

	// the Sound Chain dialog's Edit opened the plugin's editor in a window of its own (a plugin has one view at a time):
	// the area says so, with Put back in the panel; not remembered
	@Test
	public void anEditorOpenedElsewhereLeavesTheAreaSayingSo() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, opened(Kind.EMBEDDED))).does(() -> dock.moved(WOODSHED)).does(() -> dock.moved(GAIN)).does(() -> dock.putBack());
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR" + INSIDE_GAIN, "nothing / OWN_EDITOR" + INSIDE_GAIN,
			"nothing / OWN_WINDOW audio.sound-chain.editor.in-window(Test Gain) [audio.sound-chain.editor.put-back]", "open Test Gain inside / OPENING");
		assertEquals("[]", dock.getPinned().toString());
	}

	// what the host did for an editor no longer wanted there (another chip picked, the editor hidden meanwhile): a view
	// inside, or TuxGuitar's parameters, close; a window of its own stays
	@Test
	public void anEditorOpenedWhenNoLongerWantedThereCloses() {
		does(() -> dock.picked(GAIN)).does(() -> dock.picked(WOODSHED)).does(() -> dock.opened(GAIN, opened(Kind.EMBEDDED))).does(() -> dock.opened(GAIN, opened(Kind.NONE)))
			.does(() -> dock.opened(GAIN, opened(Kind.OWNED))).does(() -> dock.visibility(true, true)).does(() -> dock.opened(WOODSHED, opened(Kind.EMBEDDED)));
		sees("open Test Gain inside / OPENING", "close Test Gain, open Woodshed Guitar inside / OPENING", "close Test Gain / OPENING", "close Test Gain / OPENING", "nothing / OPENING",
			"close Woodshed Guitar / HIDDEN", "close Woodshed Guitar / HIDDEN");
	}

	// the plugin stopped (it hung, or its host crashed) and was started again: the area says so, with Show editor, which
	// opens it again; stopped again, it stays silent; another plugin's stop changes nothing
	@Test
	public void aStoppedPluginIsToldWithShowEditorWhenStartedAgain() {
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, opened(Kind.EMBEDDED))).does(() -> dock.stopped(WOODSHED, true, "", true)).does(() -> dock.stopped(GAIN, true, "", true))
			.does(() -> dock.again()).does(() -> dock.stopped(GAIN, false, "exit code 70", true)).does(() -> dock.stopped(GAIN, false, "exit code 70", false));
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR" + INSIDE_GAIN, "nothing / OWN_EDITOR" + INSIDE_GAIN,
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
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, opened(Kind.EMBEDDED))).does(() -> dock.requested(GAIN)).does(() -> dock.requested(WOODSHED))
			.does(() -> dock.stopped(GAIN, true, "", true)).does(() -> dock.requested(GAIN)).does(() -> dock.visibility(true, true)).does(() -> dock.requested(GAIN));
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR" + INSIDE_GAIN, "nothing / OWN_EDITOR" + INSIDE_GAIN, "open Woodshed Guitar in a window / OWN_EDITOR" + INSIDE_GAIN,
			"nothing / STOPPED audio.sound-chain.editor.hung-restarted(Test Gain) [audio.sound-chain.editor.show-editor]", "open Test Gain inside / OPENING", "close Test Gain / HIDDEN",
			"open Test Gain in a window / HIDDEN");
	}

	// what the area reads to execute a step and say its line
	@Test
	public void theAreaReadsAStepALineAndTheElementShown() {
		TGEditorDock.Step step = dock.picked(GAIN);
		dock.opened(GAIN, opened(Kind.EMBEDDED));
		TGEditorDock.Step moved = dock.picked(WOODSHED);
		dock.opened(WOODSHED, opened(Kind.TOO_LARGE));
		TGEditorDock.Line line = dock.line();
		Element shown = dock.element();
		assertEquals("null Test Gain INSIDE | Test Gain Woodshed Guitar INSIDE | audio.sound-chain.editor.too-large [Woodshed Guitar] audio.sound-chain.editor.show | 1 source WOODSHED 0 false",
			step.getClose() + " " + step.getOpen() + " " + step.getHow() + " | " + moved.getClose() + " " + moved.getOpen() + " " + moved.getHow() + " | " + line.getKey() + " "
				+ Arrays.toString(line.getArguments()) + " " + line.getButton() + " | " + shown.getChannel() + " " + shown.getId() + " " + shown.getClassId() + " " + shown.getRow() + " " + dock.isHidden());
	}


	// the button beside the line: Open in window, Put back in the panel, Show (a window too large), Show editor (after a
	// stop); with none, it does nothing
	@Test
	public void theButtonBesideTheLineDoesWhatItSays() {
		does(() -> dock.press()).does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, opened(Kind.EMBEDDED))).does(() -> dock.press()).does(() -> dock.press())
			.does(() -> dock.opened(GAIN, opened(Kind.TOO_LARGE))).does(() -> dock.press()).does(() -> dock.stopped(GAIN, true, "", true)).does(() -> dock.press());
		sees("nothing / NOTHING", "open Test Gain inside / OPENING", "nothing / OWN_EDITOR" + INSIDE_GAIN,
			"open Test Gain in a window / OWN_WINDOW audio.sound-chain.editor.in-window(Test Gain) [audio.sound-chain.editor.put-back]", "open Test Gain inside / OPENING",
			"open Test Gain in a window / TOO_LARGE audio.sound-chain.editor.too-large(Test Gain) [audio.sound-chain.editor.show]",
			"open Test Gain in a window / TOO_LARGE audio.sound-chain.editor.too-large(Test Gain) [audio.sound-chain.editor.show]",
			"nothing / STOPPED audio.sound-chain.editor.hung-restarted(Test Gain) [audio.sound-chain.editor.show-editor]", "open Test Gain inside / OPENING");
	}

	// the panel's Edit button: Hide editor while the editor shows, Show editor once hidden
	@Test
	public void theEditButtonHidesOrShowsTheEditor() {
		String shown = dock.editKey();
		dock.visibility(true, true);
		assertEquals("audio.sound-chain.hide-editor audio.sound-chain.show-editor", shown + " " + dock.editKey());
	}

	// the area is as high as the editor inside it (TGEditorArea.height), else it takes no room
	@Test
	public void theAreaIsAsHighAsTheEditorInside() {
		int before = dock.height(400);
		dock.picked(GAIN);
		dock.opened(GAIN, TGVst3EditorOpened.of(Kind.EMBEDDED, 300, 230, true, 1200));
		int inside = dock.height(800);
		dock.fitted(GAIN, TGVst3EditorOpened.of(Kind.EMBEDDED, 300, 345, true, 1200));
		int fitted = dock.height(800);
		dock.fitted(WOODSHED, TGVst3EditorOpened.of(Kind.EMBEDDED, 300, 999, true, 1200));
		dock.popOut();
		assertEquals("0 154 230 0", before + " " + inside + " " + fitted + " " + dock.height(800));
	}

	// the editor inside is fitted again when the area's width changed, not when only its height did (the host fitted it
	// to the width it had when it opened)
	@Test
	public void theEditorInsideIsFittedAgainWhenTheWidthChanges() {
		boolean nothingInside = dock.refits(800);
		dock.picked(GAIN);
		dock.opened(GAIN, opened(Kind.EMBEDDED));
		assertEquals("false false false true", nothingInside + " " + dock.refits(800) + " " + dock.refits(800) + " " + dock.refits(900));
	}

	// an editor the host opened (its view this size), TuxGuitar's parameters none
	private static TGPluginEditor editor(int height) throws Exception {
		return new TGPluginEditor(TGVst3EditorOpened.of(Kind.EMBEDDED, 200, height, false, 800), () -> Collections.<TGPluginParameterRow>emptyList());
	}

	// what becomes of an editor the area opened: moved elsewhere, closed (its plugin ran again: it opens again where it
	// goes), its plugin stopped, or resized by the plugin; one the area no longer follows, or another plugin's, changes
	// nothing
	@Test
	public void whatBecomesOfTheEditorOpenedIsFollowed() throws Exception {
		TGPluginEditor resized = editor(300);
		TGPluginEditor closed = editor(100);
		closed.close();
		TGPluginEditor moved = editor(100);
		moved.moved();
		TGPluginEditor stopped = editor(100);
		stopped.stopped(true, "the VST3 host did not answer for 2 s", true);
		does(() -> dock.picked(GAIN)).does(() -> dock.opened(GAIN, opened(Kind.EMBEDDED))).does(() -> dock.followed(GAIN, resized, true)).does(() -> dock.followed(GAIN, closed, false))
			.does(() -> dock.followed(GAIN, closed, true)).does(() -> dock.opened(GAIN, opened(Kind.EMBEDDED))).does(() -> dock.followed(GAIN, moved, true))
			.does(() -> dock.followed(GAIN, closed, true)).does(() -> dock.followed(WOODSHED, closed, true)).does(() -> dock.followed(GAIN, stopped, true));
		sees("open Test Gain inside / OPENING", "nothing / OWN_EDITOR" + INSIDE_GAIN, "nothing / OWN_EDITOR" + INSIDE_GAIN, "nothing / OWN_EDITOR" + INSIDE_GAIN,
			"close Test Gain, open Test Gain inside / OPENING", "nothing / OWN_EDITOR" + INSIDE_GAIN,
			"nothing / OWN_WINDOW audio.sound-chain.editor.in-window(Test Gain) [audio.sound-chain.editor.put-back]", "open Test Gain inside / OPENING", "nothing / OPENING",
			"nothing / STOPPED audio.sound-chain.editor.hung-restarted(Test Gain) [audio.sound-chain.editor.show-editor]");
	}

	// the size a plugin asked for itself is the area's height from then on
	@Test
	public void aSizeThePluginAskedForIsTheAreasHeight() throws Exception {
		dock.picked(GAIN);
		dock.opened(GAIN, opened(Kind.EMBEDDED));
		dock.followed(GAIN, editor(300), true);
		assertEquals(300, dock.height(800));
	}

	// the plugins remembered in a window of their own, as the panel's config keeps them
	@Test
	public void thePluginsRememberedAreKeptAsText() {
		assertEquals("[A, B] [] A,B", TGEditorDock.pinned("B,A") + " " + TGEditorDock.pinned("") + " " + TGEditorDock.pinned(new java.util.TreeSet<String>(Arrays.asList("B", "A"))));
	}

	// a line's text: its key said in TuxGuitar's language with its arguments, or the text it holds already
	@Test
	public void aLineIsSaidInTuxGuitarsLanguage() {
		dock.picked(GAIN);
		dock.opened(GAIN, opened(Kind.TOO_LARGE));
		String keyed = dock.line().text((key, arguments) -> key + Arrays.toString(arguments));
		dock.refused(GAIN, "ignored: not opening");
		dock.picked(WOODSHED);
		dock.refused(WOODSHED, "Woodshed Guitar did not open");
		assertEquals("audio.sound-chain.editor.too-large[Test Gain] | Woodshed Guitar did not open", keyed + " | " + dock.line().text((key, arguments) -> "translated"));
	}
	@Test
	public void elementsAreEqualByChannelElementAndPlugin() {
		assertEquals("true false false false", GAIN.equals(new Element(1, "effect-1", "GAIN", "renamed", 3)) + " " + GAIN.equals(new Element(2, "effect-1", "GAIN", "Test Gain", 1)) + " "
			+ GAIN.equals(new Element(1, "effect-2", "GAIN", "Test Gain", 1)) + " " + GAIN.equals(new Element(1, "effect-1", "CLIP", "Test Gain", 1)));
		assertEquals(GAIN.hashCode(), new Element(1, "effect-1", "GAIN", "renamed", 3).hashCode());
	}
}
