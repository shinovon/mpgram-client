import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class FoldersList extends MPList {
	
	boolean hasArchive;
	JSONArray folders;

	public FoldersList() {
		super("Folders");
		addCommand(MP.backCmd);
	}

	void loadInternal(Thread thread) throws Exception {
		JSONObject j = (JSONObject) MP.api("getFolders");
		if (hasArchive = j.getBoolean("archive", false)) {
			safeAppend(thread, "Archive", null);
		}
		folders = j.getArray("folders", null);
		if (folders != null) {
			int l = folders.size();
			for (int i = 0; i < l; ++i) {
				safeAppend(thread, folders.getObject(i).getString("t", "All chats"), null);
			}
		} else {
			safeAppend(thread, "All chats", null);
		}
	}

	void select(int i) {
		if (i == -1) return;
		if (hasArchive) {
			if (i == 0) {
				MP.chatsList.changeFolder(1);
				MP.midlet.commandAction(MP.backCmd, this);
				return;
			}
			i--;
		}
		int folderId = folders == null ? 0 : folders.getObject(i).getInt("id", 0);
		MP.chatsList.changeFolder(folderId);
		MP.midlet.commandAction(MP.backCmd, this);
	}

}
