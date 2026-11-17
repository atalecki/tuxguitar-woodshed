package app.tuxguitar.io.tg;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.song.models.TGChannelParameter;
import app.tuxguitar.song.models.TGSong;

/**
 * The shared values of a song's channel parameters (TGChannelParameter.SHARED) in a TuxGuitar file: each kept once in
 * content.xml, as the text of a TGSharedValue (with its id) in the TGSharedValues node under TGSong, however
 * many parameters hold it; such a parameter's value is SHARED and the id. The same in format 2.0 and 3.0: an older
 * version reads that reference as the parameter's value and leaves the node alone.
 */
final class TGSharedValues {

	private static final String TAG_VALUES = "TGSharedValues";
	private static final String TAG_VALUE = "TGSharedValue";
	private static final String ID = "id";

	// the ids of the shared values written, by text, in the order first written
	private final Map<String, Integer> ids = new LinkedHashMap<String, Integer>();

	// the value a parameter is written with: a shared value's reference, any other value as it is
	String written(String value) {
		if (!isShared(value)) {
			return value;
		}
		String text = value.substring(TGChannelParameter.SHARED.length());
		Integer id = this.ids.get(text);
		if (id == null) {
			id = this.ids.size();
			this.ids.put(text, id);
		}
		return TGChannelParameter.SHARED + id;
	}

	private static boolean isShared(String value) {
		return (value != null && value.startsWith(TGChannelParameter.SHARED));
	}

	// the node of the values written, under the song's (none when no value is shared)
	void write(Document document, Node nodeSong) {
		if (this.ids.isEmpty()) {
			return;
		}
		Element values = document.createElement(TAG_VALUES);
		this.ids.forEach((text, id) -> {
			Element value = document.createElement(TAG_VALUE);
			value.setAttribute(ID, String.valueOf(id));
			value.setTextContent(text);
			values.appendChild(value);
		});
		nodeSong.appendChild(values);
	}

	// each parameter holding a reference gets its shared value back from the song's node; one whose value is not there
	// (an older version saved the file again without the node) is left out
	static void read(Node nodeSong, TGSong song) {
		Map<String, String> texts = texts(nodeSong);
		song.getChannels().forEachRemaining(channel -> resolve(channel, texts));
	}

	private static void resolve(TGChannel channel, Map<String, String> texts) {
		for (int i = channel.countParameters() - 1; i >= 0; i--) {
			TGChannelParameter parameter = channel.getParameter(i);
			if (isShared(parameter.getValue())) {
				String text = texts.get(parameter.getValue().substring(TGChannelParameter.SHARED.length()));
				if (text != null) {
					parameter.setValue(TGChannelParameter.SHARED + text);
				} else {
					channel.removeParameter(i);
				}
			}
		}
	}

	// the texts of the song's node by id
	private static Map<String, String> texts(Node nodeSong) {
		Map<String, String> texts = new HashMap<String, String>();
		NodeList values = ((Element) nodeSong).getElementsByTagName(TAG_VALUE);
		for (int i = 0; i < values.getLength(); i++) {
			Element value = (Element) values.item(i);
			texts.put(value.getAttribute(ID), value.getTextContent());
		}
		return texts;
	}
}
