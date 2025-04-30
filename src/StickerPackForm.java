import java.util.Hashtable;

import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;

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
public class StickerPackForm extends MPForm {

	String id;
	String accessHash;
	String slug;
	ChatForm chatForm;
	
	public StickerPackForm(ChatForm chatForm, JSONObject json) {
		super(json.getString("title", json.getString("short_name", MP.L[Stickers_Title])));
		this.id = json.getString("id");
		this.accessHash = json.getString("access_hash");
		this.chatForm = chatForm;
	}
	
	public StickerPackForm(String slug) {
		super(slug);
		this.slug = slug;
	}

	void loadInternal(Thread thread) throws Exception {
		StringBuffer sb = new StringBuffer("getStickerSet&");
		if (slug != null) {
			sb.append("slug=").append(slug);
		} else {
			sb.append("id=").append(id)
			.append("&access_hash=").append(accessHash);
		}
		
		JSONObject j = (JSONObject) MP.api(sb.toString());
		
		id = j.getString("id");
		accessHash = j.getString("access_hash");
		
		if (!j.has("installed")) {
			addCommand(MP.addStickerPackCmd);
		}
		
		JSONArray arr = j.getArray("res");
		int l = arr.size();
		urls = new Hashtable();
		
		for (int i = 0; i < l; ++i) {
			JSONObject s = arr.getObject(i);
			
			ImageItem img = new ImageItem("", null, Item.LAYOUT_LEFT | Item.LAYOUT_TOP, null);
			if (chatForm != null) {
				img.setDefaultCommand(MP.stickerItemCmd);
				img.setItemCommandListener(MP.midlet);
			}
			safeAppend(thread, img);
			urls.put(img, s);
			
			MP.queueImage(s, img);
		}
	}

}
//#endif
