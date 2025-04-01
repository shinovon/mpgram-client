import java.util.Vector;

import javax.microedition.lcdui.List;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class ChatsList extends MPList {
	
	int limit = 20;
	int folder = -1;
	Vector ids;

	public ChatsList(String title, int folder) {
		super(title);
		this.folder = folder;
//		if (folder == 0) {
//			addCommand(MP.archiveCmd);
//		}
		addCommand(MP.foldersCmd);
		addCommand(MP.refreshCmd);
		setFitPolicy(List.TEXT_WRAP_ON);
	}

	void loadInternal(Thread thread) throws Exception {
		deleteAll();
		ids = new Vector();
		
		StringBuffer sb = new StringBuffer("getDialogs");
		if (limit != 0) {
			sb.append("&limit=").append(limit);
		}
		if (folder != -1) {
			sb.append("&f=").append(folder);
		}
		
		JSONObject j = (JSONObject) MP.api(sb.toString());
		MP.fillPeersCache(j);
		
		if (thread != this.thread) throw MP.cancelException;
		
		JSONArray dialogs = j.getArray("dialogs");
		int l = dialogs.size();
		
//		JSONObject messages = j.getObject("messages");
		
		for (int i = 0; i < l && thread == this.thread; ++i) {
			JSONObject dialog = dialogs.getObject(i);
			String id = dialog.getString("id");
			ids.addElement(id);
			JSONObject peer = MP.getPeer(id, false);
			
			sb.setLength(0);
			String name = MP.getName(peer);
			MP.appendOneLine(sb, name);
			
			JSONObject message = dialog.getObject("msg", null)/*messages.getObject(id)*/;
			if (message != null) {
				sb.append('\n');
				if (!peer.getBoolean("c", false)) {
					if (message.getBoolean("out", false)) {
						sb.append("You: ");
					} else if (id.charAt(0) == '-' && message.has("from_id")) {
						MP.appendOneLine(sb, MP.getName(message.getString("from_id"), true)).append(": ");
					}
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
	
	void changeFolder(int folderId) {
		cancel();
		folder = folderId;
		MP.midlet.start(MP.RUN_LOAD_LIST, this);
	}
	
	void shown() {
		if (!loaded || ids == null) return;
		for (int i = ids.size() - 1; i >= 0; i--) {
			if (getImage(i) != null) continue; // TODO break?
			MP.queueAvatar((String) ids.elementAt(i), new Object[] { this, new Integer(i) });
		}
	}

}
