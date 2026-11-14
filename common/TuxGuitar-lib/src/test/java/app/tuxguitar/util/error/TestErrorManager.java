package app.tuxguitar.util.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.log.TGLogLevel;
import app.tuxguitar.util.log.TGLogRecorder;

public class TestErrorManager {

	private final TGErrorManager manager = TGErrorManager.getInstance(new TGContext());

	private TGLogRecorder handled(Throwable error) {
		TGLogRecorder recorder = TGLogRecorder.attach(TGLogLevel.ERROR);
		try {
			this.manager.handleError(error);
		} finally {
			recorder.detach();
		}
		return recorder;
	}

	@Test
	public void eachHandlerGetsTheError() {
		List<String> handled = new ArrayList<String>();
		this.manager.addErrorHandler(error -> handled.add("first " + error.getMessage()));
		this.manager.addErrorHandler(error -> handled.add("second " + error.getMessage()));

		this.manager.handleError(new IllegalStateException("it broke"));

		assertEquals(List.of("first it broke", "second it broke"), handled);
	}

	@Test
	public void aHandledErrorIsLoggedWithItsStackTrace() {
		TGLogRecorder recorder = handled(new IllegalStateException("it broke"));

		assertEquals(List.of("ERROR it broke (java.lang.IllegalStateException: it broke)"), recorder.recorded());
		assertTrue(recorder.text().contains("\tat app.tuxguitar.util.error.TestErrorManager."), recorder.text());
	}

	@Test
	public void anErrorWithoutAMessageIsLoggedByItsType() {
		assertEquals(List.of("ERROR java.lang.NullPointerException (java.lang.NullPointerException)"),
				handled(new NullPointerException()).recorded());
	}
}
