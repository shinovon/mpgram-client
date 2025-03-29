import java.util.Vector;

import javax.microedition.lcdui.List;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class ChatsList extends MPList {
	
	int limit = 20;
	Vector ids;

	public ChatsList(String title) {
		super(title, List.IMPLICIT);
		setFitPolicy(List.TEXT_WRAP_ON);
	}

	void loadInternal(Thread thread) throws Exception {
		// TODO folders
		
		deleteAll();
		ids = new Vector();
		
		StringBuffer sb = new StringBuffer("getDialogs&");
		if (limit != 0) {
			sb.append("limit=").append(limit);
		}
		
		JSONObject j = (JSONObject) MP.api(sb.toString());
		
		if (thread != this.thread) throw MP.cancelException;
		
		MP.fillPeersCache(j.getNullableObject("users"), j.getNullableObject("chats"));
		
		JSONArray dialogs = j.getArray("dialogs");
		int l = dialogs.size();
		
		JSONObject messages = j.getObject("messages");
		
		for (int i = 0; i < l && thread == this.thread; ++i) {
			JSONObject dialog = dialogs.getObject(i);
			String id = dialog.getString("id");
			
			ids.addElement(id);
			
			JSONObject message = messages.getObject(id);
			String name = MP.getName(id);
			
			sb.setLength(0);
			MP.appendOneLine(sb, name).append('\n');
			
			if (message.getBoolean("out", false)) {
				sb.append("You: ");
			} else if (id.charAt(0) == '-' && message.has("from_id")) {
				MP.appendOneLine(sb, MP.getShortName(message.getString("from_id"))).append(": ");
			}
			if (message.has("media")) {
				sb.append("Media");
			} else if (message.has("fwd")) {
				sb.append("Forwarded message");
			} else  if (message.has("act")) {
				sb.append("Action");
			} else {
				MP.appendOneLine(sb, message.getString("text"));
			}
			
			int itemIdx = safeAppend(thread, sb.toString(), null);
			
			MP.queueAvatar(id, new Object[] { this, new Integer(itemIdx) });
		}
	}
	
	void select(int i) {
		if (i == -1) return;
		String id = (String) ids.elementAt(i);
		if (id == null) return;
		
		MP.openChat(id);
	}

}
