package app.tuxguitar.player.base;

/**
 * Settings' Try: beat 1 of a click, then an other beat GAP ms later, on the channel Count In clicks on (see
 * MidiPlayer.getCountInChannelId), with the click given rather than the player's. The drums of a synthesizer that has
 * them are told the click tried, then the player's click again once both notes are sent (each note sounds the sounds
 * the drums were told when it reached them); the player's click is never changed.
 */
final class MidiClickTry {

	// between beat 1 and the other beat, in ms
	static final long GAP = 400;

	private final MidiPlayer player;
	private final MidiClick click;

	MidiClickTry(MidiPlayer player, MidiClick click) {
		this.player = player;
		this.click = click;
	}

	void play() {
		try {
			int channelId = this.player.getCountInChannelId();
			this.player.sendClick(this.click);
			this.player.sendClickNote(channelId, this.click, true);
			Thread.sleep(GAP);
			this.player.sendClickNote(channelId, this.click, false);
		} catch (MidiPlayerException | InterruptedException failed) {
			failed.printStackTrace();
		} finally {
			this.player.sendClick(this.player.getClick());
		}
	}
}
