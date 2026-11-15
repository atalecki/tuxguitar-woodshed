package app.tuxguitar.practice;

/**
 * The steps of a course that the musician can go back over while it is under way (TGPracticeCourse.getSteps):
 * interleaved click-up's. Its turns are its steps, numbered alike from 1. Repeat step plays the step under way again,
 * Previous step the one before it (during the first, the first again): the course plans that step next, from its
 * Count In, then the steps after it in order. Called on the thread that plans the turns.
 */
public interface TGPracticeSteps {

	// Repeat step during the turn (from 1): its step is planned next; that step's turn
	int repeat(int turn);

	// Previous step during the turn (from 1): the step before it (the first, during the first) is planned next; that
	// step's turn
	int previous(int turn);
}
