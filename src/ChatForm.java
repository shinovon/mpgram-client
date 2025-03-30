import java.util.Hashtable;

import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.Spacer;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class ChatForm extends MPForm {

	String id;
	int limit = 20;
	
	public ChatForm(String id) {
		super(id);
		addCommand(MP.refreshCmd);
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
		
		urls = new Hashtable();
		
		MP.fillPeersCache(j.getNullableObject("users"), j.getNullableObject("chats"));
		
		setTitle(MP.getName(id));
		
		JSONArray messages = j.getArray("messages");
		int l = messages.size();
		
		StringItem s;
		
		for (int i = 0; i < l && thread == this.thread; ++i) {
			JSONObject message = messages.getObject(i);
			
			String id = message.getString("id");
			String fromId = message.has("from_id") ? message.getString("from_id") : id;
			boolean out = message.getBoolean("out", false);
			
			s = new StringItem(null, MP.getShortName(fromId));
			s.addCommand(MP.itemChatCmd);
			s.addCommand(MP.itemChatInfoCmd);
			s.addCommand(MP.replyMsgCmd);
			s.addCommand(MP.forwardMsgCmd);
			
			s.setDefaultCommand(MP.itemChatCmd);
			s.setItemCommandListener(MP.midlet);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			safeAppend(thread, s);
			urls.put(s, new String[] { fromId, id } );
			
			String text = message.getString("text", null);
			if (text != null && text.length() != 0) {
				s.addCommand(MP.copyMsgCmd);
				urls.put(id, text);
				
				if (message.has("entities")) {
					MP.wrapRichText(this, thread, text, message.getArray("entities"));
				} else {
					s = new StringItem(null, text);
					s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					safeAppend(thread, s);
				}
			}
			safeAppend(thread, new Spacer(10, 6));
		}
	}

}
