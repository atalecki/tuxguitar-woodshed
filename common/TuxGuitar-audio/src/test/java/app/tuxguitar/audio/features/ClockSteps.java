package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.tuxguitar.practice.TGPracticeSettings;
import app.tuxguitar.practice.TGPracticeUnitKind;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps that set interleaved practice's interval clock (README, Practice: Interval clock) and start a timed session;
 * starting checks the settings and makes the session, it plays nothing yet.
 */
public class ClockSteps {

	private static final String EVERY_PASS = "Count In before every pass";

	private final Practice practice;

	public ClockSteps(Practice practice) {
		this.practice = practice;
	}

	@Then("interleaved practice's interval clock is off, set to turns of {int} minutes, breaks of {int} seconds and sessions of {int} minutes")
	public void theClockIsOff(int turnMinutes, int breakSeconds, int sessionMinutes) {
		TGPracticeSettings settings = practice.settings();
		assertEquals("off, " + turnMinutes + " " + breakSeconds + " " + sessionMinutes, (settings.isIntervalClock() ? "on" : "off") + ", "
			+ settings.getTurnMinutes() + " " + settings.getBreakSeconds() + " " + settings.getSessionMinutes());
	}

	@Then("{string} is off")
	public void everyPassIsOff(String option) {
		assertEquals(EVERY_PASS + " off", option + (practice.settings().isCountInEveryPass() ? " on" : " off"));
	}

	@Given("I have turned {string} {word}")
	public void turnEveryPass(String option, String onOrOff) {
		assertEquals(EVERY_PASS, option);
		practice.settings().setCountInEveryPass("on".equals(onOrOff));
	}

	@Given("I have turned the interval clock on, with turns of {int} minute(s), breaks of {int} seconds and sessions of {int} minute(s)")
	public void turnTheClockOn(int turnMinutes, int breakSeconds, int sessionMinutes) {
		practice.settings().setIntervalClock(true);
		practice.settings().setTurnMinutes(turnMinutes);
		practice.settings().setBreakSeconds(breakSeconds);
		practice.settings().setSessionMinutes(sessionMinutes);
	}

	@When("I start interleaved practice of {unit}s over {pool} with the interval clock, with turns of {int} minutes, breaks of {int} seconds and sessions of {int} minutes")
	public void startTimed(TGPracticeUnitKind unit, int[] pool, int turnMinutes, int breakSeconds, int sessionMinutes) {
		practice.settings().setUnit(unit);
		practice.settings().setPool(pool[0], pool[1]);
		turnTheClockOn(turnMinutes, breakSeconds, sessionMinutes);
		practice.start();
	}
}
