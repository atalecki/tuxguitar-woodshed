package app.tuxguitar.util.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;

import org.junit.jupiter.api.Test;

public class TestLogLevel {

	@Test
	public void aLevelIsReadFromItsNameInAnyCase() {
		assertEquals(TGLogLevel.DEBUG, TGLogLevel.parse("debug"));
		assertEquals(TGLogLevel.TRACE, TGLogLevel.parse(" Trace "));
		assertEquals(TGLogLevel.WARN, TGLogLevel.parse("WARN"));
	}

	@Test
	public void anyOtherTextIsNoLevel() {
		assertNull(TGLogLevel.parse(null));
		assertNull(TGLogLevel.parse(""));
		assertNull(TGLogLevel.parse("verbose"));
	}

	@Test
	public void aLevelShowsWhatIsLoggedAtItAndTheMoreSevereLevels() {
		assertTrue(TGLogLevel.WARN.shows(TGLogLevel.ERROR));
		assertTrue(TGLogLevel.WARN.shows(TGLogLevel.WARN));
		assertFalse(TGLogLevel.WARN.shows(TGLogLevel.INFO));
		assertTrue(TGLogLevel.TRACE.shows(TGLogLevel.DEBUG));
		assertFalse(TGLogLevel.ERROR.shows(TGLogLevel.WARN));
	}

	@Test
	public void eachLevelIsAJavaLoggingLevel() {
		assertEquals(Level.SEVERE, TGLogLevel.ERROR.toJul());
		assertEquals(Level.WARNING, TGLogLevel.WARN.toJul());
		assertEquals(Level.INFO, TGLogLevel.INFO.toJul());
		assertEquals(Level.FINE, TGLogLevel.DEBUG.toJul());
		assertEquals(Level.FINEST, TGLogLevel.TRACE.toJul());
	}

	@Test
	public void aJavaLoggingLevelIsNamedAfterTheNearestLevelAtOrBelowIt() {
		assertEquals(TGLogLevel.ERROR, TGLogLevel.named(Level.SEVERE));
		assertEquals(TGLogLevel.INFO, TGLogLevel.named(Level.INFO));
		assertEquals(TGLogLevel.DEBUG, TGLogLevel.named(Level.CONFIG));
		assertEquals(TGLogLevel.TRACE, TGLogLevel.named(Level.FINER));
		assertEquals(TGLogLevel.TRACE, TGLogLevel.named(Level.ALL));
	}
}
