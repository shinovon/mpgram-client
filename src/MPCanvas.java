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
//#ifndef NO_CHAT_CANVAS
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextField;
import java.io.DataInputStream;

abstract class MPCanvas extends Canvas implements LangConstants {

	static final int COLOR_CHAT_BG = 0;
	static final int COLOR_CHAT_FG = 1;

	static final int COLOR_CHAT_MENU_BG = 6;
	static final int COLOR_CHAT_MENU_HIGHLIGHT_BG = 7;
	static final int COLOR_CHAT_MENU_FG = 8;
	static final int COLOR_CHAT_MENU_SEPARATOR = 9;
	static final int COLOR_CHAT_SCROLLBAR = 16;

	static int[] colors = new int[50];
	static int[] colorsCopy;
	static int[] style = new int[20];
	static boolean bg;
	static Image bgImg;
	static int bgWidth, bgHeight;

	static Keyboard keyboard;

	boolean loaded, finished, canceled;
	Thread thread;

	boolean loading;

	boolean reverse;

	int count;
	UIItem firstItem, lastItem;
	UIItem layoutStart;

	// boundaries
	int width, height;
	int clipHeight;
	int top;
	int bottom;
	int contentHeight;

	boolean arrowShown;
	boolean skipRender;

	// scroll
	int scroll;
	int scrollTarget;
	int lastScrollDir;
	int lastDragDir;
	UIItem scrollCurrentItem, scrollTargetItem;
	UIItem focusedItem;
	UIItem nextFocusItem;
	float kineticScroll;

	static final int moveSamples = 10;
	int[] moves = new int[moveSamples];
	long[] moveTimes = new long[moveSamples];
	int movesIdx;

	// animations
	boolean funcFocused;
	int bottomAnimTarget = -1;
	float bottomAnimProgress;
	boolean keyGuide;
	long keyGuideTime;

	int menuAnimTarget = -1;
	float menuAnimProgress;

	long lastPaintTime;
	boolean animating;

	// menu
	boolean menuFocused;
	UIItem menuItem;
	int[] menu;
	int menuCurrent, menuCount;
	int menuHeight;
	int menuScroll;
	int menuScrollTarget = -1;
	boolean menuFitsOnScreen;
	int menuItemHeight;

	// pointer
	int pressX, pressY, pointerX, pointerY;
	boolean pressed, dragging, longTap, contentPressed, draggingHorizontally;
	int dragged;
	long pressTime;
	int dragXHold, dragYHold;
	UIItem pointedItem, heldItem;
	boolean startSelectDir;

	Object nokiaEditor;
	boolean updateEditor;

	final boolean touch = !MP.forceKeyUI && hasPointerEvents();

	final boolean chat = this instanceof ChatCanvas;

	static void loadTheme() {
		if (colorsCopy == null) {
			try {
				DataInputStream d = new DataInputStream("".getClass().getResourceAsStream("/c/".concat(MP.theme)));
				d.readUTF();
				for (int i = 0; i < 50; ++i) {
					colors[i] = d.readInt();
				}
				for (int i = 0; i < 20; ++i) {
					style[i] = d.readByte() & 0xFF;
				}
				d.close();
			} catch (Exception e) {
				colors[ChatCanvas.COLOR_CHAT_BG] = 0x0E1621;
				colors[ChatCanvas.COLOR_CHAT_FG] = 0xFFFFFF;
				colors[ChatCanvas.COLOR_CHAT_HIGHLIGHT_BG] = 0x1A3756;
				colors[ChatCanvas.COLOR_CHAT_PANEL_BG] = 0x17212B;
				colors[ChatCanvas.COLOR_CHAT_PANEL_FG] = 0xFFFFFF;
				colors[ChatCanvas.COLOR_CHAT_PANEL_BORDER] = 0x0A121B;
				colors[ChatCanvas.COLOR_CHAT_MENU_BG] = 0x17212B;
				colors[ChatCanvas.COLOR_CHAT_MENU_HIGHLIGHT_BG] = 0x232E3C;
				colors[ChatCanvas.COLOR_CHAT_MENU_FG] = 0xFFFFFF;
				colors[ChatCanvas.COLOR_CHAT_STATUS_FG] = 0x708499;
				colors[ChatCanvas.COLOR_CHAT_STATUS_HIGHLIGHT_FG] = 0x73B9F5;
				colors[ChatCanvas.COLOR_CHAT_POINTER_HOLD] = 0xFFFFFF;
				colors[ChatCanvas.COLOR_CHAT_INPUT_ICON] = 0x6A7580;
				colors[ChatCanvas.COLOR_CHAT_SEND_ICON] = 0x5288C1;
				colors[ChatCanvas.COLOR_CHAT_INPUT_BORDER] = 0x01C272D;

				colors[UIMessage.COLOR_MESSAGE_BG] = 0x182533;
				colors[UIMessage.COLOR_MESSAGE_OUT_BG] = 0x2B5278;
				colors[UIMessage.COLOR_MESSAGE_FG] = 0xFFFFFF;
				colors[UIMessage.COLOR_MESSAGE_LINK] = 0x71BAFA;
				colors[UIMessage.COLOR_MESSAGE_LINK_FOCUS] = 0xABABAB;
				colors[UIMessage.COLOR_MESSAGE_SENDER] = 0x71BAFA;
				colors[UIMessage.COLOR_MESSAGE_ATTACHMENT_BORDER] = 0x6AB3F3;
				colors[UIMessage.COLOR_MESSAGE_ATTACHMENT_TITLE] = 0xFFFFFF;
				colors[UIMessage.COLOR_MESSAGE_ATTACHMENT_SUBTITLE] = 0x7DA8D3;
				colors[UIMessage.COLOR_MESSAGE_ATTACHMENT_FOCUS_BG] = 0x1A3756;
				colors[UIMessage.COLOR_MESSAGE_COMMENT_BORDER] = 0x31404E;
				colors[UIMessage.COLOR_MESSAGE_IMAGE] = 0xABABAB;
				colors[UIMessage.COLOR_MESSAGE_FOCUS_BORDER] = 0xFFFFFF;
				colors[UIMessage.COLOR_MESSAGE_TIME] = 0x6D7F8F;
				colors[UIMessage.COLOR_MESSAGE_OUT_TIME] = 0x7DA8D3;
				colors[UIMessage.COLOR_ACTION_BG] = 0x1E2C3A;

				colors[ChatsCanvas.COLOR_CHATS_BG] = 0x0E1621;
				colors[UIDialog.COLOR_CHATS_ITEM_HIGHLIGHT_BG] = 0x1A3756;
				colors[UIDialog.COLOR_CHATS_ITEM_HIGHLIGHT_FG] = 0xF5F5F5;
				colors[UIDialog.COLOR_CHATS_ITEM_TITLE] = 0xF5F5F5;
				colors[UIDialog.COLOR_CHATS_ITEM_TEXT] = 0x7F91A4;
				colors[UIDialog.COLOR_CHATS_ITEM_MEDIA] = 0x73B9F5;
				colors[UIDialog.COLOR_CHATS_ITEM_SEPARATOR] = 0x0A121B;

				style[UIMessage.STYLE_MESSAGE_FILL] = 1;
				style[UIMessage.STYLE_MESSAGE_ROUND] = 1;
				style[UIMessage.STYLE_MESSAGE_BORDER] = 0;
			}

			colorsCopy = new int[colors.length];
			System.arraycopy(colors, 0, colorsCopy, 0, colors.length);

			// load icons
			ChatCanvas.attachIcon = loadRLE("attach", colors[ChatCanvas.COLOR_CHAT_INPUT_ICON]);
//			ChatCanvas.backIcon = loadRLE("back", colors[ChatCanvas.COLOR_CHAT_PANEL_FG]);
//			ChatCanvas.moreIcon = loadRLE("more", colors[ChatCanvas.COLOR_CHAT_PANEL_FG]);
		}
	}

