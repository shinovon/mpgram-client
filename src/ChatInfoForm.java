import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONObject;

public class ChatInfoForm extends MPForm {

	String id;
	
	public ChatInfoForm(String id) {
		super(id);
		this.id = id;
	}

	void loadInternal(Thread thread) throws Exception {
		JSONObject peer = MP.getPeer(id, true);
		
		id = peer.getString("id");
		boolean user = id.charAt(0) != '-';
		String name = MP.getName(peer);
		setTitle(name);
		
		StringItem s;
		
		s = new StringItem(null, name);
		s.setFont(MP.medPlainFont);
		s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		append(s);
		
		JSONObject fullInfo = (JSONObject) MP.api("getFullInfo&id=".concat(id));
		
		append(fullInfo.format(0));
	}

}
