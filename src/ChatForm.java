import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class ChatForm extends MPForm {

	String id;
	int limit = 20;
	
	public ChatForm(String id) {
		super(id);
		this.id = id;
	}

	void loadInternal(Thread thread) throws Exception {
		// TODO
		deleteAll();
		
		StringBuffer sb = new StringBuffer("getHistory&media=1&peer=");
		sb.append(id);
		if (limit != 0) {
			sb.append("&limit=").append(limit);
		}
		
		JSONObject j = (JSONObject) MP.api(sb.toString());
		
		if (thread != this.thread) throw MP.cancelException;
		
		MP.fillPeersCache(j.getNullableObject("users"), j.getNullableObject("chats"));
		
		setTitle(MP.getName(id));
		
		JSONArray messages = j.getArray("messages");
		int l = messages.size();
		
		StringItem s;
		
		for (int i = 0; i < l && thread == this.thread; ++i) {
			JSONObject message = messages.getObject(i);

			String fromId = message.has("from_id") ? message.getString("from_id") : id;
			boolean out = message.getBoolean("out", false);
			
			s = new StringItem(null, MP.getShortName(fromId));
			s.addCommand(MP.peerItemCmd);
			s.setItemCommandListener(MP.midlet);
			safeAppend(thread, s);
			
			s = new StringItem(null, message.getString("text", ""));
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			safeAppend(thread, s);
		}
	}

}