	private static Image loadRLE(String path, int color) {
		try {
			int w, h;

			DataInputStream dis = new DataInputStream("".getClass().getResourceAsStream("/i/".concat(path)));

			byte[] rle = new byte[dis.available()];
			dis.read(rle);
			dis.close();

			w = (rle[0] << 24) | (rle[1] << 16) | (rle[2] << 8) | (rle[3] & 0xff);
			h = (rle[4] << 24) | (rle[5] << 16) | (rle[6] << 8) | (rle[7] & 0xff);

			byte[] alpha = new byte[w * h];
			{
				int inp = 8;
				int alphap = 0;

				while (alphap < alpha.length && rle.length - inp >= 3) {
					int count = ((rle[inp] & 0xff) << 8) | (rle[inp + 1] & 0xff);
					inp += 2;

					boolean repeat = count > 0x7fff;
					count = (count & 0x7fff) + 1;

					if (repeat) {
						byte b = rle[inp];
						inp++;
						int fillEnd = alphap + count;

						for (; alphap < fillEnd; alphap++) {
							alpha[alphap] = b;
						}
					} else {
						System.arraycopy(rle, inp, alpha, alphap, count);
						alphap += count;
						inp += count;
					}
				}
			}
			int[] rgb = new int[w * h];
			for (int i = 0; i < alpha.length; ++i) {
				rgb[i] = (alpha[i] << 24) | color;
			}
			return Image.createRGBImage(rgb, w, h, true);
		} catch (Exception e) {
			return null;
		}
	}

	MPCanvas() {
		loadTheme();

		if (chat) {
			if (bgImg == null) {
				try {
					String p = MP.wallpaperPath;
					if (p != null && p.length() != 0) {
						if (p.indexOf(':') != -1) {
							bgImg = MP.getImage(p);
						} else {
							bgImg = Image.createImage(p);
						}

						/*int i = */Math.max(bgWidth = bgImg.getWidth(), bgHeight = bgImg.getHeight());
//						int s = Math.max(getWidth(), getHeight());
//						if (i > s) {
//							bgImg = MP.resize(bgImg, s, s);
//						}
						bg = true;
					}
				} catch (Throwable e) {
					bg = false;
				}
			}

			// initialize keyboard
			switch (MP.textMethod) {
			case 0: // auto
			case 1: // nokiaui
//#ifndef NO_NOKIAUI
				if (touch) {
					try {
						nokiaEditor = NokiaAPI.createTextEditor(500, TextField.ANY, 40, 40);
						if (nokiaEditor != null) {
							NokiaAPI.TextEditor_setContent(nokiaEditor, "");
						}
					} catch (Throwable ignored) {}
					if (nokiaEditor != null) {
						updateEditor = true;
						keyboard = null;
						break;
					}
				}
//#endif
				if (MP.textMethod == 1) break;
			case 2: // j2mekeyboard
				nokiaEditor = null;
				if (keyboard == null) {
					// do not use multiline in j2mekeyboard as it's too broken
					keyboard = Keyboard.getKeyboard(this, false, getWidth(), getHeight());

					keyboard.setTextColor(colors[ChatCanvas.COLOR_CHAT_PANEL_FG]);
					keyboard.setTextHintColor(colors[ChatCanvas.COLOR_CHAT_INPUT_ICON]);
					keyboard.setCaretColor(colors[ChatCanvas.COLOR_CHAT_PANEL_FG]);
					keyboard.setTextHint(MP.L[LTextField_Hint]);
					keyboard.setLanguages(MP.inputLanguages);
				} else {
					keyboard.setListener((ChatCanvas) this);
					keyboard.reset();
				}
				break;
			case 3: // textbox
				keyboard = null;
				nokiaEditor = null;
			}
		}
	}

