/*
 * Copyright (c) 2025 Arman Jussupgaliyev
 */
import java.io.InterruptedIOException;
import java.util.Hashtable;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.Ticker;

public abstract class MPForm extends Form {

	boolean loaded, finished, canceled;
	Thread thread;
	Hashtable urls;

	public MPForm(String title) {
		super(title);
		addCommand(MP.backCmd);
		setCommandListener(MP.midlet);
	}
	
	void load() {
		if (loaded) return;
		canceled = finished = false;

		setTicker(new Ticker("Loading.."));
		if (MP.useLoadingForm) {
			MP.display(MP.loadingForm);
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

	void safeAppend(Thread thread, Item item) {
		if (thread != this.thread) throw MP.cancelException;
		append(item);
	}

	void safeAppend(Thread thread, String item) {
		if (thread != this.thread) throw MP.cancelException;
		append(item);
	}

	void safeInsert(Thread thread, int n, Item item) {
		if (thread != this.thread) throw MP.cancelException;
		insert(n, item);
	}
	
	abstract void loadInternal(Thread thread) throws Exception;

	void closed(boolean destroy) {
		if (destroy) cancel();
	}
	
	void shown() {}

}
