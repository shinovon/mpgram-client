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

	ChatsCanvas(int folder) {
		super();
		this.folder = folder;

		setTitle(MP.L[Lmpgram]);
		setCommandListener(MP.midlet);
		addCommand(MP.backCmd);
		addCommand(MP.foldersCmd);
		addCommand(MP.refreshCmd);
	}

	void loadInternal(Thread thread) throws Exception {
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

			UIDialog item = new UIDialog(dialog);
			table.put(id, item);

			if (dialog.getBoolean("pin", false))
				++pinnedCount;

			safeAdd(thread, item, false);
			if (MP.loadAvatars) MP.queueAvatar(id, item);
		}
	}

	void changeFolder(int folderId, String title) {
		cancel();
		setTitle(title.concat(" - mpgram"));
//		offsetDate = 0;
		folder = folderId;
		MP.midlet.start(MP.RUN_LOAD_FORM, this);
	}
}
//#endif