	public void load() {
		if (loaded) return;
		loaded = true;
		canceled = finished = false;

		loading = true;
		Thread thread = this.thread = Thread.currentThread();
		try {
			count = 0;
			firstItem = lastItem = null;
			scrollCurrentItem = scrollTargetItem = focusedItem = null;
			kineticScroll = scroll = 0;
			
			loadInternal(thread);
//			MP.display(this);
			queueRepaint();
		} catch (Exception e) {
			if (e == MP.cancelException || canceled || this.thread != thread) {
				// ignore exception if cancel detected
				return;
			}
			MP.display(MP.errorAlert(e), this);
			e.printStackTrace();
		} finally {
			if (this.thread == thread) {
				loading = false;
				this.thread = null;
			}
		}
	}

	protected void paint(Graphics g) {
		int w = getWidth(), h = getHeight();
		if (keyboard != null && keyboard.isVisible()) {
			h -= keyboard.paint(g, w, h);
		}
		g.setClip(0, 0, w, h);

		int contentHeight = this.contentHeight;

		long now = System.currentTimeMillis();
		long deltaTime = now - lastPaintTime;
		if (deltaTime > 500) deltaTime = 500;
		if (width != w) {
			layoutStart = firstItem;
			resized();
			if (menuFocused) {
				menuFitsOnScreen = h <= height;
				menuScroll = 0;
			}
		}
		width = w; height = h;

		if (loading && !chat) {
			g.setColor(colors[ChatsCanvas.COLOR_CHATS_BG]);
			g.fillRect(0, 0, w, h);
			g.setColor(colors[ChatsCanvas.COLOR_CHAT_FG]);
			g.drawString(MP.L[LLoading], w >> 1, h >> 3, Graphics.TOP | Graphics.HCENTER);
			return;
		}

		boolean animate = false;

		// animations

		if (bottomAnimTarget != -1) {
			if (slide(1, bottomAnimProgress, bottomAnimTarget, deltaTime)) {
				animate = true;
			} else {
				bottomAnimProgress = bottom = bottomAnimTarget;
				bottomAnimTarget = -1;
			}
		}
		if (menuAnimTarget != -1) {
			if (slide(2, menuAnimProgress, menuAnimTarget, deltaTime)) {
				animate = true;
			} else {
				menuAnimProgress = menuAnimTarget;
				menuAnimTarget = -1;
				updateEditor = true;
			}
		}

		int clipHeight = this.clipHeight = h - top - bottom;

		if (!loading) {
			// layout
			if (layoutStart != null) {
				UIItem idx = layoutStart;
				layoutStart = null;
				layout(idx, w, clipHeight);
				contentHeight = this.contentHeight;
			}

			if (nextFocusItem != null && nextFocusItem.layoutWidth != 0) {
				focusItem(nextFocusItem, touch ? 0 : reverse ? -1 : 1);
				if (!isVisible(nextFocusItem)) scrollTo(nextFocusItem);
				nextFocusItem = null;
			}

			// key scroll

			if (scrollTarget != -1) {
				if (contentHeight <= clipHeight) {
					scroll = 0;
					scrollTarget = -1;
				} else {
					if (slide(0, scroll, scrollTarget, deltaTime)) {
						animate = true;
					} else {
						scroll = scrollTarget;
						scrollTarget = -1;
					}
					if (scroll <= 0) {
						scroll = 0;
						scrollTarget = -1;
						animate = false;
					} else if (scroll >= contentHeight - clipHeight) {
						scroll = contentHeight - clipHeight;
						scrollTarget = -1;
						animate = false;
					}
				}
			}

			if (menuFocused && !menuFitsOnScreen && menuScrollTarget != -1) {
				if (slide(3, menuScroll, menuScrollTarget, deltaTime)) {
					animate = true;
				} else {
					menuScroll = menuScrollTarget;
					menuScrollTarget = -1;
				}
				if (menuScroll <= 0) {
					menuScroll = 0;
					menuScrollTarget = -1;
					animate = false;
				} else if (menuScroll >= contentHeight - clipHeight) {
					menuScroll = contentHeight - clipHeight;
					menuScrollTarget = -1;
					animate = false;
				}
			}

			if (!touch && scrollTarget == -1) {
				if (focusedItem == null && scrollCurrentItem == null && scrollTargetItem == null) {
					int d = lastScrollDir;
					if (d == 0) d = 1;
					UIItem item = getItemAt(height >> 1);
					if (item == null || !item.focusable) {
						item = getFirstFocusableItemOnScreen(null, d, clipHeight / 4);
					}
					if (item != null) focusItem(item, d);
				} else if (focusedItem != null && scrollCurrentItem == null && !isVisible(focusedItem)) {
					scrollTo(focusedItem);
				}
			}

			// touch scroll

			if (kineticScroll != 0) {
				if ((kineticScroll > -1 && kineticScroll < 1)
						|| contentHeight <= clipHeight
						|| scroll <= 0
						|| scroll >= contentHeight - clipHeight) {
					kineticScroll = 0;
				} else {
					float mul = pressed && !dragging ? 0.5f : 0.965f;
					scroll += (int) kineticScroll;
					kineticScroll *= mul;
					float f;
					if ((f = deltaTime > 33 && animating ? deltaTime / 33f : 1f) >= 2) {
						int j = (int) f - 1;
						for (int i = 0; i < j; i++) {
							scroll += (int) kineticScroll;
							kineticScroll *= mul;
						}
					}
					animate = true;
				}
			}
		}


		// limit scroll
		int scroll = this.scroll;
		if (scroll < 0) this.scroll = scroll = 0;
		else if (contentHeight <= clipHeight) this.scroll = scroll = 0;
		else if (scroll > contentHeight - clipHeight) this.scroll = scroll = contentHeight - clipHeight;

		if (!skipRender || !menuFocused) {
			// background
			g.setColor(colors[chat ? ChatCanvas.COLOR_CHAT_BG : ChatsCanvas.COLOR_CHATS_BG]);
			g.fillRect(0, 0, w, h);
			if (bgImg != null && chat) {
//				g.drawImage(bgImg, (w - bgWidth) >> 1, (h - bgHeight) >> 1, 0);
				int bgWidth = MPCanvas.bgWidth;
				int bgHeight = MPCanvas.bgHeight;
				g.drawRegion(bgImg,
						Math.max(0, (bgWidth - w) >> 1), Math.max(0, (bgHeight - h) >> 1),
						Math.min(bgWidth, w), Math.min(bgHeight, h), 0,
						Math.max(0, (w - bgWidth) >> 1), Math.max(0, (h - bgHeight) >> 1), 0);
			}
			g.setColor(colors[COLOR_CHAT_FG]);

			// render items

			UIItem item = firstItem;
			if (item != null && !loading) {
				int x = 0;
				if (pressed && draggingHorizontally) {
					int d = pointerX - pressX;
					if (d > 0) {
						x = d;
					} else if (pointedItem instanceof UIMessage) {
						x = Math.max(-100, Math.min(0, d));
					}
				}

				g.setClip(0, top, w, clipHeight);
				if (reverse) {
					clipHeight += top;
					int y = h - bottom + scroll;
					do {
						if (y < 0) break;
						y -= item.contentHeight;
						if (y > clipHeight) continue;
						item.paint(g, pointedItem == item || x > 0 ? x : 0, y, w);
					} while ((item = item.next) != null);
				} else {
					int y = top - scroll;
					clipHeight += top;
					do {
						int ih = item.contentHeight;
						if (y < -ih) {
							y += ih;
							continue;
						}
						item.paint(g, pointedItem == item || x > 0 ? x : 0, y, w);
						if ((y += ih) > clipHeight) break;
					} while ((item = item.next) != null);
				}
			}

			g.setClip(0, 0, w, h);
			paintInternal(g, w, h, now);
		} else {
			g.setClip(0, 0, w, h);
		}

		// popup menu
		if (menuAnimProgress != 0) {
			skipRender = true;
			int my;
			if (menuFitsOnScreen) {
				my = h - (int)menuAnimProgress;
			} else {
				if (menuScroll < 0) menuScroll = 0;
				else if (menuScroll > menuHeight - h) menuScroll = menuHeight - h;
				my = menuHeight - (int)menuAnimProgress - menuScroll;
			}
			g.setColor(colors[COLOR_CHAT_MENU_BG]);
			g.fillRect(0, my, w, (int)menuAnimProgress);
			if (menu != null) {
				int[] menu = this.menu;
				g.setFont(MP.medPlainFont);
				int menuItemHeight = this.menuItemHeight;
				for (int i = 0; i < menu.length; i++) {
					if (menu[i] == Integer.MIN_VALUE) break;
					if (i == menuCurrent && (!touch || menuCurrent != -1)) {
						g.setColor(colors[COLOR_CHAT_MENU_HIGHLIGHT_BG]);
						g.fillRect(0, my, w, menuItemHeight);
					}
					g.setColor(colors[COLOR_CHAT_MENU_FG]);
					g.drawString(MP.L[menu[i]], 8, my + ((menuItemHeight - MP.medPlainFontHeight) >> 1), 0);
					my += menuItemHeight;
					g.setColor(colors[COLOR_CHAT_MENU_SEPARATOR]);
					g.drawLine(20, my, w - 20, my);
				}
			}
		} else {
			skipRender = false;

			// process long tap
			if (pressed) {
				if (now - pressTime > 100) {
					kineticScroll = 0;
				}
				if (!longTap && dragged < 10 && pointedItem != null && pointedItem.focusable
						&& Math.abs(pointerX - pressX) < 5 && Math.abs(pointerY - pressY) < 5) {
					animate = true;
					if (now - pressTime > 200) {
						kineticScroll = 0;
						int size = Math.min(360, (int) (now - pressTime - 200) / 2);
//						g.setColor(colors[ChatCanvas.COLOR_CHAT_POINTER_HOLD]);
//						g.fillArc(pointerX - 25, pointerY - 25, 50, 50, 90, (size * 360) / 200);
						if (size >= 200) {
							// handle long tap
							longTap = true;
							focusItem(pointedItem, 0);
							int y = pointerY;
							pointedItem.tap(pointerX,
									reverse ? y - (scroll - bottom - pointedItem.y + height - pointedItem.contentHeight)
											: y - pointedItem.y - top - scroll,
									true);
						}
					}
				}
			}
		}

//		g.setFont(MP.smallPlainFont);
//		String s = "r:" + (System.currentTimeMillis() - now) + " t:" + (deltaTime) + " f:" + (1000 / Math.max(1, deltaTime));
//		g.setColor(0);
//		g.drawString(s, 19, h - 60, 0);
//		g.drawString(s, 21, h - 60, 0);
//		g.drawString(s, 20, h - 59, 0);
//		g.drawString(s, 20, h - 61, 0);
//		g.setColor(0x00AA00);
//		g.drawString(s, 20, h - 60, 0);

		// limit fps
		if (deltaTime < 32) {
			try {
				Thread.sleep(33 - deltaTime);
			} catch (Exception ignored) {}
		}
		animating = animate;
		if (animate) {
			repaint();
		}
		lastPaintTime = now;
	}

