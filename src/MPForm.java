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
import java.io.InterruptedIOException;
import java.util.Hashtable;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.Ticker;

public abstract class MPForm extends Form {

	boolean loaded, finished, canceled;
	Thread thread;
	Hashtable urls;
	Item focusOnFinish;

	public MPForm(String title) {
		super(title);
		addCommand(MP.backCmd);
		setCommandListener(MP.midlet);
	}
	
	void load() {
		if (loaded) return;
		loaded = true;
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
				if (focusOnFinish != null) {
					MP.display.setCurrentItem(focusOnFinish);
					focusOnFinish = null;
				}
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
