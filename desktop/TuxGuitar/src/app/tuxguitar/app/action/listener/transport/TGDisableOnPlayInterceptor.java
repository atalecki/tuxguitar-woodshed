package app.tuxguitar.app.action.listener.transport;

import java.util.ArrayList;
import java.util.List;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionException;
import app.tuxguitar.action.TGActionInterceptor;
import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.editor.practice.TGPracticeEdits;
import app.tuxguitar.util.TGContext;

public class TGDisableOnPlayInterceptor implements TGActionInterceptor {

	private TGContext context;
	private List<String> actionIds;

	public TGDisableOnPlayInterceptor(TGContext context){
		this.context = context;
		this.actionIds = new ArrayList<String>();
	}

	public boolean containsActionId(String id) {
		return this.actionIds.contains(id);
	}

	public void addActionId(String id) {
		this.actionIds.add(id);
	}

	public void removeActionId(String id) {
		this.actionIds.remove(id);
	}

	public boolean intercept(String id, TGActionContext context) throws TGActionException {
		boolean intercepted = this.disables(id);
		if( intercepted ){
			TuxGuitar.getInstance().updateCache(true);
		}
		return intercepted;
	}

	// one of its actions, while the player runs or a practice session is under way (see TGPracticeEdits.disables)
	private boolean disables(String id) {
		return (this.containsActionId(id) && TGPracticeEdits.disables(this.context, id));
	}

	// whether the items (menu items, toolbar buttons) of the action are greyed out: one of its actions, during a practice
	// session, playing or paused, unless the session lets it through (see TGPracticeEdits.greys)
	public boolean greys(String id) {
		return (this.containsActionId(id) && TGPracticeEdits.greys(this.context, id));
	}
}
