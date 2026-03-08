/*
Copyright (c) 2026 Arman Jussupgaliyev

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
//#ifndef NO_CHAT_CANVAS
import java.util.Hashtable;

public class ChatsCanvas extends MPCanvas {

	static final int COLOR_CHATS_BG = 40;

	int limit = MP.chatsLimit;
	int folder = -1;

	Hashtable table;
	int pinnedCount;
	String peerId, msgId;
	UIMessage[] msgs;

	// main mode
	ChatsCanvas(String title, int folder) {
		super();
		this.folder = folder;

		setTitle(title);
		setCommandListener(MP.midlet);
		addCommand(MP.backCmd);
		addCommand(MP.foldersCmd);
		addCommand(MP.refreshCmd);
	}

	// forward message mode
	public ChatsCanvas(String peerId, String msgId) {
		super();
		setTitle(MP.L[LForward]);
		this.folder = 0;
		this.peerId = peerId;
		this.msgId = msgId;
		addCommand(MP.archiveCmd);
		addCommand(MP.cancelCmd);
	}

	// forward messages
	public ChatsCanvas(String peerId, UIMessage[] msgs) {
		super();
		setTitle(MP.L[LForward]);
		this.folder = 0;
		this.peerId = peerId;
		this.msgId = "";
		this.msgs = msgs;
		addCommand(MP.archiveCmd);
		addCommand(MP.cancelCmd);
	}

	boolean loadInternal(Thread thread) throws Exception {
		table = new Hashtable();
		pinnedCount = 0;

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

		for (int i = 0; i < l && thread == this.thread; ++i) {
			JSONObject dialog = dialogs.getObject(i);
			String id = dialog.getString("id");

			UIDialog item = new UIDialog(dialog, msgId == null);
			table.put(id, item);

			if (dialog.getBoolean("pin", false))
				++pinnedCount;

			safeAdd(thread, item, i == 0);
			if (MP.loadAvatars) MP.queueAvatar(id, item);
		}
		return true;
	}

	void changeFolder(int folderId, String title) {
		cancel();
		setTitle(title.concat(" - mpgram"));
//		offsetDate = 0;
		folder = folderId;
		MP.midlet.start(MP.RUN_LOAD_FORM, this);
	}

	void select(UIDialog dialog) {
		String id = dialog.id;

		if (msgId != null) {
			// forward
			MP.deleteFromHistory(this);
			if (MP.current instanceof ChatCanvas && id.equals(((ChatCanvas) MP.current).id)) {
				((ChatCanvas) MP.current).startForward(peerId, msgId, msgs);
				return;
			}
			if (msgs != null) {
				MP.openLoad(new ChatCanvas(id, 0, msgs));
				return;
			}
			MP.openLoad(new ChatCanvas(id, 0, peerId, msgId));
			return;
		}

		MP.openChat(id, -1);
	}
}
//#endif
