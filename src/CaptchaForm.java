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
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import cc.nnproject.json.JSONObject;

public class CaptchaForm extends MPForm {
	
	TextField field;
	String id;

	public CaptchaForm() {
		super(MP.L[Captcha]);
		addCommand(MP.backCmd);
		addCommand(MP.authNextCmd);
	}

	void loadInternal(Thread thread) throws Exception {
		deleteAll();
		
		id = ((JSONObject) MP.api("initLogin")).getString("captcha_id");
		
		Image img = MP.getImage(MP.instanceUrl.concat(MP.API_URL + "?v=" + MP.API_VERSION
				+ "&method=getCaptchaImg&captcha_id=".concat(id)));
		
		ImageItem imgItem = new ImageItem("", img, 0, null);
		append(imgItem);
		
		field = new TextField("", "", 20, TextField.NON_PREDICTIVE);
		append(field);
		
		StringItem s = new StringItem(null, MP.L[Next], StringItem.BUTTON);
		s.setDefaultCommand(MP.authNextCmd);
		s.setItemCommandListener(MP.midlet);
		append(s);
		
		MP.display(this);
	}

}
