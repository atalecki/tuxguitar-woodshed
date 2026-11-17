package app.tuxguitar.vst3;

/**
 * What a plugin told its host: an edit from its own editor (begin, perform with the parameter's plain value, end; a
 * gesture runs from the first begin to the last end), a restart (its latency changed, say), with the flags, or that it
 * is dirty (its state changed outside any edit: a capture loaded, say). Or that its own editor's window was hidden by
 * its close box, which the host tells, as what the editor did there may not have been told.
 */
public final class TGVst3Notice {

	// in the order the host numbers them, from 1
	public enum Kind {
		BEGIN_EDIT, PERFORM_EDIT, END_EDIT, RESTART, DIRTY, EDITOR_HIDDEN
	}

	private final Kind kind;
	private final int parameter;
	private final double plain;

	public TGVst3Notice(Kind kind, int parameter, double plain) {
		this.kind = kind;
		this.parameter = parameter;
		this.plain = plain;
	}

	public Kind getKind() {
		return this.kind;
	}

	// the parameter's id, or a restart's flags
	public int getParameter() {
		return this.parameter;
	}

	public double getPlain() {
		return this.plain;
	}
}
