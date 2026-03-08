/*
Copyright (c) 2025-2026 Arman Jussupgaliyev

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

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

public class ChatsList extends MPList {
	
	int limit = MP.chatsLimit;
	int folder = -1;
//	long firstMsgDate, lastMsgDate;
//	long offsetDate;
	int offset;
	Vector/*<String>*/ ids;
	String url;
	boolean users;
	boolean noAvas;
	boolean canBan;
	String peerId, msgId;
	int pinnedCount;
	final Object lock = new Object();
	boolean invite;

	// main mode
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
	
	// contacts, members
	public ChatsList(String title, String url, String peerId, boolean canBan) {
		super(title);
		this.url = url;
		this.users = true;
		this.noAvas = true;
		this.peerId = peerId;
		this.canBan = canBan;
		addCommand(MP.backCmd);
		setFitPolicy(List.TEXT_WRAP_ON);
		if (canBan) addCommand(MP.banMemberCmd);
	}
	
	// forward message mode
	public ChatsList(String peerId, String msgId) {
		super(MP.L[LForward]);
		this.folder = 0;
		this.peerId = peerId;
		this.msgId = msgId;
		addCommand(MP.archiveCmd);
		addCommand(MP.cancelCmd);
//		setFitPolicy(List.TEXT_WRAP_ON);
	}

	// invite contacts to chat mode
	public ChatsList() {
		super("Add Member"); // TODO unlocalized
		this.url = "getContacts&fields=status";
		this.users = true;
		this.noAvas = true;
		this.invite = true;
		addCommand(MP.backCmd);
		setFitPolicy(List.TEXT_WRAP_ON);
	}

	void loadInternal(Thread thread) throws Exception {
		deleteAll();
		ids = new Vector();
		pinnedCount = 0;
		
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
			JSONArray users = j.getArray("res");
			int l = users.size();

			for (int i = 0; i < l && thread == this.thread; ++i) {
				JSONObject user = users.getObject(i);
				
				String id = user.getString("id");
				ids.addElement(id);

				sb.setLength(0);
				try {
					MP.appendOneLine(sb, MP.getName(user, false));
				} catch (Exception e) {
					// TODO add name placeholder
//					sb.append("");
				}

				if (user.has("date")) {
					sb.append('\n').append(MP.localizeDate(user.getLong("date"), 3));
				} else {
					if (user.getBoolean("b", false)) { // bot
						sb.append('\n').append(MP.L[LBot]);
					} else if (user.has("s")) { // status
						long wasOnline;
						if (user.getBoolean("s")) {
							sb.append('\n').append(MP.L[LOnline]);
						} else if ((wasOnline = user.getLong("w")) != 0) {
							sb.append('\n').append(MP.L[LLastSeen]).append(MP.localizeDate(wasOnline, 4));
						} else {
							sb.append('\n').append(MP.L[LOffline]);
						}
					}
					if (user.getBoolean("a", false)) { // admin
						sb.append(" (").append(MP.L[LAdmin]).append(')');
					}
				}
				
				insert(thread, size(), sb.toString(), id, null);
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
			
			if (dialog.getBoolean("pin", false))
				++pinnedCount;

			sb.setLength(0);
			JSONObject peer = MP.getPeer(id, false);
			String name = MP.getName(peer, false);
			MP.appendOneLine(sb, name);
			
			if (msgId == null) {
				int unread = dialog.getInt("unread", 0);
				if (unread != 0) sb.append(" +").append(unread);
				MP.appendDialog(sb.append('\n'), peer, id, dialog.getObject("msg", null));
			}
			
			insert(thread, size(), sb.toString(), id, null);
		}
	}
	
	void insert(Thread thread, int idx, String s, String id, Image img) {
		synchronized (lock) {
//#ifndef NO_AVATARS
			boolean loadAva;
			if ((loadAva = (img == null))) {
				img = id.charAt(0) == '-' ? MP.chatDefaultImg : MP.userDefaultImg;
			}
//#endif
			safeInsert(thread, idx, s, img);
			if (MP.chatsListFontSize != 0) {
				try {
					setFont(idx, MP.chatsListFontSize == 1 ? MP.smallPlainFont : MP.medPlainFont);
				} catch (Exception ignored) {}
			}
//#ifndef NO_AVATARS
			if (noAvas || !MP.loadAvatars || !loadAva) return;
			MP.queueAvatar(id, new Object[] { this, id });
//#endif
		}
	}
	
//#ifndef NO_AVATARS
	void setImage(String id, Image img) {
		synchronized (lock) {
			int i = ids.indexOf(id);
			if (i == -1) return;
			
			super.set(i, getString(i), img);
		}
	}
//#endif
	
	void select(int i) {
		if (i == -1) return;
		String id = (String) ids.elementAt(i);
		if (id == null) return;

		if (invite) {
			MP.deleteFromHistory(this);

			if (!(MP.current instanceof ChatInfoForm)) {
				// something terrible happened
				return;
			}

			((ChatInfoForm) MP.current).chatForm.invite(id);
			return;
		}
		
		if (msgId != null) {
			// forward
			MP.deleteFromHistory(this);

			// TODO topics
			MP.openChat(id, 0);
			MP.display(MP.writeForm(id, null, "", null, peerId, msgId));
			return;
		}
		
		MP.openChat(id, -1);
	}
	
	protected void sizeChanged(int w, int h) {
		super.sizeChanged(w, h);
		
		if (!finished || ids == null) return;
		for (int i = ids.size() - 1; i >= 0; i--) {
			if (MP.chatsListFontSize != 0) {
				try {
					setFont(i, MP.chatsListFontSize == 1 ? MP.smallPlainFont : MP.medPlainFont);
				} catch (Exception ignored) {}
			}
		}
	}
	
	void changeFolder(int folderId, String title) {
		cancel();
		setTitle(title.concat(" - mpgram"));
//		offsetDate = 0;
		folder = folderId;
		MP.midlet.start(MP.RUN_LOAD_FORM, this);
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
		}
	}
	
	void shown() {
		if (!finished || ids == null || ((noAvas || !MP.loadAvatars) && MP.chatsListFontSize == 0))
			return;
		for (int i = ids.size() - 1; i >= 0; i--) {
			if (MP.chatsListFontSize != 0) {
				try {
					setFont(i, MP.chatsListFontSize == 1 ? MP.smallPlainFont : MP.medPlainFont);
				} catch (Exception ignored) {}
			}
			if (getImage(i) != null) continue;
			MP.queueAvatar((String) ids.elementAt(i), new Object[] { this, new Integer(i) });
		}
	}

}
