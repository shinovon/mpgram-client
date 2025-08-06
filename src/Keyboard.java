// modified j2mekeyboard for mpgram client

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public final class Keyboard implements KeyboardConstants, Runnable {
	
	private int[][][] layouts;
	
	private String[] supportedLanguages;
	private int[] supportedLanguagesIdx;
	
	private String[] langs;
	private int[] langsIdx;
	
	private int[] specs;
	
	private int[] layoutTypes;

	private int[][][] widths;
	private int[][][] positions;
	private int[][] offsets;

	private int keyStartY;
	private int keyEndY;
	private int keyMarginY;
	private int keyHeight;
	
	private int keyboardHeight;
	private int Y;
	
	private int currentLayout;
	private int lang;
	private int spec;
	
	boolean visible;
	private boolean keepShifted;
	private boolean shifted;
	
	private ChatCanvas listener;
	
	private String text = "";
	
	private int size;
	
	private int keyboardType;
	private boolean multiLine;
	
	boolean pressed;
	boolean dragged;
	int px;
	int py;
	long pt;
	
	private int screenWidth;
	private int screenHeight;
	
	private char[][][] keyLayouts;
	int lastKey;
	int keyRepeatTicks = -1;
	boolean keyPressed;
	long kt;
	private int physicalType;
	private char[] keyVars;
	private int keyVarIdx;
	private char keyBuffer;
	private int currentPhysicalLayout;
	private boolean keyWasRepeated;
	private int[] keysHeld = new int[10];
	private int keysHeldCount;
	private boolean holdingShift;
	private boolean wasHoldingShift;

	private Thread repeatThread;
	Object pressLock = new Object();

	private int keyTextY;
	
	// стиль
	private int bgColor = DEFAULT_BACKGROUND_COLOR;
	private int keyTextColor = DEFAULT_KEY_TEXT_COLOR;
	private int keyTextShadowColor = DEFAULT_KEY_TEXT_SHADOW_COLOR;
	private int keyButtonColor = DEFAULT_BUTTON_COLOR;
	private int keyButtonHoverColor = DEFAULT_BUTTON_HOVER_COLOR;
	private int keyButtonOutlineColor = DEFAULT_BUTTON_OUTLINE_COLOR;
	private int caretColor = DEFAULT_CARET_COLOR;
	private boolean drawButtons = DEFAULT_BUTTONS;
	private boolean drawShadows = DEFAULT_TEXT_SHADOWS;
	private boolean roundButtons = DEFAULT_ROUND_BUTTONS;
	private int keyButtonPadding = DEFAULT_BUTTON_PADDING;
	private int textColor = DEFAULT_TEXT_COLOR;
	private int textHintColor = DEFAULT_TEXT_HINT_COLOR;
	
	private Font font = Font.getFont(0, 0, 0);
	private int fontHeight = font.getHeight();
	private String layoutPackRes;
	private boolean hasQwertyLayouts;
	private Font textFont = Font.getFont(0, 0, 8);
	private int textFontHeight = textFont.getHeight();

	boolean hasRepeatEvents;
	boolean hasPointerEvents = true;

	private Canvas canvas;
	
	// текстбокс
	int textBoxX;
	int textBoxY;
	private int textBoxWidth;
	private int textBoxHeight;
	private int removedTextWidth;
	private String textHint = "";
	int caretPosition;
	boolean caretFlash;
	private boolean textBoxShown;
	boolean textBoxPressed;
	private int selectionStart = -1;
	int selectionEnd = -1;
	int caretRow;
	int caretX;
	int caretCol;
	private int startX;
	private int startRow;
	int endX;
	int endRow;
	int endCol;
	private int startCol;
	private String[] textArray;
	private boolean updateText;
	private int prevTextBoxWidth;

	private String[] abc;

	private Keyboard(Canvas canvas, int keyboardType, boolean multiLine, int screenWidth, int screenHeight, String layoutPackRes) {
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		this.multiLine = multiLine;
		this.layoutPackRes = layoutPackRes == null ? DEFAULT_LAYOUT_PACK : layoutPackRes;
		this.keyboardType = keyboardType;
		if(canvas != null) {
			this.hasRepeatEvents = canvas.hasRepeatEvents();
			this.hasPointerEvents = canvas.hasPointerEvents();
			this.canvas = canvas;
		}
		
		// Physical keyboard checks
		String sysKeyboardType = System.getProperty("com.nokia.keyboard.type");
		String platform = System.getProperty("microedition.platform");
		if(sysKeyboardType == null) {
			physicalType = PHYSICAL_KEYBOARD_PHONE_KEYPAD;
			// Symbian 9.x check
			if(System.getProperty("com.symbian.midp.serversocket.support") != null ||
					System.getProperty("com.symbian.default.to.suite.icon") != null ||
					(platform != null && platform.indexOf("version=3.2") != -1)) {
				if(screenWidth > screenHeight) {
					physicalType = PHYSICAL_KEYBOARD_QWERTY;
				} else {
					physicalType = PHYSICAL_KEYBOARD_PHONE_KEYPAD;
				}
			}
		} else if(sysKeyboardType.equalsIgnoreCase("None")) {
			physicalType = PHYSICAL_KEYBOARD_NONE;
		} else if(sysKeyboardType.equalsIgnoreCase("PhoneKeypad")) {
			physicalType = PHYSICAL_KEYBOARD_PHONE_KEYPAD;
		} else {
			physicalType = PHYSICAL_KEYBOARD_QWERTY;
		}
		
		parseLayoutPack();
		layout();
	}
	
	// static methods

	/**
	 * Initialize keyboard with default settings
	 */
	public static Keyboard getKeyboard(Canvas canvas) {
		return new Keyboard(canvas, KEYBOARD_DEFAULT, false, 0, 0, null);
	}
	
	/**
	 * Initialize keyboard
	 */
	public static Keyboard getKeyboard(Canvas canvas, int keyboardType) {
		return new Keyboard(canvas, keyboardType, false, 0, 0, null);
	}

	/**
	 * Initialize keyboard
	 */
	public static Keyboard getKeyboard(Canvas canvas, boolean multiLine, int screenWidth, int screenHeight) {
		return new Keyboard(canvas, KEYBOARD_DEFAULT, multiLine, screenWidth, screenHeight, null);
	}
	
	/**
	 * Initialize keyboard
	 */
	public static Keyboard getKeyboard(Canvas canvas, int keyboardType, boolean multiLine, int screenWidth, int screenHeight) {
		return new Keyboard(canvas, keyboardType, multiLine, screenWidth, screenHeight, null);
	}

	/**
	 * Initialize keyboard with custom layout pack
	 */
	public static Keyboard getKeyboard(Canvas canvas, int keyboardType, boolean multiLine, int screenWidth, int screenHeight, String layoutPackRes) {
		return new Keyboard(canvas, keyboardType, multiLine, screenWidth, screenHeight, layoutPackRes);
	}
	
	/**
	 * Get supported languages in default layout pack
	 */
	public static String[] getSupportedLanguages() {
		return getSupportedLanguages(DEFAULT_LAYOUT_PACK);
	}
	
	/**
	 * Get supported languages in specific layout pack
	 */
	public static String[] getSupportedLanguages(String layoutPackRes) {
		try {
			JSONObject json = (JSONObject) readJSONRes(layoutPackRes);
			JSONArray arr = json.getArray("languages");
			String[] res = new String[arr.size()];
			Enumeration e = arr.elements();
			int i = 0;
			while(e.hasMoreElements()) {
				JSONObject o = (JSONObject) e.nextElement();
				String lng = (String) o.keys().nextElement();
				res[i++] = o.getString(lng) + " [" + lng + "]";
			}
			return res;
		} catch (IOException e) {
			throw new RuntimeException(e.toString());
		}
	}
	
	// public methods
	
	/**
	 * Get input text
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * Get text length
	 */
	public int getLength() {
		return text.length();
	}
	
	/**
	 * Set text
	 * @param s
	 */
	public void setText(String s) {
		// clear selection
		selectionStart = -1;
		selectionEnd = -1;
		if(size > 0 && s.length() > size) {
			s = s.substring(0, size);
		}
		caretPosition = s.length();
		text = s;
		updateText = true;
	}
	
	/**
	 * Append text
	 * @param s
	 */
	public void appendText(String s) {
		if(size > 0 && text.length() >= size) return;
		if(size > 0 && text.length()+s.length() >= size) {
			text += s;
			text = text.substring(0, size);
			caretPosition = text.length();
			updateText = true;
			return;
		}
		caretPosition += s.length();
		text += s;
		updateText = true;
	}
	
	/**
	 * Insert text
	 * @param s
	 * @param index
	 */
	public void insertText(String s, int index) {
		if(size > 0 && text.length() >= size) return;
		text = text.substring(0, index) + s + text.substring(index);
		caretPosition += s.length();
		updateText = true;
	}
	
	/**
	 * Remove one char
	 * @param index
	 */
	public void removeChar(int index) {
		text = text.substring(0, index) + text.substring(index + 1);
		caretPosition --;
		updateText = true;
	}
	
	/**
	 * Remove text region
	 * @param start
	 * @param end
	 */
	public void remove(int start, int end) {
		text = text.substring(0, start) + text.substring(end);
		caretPosition -= end - start;
		if(caretPosition < 0) caretPosition = 0;
		updateText = true;
	}
	
	/**
	 * Set max text size
	 * @param size
	 */
	public void setSize(int size) {
		this.size = size;
		if(text.length() > size) {
			text = text.substring(0, size);
			updateText = true;
		}
	}

	/**
	 * Clear input
	 */
	public void clear() {
		text = "";
		caretPosition = 0;
		updateText = true;
		selectionStart = selectionEnd = -1;
	}
	
	/**
	 * Reset input
	 */
	public void reset() {
		text = "";
		shifted = false;
		keepShifted = false;
		if(hasQwertyLayouts) {
			currentLayout = langsIdx[lang = 0];
		}
		updateText = true;
		selectionStart = selectionEnd = -1;
	}

	
	/**
	 * Set caret position
	 * @param i
	 */
	public void setCaretPostion(int i) {
		caretFlash = true;
		caretPosition = i;
		if(caretPosition < 0) caretPosition = 0;
		if(caretPosition > text.length()) caretPosition = text.length();
	}
	
	/**
	 * Get caret position
	 */
	public int getCaretPosition() {
		return caretPosition;
	}
	
	/**
	 * Set shift state
	 */
	public void setShifted(boolean shifted) {
		this.shifted = true;
		this.keepShifted = false;
	}
	
	/**
	 * Set current input language
	 * <p><i>Will be ignored if there is not QWERTY layouts available</i></p>
	 * @param language
	 */
	public void setLanguage(String language) {
		if(!hasQwertyLayouts) {
			return;
		}
		for(int i = 0; i < langs.length; i++) {
			if(langs[i].equalsIgnoreCase(language)) {
				lang = i;
				currentLayout = langsIdx[i];
				break;
			}
		}
	}
	
	/**
	 * @return Height of virtual keyboard, 0 if virtual keyboard is hidden
	 */
	public int getHeight() {
		if(hasPointerEvents) return 0;
		return keyboardHeight;
	}
	
	/**
	 * Show and focus keyboard
	 * @see #setVirtualKeyboardVisible
	 */
	public void show() {
		if(visible) {
			return;
		}
		visible = true;
		try {
			repeatThread = new Thread(this);
			repeatThread.start();
		} catch (Exception e) {
		}
	}
	
	/**
	 * Hide keyboard
	 */
	public void hide() {
		visible = false;
		try {
			repeatThread.interrupt();
		} catch (Exception e) {
		}
	}
	
	/**
	 * @return Is keyboard focused
	 */
	public boolean isVisible() {
		return visible;
	}
	
	/**
	 * Set physical keyboard type
	 * @param physicalKeyboardType
	 */
	public void setPhysicalKeyboardType(int physicalKeyboardType) {
		this.physicalType = physicalKeyboardType;
	}
	
	public int getPhysicalKeyboardType() {
		return physicalType;
	}
	
	/**
	 * Set virtual keyboard visibility
	 * Defaults to Canvas.hasPointerEvents()
	 * @param visible
	 */
	public void setVirtualKeyboardVisible(boolean visible) {
		this.hasPointerEvents = visible;
	}

	/**
	 * Set keyboard listener
	 * @param listener
	 */
	public void setListener(ChatCanvas listener) {
		this.listener = listener;
	}
	
	
	
	/**
	 * Draw virtual keyboard
	 * @param g
	 * @param screenWidth
	 * @param screenHeight
	 * @return Height of virtual keyboard, 0 if virtual keyboard is hidden
	 */
	public int paint(Graphics g, int screenWidth, int screenHeight) {
		if(!visible || !hasPointerEvents) return 0;
		// если размеры экрана изменились, то сделать релэйаут
		if(this.screenWidth == 0 || screenWidth != this.screenWidth || screenHeight != this.screenHeight) {
			this.screenWidth = screenWidth;
			this.screenHeight = screenHeight;
			layout();
		}
		g.translate(0, Y = screenHeight - keyboardHeight);
		g.setFont(font);
		g.setColor(bgColor);
		g.fillRect(0, 0, screenWidth, keyboardHeight);
		int y = keyStartY;
		int l = currentLayout;
		for(int row = 0; row < layouts[l].length; row++) {
			int x = offsets[l][row];
			for(int i = 0; i < layouts[l][row].length; i++) {
				x += drawKey(g, row, i, x, y, l);
			}
			y += keyHeight + keyMarginY;
		}
		g.translate(0, -Y);
		return keyboardHeight;
	}
	
	/**
	 * Draw textbox
	 * <p>Doesn't fill background
	 */
	public void drawTextBox(Graphics g, int x, int y, int width, int height) {
		textBoxShown = true;
		this.textBoxX = x;
		this.textBoxY = y;
		this.textBoxWidth = width;
		this.textBoxHeight = height;
		g.setFont(textFont);
		int textY = multiLine ? y : y + ((height - textFont.getHeight()) >> 1);
		String s = "";
		boolean hint = text.length() == 0;
		if(!hint) {
			g.setColor(textColor);
			s = text;
		} else {
			g.setColor(textHintColor);
			s = textHint;
		}
		int th = textFont.getHeight() + 2;
		if(multiLine && !hint) {
			String[] arr = getTextArray();
			int yo = 0;
			while(th * arr.length > height + yo) {
				yo += th;
			}
			int ty = -yo;
			for(int i = 0; i < arr.length; i++) {
				if(ty >= 0) {
					g.drawString(arr[i], x + 2, ty + textY, 0);
				}
				ty += th;
			}
			int cx = x + 2 + caretX;
			int cy = textY + (th * caretRow);
			if(selectionEnd != -1) {
				int strow = startRow;
				int endrow = endRow;
				int stx = startX;
				int endx = endX;
				int stcol = startCol;
				int endcol = endCol;
				if(selectionStart > selectionEnd) {
					strow = endRow;
					endrow = startRow;
					stx = endX;
					endx = startX;
					stcol = endCol;
					endcol = startCol;
				}
				for(int i = strow; i <= endrow; i++) {
					int lx = 0;
					int substart = 0;
					int subend = arr[i].length();
					if(i == strow) {
						lx = stx;
						substart = stcol;
					}
					int lw = -1;
					if(i == endrow) {
						lw = endx - (i == strow ? stx : 0);
						subend = endcol;
					}
					String ls = arr[i].substring(substart, subend);
					if(lw < 0) {
						lw = textFont.stringWidth(ls);
					}
					int ly = textY + (th * i);
					g.setColor(caretColor);
					g.fillRect(x + 2 + lx, ly, lw, th);
					g.setColor(~caretColor);
					g.drawString(ls, x + 2 + lx, ly, 0);
				}
			}
			drawCaret(g, cx, cy);
		} else {
			if(!hint) {
				int ww = 0;
				while(textFont.stringWidth(s) >= width - 4) {
					ww += textFont.charWidth(s.charAt(0));
					s = s.substring(1);
				}
				removedTextWidth = ww;
			} else {
				removedTextWidth = 0;
			}
			g.drawString(s, x + 2, textY, 0);
			if(selectionEnd != -1) {
				int start = Math.min(selectionStart, selectionEnd);
				int end = Math.max(selectionStart, selectionEnd);
				int startX = textFont.stringWidth(text.substring(0, start)) - removedTextWidth;
				String selected = text.substring(start, end);
				int selectedW = textFont.stringWidth(selected);
				g.setColor(caretColor);
				g.fillRect(x + 2 + startX, textY, selectedW, th);
				g.setColor(~caretColor);
				g.drawString(selected, x + 2 + startX, textY, 0);
			}
			s = text;
			if(s.length() > 0 && caretPosition != s.length()) {
				s = s.substring(0, caretPosition);
			}
			drawCaret(g, x + textFont.stringWidth(s) + 2 - removedTextWidth, textY);
		}
	}
	
	/**
	 * Draw textbox overlay (current language label)
	 * @param g
	 */
	public void drawOverlay(Graphics g) {
		if(physicalType == PHYSICAL_KEYBOARD_PHONE_KEYPAD && keyboardType == KEYBOARD_DEFAULT && !hasPointerEvents) {
			String s = abc[currentPhysicalLayout];
			String l = langs[currentPhysicalLayout];
			int w = textFont.stringWidth(s.toUpperCase());
			g.setColor(0xaaaaaa);
			g.fillRect(0, 0, w, textFontHeight);
			g.setColor(0);
			g.drawString(l != null ? (l.toUpperCase() + (shifted ? (keepShifted ? "!" : "#") : "")) : shifted ? keepShifted ? s.toUpperCase() : Character.toUpperCase(s.charAt(0)) + s.substring(1) : s, 0, 0, 0);
		}
	}
	
	
	
	/**
	 * Handle pointer press event
	 * @return true if event is grabbed
	 */
	public boolean pointerPressed(int x, int y) {
		if(y >= Y && visible && hasPointerEvents) {
			pressed = true;
			pt = System.currentTimeMillis();
			px = x;
			py = y;
			synchronized(pressLock) {
				pressLock.notify();
			}
			if(drawButtons) _requestRepaint();
			return true;
		}
		if(textBoxShown && visible && x > textBoxX && y > textBoxY && x < textBoxX + textBoxWidth && y < textBoxY + textBoxHeight) {
			pressed = true;
			pt = System.currentTimeMillis();
			px = x;
			py = y;
			textBoxPressed = true;
			_setCaretPosition(x - textBoxX, y - textBoxY);
			selectionEnd = -1;
			selectionStart = caretPosition;
			startX = caretX;
			startRow = caretRow;
			startCol = caretCol;
			_requestTextBoxRepaint();
			return true;
		}
		return false;
	}
	
	/**
	 * Handle pointer release event
	 * @return true if event is grabbed
	 */
	public boolean pointerReleased(int x, int y) {
		if(pressed) {
			if(textBoxPressed) {
				_setCaretPosition(x - textBoxX, y - textBoxY);
				if(dragged) {
					selectionEnd = caretPosition;
					endX = caretX;
					endRow = caretRow;
					endCol = caretCol;
				}
				textBoxPressed = false;
			} else {
				handleTap(x, y-Y, false);
			}
			dragged = pressed = false;
			return true;
		}
		return false;
	}

	/**
	 * Handle pointer drag event
	 * @return true if event is grabbed
	 */
	public boolean pointerDragged(int x, int y) {
		if(pressed) {
			// filter
			if(py == x && py == y) return true;
			px = x;
			py = y;
			dragged = true;
			return true;
		}
		return false;
	}
	
	
	/**
	 * Handle key press event
	 * @return true if event is grabbed
	 */
	public boolean keyPressed(int key) {
		if(!visible) return false;
		boolean grabbed;
		switch(key) {
		case -3:
		case -4:
			if(grabbed = moveCaret(key == -3 ? -1 : 1))
				_keyPressed(key);
			return grabbed;
		case -1:
		case -2:
		case -6:
			return false;
		case -7:
		case -5:
		default:
			if(grabbed = handleKey(key, false))
				_keyPressed(key);
			return grabbed;
		}
	}
	
	/**
	 * Handle key repeat event
	 * @return true if event is grabbed
	 */
	public boolean keyRepeated(int key) {
		if(!visible) return false;
		if(keyPressed) {
			synchronized(this) {
				if(key == -3 || key == -4) {
					if(moveCaret(key == -3 ? -1 : 1))
						_keyPressed(key);
					return true;
				}
				int i = 0;
				int k = 0;
				while(i < keysHeldCount && (k = keysHeld[i++]) != 0);
				if(k != key) return false;
			}
			handleKey(key, true);
			return true;
		}
		return false;
	}
	
	/**
	 * Handle key release event
	 * @return true if event is grabbed
	 */
	public synchronized boolean keyReleased(int key) {
		if(keyPressed) {
			if(key == '#' && physicalType == PHYSICAL_KEYBOARD_PHONE_KEYPAD && keyboardType == KEYBOARD_DEFAULT) {
				if(!wasHoldingShift) {
					shiftKey();
				}
				holdingShift = false;
			}
			int i = -1;
			while(++i < keysHeldCount) {
				int k;
				if((k = keysHeld[i]) == 0) return false;
				if(k != key) continue;
				keysHeldCount--;
				int s;
				if((s = keysHeldCount - i) > 0)
					System.arraycopy(keysHeld, i + 1, keysHeld, i, s);
				keysHeld[keysHeldCount] = 0;
				if(keysHeldCount == 0) {
					keyPressed = keyWasRepeated = false;
				}
				return true;
			}
		}
		return false;
	}
	
	// style
	
	/**
	 * Set virtual keyboard background color
	 * @param color
	 */
	public void setBackgroundColor(int color) {	
		this.bgColor = color;
	}
	
	/**
	 * Set buttons background color
	 * @param color
	 */
	public void setButtonColor(int color) {	
		this.keyButtonColor = color;
	}
	
	/**
	 * Set hovered button color
	 * @param color
	 */
	public void setButtonHoverColor(int color) {	
		this.keyButtonHoverColor = color;
	}
	
	/**
	 * Set buttons outline color
	 * @param color
	 */
	public void setButtonOutlineColor(int color) {	
		this.keyButtonOutlineColor = color;
	}

	/**
	 * Set buttons foreground color
	 * @param color
	 */
	public void setKeyTextColor(int color) {	
		this.keyTextColor = color;
	}
	
	/**
	 * Задать цвет обводки текста
	 * @param color
	 */
	public void setKeyTextShadowColor(int color) {	
		this.keyTextShadowColor = color;
	}

	/**
	 * Set textbox flashing cursor color
	 * @param color
	 */
	public void setCaretColor(int color) {
		this.caretColor = color;
	}

	/**
	 * Set textbox foreground color
	 * @param color
	 */
	public void setTextColor(int color) {
		this.textColor = color;
	}

	/**
	 * Set textbox hint text color
	 * @param color
	 */
	public void setTextHintColor(int color) {
		this.textHintColor = color;
	}
	
	/**
	 * Enable buttons drawing
	 * @param enabled
	 */
	public void setButtons(boolean enabled) {
		this.drawButtons = enabled;
	}
	
	/**
	 * Enable keys text shadows
	 * @param enabled
	 */
	public void setTextShadows(boolean enabled) {
		this.drawShadows = enabled;
	}
	
	/**
	 * Enable buttons outline drawing
	 * @param enabled
	 */
	public void setRoundButtons(boolean enabled) {
		this.roundButtons = enabled;
	}
	
	/**
	 * Set button padding
	 * @param padding
	 */
	public void setButtonPadding(int padding) {
		this.keyButtonPadding = padding;
	}
	
	/**
	 * Set keys text font
	 * @param font
	 */
	public void setKeyFont(Font font) {
		this.font = font;
		this.fontHeight = font.getHeight();
		this.keyTextY = ((keyHeight - fontHeight) >> 1) + 1;
	}
	
	/**
	 * Set textbox font
	 * @param font
	 */
	public void setTextFont(Font font) {
		this.textFont = font;
		this.textFontHeight = font.getHeight();
	}
	
	/**
	 * Set textbox hint
	 * @param textHint
	 */
	public void setTextHint(String textHint) {
		this.textHint = textHint;
	}
	
	/**
	 * <p>Изменить доступные языки</p>
	 * @param languages Можно пустой массив чтобы выбрать все доступные языки,<br>
	 * но возможно тогда юзверю придется много раз нажимать на кнопку языка чтобы найти нужный
	 */
	public void setLanguages(String[] languages) {
		if(languages.length == 0 || !hasQwertyLayouts) {
			langs = supportedLanguages;
			langsIdx = supportedLanguagesIdx;
		} else {
			Vector v = new Vector();
			for(int i = 0; i < languages.length; i++) {
				for(int j = 0; j < supportedLanguages.length; j++) {
					if(languages[i].equalsIgnoreCase(supportedLanguages[j])) {
						v.addElement(new Integer(j));
						break;
					}
				}
			}
			int l = v.size();
			if(l < languages.length) {
				// предупреждение в логи о том что некоторые языки не были добавлены
				System.out.println("Some selected languages are not supported by current layout pack and skipped!");
			}
			langs = new String[l];
			langsIdx = new int[l];
			for(int i = 0; i < l; i++) {
				int k = ((Integer)v.elementAt(i)).intValue();
				langs[i] = supportedLanguages[k];
				langsIdx[i] = supportedLanguagesIdx[k];
			}
		}
		if(hasQwertyLayouts) {
			currentLayout = langsIdx[0];
		}
	}
	
	// private methods
	
	private synchronized boolean moveCaret(int i) {
		caretFlash = true;
		if(keyBuffer != 0) {
			_flushKeyBuffer();
			return true;
		}
		if(!holdingShift && selectionEnd != -1) {
			caretPosition = i == -1 ? Math.min(selectionStart, selectionEnd) : Math.max(selectionStart, selectionEnd);
			selectionEnd = -1;
			_requestTextBoxRepaint();
			return true;
		}
		caretPosition += i;
		if(caretPosition < 0) {
			caretPosition = 0;
			caretCol = 0;
			caretX = 0;
			caretRow = 0;
			return false;
		} else if(caretPosition > text.length()) {
			caretPosition = text.length();
			return false;
		} else if(i == -1) {
			char c = text.charAt(caretPosition);
			if(multiLine && c == '\n') {
				caretRow--;
				String s = getTextArray()[caretRow];
				caretCol = s.length();
				caretX = textFont.stringWidth(s);
			} else {
				caretCol--;
				caretX -= textFont.charWidth(c);
			}
		} else {
			char c = text.charAt(caretPosition-1);
			if(multiLine && c == '\n') {
				caretX = 0;
				caretCol = 0;
				caretRow++;
			} else {
				caretCol++;
				caretX += textFont.charWidth(c);
			}
		}
		if(holdingShift) {
			if(selectionEnd == -1) {
				selectionStart = (selectionEnd = caretPosition) - i;
			} else {
				selectionEnd = caretPosition;
			}
			wasHoldingShift = true;
		}
		_requestTextBoxRepaint();
		return true;
	}
	
	private synchronized void _keyPressed(int key) {
		keyPressed = true;
		lastKey = key;
		kt = System.currentTimeMillis();
		if(keysHeldCount == keysHeld.length) {
			keysHeld[0] = keysHeldCount = 0;
		} else {
			add: {
				int i = 0;
				int k;
				while(i < keysHeldCount) {
					if((k = keysHeld[i++]) == 0) break;
					if(k == key) break add;
				}
				keysHeld[keysHeldCount++] = key;
			}
		}
		synchronized(pressLock) {
			pressLock.notify();
		}
	}
	
	synchronized void _repeatKey() {
		if(keysHeldCount > 1) return;
		handleKey(keysHeld[keysHeldCount - 1], true);
	}
	
	void _flushKeyBuffer() {
		if(keyBuffer != 0) {
			type(keyBuffer);
		}
		keyRepeatTicks = keyVarIdx = keyBuffer = 0;
	}
	
	private boolean handleKey(int key, boolean repeated) {
		if(physicalType == PHYSICAL_KEYBOARD_PHONE_KEYPAD) {
			if(repeated) {
				if(key >= '1' && key <= '9') {
					if(!keyWasRepeated || keyboardType == KEYBOARD_PHONE_NUMBER || keyboardType == KEYBOARD_NUMERIC || keyboardType == KEYBOARD_DECIMAL) {
						keyBuffer = (char) key;
						_flushKeyBuffer();
					}
				} else if(key == -7 || key == -8 || key == '\b') {
					backspace(false);
				} else if(key == '0') {
					if(!keyWasRepeated || keyboardType == KEYBOARD_NUMERIC || keyboardType == KEYBOARD_DECIMAL) {
						keyBuffer = keyboardType == KEYBOARD_PHONE_NUMBER ? '+' : '0';
						_flushKeyBuffer();
					}
				} else if(key == '#') {
					wasHoldingShift = holdingShift = true;
				} else if(key != '*' && key >= 32) {
					if(canvas != null) {
						String keyName = canvas.getKeyName(key);
						if(keyName.length() == 1) {
							type(keyName.charAt(0));
						}
					} else {
						type((char) key);
					}
				}
				keyWasRepeated = true;
				return true;
			} else {
				if(key >= '1' && key <= '9') {
					switch(keyboardType) {
					case KEYBOARD_DEFAULT:
						if(keyRepeatTicks > 0 && lastKey == key) {
							if(keyVarIdx++ == keyVars.length-1) {
								keyVarIdx = 0;
							}
							if(keyVars == null) return true;
							keyBuffer = (char) keyVars[keyVarIdx];
						} else {
							_flushKeyBuffer();
							keyVars = keyLayouts[currentPhysicalLayout][key-'0'];
							if(keyVars == null) return true;
							keyBuffer = (char) keyVars[keyVarIdx];
						}
						break;
					case KEYBOARD_PHONE_NUMBER:
					case KEYBOARD_NUMERIC:
					case KEYBOARD_DECIMAL:
						keyBuffer = (char) key;
						_flushKeyBuffer();
					}
					_requestRepaint();
					keyRepeatTicks = KEY_REPEAT_TICKS;
					return true;
				}
				switch(key) {
				case '0':
					keyVars = null;
					switch(keyboardType) {
					case KEYBOARD_DEFAULT:
						if(keyRepeatTicks > 0 && lastKey == key) {
							if(keyVarIdx++ == 2) {
								keyVarIdx = 0;
								keyBuffer = ' ';
							} else if(keyVarIdx == 1) {
								keyBuffer = '0';
							} else {
								keyBuffer = '\n';
							}
						} else {
							_flushKeyBuffer();
							keyBuffer = ' ';
						}
						break;
					case KEYBOARD_PHONE_NUMBER:
						_flushKeyBuffer();
						keyBuffer = '0';
						break;
					case KEYBOARD_NUMERIC:
					case KEYBOARD_DECIMAL:
						keyBuffer = (char) key;
						_flushKeyBuffer();
					}
					_requestRepaint();
					keyRepeatTicks = KEY_REPEAT_TICKS;
					return true;
				case '#':
					switch(keyboardType) {
					case KEYBOARD_DEFAULT:
						wasHoldingShift = false;
						holdingShift = true;
//						shiftKey();
						break;
					case KEYBOARD_PHONE_NUMBER:
						_flushKeyBuffer();
						type('#');
						break;
					case KEYBOARD_NUMERIC:
						break;
					case KEYBOARD_DECIMAL:
						_flushKeyBuffer();
						type('-');
						break;
					}
					return true;
				case '*':
					switch(keyboardType) {
					case KEYBOARD_DEFAULT:
						langKey();
						break;
					case KEYBOARD_NUMERIC:
						break;
					case KEYBOARD_PHONE_NUMBER:
						_flushKeyBuffer();
						type('*');
						break;
					case KEYBOARD_DECIMAL:
						_flushKeyBuffer();
						type('.');
						break;
					}
					return true;
				}
				_flushKeyBuffer();
				switch(key) {
				case -7:
				case -8:
				case '\b':
					_flushKeyBuffer();
					if(text.length() == 0) {
						return key != -7;
					} else {
						backspace(false);
					}
					return true;
				case 13:
				case 80:
					enter();
					return true;
				case ' ':
					space();
					return true;
				default:
					if(key >= 32) {
						if(canvas != null) {
							String keyName = canvas.getKeyName(key);
							if(keyName.length() == 1) {
								type(keyName.charAt(0));
							}
						} else {
							type((char) key);
						}
						return true;
					}
				}
			}
		} else { // qwerty
			switch(key) {
			case -5:
			case -6:
			case -7:
				return false;
			case '\b':
			case -8:
				backspace(false);
				return true;
			case 127: // delete key
				backspace(true);
				return true;
			case 10:
			case 13:
			case 80:
				enter();
				return true;
			case ' ':
				space();
				return true;
			default:
				if(key >= 32) {
					if(canvas != null) {
						String keyName = canvas.getKeyName(key);
						if(keyName.length() == 1) {
							type(keyName.charAt(0));
						}
					} else {
						type((char) key);
					}
					return true;
				}
			}
		}
		return false;
	}

	void _repeatPress(int x, int y) {
		handleTap(x, y-Y, true);
	}
	
	private void handleTap(int x, int y, boolean repeated) {
		int row = div(y - keyStartY, keyHeight + keyMarginY);
		if(row == 4) row = 3;
		if(repeated && row != 2) return;
		if(row >= 0 && row <= 3) {
			if(x < 0 || x > screenWidth) return;
			int l = currentLayout;
			int kx = offsets[l][row];
			for(int col = 0; col < layouts[l][row].length; col++) {
				int w = widths[l][row][col];
				if(x > kx && x < kx+w) {
					int key = layouts[l][row][col];
					switch(key) {
					case KEY_SHIFT:
						if(!repeated) shiftKey();
						break;
					case KEY_BACKSPACE:
						backspace(false);
						break;
					case KEY_LANG:
						if(!repeated) langKey();
						break;
					case KEY_MODE:
						if(!repeated) modeKey();
						break;
					case KEY_RETURN:
						if(!repeated) enter();
						break;
					case KEY_SPACE:
						if(!repeated) space();
						break;
					case KEY_EMPTY:
					case KEY_UNDEFINED:
						break;
					default:
						if(!repeated) type((char) key);
						break;
					}
					break;
				}
				kx += w;
			}
		}
	}
	
	private void modeKey() {
		shifted = false;
		if(layoutTypes[currentLayout] == 0) {
			currentLayout = specs[spec = 0];
		} else if(layoutTypes[currentLayout] == 1) {
			currentLayout = langsIdx[lang];
		}
		_requestRepaint();
	}
	
	private void langKey() {
		shifted = false;
		int l = langs.length;
		if (l > 2 && listener != null && listener.requestLanguageChange()) {
			return;
		}
		lang++;
		if(lang >= l) {
			lang = 0;
		}
		currentPhysicalLayout = langsIdx[lang];
		currentLayout = langsIdx[lang];
		if(listener != null) listener.onKeyboardLanguageChanged();
		_requestRepaint();
	}

	private void enter() {
		// если мультилайн мод, добавить \n, иначе послать эвент
		if(multiLine) {
			type('\n');
		} else {
			if(listener != null) listener.onKeyboardDone();
		}
		textUpdated();
	}

	private void shiftKey() {
		if(layoutTypes[currentLayout] == 1) {
			keepShifted = false;
			shifted = false;
			spec++;
			if(spec > 1) spec = 0;
			currentLayout = specs[spec];
		} else if(shifted && !keepShifted) {
			keepShifted = true;
		} else {
			keepShifted = false;
			shifted = !shifted;
		}
		_requestRepaint();
	}

	private void textUpdated() {
		if(caretPosition > text.length()) caretPosition = text.length();
		updateText = true;
		if(listener != null) listener.onKeyboardTextUpdated();
		_requestRepaint();
	}

	private void type(char c) {
		if(size > 0 && text.length() >= size) return;
		if(shifted) {
			c = Character.toUpperCase(c);
			if(!keepShifted) shifted = false;
		}
		if(listener != null && !listener.onKeyboardType(c)) return;
		if(selectionEnd != -1) {
			int start = Math.min(selectionStart, selectionEnd);
			remove(start, Math.max(selectionStart, selectionEnd));
			selectionEnd = -1;
			selectionStart = caretPosition = start;
		}
		if(multiLine) {
			if(c == '\n') {
				caretX = 0;
				caretCol = 0;
				caretRow++;
			} else {
				caretCol ++;
				caretX += textFont.charWidth(c);
			}
		}
		if(caretPosition != text.length()) {
			String s = caretPosition == 0 ? "" : text.substring(0, caretPosition);
			s += c;
			s += text.substring(caretPosition);
			if(s.length() > size && size > 0) s = s.substring(0, size);
			text = s;
		} else {
			text += c;
		}
		caretPosition ++;
		textUpdated();
	}
	
	private void space() {
		type(' ');
	}
	
	private void backspace(boolean forward) {
		if(keyBuffer != 0) {
			keyBuffer = 0;
			_flushKeyBuffer();
			_requestRepaint();
			return;
		}
		if(listener != null && !listener.onKeyboardBackspace()) {
			_requestRepaint();
			return;
		}
		if(text.length() > 0) {
			if(selectionEnd != -1) {
				int start = Math.min(selectionStart, selectionEnd);
				remove(start, Math.max(selectionStart, selectionEnd));
				selectionEnd = -1;
				selectionStart = caretPosition = start;
				textUpdated();
				return;
			}
			if(forward) {
				if(caretPosition < text.length()) {
					text = text.substring(0, caretPosition) + text.substring(caretPosition + 1);
				}
			} else if(caretPosition == text.length()) {
				if(multiLine) {
					char c = text.charAt(text.length() - 1);
					if(c == '\n') {
						caretRow--;
						String s = getTextArray()[caretRow];
						caretCol = s.length();
						caretX = textFont.stringWidth(s);
					} else {
						caretCol--;
						caretX -= textFont.charWidth(c);
					}
				}
				text = text.substring(0, text.length() - 1);
				caretPosition --;
			} else if(caretPosition > 0) {
				if(multiLine) {
					char c = text.charAt(caretPosition - 1);
					if(c == '\n') {
						caretRow--;
						String s = getTextArray()[caretRow];
						caretCol = s.length();
						caretX = textFont.stringWidth(s);
					} else {
						caretCol--;
						caretX -= textFont.charWidth(c);
					}
				}
				text = text.substring(0, caretPosition - 1) + text.substring(caretPosition);
				caretPosition --;
			}
			if(caretPosition == 0 && (keyboardType != PHYSICAL_KEYBOARD_QWERTY || hasPointerEvents)) {
				shifted = true;
			}
		}
		caretFlash = true;
		textUpdated();
	}
	
	void cancel() {
		if(listener != null) listener.onKeyboardCancel();
		hide();
	}
	
	protected void _setCaretPosition(int x, int y) {
		x -= 2;
		if(multiLine) {
			String[] arr = getTextArray();
			int textHeight = textFont.getHeight() + 2;
			int line = y / textHeight;
			if(arr != null && line >= 0 && line < arr.length) {
				int lineLength = arr[line].length();
				int i = 0;
				int j = 0;
				caretCol = 0;

				int k;
				for (k = x; caretCol <= lineLength; ++caretCol) {
					j = i;
					if ((i = textFont.substringWidth(arr[line], 0, caretCol)) >= k) {
						break;
					}
				}
				int l = (i - j) / 2;
				int m;
				if (k >= j + l) {
					m = i;
				} else {
					--caretCol;
					m = j;
				}
				caretX = m;
				caretRow = line;
				caretCol = Math.min(Math.max(0, caretCol), lineLength);
				int n = caretCol;
				while (true) {
					caretPosition = n;
					if (line <= 0) {
						return;
					}
					--line;
					n = caretPosition + arr[line].length();
				}
			}
			return;
		}
		int xx = x + removedTextWidth;
		int i = 0;
		for(i = text.length(); i > 0; i--) {
			if(textFont.stringWidth(text.substring(0, i-1)) + (textFont.charWidth(text.charAt(i-1)) >> 1) + 1 < xx) {
				break;
			}
		}
		setCaretPostion(i);
	}

	void _requestRepaint() {
		if(listener != null) listener.onKeyboardRepaintRequested();
	}
	
	void _requestTextBoxRepaint() {
		if(listener != null) listener.onTextBoxRepaintRequested();
	}
	
	private void drawCaret(Graphics g, int caretX, int caretY) {
		if(keyBuffer != 0) {
			char c = keyBuffer;
			if(shifted) c = Character.toUpperCase(c);
			int w = textFont.charWidth(c);
			g.setColor(caretColor);
			g.fillRect(caretX, caretY, w, textFontHeight);
			g.setColor(~caretColor);
			g.drawChar(c, caretX, caretY, 0);
			/*
			if(keyVars != null) {
				w = textFont.charsWidth(keyVars, 0, keyVars.length)+1;
				g.setColor(~caretColor);
				g.fillRect(caretX, caretY-textFontHeight, w, textFontHeight);
				g.setColor(caretColor);
				g.drawRect(caretX, caretY-textFontHeight, w, textFontHeight);
				g.setColor(caretColor);
				g.drawChars(keyVars, 0, keyVars.length, caretX+1, caretY-textFontHeight, 0);
			}
			*/
		}
		if(caretFlash) {
			g.setColor(caretColor);
			g.drawLine(caretX, caretY, caretX, caretY + textFontHeight);
		}
	}
	


	private void drawKeyButton(Graphics g, int x, int y, int w) {
		if(!drawButtons) return;
		int h = keyHeight;
		g.setColor(pressed && px > x && px < x + w && py-Y > y && py-Y < y+h ? keyButtonHoverColor : keyButtonColor);
		x += keyButtonPadding;
		y += keyButtonPadding;
		w -= keyButtonPadding*2;
		h -= keyButtonPadding*2;
		g.fillRect(x, y, w, h);
		g.setColor(keyButtonOutlineColor);
		// если паддинг = 0, рисовать границы
		if(keyButtonPadding == 0) {
			g.drawRect(x, y, w, h);
		} else if(roundButtons) {
			g.drawLine(x, y, x, y);
			g.drawLine(x+w-1, y, x+w-1, y);
			g.drawLine(x, y+h-1, x, y+h-1);
			g.drawLine(x+w-1, y+h-1, x+w-1, y+h-1);
		} 
	}

	private int drawKey(Graphics g, int row, int column, int x, int y, int l) {
		int key = layouts[l][row][column];
		int w = widths[l][row][column];
		if(key == KEY_UNDEFINED) return w;
		drawKeyButton(g, x, y, w);
		String s = null;
		char c = 0;
		boolean b = false;
		switch(key) {
		case KEY_SHIFT:
			b = true;
			// в спец.символах это должно быть табами
			// если ширина кнопки такая же как у обычных клавиш, то отображать ^ вместо шифта
			// и вообще надо приделать картинки
			s = layoutTypes[currentLayout] == 1 ? (spec+1)+"/2" : w <= widths[l][0][0] ? "^" : "shift";
			break;
		case KEY_BACKSPACE:
			b = true;
			s = "<-";
			break;
		case KEY_LANG:
			b = true;
			s = langs[lang];
			break;
		case KEY_MODE:
			b = true;
			s = layoutTypes[currentLayout] == 0 ? "!#1" : abc[currentLayout];
			break;
		case KEY_RETURN:
			b = true;
			s = multiLine ? "->" : "OK";
			break;
		case KEY_SPACE:
			b = true;
			s = "space";
			break;
		case KEY_EMPTY:
			// если 0, то клавиша пустая 
			return w;
		default:
			c = (char) key;
			break;
		}
		y += keyTextY;
		if(b) {
			x += (w - font.stringWidth(s)) >> 1;
			if(drawShadows) {
				g.setColor(keyTextShadowColor);
				g.drawString(s, x+1, y+1, 0);
				g.drawString(s, x+1, y-1, 0);
				g.drawString(s, x-1, y+1, 0);
				g.drawString(s, x-1, y-1, 0);
			}
			g.setColor(keyTextColor);
			g.drawString(s, x, y, 0);
		} else if(key != 0) {
			if(shifted && l < langs.length)
				c = Character.toUpperCase(c);
			x += (w - font.charWidth(c)) >> 1;
			if(drawShadows) {
				g.setColor(keyTextShadowColor);
				g.drawChar(c, x+1, y+1, 0);
				g.drawChar(c, x+1, y-1, 0);
				g.drawChar(c, x-1, y+1, 0);
				g.drawChar(c, x-1, y-1, 0);
			}
			g.setColor(keyTextColor);
			g.drawChar(c, x, y, 0);
		}
		return w;
	}
	
	private void parseLayoutPack() {
		try {
			JSONObject json = (JSONObject) readJSONRes(layoutPackRes);
			String m;
			switch(keyboardType) {
			case KEYBOARD_URL:
				m = "url";
				break;
			case KEYBOARD_NUMERIC:
				m = "numeric";
				break;
			case KEYBOARD_DECIMAL:
				m = "decimal";
				break;
			case KEYBOARD_PHONE_NUMBER:
				m = "phone_number";
				break;
			case KEYBOARD_DEFAULT:
			default:
				m = "default";
				break;
			}
			json = json.getObject("keyboards");
			if(!json.has(m)) {
				throw new RuntimeException("Layout pack " + layoutPackRes + " does not have " + m + " keyboard!");
			}
			if(json.getObject(m).has("base")) {
				json = json.getObject(json.getObject(m).getString("base"));
			} else {
				json = json.getObject(m);
			}
			JSONArray arr = json.getArray("supported_languages");
			int i = arr.size();
			supportedLanguages = new String[i];
			supportedLanguagesIdx = new int[i];
			if(hasQwertyLayouts = i != 0) {
				arr.copyInto(supportedLanguages, 0, i);
			}
			arr = json.getArray("layouts");
			i = arr.size();
			layouts = new int[i][4][];
			keyLayouts = new char[i][10][];
			layoutTypes = new int[i];
			abc = new String[i];
			Enumeration e = arr.elements();
			i = 0;
			Vector specialsVector = new Vector();
			while(e.hasMoreElements()) {
				JSONObject j = (JSONObject) e.nextElement();
				String type = j.getNullableString("type");
				layoutTypes[i] = type == null ? 0 : type.equals("special") ? 1 : 0;
				if(type != null) {
					if(type.equals("qwerty")) {
						String lng = j.getNullableString("lang");
						if(lng != null) {
							for(int k = 0; k < supportedLanguages.length; k++) {
								if(supportedLanguages[k].equals(lng)) {
									supportedLanguagesIdx[k] = i;
									break;
								}
							}
						}
					} if(type.equals("special")) {
						specialsVector.addElement(new Integer(i));
					}
				}
				abc[i] = j.getNullableString("abc");
				int[][] l = new int[4][];
				char[][] o = new char[10][];
				JSONArray a = (JSONArray) readJSONRes(j.getString("res"));
				JSONArray t = a.getArray(0);
				for(int k = 0; k < 4; k++) {
					JSONArray b = t.getArray(k);
					int n = b.size();
					l[k] = new int[b.size()];
					for(int p = 0; p < n; p++) {
						Object c = b.get(p);
						if(c instanceof String) {
							l[k][p] = ((String) c).charAt(0);
						} else if(c instanceof Integer) {
							l[k][p] = ((Integer) c).intValue();
						}
					}
				}
				if(a.size() > 1) {
					JSONObject s = a.getObject(1);
					for(int k = 1; k < 10; k++) {
						JSONArray b = s.getArray(Integer.toString(k));
						int n = b.size();
						o[k] = new char[b.size()];
						for(int p = 0; p < n; p++) {
							Object c = b.get(p);
							if(c instanceof String) {
								o[k][p] = ((String) c).charAt(0);
							} else if(c instanceof Integer) {
								o[k][p] = (char) ((Integer) c).intValue();
							}
						}
					}
					keyLayouts[i] = o;
				}
				layouts[i] = l;
				i++;
			}
			int l = specialsVector.size();
			specs = new int[l];
			for(i = 0; i < l; i++) {
				specs[i] = ((Integer)specialsVector.elementAt(i)).intValue();
			}
			setLanguages(new String[0]);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.toString());
		}
	}
	
	private void layout() {
		if(screenWidth == 0 || screenHeight == 0) {
			return; 
		}
		keyStartY = 2;
		keyEndY = 2;
		int h = screenHeight / 10;
		if(screenHeight == 640) {
			h = 58;
		}
		keyHeight = h;
		//keyMarginY = 2;
		int w1 = screenWidth / 10;
		widths = new int[layouts.length][4][];
		positions = new int[layouts.length][4][];
		offsets = new int[layouts.length][4];
		for(int l = 0; l < layouts.length; l++) {
			double dw = (double) screenWidth / (double)layouts[l][0].length;
			int w = (int) dw;
			int fz = layouts[l][2].length-2;
			int fw = ((int) (screenWidth - dw * fz)) >> 1;
			for(int row = 0; row < 4; row++) {
				if(row == 3 && layouts[l][3].length < layouts[l][2].length) {
					w = w1;
					fw = ((int) (screenWidth - w * 7)) >> 1;
				}
				int x = 0;
				int c1 = layouts[l][row].length;
				widths[l][row] = new int[c1];
				positions[l][row] = new int[c1];
				for(int col = 0; col < c1; col++) {
					int key = layouts[l][row][col];
					int kw = w;
					switch(key) {
					case KEY_SHIFT:
					case KEY_BACKSPACE:
					case KEY_MODE:
					case KEY_RETURN:
						kw = fw;
						break;
					case KEY_SPACE:
						kw *= 3;
						break;
					}
					widths[l][row][col] = kw;
					positions[l][row][col] = x;
					x+=kw;
				}
				offsets[l][row] = (screenWidth - x) >> 1;
			}
		}
		keyboardHeight = keyStartY + keyEndY + (keyHeight + keyMarginY) * 4;
		keyTextY = ((keyHeight - fontHeight) >> 1) + 1;
	}
	
	private String[] getTextArray() {
		if (!multiLine) return null;
		if (textArray == null || updateText || textBoxWidth != prevTextBoxWidth) {
			textArray = getTextArray(text, textFont, textBoxWidth-4);
			updateText = false;
			prevTextBoxWidth = textBoxWidth;
		}
		return textArray;
	}
	
	// utils
	
	private static Object readJSONRes(String res) throws IOException {
		InputStream is = "".getClass().getResourceAsStream(KEYBOARD_LAYOUTS_DIR + res);
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		byte[] buf = new byte[128];
		int i;
		while((i = is.read(buf)) != -1) {
			o.write(buf, 0, i);
		}
		is.close();
		String s = new String(o.toByteArray(), "UTF-8"); 
		o.close();
		if(s.charAt(0) == '{')
			return MP.parseObject(s);
		else if(s.charAt(0) == '[')
			return MP.parseArray(s);
		return null;
	}
	
	private static String[] getTextArray(String s, Font font, int maxWidth) {
		if (s == null)
			return new String[0];
		if (maxWidth > 0) {
			boolean var4 = s.indexOf('\n') != -1;
			if (font.stringWidth(s) <= maxWidth) {
				return var4 ? split(s, '\n') : new String[] { s };
			} else {
				Vector list = new Vector();
				if (!var4) {
					splitToWidth(s, font, maxWidth, list);
				} else {
					char[] var7 = s.toCharArray();
					int var8 = 0;

					for (int var9 = 0; var9 < var7.length; ++var9) {
						if (var7[var9] == 10 || var9 == var7.length - 1) {
							String var11 = var9 == var7.length - 1 ? new String(var7, var8, var9 + 1 - var8)
									: new String(var7, var8, var9 - var8);
							if (font.stringWidth(var11) <= maxWidth) {
								list.addElement(var11);
							} else {
								splitToWidth(var11, font, maxWidth, list);
							}

							var8 = var9 + 1;
						}
					}
				}

				String[] r = new String[list.size()];
				list.copyInto(r);
				return r;
			}
		} else {
			return new String[] { s };
		}
	}

	private static void splitToWidth(String s, Font font, int maxWidth, Vector list) {
		char[] arr = s.toCharArray();
		int k = 0;
		int i = 0;
		int w = 0;

		while (true) {
			while (i < arr.length) {
				if ((w += font.charWidth(arr[i])) > maxWidth) {
					int j = i;

					while (arr[j] != ' ') {
						--j;
						if (j < k) {
							j = i;
							break;
						}
					}

					list.addElement(new String(arr, k, j - k));
					k = arr[j] != ' ' && arr[j] != '\n' ? j : j + 1;
					w = 0;
					i = k;
				} else {
					++i;
				}
			}

			list.addElement(new String(arr, k, i - k));
			return;
		}
	}
	
	private static String[] split(String s, char c) {
		char[] arr = s.toCharArray();
		int i = 0;
		Vector list = null;

		for (int j = 0; j < arr.length; ++j) {
			if (arr[j] == c) {
				if (list == null) {
					list = new Vector();
				}

				list.addElement(new String(arr, i, j - i));
				i = j + 1;
			}
		}

		if (list == null) {
			return new String[] { s };
		}
		if (i < arr.length) {
			list.addElement(new String(arr, i, arr.length - i));
		}
		String[] r = new String[list.size()];
		list.copyInto(r);
		return r;
	}

	// деление без остатка
	private static int div(int i, int j) {
		double d = i / (double) j;
		return (int)(d - d % 1);
	}
	
	// KeyboardThread
	
	private static final int holdTime = 500;
	private static final int repeatTime = 100;
	private static final int keyHoldTime = 500;
	private static final int keyRepeatTime = 100;
	
	public void run() {
		try {
			int touchTicks = 0;
			int keyTicks = 0;
			int flashTicks = 0;
			while(visible) {
				if(pressed) {
					if(textBoxPressed) {
						if(dragged) {
							_setCaretPosition(px - textBoxX, py - textBoxY);
							selectionEnd = caretPosition;
							endX = caretX;
							endRow = caretRow;
							endCol = caretCol;
							dragged = false;
							_requestTextBoxRepaint();
						}
					} else {
						if(touchTicks > 10) {
							if(System.currentTimeMillis() - pt >= holdTime) {
								_repeatPress(px, py);
								Thread.sleep(repeatTime);
								_requestRepaint();
								continue;
							}
						} else {
							touchTicks++;
						}
						if(dragged) {
							_requestRepaint();
						}
					}
				} else if(keyPressed) {
					caretFlash = false;
					if(keyTicks > 10 && !hasRepeatEvents) {
						if(System.currentTimeMillis() - kt >= keyHoldTime) {
							_repeatKey();
							Thread.sleep(keyRepeatTime);
							_requestRepaint();
							continue;
						}
					} else {
						keyTicks++;
					}
				} else if(keyRepeatTicks > 0) {
					keyRepeatTicks--;
					caretFlash = false;
					if(keyRepeatTicks == 0) {
						_flushKeyBuffer();
					}
				} else {
					touchTicks = 0;
					if(flashTicks-- <= 0) {
						caretFlash = !caretFlash;
						flashTicks = 10;
						_requestTextBoxRepaint();
					}
					/*
					synchronized(pressLock) {
						pressLock.wait();
					}
					*/
				}
				Thread.sleep(50);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
