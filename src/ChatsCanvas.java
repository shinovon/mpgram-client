import java.util.Hashtable;

public class ChatsCanvas extends MPCanvas {

	static final int COLOR_CHATS_BG = 40;
	static final int COLOR_CHATS_ITEM_HIGHLIGHT_BG = 41;
	static final int COLOR_CHATS_ITEM_TITLE = 42;
	static final int COLOR_CHATS_ITEM_TEXT = 43;
	static final int COLOR_CHATS_ITEM_MEDIA = 44;

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
