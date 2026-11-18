package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import app.tuxguitar.vst3.TGVst3Exception;
import app.tuxguitar.vst3.TGVst3Instance;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps on the window of Woodshed Guitar's open editor as the user and Windows change it: the screen it is on goes to
 * another DPI (as Windows tells the window), the user drags its border to another width; and how the editor is laid
 * out then, read from the editor ("Woodshed.layout": the scale it was told, its window's DPI, its size, its text's
 * height, its sliders' widths, whether every label holds its text).
 */
public class WoodshedWindowSteps {

	private final GuitarSteps guitar;
	// its text's height at 96 DPI, once read
	private int textAt96;

	public WoodshedWindowSteps(GuitarSteps guitar) {
		this.guitar = guitar;
	}

	private TGVst3Instance running(int track) throws TGVst3Exception {
		return this.guitar.running(track);
	}

	// the editor's report, "name=value" pairs a space apart
	private Map<String, String> layout(int track) throws TGVst3Exception {
		String report = new String(running(track).message("Woodshed.layout", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap()), StandardCharsets.UTF_8);
		Map<String, String> read = new HashMap<String, String>();
		for (String pair : report.split(" ")) {
			String[] parts = pair.split("=", 2);
			read.put(parts[0], parts[parts.length - 1]);
		}
		return read;
	}

	@Given("Woodshed Guitar's editor of track {int} has gone to a screen of {int} DPI")
	@When("Woodshed Guitar's editor of track {int} goes to a screen of {int} DPI")
	public void goesToAScreenOf(int track, int dpi) throws TGVst3Exception {
		running(track).message("Woodshed.dpi-changed", Map.of("dpi", (long) dpi), Collections.<String, byte[]>emptyMap());
	}

	@Given("I have dragged the border of Woodshed Guitar's editor of track {int} until it is {int} wide")
	@When("I drag the border of Woodshed Guitar's editor of track {int} until it is {int} wide")
	public void dragTo(int track, int width) throws TGVst3Exception {
		long height = Long.parseLong(layout(track).get("size").split("x")[1]);
		running(track).message("Woodshed.drag-to", Map.of("width", (long) width, "height", height), Collections.<String, byte[]>emptyMap());
	}

	// drawn at the scale of the screen its window is on: told the window's DPI, as high as the design at that scale
	@Then("Woodshed Guitar's editor of track {int} is laid out at its window's scale")
	public void atItsWindowsScale(int track) throws TGVst3Exception {
		Map<String, String> layout = layout(track);
		double scale = Integer.parseInt(layout.get("dpi")) / 96.0;
		assertEquals(scale + " " + Math.round(230 * scale), Double.parseDouble(layout.get("scale")) + " " + layout.get("size").split("x")[1]);
	}

	// its size: the height exact (it never stretches), the width within 2% (a window moved to another screen takes the
	// frame Windows suggests: the whole window scaled, its border too); its text as tall as at 96 DPI times the scale,
	// within a pixel, once read at 96 DPI
	@Then("Woodshed Guitar's editor of track {int} is laid out at scale {double}, about {int} by {int}")
	public void laidOutAt(int track, double scale, int width, int height) throws TGVst3Exception {
		Map<String, String> layout = layout(track);
		String[] size = layout.get("size").split("x");
		int text = Integer.parseInt(layout.get("text"));
		this.remember(text, scale);
		assertEquals(scale + " " + height + " wide enough true text scaled true labels hold their texts true",
			Double.parseDouble(layout.get("scale")) + " " + size[1] + " wide enough " + (Math.abs(Integer.parseInt(size[0]) - width) <= width / 50) + " text scaled " + this.isScaled(text, scale)
				+ " labels hold their texts " + layout.get("labels-fit"), "text " + text + " at 96 DPI " + this.textAt96 + " size " + layout.get("size"));
	}

	// the text's height read at 96 DPI
	private void remember(int text, double scale) {
		if (scale == 1.0) {
			this.textAt96 = text;
		}
	}

	// drawn in a font, as tall as at 96 DPI times the scale, within a pixel
	private boolean isScaled(int text, double scale) {
		return text > 0 && Math.abs(text - this.textAt96 * scale) <= 1;
	}

	// its profile's line as drawn, then its tooltip's text
	private String[] profileLine(int track) throws TGVst3Exception {
		return new String(running(track).message("Woodshed.profile-line", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap()), StandardCharsets.UTF_8).split("\n", -1);
	}

	// what is drawn: a start of the whole line, then the ellipsis, cut where the line's own font fills its box
	@Then("Woodshed Guitar's editor of track {int} shows its profile line cut, ending in {string}, the whole line {string} in its tooltip")
	public void profileLineCut(int track, String ellipsis, String line) throws TGVst3Exception {
		String[] shown = profileLine(track);
		String start = shown[0].substring(0, Math.max(0, shown[0].length() - ellipsis.length()));
		assertEquals("true true true true " + line, shown[0].endsWith(ellipsis) + " " + !start.isEmpty() + " " + line.startsWith(start) + " " + shown[0].equals(shown[2]) + " " + shown[1]);
	}

	@Then("Woodshed Guitar's editor of track {int} shows its whole profile line {string}, without a tooltip")
	public void profileLineWhole(int track, String line) throws TGVst3Exception {
		assertEquals(line + "\n\n" + line, String.join("\n", profileLine(track)));
	}

	@Then("Woodshed Guitar's editor of track {int} is {int} wide, its sliders {int} wide, every label holding its text")
	public void sliders(int track, int width, int sliders) throws TGVst3Exception {
		Map<String, String> layout = layout(track);
		assertEquals(width + " " + sliders + "," + sliders + " true", layout.get("size").split("x")[0] + " " + layout.get("sliders") + " " + layout.get("labels-fit"));
	}
}
