package app.tuxguitar.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.thread.TGThreadHandler;
import app.tuxguitar.thread.TGThreadLoop;
import app.tuxguitar.thread.TGThreadManager;
import app.tuxguitar.thread.TGThreadPriority;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.TGLock;

/**
 * The editor's lock taken for an action (lockFor, as the app's lockable actions take it before they run) is given back
 * for that action only (unlockFor, once it has run or failed): an action refused before it ran (as an interceptor
 * refuses it, failing without running) gives back nothing, so the lock stays as the actions around it hold it.
 */
public class TestEditorManagerLocking {

	private final TGContext context = new TGContext();
	private final TGEditorManager editor = TGEditorManager.getInstance(context);
	private final TGActionContext actionContext = new TGActionContext() {
	};

	// the lock tells threads apart by the thread manager's ids: here the test's thread
	public TestEditorManagerLocking() {
		TGThreadManager.getInstance(context).setThreadHandler(new TGThreadHandler() {
			public void start(TGThreadPriority priority, Runnable runnable) { }
			public void loop(TGThreadLoop loop) { }
			public void yield() { }
			public void dispose() { }
			public Object getThreadId() { return Thread.currentThread(); }
		});
		editor.setLockControl(new TGLock(context));
	}

	@Test
	public void theLockTakenForAnActionIsGivenBackOnce() {
		editor.lockFor("action.one", actionContext);
		String locked = String.valueOf(held());
		editor.unlockFor("action.one", actionContext);
		String unlocked = String.valueOf(held());
		editor.lock();
		editor.unlockFor("action.one", actionContext);
		assertEquals("true false true", locked + " " + unlocked + " " + held());
	}

	// the editor's lock held by this thread
	private boolean held() {
		return editor.isUnderLockControl();
	}

	// another action runs inside one that holds the lock and is refused before it runs: the lock stays the outer one's,
	// and giving that one back frees it
	@Test
	public void anActionRefusedBeforeItRanGivesNothingBack() {
		editor.lockFor("action.outer", actionContext);
		editor.unlockFor("action.refused", actionContext);
		String during = String.valueOf(held());
		editor.unlockFor("action.outer", actionContext);
		assertEquals("true false", during + " " + held());
	}
}