	public void queueRepaint() {
		if (isShown()) repaint();
	}

	boolean focusItem(UIItem item, int dir) {
		if (focusedItem == item) return true;
		if (focusedItem != null) {
			focusedItem.lostFocus();
		}
		scrollCurrentItem = item;
		if (scrollTargetItem != item) scrollTargetItem = null;
		focusedItem = item;
		if (item != null) {
			if (!focusedItem.grabFocus(dir)) {
				focusedItem = null;
				return false;
			}
			return true;
		}
		return false;
	}

	void scrollTo(UIItem item) {
		scrollCurrentItem = item;
		scrollTo(item.y);
	}

	void scrollTo(int y) {
		if (MP.fastScrolling) {
			scroll = y;
			return;
		}
		scrollTarget = y;
	}

	boolean isVisible(UIItem item) {
		return item != null && item.y + item.contentHeight > scroll && item.y < scroll + clipHeight;
	}

	boolean isVisible(UIItem item, int offset) {
		return item != null && item.y + item.contentHeight + offset > scroll && item.y < scroll + clipHeight - offset;
	}

	boolean isCornerVisible(UIItem item, int dir) {
		if (dir == (reverse ? 1 : -1)) {
			// isTopVisible
			return item.y >= scroll && item.y < scroll + height;
		} else {
			// isBottomVisible
			return item.y + item.contentHeight > scroll && item.y + item.contentHeight <= scroll + clipHeight;
		}
	}

