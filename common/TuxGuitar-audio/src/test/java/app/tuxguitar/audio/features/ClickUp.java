package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertTrue;

import app.tuxguitar.audio.action.TGPractiseAction;
import app.tuxguitar.practice.TGClickUpLadder;
import app.tuxguitar.practice.TGClickUpSettings;

/**
 * Interleaved click-up in a scenario: the settings as the steps set them, and the click-up started last, its steps
 * worked out (or why starting was refused); practising it plays it offline. Cucumber makes one per scenario and gives it
 * to every step class that takes it.
 */
public class ClickUp {

	private final AudioWorld world;
	private final TGClickUpSettings settings = new TGClickUpSettings();
	private TGClickUpLadder ladder;

	public ClickUp(AudioWorld world) {
		this.world = world;
	}

	TGClickUpSettings settings() {
		return this.settings;
	}

	// starting interleaved click-up as set: its steps, or the reason it is refused, as the world's refusal (no playback)
	void start() {
		this.ladder = this.world.start(() -> TGClickUpLadder.of(this.settings, this.world.song()));
	}

	// whether starting was accepted
	boolean isStarted() {
		return this.ladder != null;
	}

	// the click-up started last, which must have been accepted
	TGClickUpLadder ladder() {
		return this.world.accepted(this.ladder, "interleaved click-up");
	}

	// starts it as set, then practises it offline (TGPractiseAction) to its end
	void practise() {
		this.start();
		this.world.fixture().actionContext.setAttribute(TGPractiseAction.ATTRIBUTE_CLICK_UP, this.ladder());
		this.world.execute(new TGPractiseAction(this.world.fixture().context));
		assertTrue(this.world.succeeded());
	}
}
