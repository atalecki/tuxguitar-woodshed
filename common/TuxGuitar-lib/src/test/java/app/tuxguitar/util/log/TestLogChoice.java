package app.tuxguitar.util.log;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class TestLogChoice {

	private static String chosen(TGLogLevel commandLine, TGLogLevel settings, boolean debugHelper) {
		return TGLogChoice.resolve(commandLine, settings, debugHelper).toString();
	}

	// the command line wins over the settings and the debug helper; the helper raises a quieter level (or none set) to
	// DEBUG and leaves a more verbose one as it is; without the command line and the helper the settings' level holds
	@Test
	public void theCommandLineWinsThenTheHelperRaisesToDebugThenTheSettingsHold() {
		assertEquals(Arrays.asList("ERROR (-Dtuxguitar.log.level)", "DEBUG (debug helper plugin)", "DEBUG (debug helper plugin)",
				"TRACE (settings)", "DEBUG (settings)", "INFO (settings)"),
				Arrays.asList(chosen(TGLogLevel.ERROR, TGLogLevel.TRACE, true), chosen(null, TGLogLevel.WARN, true), chosen(null, null, true),
						chosen(null, TGLogLevel.TRACE, true), chosen(null, TGLogLevel.DEBUG, true), chosen(null, TGLogLevel.INFO, false)));
	}

	@Test
	public void withNothingSetTheLevelIsWarn() {
		TGLogChoice choice = TGLogChoice.resolve(null, null, false);

		assertEquals(TGLogLevel.WARN, choice.getLevel());
		assertEquals(TGLogChoice.Source.DEFAULT, choice.getSource());
		assertEquals("WARN (default)", choice.toString());
	}
}
