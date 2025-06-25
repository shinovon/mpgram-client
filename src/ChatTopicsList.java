import javax.microedition.lcdui.List;

public class ChatTopicsList extends List {
	
	ChatInterface chatForm;

	ChatTopicsList(ChatInterface chatForm, String title) {
		super(title, List.IMPLICIT);
		this.chatForm = chatForm;
		setFitPolicy(List.TEXT_WRAP_ON);
		addCommand(MP.backCmd);
		addCommand(MP.chatInfoCmd);
		addCommand(List.SELECT_COMMAND);
		setSelectCommand(List.SELECT_COMMAND);
		setCommandListener(MP.midlet);
	}

}
