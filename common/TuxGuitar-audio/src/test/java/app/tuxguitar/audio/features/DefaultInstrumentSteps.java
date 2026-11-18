package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Locale;

import app.tuxguitar.audio.TGDefaultInstrument;
import app.tuxguitar.audio.TGDefaultInstrumentSource;
import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.vst3.TGVst3Hosting;
import app.tuxguitar.vst3.TGVst3Plugin;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;

/**
 * Steps on the default instruments of the kinds of tracks: set as the settings set them, on the default source the app
 * installs (TGLiveAudio.install), the plugins the VST3 scan found named by their class id.
 */
public class DefaultInstrumentSteps {

	private final AudioWorld world;

	public DefaultInstrumentSteps(AudioWorld world) {
		this.world = world;
	}

	@ParameterType("guitar|bass|drums|other")
	public TGInstrumentKind kind(String kind) {
		return TGInstrumentKind.valueOf(kind.toUpperCase(Locale.ROOT));
	}

	private TGDefaultInstrumentSource source() {
		return TGDefaultInstrumentSource.install(world.fixture().context);
	}

	@Given("the default instrument for {kind} tracks is {string}")
	public void theDefaultInstrumentIs(TGInstrumentKind kind, String name) {
		TGVst3Plugin plugin = TGVst3Hosting.getInstance(world.fixture().context).find(name);
		assertNotNull(plugin, name);
		TGDefaultInstrument instrument = TGDefaultInstrument.parse(plugin.getClassId(), plugin.getName()).get();
		source().use(source().getDefaults().with(kind, instrument));
	}
}
