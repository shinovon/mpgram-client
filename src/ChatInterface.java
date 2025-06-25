import javax.microedition.lcdui.Ticker;

// extends Displayable
interface ChatInterface {
	
	static final int UPDATE_USER_STATUS = 1;
	static final int UPDATE_USER_TYPING = 2;
	static final int UPDATE_NEW_MESSAGE = 3;
	static final int UPDATE_DELETE_MESSAGES = 4;
	static final int UPDATE_EDIT_MESSAGE = 5;
	
	// getters
	String id();
	String postId();
	String query();
	String mediaFilter();
	String username();
	boolean update();
	boolean endReached();
	boolean forum();
	boolean switched();
	int topMsgId();
	int firstMsgId();
	JSONArray topics();
	ChatInterface parent();
	
	// setters
	void setParent(ChatInterface parent);
	void setQuery(String s);
	void setUpdate(boolean b);
	void setBotAnswer(JSONObject j);
	
	// from Displayable
	String getTitle();
	boolean isShown();
	Ticker getTicker();
	void setTicker(Ticker t);
	
	void reset();
	void openMessage(String msg, int topMsg);
	void sent();
	void handleUpdate(int type, JSONObject update);
	void handleBotAnswer(JSONObject j);
	void paginate(int dir);
	void openTopic(int topMsgId, boolean canWrite, String title);

}
