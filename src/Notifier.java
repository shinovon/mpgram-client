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
//#ifndef NO_NOTIFY
import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.lcdui.Image;

import org.pigler.api.PiglerAPI;

import com.nokia.mid.ui.SoftNotification;
import com.nokia.mid.ui.SoftNotificationListener;

// TODO: rewrite this mess

public class Notifier implements SoftNotificationListener {

	private static Image icon;
	
	static Hashtable nokiaIds = new Hashtable();
	static Hashtable piglerIds = new Hashtable();
	
	private static Notifier inst;
	private static PiglerListener piglerListener;
	private static Object piglerApi;
	
	public static boolean init() {
		try {
			Class.forName("com.nokia.mid.ui.SoftNotification");
			if (inst != null) return true;
			inst = new Notifier();
			icon = Image.createImage("/m.png");
		} catch (Exception ignored) {}
		
		try {
			Class.forName("org.pigler.api.PiglerAPI");
			if (piglerApi != null) return true;
			piglerListener = new PiglerListener();
			piglerApi = new PiglerAPI();
			((PiglerAPI) piglerApi).init();
			((PiglerAPI) piglerApi).setListener(piglerListener);
			try {
				((PiglerAPI) piglerApi).removeAllNotifications();
			} catch (Throwable ignored) {}
			return true;
		} catch (Throwable e) {
			return false;
		}
	}
	
	public static boolean post(String peerId, String peer, String text, int mode, Image image) {
		int id = 0;
		try {
			if (piglerApi != null) {
				try {
					Class.forName("org.pigler.api.PiglerAPI");
					((PiglerAPI) piglerApi).showGlobalPopup(peer, text, 0);
				} catch (Throwable ignored) {}
			}
			
			if (mode == 3) {
				Class.forName("org.pigler.api.PiglerAPI");
				if (piglerApi == null) return false;
				
				try {
					if (((PiglerAPI) piglerApi).isSingleLine()) {
						text = null;
					}
				} catch (Throwable ignored) {}
				
				if (piglerIds.contains(peerId)) {
					for (Enumeration keys = piglerIds.keys(); keys.hasMoreElements(); ) {
						Object key = keys.nextElement();
						if (peerId.equals(piglerIds.get(key))) {
							id = ((Integer) key).intValue();
							break;
						}
					}
				}
				
				if (id == 0) {
					try {
						id = ((PiglerAPI) piglerApi).createNotification(peer, text, image == null && !MP.notifyAvas ? icon : image, true);
						piglerIds.put(new Integer(id), peerId);
						return true;
					} catch (Exception e) {
						if (piglerIds.size() == 0) throw e;
					
						// most likely an overflow, try reusing old ids
						id = ((Integer) piglerIds.keys().nextElement()).intValue();
						if (image == null) image = icon;
					}
				}
				if (image == null) {
					((PiglerAPI) piglerApi).updateNotification(id, peer, text);
				} else {
					((PiglerAPI) piglerApi).updateNotification(id, peer, text, image);
				}
				piglerIds.put(new Integer(id), peerId);
				return true;
			} else if (mode == 2) {
				Class.forName("com.nokia.mid.ui.SoftNotification");
				if (nokiaIds.contains(peerId)) {
					for (Enumeration keys = nokiaIds.keys(); keys.hasMoreElements(); ) {
						Object key = keys.nextElement();
						if (nokiaIds.get(key).equals(peerId)) {
							id = ((Integer) key).intValue();
							break;
						}
					}
				}
				if (id == 0) {
					try {
						SoftNotification s = SoftNotification.newInstance();
						s.setListener(inst);
						s.setText(peer.concat("\n").concat(text), peer);
						s.post();
						id = s.getId();
						nokiaIds.put(new Integer(id), peerId);
						return true;
					} catch (Exception e) {
						if (nokiaIds.size() == 0) throw e;
						
						id = ((Integer) nokiaIds.keys().nextElement()).intValue();
					}
				}
				SoftNotification s = SoftNotification.newInstance(id);
				s.setListener(inst);
				s.setText(peer.concat("\n").concat(text), peer);
				s.post();
				nokiaIds.put(new Integer(id), peerId);
				return true;
			}
		} catch (Throwable ignored) {}
		return false;
	}

	public static void remove(String peerId) {
		if (nokiaIds.contains(peerId)) {
			for (Enumeration keys = nokiaIds.keys(); keys.hasMoreElements(); ) {
				Object key = keys.nextElement();
				if (!peerId.equals(nokiaIds.get(key))) continue;
				
				int id = ((Integer) key).intValue();
				try {
					SoftNotification.newInstance(id).remove();
				} catch (Throwable ignored) {}
				nokiaIds.remove(key);
				break;
			}
		}
		
		if (piglerApi == null || !piglerIds.contains(peerId))
			return;
		
		for (Enumeration keys = piglerIds.keys(); keys.hasMoreElements(); ) {
			Object key = keys.nextElement();
			if (!peerId.equals(piglerIds.get(key))) continue;
			
			int id = ((Integer) key).intValue();
			try {
				((PiglerAPI) piglerApi).removeNotification(id);
			} catch (Throwable ignored) {}
			piglerIds.remove(key);
			break;
		}
	}
	
	public static void updateImage(String peerId, Image image) {
		if (piglerApi == null || !piglerIds.contains(peerId))
			return;
		
		for (Enumeration keys = piglerIds.keys(); keys.hasMoreElements(); ) {
			Object key = keys.nextElement();
			if (!peerId.equals(piglerIds.get(key))) continue;
			
			try {
				((PiglerAPI) piglerApi).updateNotification(((Integer) key).intValue(), image);
			} catch (Exception ignored) {}
			break;
		}
	}
	
	public static void close() {
		try {
			for (Enumeration keys = nokiaIds.keys(); keys.hasMoreElements(); ) {
				int id = ((Integer) keys.nextElement()).intValue();
				
				try {
					SoftNotification.newInstance(id).remove();
				} catch (Throwable ignored) {}
			}
		} catch (Throwable ignored) {}
		
		if (piglerApi == null) return;

		try {
			for (Enumeration keys = piglerIds.keys(); keys.hasMoreElements(); ) {
				int id = ((Integer) keys.nextElement()).intValue();
				
				try {
					((PiglerAPI) piglerApi).removeNotification(id);
				} catch (Throwable ignored) {}
			}
		} catch (Throwable ignored) {}
		
		try {
			((PiglerAPI) piglerApi).removeAllNotifications();
		} catch (Throwable ignored) {}
		try {
			((PiglerAPI) piglerApi).close();
		} catch (Throwable ignored) {}
	}

	public void notificationDismissed(SoftNotification notification) {
	}

	public void notificationSelected(SoftNotification notification) {
		String peerId = (String) nokiaIds.get(new Integer(notification.getId()));
		if (peerId == null) return;
		
		remove(peerId);
		MP.openChat(peerId, 0);
	}
	
}
//#endif