	UIItem getFirstFocusableItemOnScreen(UIItem offset, int dir, int offsetHeight) {
		offset = offset == null ? (dir == 1 ? firstItem : lastItem) : (dir == -1 ? offset.prev : offset.next);
		UIItem res = null;
		while (offset != null) {
			UIItem t = offset;
			if (t.focusable && isVisible(t, offsetHeight)) {
				res = t;
				break;
			}
			offset = (dir == -1 ? offset.prev : offset.next);
		}
		return res;
	}

	UIItem getItemAt(int y) {
		if (reverse) {
			y = height + scroll - bottom - y;
		} else {
			y -= top - scroll;
		}
		if (y < scroll || y > clipHeight + scroll) return null;
		UIItem item = firstItem;
		if (item == null) return null;
		do {
			if (y >= item.y && y < item.y + item.contentHeight) {
				return item;
			}
		} while ((item = item.next) != null);
		return null;
	}

	public void requestPaint(UIItem item) {
		if (item == null || !isVisible(item)) return;
		queueRepaint();
	}

	private boolean slide(int mode, float val, int target, long deltaTime) {
		if (!MP.fastScrolling) {
			float d = Math.abs(target - val);
			boolean dir = target - val > 0;
			if (d < 1) {
				return false;
			} else // if (mode == 0) {
				if (d < 5) {
					if (dir) {
						++ val;
					} else {
						-- val;
					}
				} else for (int i = 0; i < 1 + (deltaTime > 33 && animating ? (deltaTime / 33) - 1 : 0); ++i) {
//					val = MP.lerp(val, target, 4, 20);
					val = val + ((target - val) * 4F / 20);
				}
			/* } else {
				float f = 20F * (1 + (deltaTime > 33 && animating ? (deltaTime / 33f) - 1 : 0));
				if (dir) {
					if ((val += f) >= target) {
						val = target;
					}
				} else {
					if ((val -= f) <= target) {
						val = target;
					}
				}

			}*/
		} else {
			val = target;
		}

		if (mode == 0) {
			scroll = (int) val;
		} else if (mode == 1) {
			bottom = (int) (bottomAnimProgress = val);
		} else if (mode == 2) {
			menuAnimProgress = val;
		} else /*if (mode == 3)*/ {
			menuScroll = (int) val;
		}

		return !MP.fastScrolling;
	}

	private int mapGameAction(int key) {
		switch (key) {
		case -1:
			return Canvas.UP;
		case -2:
			return Canvas.DOWN;
		case -3:
			return Canvas.LEFT;
		case -4:
			return Canvas.RIGHT;
		default:
			int g = 0;
			try {
				g = getGameAction(key);
			} catch (Exception ignored) {}
			return g <= 0 ? 0 : g;
		}
	}

	private int mapKey(int key) {
		if (key == -21 || key == 21) {
			return -6;
		}
		if (key == -22 || key == 22) {
			return -7;
		}
		return key;
	}

	protected void keyPressed(int key) {
		super.keyPressed(key);
		key(key, false);
	}

	protected void keyRepeated(int key) {
		super.keyRepeated(key);
		// TODO own repeater thread
		key(key, true);
	}

	protected void keyReleased(int key) {
		super.keyReleased(key);
		if (chat && keyboard != null && keyboard.isVisible() && keyboard.keyReleased(key)) {
			// return;
		}
	}

