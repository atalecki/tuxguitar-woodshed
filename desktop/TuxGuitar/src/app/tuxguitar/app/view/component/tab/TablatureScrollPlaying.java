package app.tuxguitar.app.view.component.tab;

import app.tuxguitar.app.system.config.TGConfigKeys;
import app.tuxguitar.app.system.config.TGConfigManager;
import app.tuxguitar.graphics.control.TGLayout;
import app.tuxguitar.graphics.control.TGMeasureImpl;
import app.tuxguitar.player.base.MidiPlayerMode;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.ui.resource.UIRectangle;
import app.tuxguitar.util.TGContext;

public class TablatureScrollPlaying {

	private final static int SCROLL_MARGIN = 5;		// to avoid reaching target a bit early (that would stop scrolling for a short delay)

	private long tLastUpdateScrollMs;
	private long tlastUpdateTargetSpeedMs;
	private int lastUpdateTargetSpeedMeasureNumber;
	private float targetSpeed;
	private Integer lastDirection;
	private float toScroll = 0f;
	private float speed;
	private long minDT;

	public TablatureScrollPlaying(TGContext context) {
		this.lastDirection = null;
		this.minDT = 1000 / TGConfigManager.getInstance(context).getIntegerValue(TGConfigKeys.SCROLLING_MAX_FPS);
	}

	public void reset(int direction) {
		if ((this.lastDirection == null) || (this.lastDirection != direction)) {
			this.lastDirection = direction;
		}
		this.reset();
	}

	private void reset() {
		this.tLastUpdateScrollMs = 0;
		this.tlastUpdateTargetSpeedMs = 0;
		this.lastUpdateTargetSpeedMeasureNumber = 0;
		this.targetSpeed = 0f;
		this.toScroll = 0f;
		this.speed = 0f;
	}

	public Integer scrollTo(TGMeasureImpl measure, int pos, int target, MidiPlayerMode mode, UIRectangle area, TGLayout layout) {
		// target measure not visible? Immediate jump
		if (!layout.isFullyVisible(measure, area)) {
			return this.jumpTo(pos, target);
		}
		return this.scrollShownTo(measure, pos, target, mode, area, layout);
	}

	// don't scroll if the end is already displayed
	private Integer scrollShownTo(TGMeasureImpl measure, int pos, int target, MidiPlayerMode mode, UIRectangle area, TGLayout layout) {
		if (this.isEndShown(measure.getTrack(), mode, area, layout)) {
			return null;
		}
		return this.continousScrollTo(pos, target, mode, measure, layout);
	}

	// the end: the end of the loop, else the last scrollable item (line or measure)
	private boolean isEndShown(TGTrack track, MidiPlayerMode mode, UIRectangle area, TGLayout layout) {
		return this.isLoopEndShown(track, mode, area, layout) || this.isShown(track, this.lastScrollableIndex(track, layout), area, layout);
	}

	private boolean isLoopEndShown(TGTrack track, MidiPlayerMode mode, UIRectangle area, TGLayout layout) {
		return mode.isLoop() && this.isShown(track, this.loopEndNumber(track, mode) - 1, area, layout);
	}

	private int loopEndNumber(TGTrack track, MidiPlayerMode mode) {
		int endLoopMeasureNumber = mode.getLoopEHeader();
		return (endLoopMeasureNumber == -1 ? track.countMeasures() : endLoopMeasureNumber);
	}

	private boolean isShown(TGTrack track, int measureIndex, UIRectangle area, TGLayout layout) {
		return layout.isFullyVisible((TGMeasureImpl) track.getMeasure(measureIndex), area);
	}

	// the last scrollable item: the first of the measures at the end with timeToNextScrollMs zero
	private int lastScrollableIndex(TGTrack track, TGLayout layout) {
		int lastIndex = track.countMeasures() - 1;
		while (this.isScrollEnd(lastIndex - 1, layout)) {
			lastIndex--;
		}
		return lastIndex;
	}

	private boolean isScrollEnd(int measureIndex, TGLayout layout) {
		return (measureIndex >= 0) && Integer.valueOf(0).equals(layout.getTimeToNextScrollMs(measureIndex));
	}

	private Integer continousScrollTo(int pos, int target, MidiPlayerMode mode, TGMeasureImpl measure, TGLayout layout) {
		if (pos - target + SCROLL_MARGIN <= 0) {
			this.reset();
			return null;
		}
		return this.scrollFrameAt(System.currentTimeMillis(), pos, target, mode, measure, layout);
	}

	// one frame at most every minDT (the clock, epoch milliseconds, is always more than minDT past the 0 of a reset)
	private Integer scrollFrameAt(long t, int pos, int target, MidiPlayerMode mode, TGMeasureImpl measure, TGLayout layout) {
		if (t - this.tLastUpdateScrollMs < this.minDT) {
			return null;
		}
		Integer scrollIncrement = this.scrollFrame(t, pos, target, mode, measure, layout);
		this.tLastUpdateScrollMs = t;
		return scrollIncrement;
	}

