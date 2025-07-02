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
import javax.microedition.lcdui.Ticker;

// extends Displayable
interface MPChat {
	
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
	MPChat parent();
	
	// setters
	void setParent(MPChat parent);
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
	
	void load();

}
