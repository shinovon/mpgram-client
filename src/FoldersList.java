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
		JSONArray folders = j.getArray("folders", null);
		if (folders != null) {
			this.folders = folders;
			int l = folders.size();
			for (int i = 0; i < l; ++i) {
				safeAppend(thread, folders.getObject(i).getString("t", "All chats"), null);
			}
		} else {
			safeAppend(thread, "All chats", null);
		}
		if (hasArchive = j.getBoolean("archive", false)) {
			safeAppend(thread, "Archive", null);
		}
	}

	void select(int i) {
		if (i == -1) return;
		if (hasArchive && i == size() - 1) {
			MP.chatsList.changeFolder(1, getString(i));
			MP.midlet.commandAction(MP.backCmd, this);
			return;
		}
		int folderId = folders == null ? 0 : folders.getObject(i).getInt("id", 0);
		MP.chatsList.changeFolder(folderId, getString(i));
		MP.midlet.commandAction(MP.backCmd, this);
	}

}
