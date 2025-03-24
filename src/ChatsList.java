import javax.microedition.lcdui.List;

public class ChatsList extends MPList {

	public ChatsList(String title) {
		super(title, List.IMPLICIT);
		setFitPolicy(List.TEXT_WRAP_ON);
	}

	void loadInternal(Thread thread) throws Exception {
		
	}

}
