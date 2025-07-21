import javax.microedition.lcdui.List;

public class ChatTopicsList extends List {
	
	MPChat chatForm;

	ChatTopicsList(MPChat chatForm, String title) {
		super(title, List.IMPLICIT);
		this.chatForm = chatForm;
		addCommand(MP.backCmd);
		addCommand(MP.chatInfoCmd);
		addCommand(List.SELECT_COMMAND);
//#ifndef MIDP1
		setSelectCommand(List.SELECT_COMMAND);
		setFitPolicy(List.TEXT_WRAP_ON);
//#endif
		setCommandListener(MP.midlet);
	}

}
