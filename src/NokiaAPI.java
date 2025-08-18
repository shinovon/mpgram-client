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
//#ifndef NO_NOKIAUI
import javax.microedition.lcdui.Font;

import com.nokia.mid.ui.Clipboard;
import com.nokia.mid.ui.S60TextEditor;
import com.nokia.mid.ui.TextEditor;
import com.nokia.mid.ui.TextEditorListener;

public class NokiaAPI {
	public static final int ACTION_CONTENT_CHANGE = 1;
	public static final int ACTION_OPTIONS_CHANGE = 2;
	public static final int ACTION_CARET_MOVE = 4;
	public static final int ACTION_TRAVERSE_PREVIOUS = 8;
	public static final int ACTION_TRAVERSE_NEXT = 16;
	public static final int ACTION_PAINT_REQUEST = 32;
	public static final int ACTION_DIRECTION_CHANGE = 64;
	public static final int ACTION_INPUT_MODE_CHANGE = 128;
	public static final int ACTION_LANGUAGE_CHANGE = 256;
	public static final int ACTION_TRAVERSE_OUT_SCROLL_UP = 512;
	public static final int ACTION_TRAVERSE_OUT_SCROLL_DOWN = 1024;
	public static final int ACTION_SCROLLBAR_CHANGED = 2048;
	
	private static ChatCanvas textEditorListener;
	
	public static boolean copy(String text) {
		try {
			Clipboard.copyToClipboard(text);
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	public static Object createTextEditor(int maxSize, int constraints, int width, int height) {
		try {
			TextEditor editor = TextEditor.createTextEditor("", maxSize, constraints, width, height);
			editor.setTextEditorListener(new TextEditorListener() {

				public void inputAction(TextEditor textEditor, int actions) {
					if (textEditorListener == null)
						return;
					textEditorListener.inputAction(actions);
				}
				
			});
			return editor;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static void TextEditor_setParent(Object editor, ChatCanvas parent) {
		((TextEditor) editor).setParent(parent);
		NokiaAPI.textEditorListener = parent;
	}
	
	public static void TextEditor_setBackgroundColor(Object editor, int bgColor) {
		((TextEditor) editor).setBackgroundColor(bgColor);
	}
	
	public static void TextEditor_setForegroundColor(Object editor, int fgColor) {
		((TextEditor) editor).setForegroundColor(fgColor);
	}
	
	
	public static void TextEditor_setFont(Object editor, Font font) {
		((TextEditor) editor).setFont(font);
	}
	
	public static void TextEditor_setSize(Object editor, int x, int y, int width, int height) {
		((TextEditor) editor).setPosition(x, y);
		((TextEditor) editor).setSize(width, height);
	}
	
	public static void TextEditor_setContent(Object editor, String text) {
		((TextEditor) editor).setContent(text);
	}
	
	public static void TextEditor_setVisible(Object editor, boolean visible) {
		((TextEditor) editor).setVisible(visible);
	}
	
	public static void TextEditor_setFocus(Object editor, boolean focus) {
		((TextEditor) editor).setFocus(focus);
	}
	
	public static void TextEditor_setMultiline(Object editor, boolean multiline) {
		((TextEditor) editor).setMultiline(multiline);
	}
	
	public static void TextEditor_setTouchEnabled(Object editor, boolean touchEnabled) {
		try {
			Class.forName("com.nokia.mid.ui.S60TextEditor");
			((S60TextEditor) editor).setTouchEnabled(touchEnabled);
		} catch (Throwable ignored) {}
	}
	
	public static void TextEditor_setIndicatorVisibility(Object editor, boolean visible) {
		try {
			Class.forName("com.nokia.mid.ui.S60TextEditor");
			((S60TextEditor) editor).setIndicatorVisibility(visible);
		} catch (Throwable ignored) {}
	}
	
	public static boolean TextEditor_isVisible(Object editor) {
		return ((TextEditor) editor).isVisible();
	}
	
	public static String TextEditor_getContent(Object editor) {
		return ((TextEditor) editor).getContent();
	}
	
}
//#endif
