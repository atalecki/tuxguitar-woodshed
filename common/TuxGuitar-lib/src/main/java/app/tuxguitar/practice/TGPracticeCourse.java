package app.tuxguitar.practice;

import java.util.Optional;

import app.tuxguitar.song.models.TGMeasureHeader;

/**
 * What a practice session plays, turn by turn, as the player plays it live and the audio renders it offline: each
 * turn's plan (TGPracticePlan: what it plays, at what tempo scale, its passes, the break after it, whether it is the
 * last), made ahead of playing it live, or as it starts offline; the flags it lets the song take while under way; how
 * the app counts its turns; and whether it has ended. Interleaved practice's random choice is one
 * (TGPracticeSession.course), interleaved click-up's steps another (TGClickUpLadder.course). The player plans on its
 * thread while an edit checks a flag, or ends it, on another.
 */
public interface TGPracticeCourse {

	// the plan of the next turn, made ahead of playing it, once the turn before has started; started() says when it
	// starts
	TGPracticePlan ahead();

	// the turn planned last has started playing
	void started();

	// the plan of the next turn, which starts playing now
	TGPracticePlan next();

	// the Count In clicks before a turn's pass whose first measure is the header, as it plays it now (0: none)
	int clicks(TGMeasureHeader first);

	// refuses a flag, set now (the measure from 1, the difficulty), that the turns to come could not play
	void checkFlag(int measure, int difficulty) throws TGPracticeRefusal;

	// the practice mode's name, as a message about it starts: "Interleaved practice", "Interleaved click-up"
	String getModeName();

	// whether a clock times it: the app shows the time left, not the number of turns
	boolean isTimed();

	// the number of turns it plays; timed, the most it can hold
	int getTurns();

	// whether it has ended: after its last turn, or as Stop, moving the position or an edit ends it
	boolean isEnded();

	// ends it; one that has ended stays ended
	void end();

	// its steps, which Repeat step and Previous step go back over (interleaved click-up's); none for one whose turns
	// are not steps (interleaved practice's random choice)
	Optional<TGPracticeSteps> getSteps();
}