	void key(int key, boolean repeat) {
		int game = mapGameAction(key);
		key = mapKey(key);
		String s/* = null*/;
		try {
			s = getKeyName(key).toLowerCase();
			if (s.equals("send") || s.equals("call")
					|| (MP.symbian && key == -10)) {
				game = -10;
			} else if (s.indexOf("back") != -1 && s.indexOf("backspace") == -1) {
				game = -11;
			}
		} catch (Exception ignored) {}
		boolean repaint = false;
		if (chat && keyboard != null && keyboard.isVisible() && game >= 0 && (repeat ? keyboard.keyRepeated(key) : keyboard.keyPressed(key))) {
			// keyboard grabbed event
		} else if (key == -7 || (MP.blackberry && (key == 'p' || key == 'P'))) {
			if (repeat) return;
			// back
			if (menuFocused) {
				closeMenu();
			} else if (!handleRightSoft()) {
				funcFocused = true;
				bottomAnimTarget = MP.smallBoldFontHeight + 4;
				keyGuide = false;
			}
			repaint = true;
		} else if (key == -6 || (MP.blackberry && (key == 'q' || key == 'Q'))) {
			if (repeat || loading) return;
			// menu
			if (menuFocused) {
				closeMenu();
			} else if (!handleLeftSoft()) {
				if (focusedItem != null && focusedItem.focusable) {
					int[] menu = focusedItem.menu();
					if (menu != null && menu.length != 0) {
						showMenu(focusedItem, menu);
					}
				}
			}
			repaint = true;
		} else if (menuFocused && game >= 0) {
			if (menuCurrent == -1) menuCurrent = 0;
			if (game == Canvas.UP) {
				if (menuCurrent-- == 0) {
					menuCurrent = menuCount - 1;
					if (!menuFitsOnScreen) {
						menuScrollTarget = menuHeight - height;
					}
				} else if (!menuFitsOnScreen && menuCurrent * (menuItemHeight) - menuScroll < 30) {
					menuScrollTarget = menuScroll - height / 5;
				}
				repaint = true;
			} else if (game == Canvas.DOWN) {
				if (menuCurrent++ == menuCount - 1) {
					menuCurrent = 0;
					if (!menuFitsOnScreen) {
						menuScrollTarget = 0;
					}
				} else if (!menuFitsOnScreen && menuCurrent * (menuItemHeight) - menuScroll > height - 30) {
					menuScrollTarget = menuScroll + height / 5;
				}
				repaint = true;
			} else if (key == -5 || game == Canvas.FIRE) {
				menuAction(menuCurrent);
				repaint = true;
			}
		} else if (loading) {
			// prevent NPE below
		} else if (handleKey(key, game)) {
			// grabbed
		} else if (key == -5 || game == Canvas.FIRE) {
			// action
			if (focusedItem != null) {
				if (focusedItem.action()) {
					queueRepaint();
				}
			}
		} else if (key >= Canvas.KEY_NUM0 && key <= Canvas.KEY_NUM9) {
			// ignore
		} else if (game == Canvas.DOWN || game == Canvas.UP) {
			scrolled();
			scroll: {
				int dir = game == Canvas.DOWN ? 1 : -1;
				int dir2 = dir;
				if (reverse) dir = -dir;
				final int scrollAmount = clipHeight / 4;
				if (scrollTargetItem == null && scrollCurrentItem == null) {
					scrollTargetItem = getFirstFocusableItemOnScreen(null, 1, 0);
					if (touch && isVisible(scrollTargetItem)) {
						focusItem(scrollTargetItem, reverse ? -1 : 1);
						repaint = true;
						break scroll;
					}
				}
				if (focusedItem != null) {
					int t = focusedItem.traverse(game);
					if (t != Integer.MIN_VALUE) {
						repaint = true;
						if (t != Integer.MAX_VALUE) {
							scrollTo(scroll + scrollAmount * dir);
						}
						break scroll;
					}
				}
				if (lastScrollDir != dir) {
					UIItem item = scrollCurrentItem;
					if (item != null && !isVisible(item)) {
						scrollCurrentItem = null;
					}
					scrollTargetItem = null;
					lastScrollDir = dir;
				}
				if (scrollCurrentItem != null && scrollTargetItem == null) {
					// get next scroll target
					scrollTargetItem = getFirstFocusableItemOnScreen(scrollCurrentItem, dir, 0);
				}
				UIItem item = scrollTargetItem;
				if (item != null && isVisible(item)) {
					focusItem(item, dir2);
				}
				if (scrollTargetItem != null && isVisible(scrollTargetItem)) {
					repaint = true;
					focusItem(scrollTargetItem, dir2);
					scrollCurrentItem = scrollTargetItem;
					if (isCornerVisible(scrollTargetItem, dir2) && isVisible(scrollTargetItem)) {
						scrollTargetItem = getFirstFocusableItemOnScreen(scrollCurrentItem, dir, 0);
						if (scrollTargetItem != null && isCornerVisible(scrollTargetItem, dir2) && isVisible(scrollTargetItem, clipHeight / 8)) {
							break scroll;
						}
					}
				}
				repaint = true;
				if (dir == 1) {
					scrollTo(Math.min(scroll + scrollAmount, contentHeight - clipHeight));
				} else {
					scrollTo(Math.max(scroll - scrollAmount, 0));
				}

				if (scrollTargetItem != null && isVisible(scrollTargetItem) && isCornerVisible(scrollTargetItem, dir2)) {
					scrollTargetItem = null;
				}
			}
		} else if (game == Canvas.LEFT || game == Canvas.RIGHT) {
			if (focusedItem != null && focusedItem.traverse(game) == Integer.MAX_VALUE) {
				repaint = true;
			}
		}
		if (repaint) {
			queueRepaint();
		}
	}

	protected void pointerPressed(int x, int y) {
		if (chat && keyboard != null && keyboard.pointerPressed(x, y)) return;
		focusItem(null, 0);
		pressed = true;
		dragging = false;
		draggingHorizontally = false;
		dragged = 0;
		dragXHold = 0;
		dragYHold = 0;
		pressX = pointerX = x;
		pressY = pointerY = y;
		movesIdx = 0;
		pressTime = System.currentTimeMillis();
		if (!menuFocused && !loading && y > top && y < top + clipHeight &&
				// not touching arrow icon
				!(arrowShown && x > width - 40
						&& (reverse ? (y > height - bottom - 40) : (y < top + 40)))) {
			pointedItem = getItemAt(y);
			contentPressed = true;
		} else {
			pointedItem = null;
		}
		queueRepaint();
	}


