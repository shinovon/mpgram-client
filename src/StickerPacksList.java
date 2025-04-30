import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

/*
Copyright (c) 2022-2025 Arman Jussupgaliyev

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
//#ifndef NO_STICKERS
public class StickerPacksList extends MPList {
	
	JSONArray sets;
	ChatForm chatForm;

	public StickerPacksList(ChatForm form) {
		super(MP.L[Stickers_Title]);
		this.chatForm = form;
		addCommand(MP.backCmd);
	}

	void loadInternal(Thread thread) throws Exception {
		JSONObject j = (JSONObject) MP.api("getStickerSets");
		
		JSONArray sets = this.sets = j.getArray("res");
		int l = sets.size();
		
		for (int i = 0; i < l; ++i) {
			safeAppend(thread, sets.getObject(i).getString("title"), null);
		}
	}

	void select(int i) {
		if (i == -1) return;
		MP.openLoad(new StickerPackForm(chatForm, sets.getObject(i)));
	}

}
//#endif
