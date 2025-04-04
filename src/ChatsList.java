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
	int offset;
	Vector ids;
	String url;
	String arrayName;
	boolean users;
	boolean noAvas;

	public ChatsList(String title, int folder) {
		super(title);
		this.folder = folder;
//		if (folder == 0) {
//			addCommand(MP.archiveCmd);
//		}
		addCommand(MP.backCmd);
		addCommand(MP.foldersCmd);
		addCommand(MP.refreshCmd);
		setFitPolicy(List.TEXT_WRAP_ON);
	}
	
	public ChatsList(String title, String url, String arrayName) {
		super(title);
		this.url = url;
		this.arrayName = arrayName;
		this.users = true;
		this.noAvas = true;
		addCommand(MP.backCmd);
		setFitPolicy(List.TEXT_WRAP_ON);
	}

	void loadInternal(Thread thread) throws Exception {
		deleteAll();
		ids = new Vector();
		
		StringBuffer sb = new StringBuffer(url != null ? url : "getDialogs");
		if (limit != 0) {
			sb.append("&limit=").append(limit);
		}
		if (folder != -1) {
			sb.append("&f=").append(folder);
		}
		if (offset != 0) {
			sb.append("&offset=").append(offset);
			addCommand(MP.prevPageCmd);
		} else {
			removeCommand(MP.prevPageCmd);
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
		
		if (users) {
			if (j.has("res")) j = j.getObject("res");
			
			JSONArray users = j.getArray(arrayName != null ? arrayName : "users");
			int l = users.size();

			for (int i = 0; i < l && thread == this.thread; ++i) {
				JSONObject user = users.getObject(i);
				
				String id = user.getString("id");
				ids.addElement(id);

				sb.setLength(0);
				MP.appendOneLine(sb, MP.getName(user));
				
				if (user.has("s")) {
					long wasOnline;
					if (user.getBoolean("s")) {
						sb.append('\n').append(MP.L[Online]);
					} else if ((wasOnline = user.getLong("w")) != 0) {
						sb.append('\n').append(MP.L[LastSeen]).append(MP.localizeDate(wasOnline, 4));
					} else {
						sb.append('\n').append(MP.L[Offline]);
					}
				}
				
				int itemIdx = safeAppend(thread, sb.toString(), null);
				
				if (noAvas || !MP.loadAvatars) continue;
				MP.queueAvatar(id, new Object[] { this, new Integer(itemIdx) });
			}
			
			if (l == limit && j.has("count")) {
				addCommand(MP.nextPageCmd);
			} else {
				removeCommand(MP.nextPageCmd);
			}
			return;
		}
		
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
			if (dialog.has("unread")) {
				sb.append(" +").append(dialog.getInt("unread"));
			}
			
			JSONObject message = dialog.getObject("msg", null)/*messages.getObject(id)*/;
			if (message != null) {
				sb.append('\n')
				.append(MP.localizeDate(message.getLong("date"), 2)).append(' ');
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
			
			if (noAvas || !MP.loadAvatars) continue;
			MP.queueAvatar(id, new Object[] { this, new Integer(itemIdx) });
		}
	}
	
	void select(int i) {
		if (i == -1) return;
		String id = (String) ids.elementAt(i);
		if (id == null) return;
		
		MP.openChat(id, -1);
	}
	
	void changeFolder(int folderId, String title) {
		cancel();
		setTitle(title.concat(" - mpgram"));
//		offsetDate = 0;
		folder = folderId;
		MP.midlet.start(MP.RUN_LOAD_LIST, this);
	}
	
	void paginate(int dir) {
		cancel();
		if (users) {
			if (dir == 1) {
				offset += limit;
			} else if (dir == -1) {
				if ((offset -= limit) < 20) {
					offset = 0;
				}
			} else {
				offset = 0;
			}
			load();
			return;
		}
	}
	
	void shown() {
		if (!finished || ids == null || noAvas) return;
		for (int i = ids.size() - 1; i >= 0; i--) {
			if (getImage(i) != null) continue; // TODO break?
			MP.queueAvatar((String) ids.elementAt(i), new Object[] { this, new Integer(i) });
		}
	}

}
