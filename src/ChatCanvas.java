import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

public class ChatCanvas extends Canvas implements ChatInterface {

	Thread thread;
	
	String id;
	String username;
	String query;
	String startBot;
	String title;
	String mediaFilter;
	
	int limit = MP.messagesLimit;
	int addOffset, offsetId;
	int messageId, topMsgId;
	
	void load() {
		// TODO
	}
	
	void closed(boolean destroy) {
		if (destroy) cancel();
	}
	
	void cancel() {
		
	}
	
	// Canvas

	protected void paint(Graphics g) {
		// TODO Auto-generated method stub
		
	}
	
	protected void keyPressed(int key) {
		
	}
	
	protected void keyReleased(int key) {
		
	}
	
	protected void pointerPressed(int x, int y) {
		
	}
	
	protected void pointerDragged(int x, int y) {
		
	}
	
	protected void pointerReleased(int x, int y) {
		
	}
	
	// interface getters

	public String id() {
		return id;
	}

	public String postId() {
		// TODO Auto-generated method stub
		return null;
	}

	public String query() {
		return query;
	}

	public String mediaFilter() {
		return mediaFilter;
	}

	public String username() {
		return username;
	}

	public boolean update() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean endReached() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean forum() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean switched() {
		// TODO Auto-generated method stub
		return false;
	}

	public int topMsgId() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int firstMsgId() {
		// TODO Auto-generated method stub
		return 0;
	}

	public JSONArray topics() {
		// TODO Auto-generated method stub
		return null;
	}

	public ChatInterface parent() {
		// TODO Auto-generated method stub
		return null;
	}
	
	// interface setters

	public void setParent(ChatInterface parent) {
		// TODO Auto-generated method stub
		
	}

	public void setQuery(String s) {
		// TODO Auto-generated method stub
		
	}

	public void setUpdate(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public void setBotAnswer(JSONObject j) {
		// TODO Auto-generated method stub
		
	}
	
	//

	public void reset() {
		cancel();
		
	}

	public void openMessage(String msg, int topMsg) {
		// TODO Auto-generated method stub
		
	}

	public void sent() {
		// TODO Auto-generated method stub
		
	}

	public void handleUpdate(int type, JSONObject update) {
		// TODO Auto-generated method stub
		
	}

	public void handleBotAnswer(JSONObject j) {
		// TODO Auto-generated method stub
		
	}

	public void paginate(int dir) {
		// TODO Auto-generated method stub
		
	}

	public void openTopic(int topMsgId, boolean canWrite, String title) {
		// TODO Auto-generated method stub
		
	}

}
