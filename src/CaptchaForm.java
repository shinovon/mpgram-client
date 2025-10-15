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

public class CaptchaForm extends MPForm {
	
	TextField field;
	String id;

	public CaptchaForm() {
		super(MP.L[LCaptcha]);
		addCommand(MP.backCmd);
		addCommand(MP.authNextCmd);
	}

	void loadInternal(Thread thread) throws Exception {
		deleteAll();
		
		id = ((JSONObject) MP.api("initLogin")).getString("captcha_id");
		
		Image img = MP.getImage(MP.instanceUrl.concat(MP.API_URL + "?v=" + MP.API_VERSION
				+ "&method=getCaptchaImg&captcha_id=".concat(id)));

		int[] imgSize = resizeFit(img.getWidth(), img.getHeight(), getWidth(), getHeight()*3/4, true);

		img = MP.resize(img, imgSize[0], imgSize[1]);
		
		ImageItem imgItem = new ImageItem("", img, 0, null);
		append(imgItem);
		
		field = new TextField("", "", 20, TextField.NON_PREDICTIVE);
		append(field);
		
		StringItem s = new StringItem(null, MP.L[LNext], StringItem.BUTTON);
		s.setDefaultCommand(MP.authNextCmd);
		s.setItemCommandListener(MP.midlet);
		append(s);
		
		MP.display(this);
	}

	// https://github.com/gtrxAC/discord-j2me/blob/e53e97f93c27682048687f4f18f13b8ae9fb24e6/src/com/gtrxac/discord/Util.java#L85
	public static int[] resizeFit(int imgW, int imgH, int maxW, int maxH, boolean mustUpscale) {
		int imgAspect = imgW*100 / imgH;
		int maxAspect = maxW*100 / maxH;
		int width, height;

		if (!mustUpscale && imgW <= maxW && imgH <= maxH) {
			width = imgW;
			height = imgH;
		}
		else if (imgAspect > maxAspect) {
			width = maxW;
			height = (maxW*100)/imgAspect;
		} else {
			height = maxH;
			width = (maxH*imgAspect)/100;
		}

		return new int[]{width, height};
	}
}
