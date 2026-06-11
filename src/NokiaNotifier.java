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
//#ifndef NO_NOKIAUI
//#ifndef NO_NOTIFY
import com.nokia.mid.ui.SoftNotification;
import com.nokia.mid.ui.SoftNotificationListener;

import java.util.Enumeration;

public class NokiaNotifier implements SoftNotificationListener {

	/** @noinspection FieldCanBeLocal*/
	private static Object listener;

	static void init() {
		if (listener != null) return;

		listener = new NokiaNotifier();
	}

	static boolean post(String peerId, String peer, String text, int id) throws Exception {
		if (MP.nokiaIds.contains(peerId)) {
			for (Enumeration keys = MP.nokiaIds.keys(); keys.hasMoreElements(); ) {
				Object key = keys.nextElement();
				if (MP.nokiaIds.get(key).equals(peerId)) {
					id = ((Integer) key).intValue();
					break;
				}
			}
		}
		if (id == 0) {
			try {
				SoftNotification s = SoftNotification.newInstance();
				s.setListener((SoftNotificationListener) listener);
				s.setText(peer.concat("\n").concat(text), peer);
				s.post();
				id = s.getId();
				MP.nokiaIds.put(new Integer(id), peerId);
				return true;
			} catch (Exception e) {
				if (MP.nokiaIds.size() == 0) throw e;

				id = ((Integer) MP.nokiaIds.keys().nextElement()).intValue();
			}
		}
		SoftNotification s = SoftNotification.newInstance(id);
		s.setListener((SoftNotificationListener) listener);
		s.setText(peer.concat("\n").concat(text), peer);
		s.post();
		MP.nokiaIds.put(new Integer(id), peerId);
		return false;
	}

	public static void remove(String peerId) {
		if (!MP.nokiaIds.contains(peerId)) return;
		for (Enumeration keys = MP.nokiaIds.keys(); keys.hasMoreElements(); ) {
			Object key = keys.nextElement();
			if (!peerId.equals(MP.nokiaIds.get(key))) continue;

			int id = ((Integer) key).intValue();
			try {
				SoftNotification.newInstance(id).remove();
			} catch (Throwable ignored) {}
			MP.nokiaIds.remove(key);
			break;
		}
	}

	public static void close() {
		try {
			for (Enumeration keys = MP.nokiaIds.keys(); keys.hasMoreElements(); ) {
				int id = ((Integer) keys.nextElement()).intValue();

				try {
					SoftNotification.newInstance(id).remove();
				} catch (Throwable ignored) {}
			}
		} catch (Throwable ignored) {}
	}

	public void notificationDismissed(SoftNotification notification) {
	}

	public void notificationSelected(SoftNotification notification) {
		String peerId = (String) MP.nokiaIds.get(new Integer(notification.getId()));
		if (peerId == null) return;

		MP.removeNotification(peerId);
		MP.openChat(peerId, 0);
	}
}
//#endif
//#endif
