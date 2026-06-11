/*
Copyright (c) 2025-2026 Arman Jussupgaliyev

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
//#ifndef NO_NOTIFY
//#ifndef NO_NOKIAUI
import org.pigler.api.IPiglerTapHandler;
import org.pigler.api.PiglerAPI;

import javax.microedition.lcdui.Image;
import java.util.Enumeration;

public class PiglerNotifier implements IPiglerTapHandler {

	/** @noinspection FieldCanBeLocal*/
	private static Object listener;
	private static Object api;

	static void init() {
		if (api != null) return;
		listener = new PiglerNotifier();
		api = new PiglerAPI();
		((PiglerAPI) api).init();
		((PiglerAPI) api).setListener((PiglerNotifier) listener);
		try {
			((PiglerAPI) api).removeAllNotifications();
		} catch (Throwable ignored) {}
	}

	public static void showGlobalPopup(String peer, String text) throws Exception {
		((PiglerAPI) api).showGlobalPopup(peer, text, 0);
	}

	public static void post(String peerId, String peer, String text, int id, Image image) throws Exception {
		try {
			if (((PiglerAPI) api).isSingleLine()) {
				text = null;
			}
		} catch (Throwable ignored) {}

		if (MP.piglerIds.contains(peerId)) {
			for (Enumeration keys = MP.piglerIds.keys(); keys.hasMoreElements(); ) {
				Object key = keys.nextElement();
				if (peerId.equals(MP.piglerIds.get(key))) {
					id = ((Integer) key).intValue();
					break;
				}
			}
		}

		if (id == 0) {
			try {
				id = ((PiglerAPI) api).createNotification(peer, text, image == null && !MP.notifyAvas ? MP.icon : image, true);
				MP.piglerIds.put(new Integer(id), peerId);
				return;
			} catch (Exception e) {
				if (MP.piglerIds.size() == 0) throw e;

				// most likely an overflow, try reusing old ids
				id = ((Integer) MP.piglerIds.keys().nextElement()).intValue();
				if (image == null) image = MP.icon;
			}
		}
		if (image == null) {
			((PiglerAPI) api).updateNotification(id, peer, text);
		} else {
			((PiglerAPI) api).updateNotification(id, peer, text, image);
		}
		MP.piglerIds.put(new Integer(id), peerId);
	}

	static void updateImage(String peerId, Image image) {
		if (!MP.piglerIds.contains(peerId)) return;
		for (Enumeration keys = MP.piglerIds.keys(); keys.hasMoreElements(); ) {
			Object key = keys.nextElement();
			if (!peerId.equals(MP.piglerIds.get(key))) continue;

			try {
				((PiglerAPI) api).updateNotification(((Integer) key).intValue(), image);
			} catch (Exception ignored) {}
			break;
		}
	}

	static void remove(String peerId) {
		if (!MP.piglerIds.contains(peerId)) return;

		for (Enumeration keys = MP.piglerIds.keys(); keys.hasMoreElements(); ) {
			Object key = keys.nextElement();
			if (!peerId.equals(MP.piglerIds.get(key))) continue;

			int id = ((Integer) key).intValue();
			try {
				((PiglerAPI) api).removeNotification(id);
			} catch (Throwable ignored) {}
			MP.piglerIds.remove(key);
			break;
		}
	}

	static void close() {
		try {
			for (Enumeration keys = MP.piglerIds.keys(); keys.hasMoreElements(); ) {
				int id = ((Integer) keys.nextElement()).intValue();

				try {
					((PiglerAPI) api).removeNotification(id);
				} catch (Throwable ignored) {}
			}
		} catch (Throwable ignored) {}

		try {
			((PiglerAPI) api).removeAllNotifications();
		} catch (Throwable ignored) {}
		try {
			((PiglerAPI) api).close();
		} catch (Throwable ignored) {}
	}

	public void handleNotificationTap(int uid) {
		String peerId = (String) MP.piglerIds.get(new Integer(uid));
		if (peerId == null) return;

		MP.removeNotification(peerId);
		MP.notificationMessages.remove(peerId);
		MP.openChat(peerId, 0);
	}

}
//#endif
//#endif