	protected void pointerDragged(int x, int y) {
		if (chat && keyboard != null && keyboard.pointerDragged(x, y)) return;
		long now = System.currentTimeMillis();
		if (contentPressed || (menuFocused && !menuFitsOnScreen)) {
			if (longTap && !menuFocused) {
				boolean d = y > pointerY;
				if (heldItem == null) {
					heldItem = pointedItem;
					startSelectDir = d;
				}
				if (heldItem instanceof UIMessage) {
					UIItem item = getItemAt(y);
					if (item instanceof UIMessage && (item != pointedItem)) {
						if (d == startSelectDir) {
							if (((UIMessage) heldItem).selected) {
								((UIMessage) item).select();
							} else {
								((UIMessage) item).unselect();
							}
						} else if (((UIMessage) heldItem).selected) {
							((UIMessage) pointedItem).unselect();
						} else {
							((UIMessage) pointedItem).select();
						}
						pointedItem = item;
					}
				}
			} else {
				final int dY = pointerY - y;
				final int dX = pointerX - x;
				dragged += Math.abs(dX) + Math.abs(dY);
				if (dragging || dY > 1 || dY < -1
						|| dragYHold + dY > 2 || dragYHold + dY < -2
						|| dX > 1 || dX < -1
						|| dragXHold + dX > 2 || dragXHold + dX < -2) {
//					int dx2 = dX;
					int dy2 = dY;
					if (now - pressTime < 100) {
//						dx2 += dragXHold;
						dy2 += dragYHold;
					}
					if (menuFocused) {
						menuScroll += dy2;
						if (kineticScroll * dy2 < 0) kineticScroll = 0;
						lastDragDir = dy2 < 0 ? -1 : 1;
					} else if (chat
							&& (draggingHorizontally || (!dragging && Math.abs(/*dy2*/dX) > Math.abs(dy2)))) {
						if (!draggingHorizontally) {
							focusItem(pointedItem, 0);
						}
						draggingHorizontally = true;
					} else {
						if (reverse) dy2 = -dy2;
						scroll += dy2;
						if (kineticScroll * dy2 < 0) kineticScroll = 0;
						lastDragDir = dy2 < 0 ? -1 : 1;
					}
					dragging = true;
					dragXHold = 0;
					dragYHold = 0;
					scrollTarget = -1;
				} else {
					// hold dragged units until it reaches threshold
					dragXHold += dX;
					dragYHold += dY;
				}
				int prev = movesIdx - 1;
				if (prev < 0) prev += moveSamples;
				long prevTime = moveTimes[prev];
				if (now - prevTime <= 1) {
					moves[prev] += dY;
					moveTimes[prev] = now;
				} else {
					moves[movesIdx] = dY;
					moveTimes[movesIdx] = now;
					movesIdx = (movesIdx + 1) % moveSamples;
				}
			}
		}
		pointerX = x;
		pointerY = y;
		queueRepaint();
	}

	protected void pointerReleased(int x, int y) {
		if (keyboard != null && keyboard.pointerReleased(x, y)) return;
		long now = System.currentTimeMillis();
		if (contentPressed || menuFocused) {
			if (!longTap) {
				if (!dragging || (dragged < 10 && Math.abs(x - pressX) < 4 && Math.abs(y - pressY) < 4)) {
					if (kineticScroll != 0) {
						kineticScroll = 0;
					} else if (now - pressTime < 300) {
						if (menuFocused) {
							int my = (menuFitsOnScreen ? height : (menuHeight - menuScroll)) - (int)menuAnimProgress;
							if (y < my || x < 20 || x > width - 20 || menu == null) {
								closeMenu();
								queueRepaint();
							} else if (!longTap && now - pressTime < 300 && menuAnimTarget == -1) {
								doMenuAction((y - my) / menuItemHeight);
							}
						} else if (pointedItem != null && pointedItem.focusable) {
							focusItem(pointedItem, 0);
							pointedItem.tap(x,
									reverse ? y - (scroll - bottom - pointedItem.y + height - pointedItem.contentHeight)
											: y - pointedItem.y - top + scroll,
									false);
						}
					}
				} else if (!draggingHorizontally || !handleSwipe()) {
					int move = 0;
					long moveTime = 0, lastTime = 0;
					for (int i = 0; i < moveSamples; i++) {
						int idx = (movesIdx + moveSamples - 1 - i) % moveSamples;
						long time = moveTimes[idx];
						if (time == 0) {
							break;
						}
						if (i == 0) {
							lastTime = time;
						}
						if ((time = now - time) > 200) {
							break;
						}
						move += moves[idx];
						moveTime += time;
					}
					if (moveTime == 0) moveTime = 1;
					long holdTime = now - lastTime;
					if (moveTime > 0 && holdTime < 150) {
						// release kinetic velocity
						float res = (130f * move) / moveTime;
						if (holdTime > 28) {
							if (Math.abs(res) > Math.abs(move))
								res = move;
							res *= 25f / (holdTime - 10);
						}
						float abs = Math.abs(res);
						if (abs >= 1) {
							if (abs > 100) {
								res = (res < 0 ? -60 : 60);
							}
							if (reverse) res = -res;
							if (kineticScroll * res < 0) kineticScroll = 0;
							kineticScroll += res;
							scrolled();
						}
					}
				}
			}
		} else if (touch && now - pressTime < 300) {
			tap(x, y);
		}
		dragXHold = 0;
		dragYHold = 0;
		pointedItem = null;
		heldItem = null;
		dragging = false;
		draggingHorizontally = false;
		contentPressed = false;
		pressed = false;
		longTap = false;
		pointerX = x;
		pointerY = y;
		queueRepaint();
	}

