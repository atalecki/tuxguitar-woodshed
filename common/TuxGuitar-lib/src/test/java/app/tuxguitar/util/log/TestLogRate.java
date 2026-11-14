package app.tuxguitar.util.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestLogRate {

	private static final long MS = 1000000L;

	@Test
	public void theFirstMessageGoesThenOneAtMostPerInterval() {
		TGLogRate rate = new TGLogRate(1000);
		long start = 123456789L;

		assertTrue(rate.allow(start));
		assertFalse(rate.allow(start + 10 * MS));
		assertFalse(rate.allow(start + 999 * MS));
		assertTrue(rate.allow(start + 1000 * MS));
		assertFalse(rate.allow(start + 1500 * MS));
	}

	@Test
	public void theMessagesHeldBackAreCountedUntilTaken() {
		TGLogRate rate = new TGLogRate(1000);
		rate.allow(0);
		rate.allow(MS);
		rate.allow(2 * MS);

		assertEquals(2, rate.takeHeldBack());
		assertEquals(0, rate.takeHeldBack());
	}

	@Test
	public void aNegativeClockReadingIsNoSpecialCase() {
		TGLogRate rate = new TGLogRate(1000);

		assertTrue(rate.allow(-5000 * MS));
		assertFalse(rate.allow(-4500 * MS));
		assertTrue(rate.allow(-4000 * MS));
	}
}
