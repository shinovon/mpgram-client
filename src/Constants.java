/*
Copyright (c) 2026 Arman Jussupgaliyev

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
public interface Constants {
	
	// threading tasks
	int RUN_SEND_MESSAGE = 1;
	int RUN_VALIDATE_AUTH = 2;
	int RUN_IMAGES = 3;
	int RUN_LOAD_FORM = 4;
	int RUN_AUTH = 6;
	int RUN_DELETE_MESSAGE = 7;
	int RUN_RESOLVE_INVITE = 8;
	int RUN_IMPORT_INVITE = 9;
	int RUN_JOIN_CHANNEL = 10;
	int RUN_LEAVE_CHANNEL = 11;
	int RUN_CHECK_OTA = 12;
	int RUN_CHAT_UPDATES = 13;
	int RUN_SET_TYPING = 14;
	int RUN_KEEP_ALIVE = 15;
	int RUN_CLOSE_CONNECTION = 16;
	int RUN_BOT_CALLBACK = 17;
	int RUN_BAN_MEMBER = 18;
	int RUN_ZOOM_VIEW = 19;
	int RUN_PIN_MESSAGE = 20;
	int RUN_SEND_STICKER = 21;
	int RUN_INSTALL_STICKER_SET = 22;
	int RUN_LOAD_PLAYLIST = 23;
	int RUN_PLAYER_LOOP = 24;
	int RUN_CANCEL_UPDATES = 25;
	int RUN_DOWNLOAD_DOCUMENT = 26;
	int RUN_LOGOUT = 27;
	int RUN_START_PLAYER = 28;
	int RUN_OPEN_URL = 29;
	int RUN_UNINSTALL_STICKER_SET = 30;
	int RUN_RESET_SETTINGS = 31;

	long ZERO_CHANNEL_ID = -1000000000000L;

	// RMS
	String SETTINGS_RECORD_NAME = "mp4config";
	String AUTH_RECORD_NAME = "mp4user";
	String AVATAR_RECORD_PREFIX = "mcA";

	// URLs
	String DEFAULT_INSTANCE_URL = "http://mp2.nnchan.ru/"; // TODO
	String API_URL = "api.php";
	String AVA_URL = "ava.php";
	String FILE_URL = "file.php";
	String VOICE_URL = "voice.php";
	String OTA_URL = "http://nnproject.cc/mp/upd.php";

	String API_VERSION = "11";

//#ifdef MINI
//#	boolean MINI_BUILD = true;
//#else
	boolean MINI_BUILD = false;
//#endif

}