	void showMenu(UIItem item, int[] menu) {
		if (keyboard != null && keyboard.isVisible()) onKeyboardCancel();
		if (item == null && menu[0] != LDelete && MP.playerState != 0) {
			int[] t = menu;
			menu = new int[menu.length + 1];
			System.arraycopy(t, 0, menu, 0, t.length);
			menu[t.length] = LOpenPlayer;
		}
		kineticScroll = 0;
		this.menuItem = item;
		this.menu = menu;
		menuCurrent = touch ? -1 : 0;
		menuFocused = true;
		skipRender = false;
		updateEditor = true;
		int len = menu.length;
		for (int i = 0; i < len; i++) {
			if (menu[i] == Integer.MIN_VALUE) {
				len = i;
				break;
			}
		}
		menuCount = len;
		menuItemHeight = MP.medPlainFontHeight + (touch ? 16 : 8);
		int h = menuItemHeight * len;
		if (touch && h >= height - 20) {
			this.menu = new int[len + 1];
			System.arraycopy(menu, 0, this.menu, 0, len);
			this.menu[len] = LBack;
			menuCount++;
			h += menuItemHeight;
		}
		menuHeight = h;
		menuFitsOnScreen = h <= height;
		menuScroll = 0;
		menuAnimTarget = h;

		if (len != 0 && menu != null) {
			for (int i = 0; i < colors.length; ++i) {
				if (i == COLOR_CHAT_MENU_BG || i == COLOR_CHAT_MENU_HIGHLIGHT_BG
						|| i == COLOR_CHAT_MENU_FG || i == COLOR_CHAT_MENU_SEPARATOR)
					continue;
				int c = colorsCopy[i];
				colors[i] = ((((c >> 16) & 0xFF) * 15) >> 5) << 16 | ((((c >> 8) & 0xFF) * 15) >> 5) << 8 | ((((c) & 0xFF) * 15) >> 5);
			}
		}

		updateColors();
	}

	void closeMenu() {
		menuFocused = false;
		menuItem = null;
		menu = null;
		menuAnimTarget = 0;
		System.arraycopy(colorsCopy, 0, colors, 0, colors.length);
		updateColors();
		skipRender = false;
		updateEditor = true;
	}

	private void updateColors() {
		UIItem item = firstItem;
		if (item == null) return;
		do {
			if (!(item instanceof UIMessage))
				continue;
			((UIMessage) item).updateColors = true;
		} while ((item = item.next) != null);
	}

	public void onKeyboardCancel() {
		skipRender = false;
		keyboard.hide();
		queueRepaint();
	}

	private void doMenuAction(int i) {
		if (i >= menu.length) return;

		if (menu[i] == LBack) {
			// just close
		} else if (menuItem == null) {
			menuAction(menu[i]);
		} else {
			menuItem.menuAction(menu[i]);
		}
		closeMenu();
		queueRepaint();
	}

	public void requestLayout(UIItem item) {
		if (layoutStart != null || item == null) {
			layoutStart = firstItem;
		} else {
			layoutStart = (UIItem) item;
		}
		if (!loading) queueRepaint();
	}

	private void layout(UIItem offsetItem, int w, int h) {
		if (count == 0 || offsetItem == null) return;
		boolean offset = false;
		if (offsetItem.prev != null) {
			offsetItem = offsetItem.prev;
			offset = true;
		}

		int prevScroll = scroll;
		int prevScrollItemY = 0;
		UIItem scrollItem = scrollCurrentItem;
		if (scrollItem != null) {
			prevScrollItemY = scrollItem.y;
		}

		UIItem item = offsetItem;
		int y = 0;
		do {
			if (offset && item == offsetItem) {
				y = item.y;
			} else {
				item.y = y;
			}
			y += item.layout(w);
		} while ((item = item.next) != null);

		contentHeight = y;

		if (prevScrollItemY != 0 && scroll != 0) {
			scroll = prevScroll - prevScrollItemY + scrollItem.y;
		}
	}

	void safeAdd(Thread thread, UIItem item, boolean focus) {
		if (thread != this.thread) throw MP.cancelException;
		add(item);
		if (focus) {
			nextFocusItem = item;
		}
	}

	void safeAddFirst(Thread thread, UIItem item) {
		if (thread != this.thread) throw MP.cancelException;
		addFirst(item);
	}

	void addFirst(UIItem item) {
		if (item == null) return;
		count++;
		if (firstItem == null) {
			firstItem = lastItem = item;
		} else {
			item.next = firstItem;
			firstItem.layoutWidth = 0;
			firstItem = (firstItem.prev = item);
		}
		item.container = this;
		requestLayout(item);
	}

	void add(UIItem item) {
		if (item == null) return;
		count++;
		if (firstItem == null || lastItem == null) {
			firstItem = lastItem = item;
		} else {
			item.prev = lastItem;
			lastItem.layoutWidth = 0;
			lastItem = (lastItem.next = item);
		}
		item.container = this;
		requestLayout(item);
	}

	void remove(UIItem item) {
		if (item == null) return;
		UIItem i = firstItem;
		if (i == null) return;
		do {
			if (i == item) {
				if (item == firstItem) {
					firstItem = item.next;
				}
				if (item == lastItem) {
					lastItem = item.prev;
				}
				if (item.prev != null) {
					item.prev.layoutWidth = 0;
					item.prev.next = item.next;
				}
				if (item.next != null) {
					item.next.layoutWidth = 0;
					item.next.prev = item.prev;
				}
				item.container = null;
				count--;
				requestLayout(item.prev);
				break;
			}
		} while ((i = i.next) != null);
		if (lastItem == null) {
			lastItem = firstItem;
		}
	}

	void cancel() {
		loaded = false;
		if (finished || thread == null) return;
		canceled = true;
		MP.midlet.cancel(thread, false);
		thread = null;
	}

	public void hideNotify() {
		closeMenu();
	}

	public void showNotify() {
		skipRender = false;
		repaint();
	}

	void closed(boolean destroy) {
		if (destroy) cancel();
	}

	protected void scrolled() {

	}

	protected void resized() {

	}

	protected void menuAction(int i) {

	}

	protected void tap(int x, int y) {

	}

	protected boolean handleSwipe() {
		return false;
	}

	protected boolean handleLeftSoft() {
		return false;
	}

	protected boolean handleKey(int key, int game) {
		return false;
	}

	protected boolean handleRightSoft() {
		return false;
	}

	protected void paintInternal(Graphics g, int w, int h, long now) {

	}

	abstract void loadInternal(Thread thread) throws Exception;

}
//#endif
