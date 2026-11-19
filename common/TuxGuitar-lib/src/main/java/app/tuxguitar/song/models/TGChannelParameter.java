package app.tuxguitar.song.models;

import app.tuxguitar.song.factory.TGFactory;

public abstract class TGChannelParameter {

	// the start of a shared value: what follows it (a plugin's capture, say) a TuxGuitar file keeps once however many
	// parameters hold it
	public static final String SHARED = "tg-shared:";

	private String key;
	private String value;

	public TGChannelParameter(){
		super();
	}

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void copyFrom(TGChannelParameter channelParameter){
		this.setKey(channelParameter.getKey());
		this.setValue(channelParameter.getValue());
	}

	public TGChannelParameter clone(TGFactory factory){
		TGChannelParameter tgChannelParameter = factory.newChannelParameter();
		tgChannelParameter.copyFrom(this);
		return tgChannelParameter;
	}
}
