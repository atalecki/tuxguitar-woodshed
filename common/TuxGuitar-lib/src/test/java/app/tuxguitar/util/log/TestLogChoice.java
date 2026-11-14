package app.tuxguitar.util.log;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestLogChoice {

	@Test
	public void theCommandLineLevelWinsOverTheSettingsAndTheDebugHelper() {
		assertEquals("ERROR (-Dtuxguitar.log.level)", TGLogChoice.resolve(TGLogLevel.ERROR, TGLogLevel.TRACE, true).toString());
	}

	@Test
	public void theDebugHelperRaisesAQuieterLevelToDebug() {
		assertEquals("DEBUG (debug helper plugin)", TGLogChoice.resolve(null, TGLogLevel.WARN, true).toString());
		assertEquals("DEBUG (debug helper plugin)", TGLogChoice.resolve(null, null, true).toString());
	}

	@Test
	public void theDebugHelperLeavesAMoreVerboseSettingAsItIs() {
		assertEquals("TRACE (settings)", TGLogChoice.resolve(null, TGLogLevel.TRACE, true).toString());
		assertEquals("DEBUG (settings)", TGLogChoice.resolve(null, TGLogLevel.DEBUG, true).toString());
	}

	@Test
	public void withoutTheCommandLineAndTheHelperTheSettingsLevelHolds() {
		assertEquals("INFO (settings)", TGLogChoice.resolve(null, TGLogLevel.INFO, false).toString());
	}

	@Test
	public void withNothingSetTheLevelIsWarn() {
		TGLogChoice choice = TGLogChoice.resolve(null, null, false);

		assertEquals(TGLogLevel.WARN, choice.getLevel());
		assertEquals(TGLogChoice.Source.DEFAULT, choice.getSource());
		assertEquals("WARN (default)", choice.toString());
	}
}