	// the first frame after a reset only starts the clock
	private Integer scrollFrame(long t, int pos, int target, MidiPlayerMode mode, TGMeasureImpl measure, TGLayout layout) {
		if (this.tLastUpdateScrollMs == 0) {
			return null;
		}
		Integer scrollIncrement = this.takeWholePixels(t - this.tLastUpdateScrollMs);
		this.updateSpeed(t, pos, target, mode, measure, layout);
		return scrollIncrement;
	}

	// the whole pixels scrolled at this speed since the last frame, keeping the fraction for the next
	private Integer takeWholePixels(long dt) {
		this.toScroll += dt * this.speed / 1000;
		int scrollIncrement = Math.round(this.toScroll);
		if (scrollIncrement <= 0) {
			return null;
		}
		this.toScroll -= scrollIncrement;
		return scrollIncrement;
	}

	private void updateSpeed(long t, int pos, int target, MidiPlayerMode mode, TGMeasureImpl measure, TGLayout layout) {
		int distanceToScroll = pos - target;
		if (distanceToScroll <= 0) {
			this.reset();
			return;
		}
		this.updateSpeedWithin(t, distanceToScroll, layout.getTimeToNextScrollMs(measure.getNumber() - 1), mode, measure, layout);
	}

	private void updateSpeedWithin(long t, int distanceToScroll, Integer timeToScrollMs, MidiPlayerMode mode, TGMeasureImpl measure, TGLayout layout) {
		// null: layout not yet initialized
		if (timeToScrollMs != null) {
			// consider effective tempo
			int timeLeftMs = this.timeLeftMs(t, distanceToScroll, timeToScrollMs * 100 / mode.getCurrentPercent(), measure);
			this.updateSpeedIn(t, distanceToScroll, timeLeftMs, measure, layout);
		}
	}

	// update target speed : at initialization, then once per measure, then the whole time is left; else what is left
	// of it since
	private int timeLeftMs(long t, int distanceToScroll, int timeToScrollMs, TGMeasureImpl measure) {
		if (this.isNewTarget(measure)) {
			this.lastUpdateTargetSpeedMeasureNumber = measure.getNumber();
			this.tlastUpdateTargetSpeedMs = t;
			this.targetSpeed = 1000 * distanceToScroll / timeToScrollMs;
			return timeToScrollMs;
		}
		return (int) (timeToScrollMs - t + this.tlastUpdateTargetSpeedMs);
	}

	private boolean isNewTarget(TGMeasureImpl measure) {
		return (this.targetSpeed == 0f) || (measure.getNumber() != this.lastUpdateTargetSpeedMeasureNumber);
	}

	private void updateSpeedIn(long t, int distanceToScroll, int timeLeftMs, TGMeasureImpl measure, TGLayout layout) {
		if (timeLeftMs > 0) {
			float maxSpeed = 1000 * distanceToScroll / timeLeftMs;	// no faster than that, or played measure may get out of displayed area
			this.updateSpeedUpTo(t, distanceToScroll, maxSpeed, measure, layout);
		}
	}

	private void updateSpeedUpTo(long t, int distanceToScroll, float maxSpeed, TGMeasureImpl measure, TGLayout layout) {
		if (this.speed == 0) { //arbitrary
			// don't start too slow (use case: start with caret in first displayed measure)
			// but don't start too fast either (use case: start with caret in last displayed measure,
			//		it's better here to fail the continuous scrolling than to speed up too much)
			this.speed = maxSpeed / 3;
		}
		else {
			this.approachSpeed(t, distanceToScroll, maxSpeed, measure, layout);
		}
	}

	private void approachSpeed(long t, int distanceToScroll, float maxSpeed, TGMeasureImpl measure, TGLayout layout) {
		if (this.isNearStart(distanceToScroll, maxSpeed, measure, layout)) {
			this.speed = maxSpeed;	// don't exceed this speed when getting close from start of display area (do not mask played measure)
		}
		else {
			this.filterSpeed(t, measure);
		}
	}

	private boolean isNearStart(int distanceToScroll, float maxSpeed, TGMeasureImpl measure, TGLayout layout) {
		return (this.speed >= maxSpeed) && (distanceToScroll <= layout.getMeasureScrollableSize(measure));
	}

	private void filterSpeed(long t, TGMeasureImpl measure) {
		// low pass filter, arbitrary parameters
		long lpf = this.speed < this.targetSpeed ?
						  3 * measure.getHeader().getDurationInMs()			// short filter duration: brake fast
						: measure.getHeader().getDurationInMs() / 3;		// long  filter duration: accelerate slowly
		this.speed += (this.targetSpeed - this.speed) * (t - this.tLastUpdateScrollMs) / lpf;
	}

	// discrete scroll
	private int jumpTo(int pos, int target) {
		this.reset();
		return pos - target;
	}

}
