
public class CaptchaForm extends MPForm {

	public CaptchaForm() {
		super("Captcha");
		addCommand(MP.backCmd);
		addCommand(MP.nextCmd);
	}

	void loadInternal(Thread thread) throws Exception {
		deleteAll();
		
		// TODO
	}

}
