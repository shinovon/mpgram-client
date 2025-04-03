/*
Copyright (c) 2025 Arman Jussupgaliyev

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
import java.util.Vector;

import javax.microedition.lcdui.List;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class ChatsList extends MPList implements LangConstants {
	
	int limit = MP.chatsLimit;
	int folder = -1;
//	long firstMsgDate, lastMsgDate;
//	long offsetDate;
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
//		if (offsetDate != 0) {
//			sb.append("&offset_date=").append(offsetDate);
//			addCommand(MP.prevPageCmd);
//		} else {
//			removeCommand(MP.prevPageCmd);
//		}
		
		JSONObject j = (JSONObject) MP.api(sb.toString());
		MP.fillPeersCache(j);
		
		if (thread != this.thread) throw MP.cancelException;
		
		JSONArray dialogs = j.getArray("dialogs");
		int l = dialogs.size();
		
//		if (l == limit) {
//			addCommand(MP.nextPageCmd);
//		} else {
//			removeCommand(MP.nextPageCmd);
//		}
		
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
//				if (i == 0) {
//					firstMsgDate = message.getLong("date");
//				} else if (i == l - 1) {
//					lastMsgDate = message.getLong("date");
//				}
				sb.append('\n');
				if (!peer.getBoolean("c", false)) {
					if (message.getBoolean("out", false)) {
						sb.append(MP.L[You_Prefix]);
					} else if (id.charAt(0) == '-' && message.has("from_id")) {
						MP.appendOneLine(sb, MP.getName(message.getString("from_id"), true)).append(": ");
					}
				}
				if (message.has("media")) {
					sb.append(MP.L[Media]);
				} else if (message.has("fwd")) {
					sb.append(MP.L[ForwardedMessage]);
				} else  if (message.has("act")) {
					sb.append(MP.L[Action]);
				} else {
					MP.appendOneLine(sb, message.getString("text"));
				}
			}
			
			int itemIdx = safeAppend(thread, sb.toString(), null);
			
			if (!MP.loadAvatars) continue;
			MP.queueAvatar(id, new Object[] { this, new Integer(itemIdx) });
		}
	}
	
	void select(int i) {
		if (i == -1) return;
		String id = (String) ids.elementAt(i);
		if (id == null) return;
		
		MP.openChat(id);
	}
	
	void changeFolder(int folderId, String title) {
		cancel();
		setTitle(title.concat(" - mpgram"));
//		offsetDate = 0;
		folder = folderId;
		MP.midlet.start(MP.RUN_LOAD_LIST, this);
	}
	
//	void paginate(int dir) {
//		if (dir == 1) {
//			offsetDate = lastMsgDate;
//		} else if (dir == -1) {
//			offsetDate = firstMsgDate;
//		} else {
//			offsetDate = 0;
//		}
//	}
	
	void shown() {
		if (!finished || ids == null) return;
		for (int i = ids.size() - 1; i >= 0; i--) {
			if (getImage(i) != null) continue; // TODO break?
			MP.queueAvatar((String) ids.elementAt(i), new Object[] { this, new Integer(i) });
		}
	}

}
