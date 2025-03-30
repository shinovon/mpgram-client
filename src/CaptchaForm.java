import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import cc.nnproject.json.JSONObject;

public class CaptchaForm extends MPForm {
	
	TextField field;
	String id;

	public CaptchaForm() {
		super("Captcha");
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
		
		StringItem s = new StringItem(null, "Next", StringItem.BUTTON);
		s.setDefaultCommand(MP.authNextCmd);
		s.setItemCommandListener(MP.midlet);
		append(s);
		
		MP.display(this);
	}

}
