package app.tuxguitar.audio;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import app.tuxguitar.event.TGEvent;
import app.tuxguitar.event.TGEventManager;
import app.tuxguitar.player.base.MidiPlayerEvent;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonUtil;
import app.tuxguitar.vst3.TGVst3Instance;

/**
 * The quiet hint an app shows while a plugin Play stopped waiting for still loads: Play waits for the plugins of the
 * song's chains that restore their state in the background, for a while at most (TGVst3Instance.isRestoring); when it
 * starts while one still restores, the hint names it and what it loads ("Woodshed Guitar is still loading Clean.nam";
 * "... its sound" when the plugin did not say), until that plugin has finished (or no longer runs). Play that waited
 * until they were ready shows none. An app reads the hint as often as it likes, off its UI thread (each read asks the
 * plugins' hosts); null for none.
 */
public final class TGLoadingHint {

	// the plugins of the live renders still restoring when Play last started (see TGLivePlugins.getRunning): no lock is
	// held while their hosts are asked, so a slow host never holds Play up
	private volatile List<TGVst3Instance> waited = new CopyOnWriteArrayList<TGVst3Instance>();

	private TGLoadingHint() {
	}

	// what an app installs at startup: Play starting is heard from then on
	public static TGLoadingHint install(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGLoadingHint.class.getName(), key -> listening(key, new TGLoadingHint()));
	}

	private static TGLoadingHint listening(TGContext context, TGLoadingHint hint) {
		TGEventManager.getInstance(context).addListener(MidiPlayerEvent.EVENT_TYPE, event -> hint.heard(context, event));
		return hint;
	}

	private void heard(TGContext context, TGEvent event) {
		if (Integer.valueOf(MidiPlayerEvent.NOTIFY_STARTED).equals(event.getAttribute(MidiPlayerEvent.PROPERTY_NOTIFICATION_TYPE))) {
			this.started(TGLivePlugins.getInstance(context).getRunning());
		}
	}

	// Play started (it waited what it would): the plugins still restoring are the ones it stopped waiting for
	private void started(List<TGVst3Instance> running) {
		this.waited = running.stream().filter(instance -> instance.getRestoring() != null).collect(Collectors.toCollection(CopyOnWriteArrayList::new));
	}

	// the first of those still restoring, its hint; those that have finished are forgotten
	public String getText() {
		List<TGVst3Instance> waited = this.waited;
		for (TGVst3Instance instance : waited) {
			String what = instance.getRestoring();
			if (what != null) {
				return instance.getName() + " is still loading " + (what.isEmpty() ? "its sound" : what);
			}
			waited.remove(instance);
		}
		return null;
	}
}
