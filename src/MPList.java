/*
 * Copyright (c) 2025 Arman Jussupgaliyev
 */
import java.io.InterruptedIOException;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.Ticker;

public abstract class MPList extends List {

	boolean loaded, finished, canceled;
	Thread thread;

	public MPList(String title, int listType) {
		super(title, listType);
	}
	
	// TODO
	
	void load() {
		if (loaded) return;
		canceled = finished = false;
		
		if (MP.useLoadingForm) {
			MP.display(MP.loadingForm);
		} else {
			setTicker(new Ticker("Loading.."));
		}
		Thread thread = this.thread = Thread.currentThread();
		try {
			deleteAll();
			
			loadInternal(thread);
			finished = true;
			if (MP.useLoadingForm && MP.current == this) {
				MP.display(this);
			}
		} catch (InterruptedException e) {
		} catch (InterruptedIOException e) {
		} catch (Exception e) {
			if (e == MP.cancelException || canceled || this.thread != thread) {
				// ignore exception if cancel detected
				return;
			}
			MP.display(MP.errorAlert(e.toString()), this);
			e.printStackTrace();
		} finally {
			setTicker(null);
			
			if (this.thread == thread) {
				this.thread = null;
			}
		}
	}

	void cancel() {
		loaded = false;
		if (finished || thread == null) return;
		canceled = true;
		thread.interrupt();
		thread = null;
	}

	int safeAppend(Thread thread, String text, Image image) {
		if (thread != this.thread) throw MP.cancelException;
		return append(text, image);
	}

	int safeInsert(Thread thread, int n, String text, Image image) {
		if (thread != this.thread) throw MP.cancelException;
		return insert(n, text, image);
	}
	
	abstract void loadInternal(Thread thread) throws Exception;

	void closed(boolean destroy) {
		if (destroy) cancel();
	}

}
