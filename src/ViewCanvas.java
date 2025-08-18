/**
 * Copyright (c) 2021 Fyodor Ryzhov
 * Copyright (c) 2024-2025 Arman Jussupgaliyev
 */
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

// from njtai
public class ViewCanvas extends Canvas implements Runnable, LangConstants {
	protected int zoom = 1;
	protected int x = 0;
	protected int y = 0;

	protected Thread loader;
	protected boolean error;

	private Image toDraw;

	private boolean firstDraw = true;

	private boolean resizing;
	private boolean rotate;
	
	private String peer;
	private String id;


	/**
	 * Creates the view.
	 * 
	 * @param emo  Object with data.
	 * @param prev Previous screen.
	 * @param page Number of page to start.
	 */
	public ViewCanvas(String peer, String id) {
		this.peer = peer;
		this.id = id;
		toDraw = null;
		System.gc();
		loader = new Thread(this);
		loader.start();
		setFullScreenMode(true);
	}

	public final void run() {
		try {
			synchronized (this) {
				error = false;
				zoom = 1;
				x = 0;
				y = 0;
				toDraw = null;
				try {
					repaint();
					resize();
					zoom = 1;
				} catch (Exception e) {
					error = true;
					e.printStackTrace();
				}
				repaint();
			}
		} catch (OutOfMemoryError e) {
			toDraw = null;
			MP.midlet.commandAction(MP.backCmd, this);
			try {
				Thread.sleep(100);
			} catch (Exception ignored) {}
			MP.display(new Alert("Error", "Not enough memory to continue viewing.", null, AlertType.ERROR)); // TODO unlocalized
			return;
		}
	}

	/**
	 * Called when image must change it's zoom.
	 * 
	 * @param size New zoom to apply.
	 */
	protected void resize() {
		if (resizing) return;
		resizing = true;
		try {
			toDraw = null;
			System.gc();
			repaint();
			Image origImg;
			int l = -1;
			byte[] b;
			try {
				b = MP.get(MP.instanceUrl + MP.FILE_URL + "?a&c=" + peer + "&m=" + id + "&p=rprev&s="
						+ (Math.min(getWidth(), getHeight()) * zoom));
				l = b.length;
				origImg = Image.createImage(b, 0, b.length);
				b = null;
				System.gc();
			} catch (RuntimeException e) {
				e.printStackTrace();
				System.out.println("Failed to decode an image in resizing. Size=" + l + "bytes");
				origImg = null;
			}
			resizing = false;
			if (origImg == null) {
				error = true;
				toDraw = null;
				return;
			}
			
			toDraw = origImg;
		} catch (Throwable e) {
			e.printStackTrace();
			resizing = false;
			error = true;
			toDraw = null;
			return;
		}
	}
	
	protected void paint(Graphics g) {
		try {
			Font f = MP.smallPlainFont;

			int w = getWidth(), h = getHeight();
			g.setFont(f);
			int fh = f.getHeight();
			if (toDraw == null) {
				if (firstDraw) {
					firstDraw = false;
					g.setGrayScale(0);
					g.fillRect(0, 0, w, h);
				}
				
				String info;
				if (error) {
					g.setGrayScale(0);
					g.fillRect(0, 0, w, h);
					info = MP.L[FailedToLoadImage];
				} else {
					info = MP.L[Loading];
				}
				g.setGrayScale(0);
				int tw = f.stringWidth(info);
				g.fillRect(w / 2 - tw / 2, h / 2, tw,  fh);
				g.setGrayScale(255);
				g.drawString(info, w / 2, h / 2, Graphics.HCENTER | Graphics.TOP);
				if (hasPointerEvents()) {
					// grads
					fillGrad(g, w * 3 / 4, h - 50, w / 4, 51, 0, 0x222222);
					// lines
					g.setGrayScale(255);
					g.drawLine(w * 3 / 4, h - 50, w, h - 50);
					g.drawLine(w * 3 / 4, h - 50, w * 3 / 4, h);
					// captions
					g.setGrayScale(255);
					g.drawString(touchCaps[3], w * 7 / 8, h - 25 - fh / 2, Graphics.TOP | Graphics.HCENTER);
				}
			} else {
				// bg fill
				g.setGrayScale(0);
				g.fillRect(0, 0, w, h);
				
				// limit offset
				int hw = (toDraw.getWidth() - w) / 2;
				int hh = (toDraw.getHeight() - h) / 2;
				if (hw < 0) hw = 0;
				if (hh < 0) hh = 0;
				
				if (x < -hw) x = -hw;
				if (x > hw) x = hw;
				if (y < -hh - 50) y = -hh - 50;
				if (y > hh + 50) y = hh + 50;
				
				if (zoom != 1) {
					g.drawImage(toDraw, (int) x + w / 2, (int) y + h / 2,
							Graphics.HCENTER | Graphics.VCENTER);
				} else {
					g.drawImage(toDraw, (w - toDraw.getWidth()) / 2, (h - toDraw.getHeight()) / 2,
							0);
				}
			}
			// touch captions
			if (hasPointerEvents() && touchCtrlShown) {

				// captions

				fillGrad(g, w * 3 / 4, h - 50, w / 4, 51, 0,
						touchHoldPos == 7 ? 0x357EDE : 0x222222);
				g.setGrayScale(255);
				g.drawString(touchCaps[3], w * (1 + 3 * 2) / 8,
						h - 25 - fh / 2, Graphics.TOP | Graphics.HCENTER);
				g.setGrayScale(255);
				g.drawLine(w * 3 / 4, h - 50, w, h - 50);
				g.drawLine(w * 3 / 4, h - 50, w * 3 / 4, h);

				for (int i = 0; i < 3; i++) {
					fillGrad(g, w * i / 3, 0, w / 3 + 1, 50, touchHoldPos == (i + 1) ? 0x357EDE : 0x222222,
							0);
					g.setGrayScale(255);
					g.drawString(touchCaps[i], w * (1 + i * 2) / 6, 25 - fh / 2, Graphics.TOP | Graphics.HCENTER);
				}
				// bottom hor line
				g.setGrayScale(255);
				g.drawLine(0, 50, w, 50);
				// vert lines between btns
				g.drawLine(w / 3, 0, w / 3, 50);
				g.drawLine(w * 2 / 3, 0, w * 2 / 3, 50);
			}
			
			// hud
			String zoomN = Integer.toString((int) zoom);
			if (zoomN.length() > 3)
				zoomN = zoomN.substring(0, 3);
			zoomN = "x" + zoomN;

			// zoom
			g.setColor(0);
			g.fillRect(w - f.stringWidth(zoomN), 0, f.stringWidth(zoomN), fh);
			g.setColor(-1);
			g.drawString(zoomN, w - f.stringWidth(zoomN), 0, 0);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				MP.display(new Alert("Repaint error", e.toString(), null, AlertType.ERROR));
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	String[] touchCaps = new String[] { "x1", "x2", "x3", MP.L[Back] };

	boolean touchCtrlShown = true;

	protected final void keyPressed(int k) {
		k = qwertyToNum(k);
		if (k == -7 || k == -22 || k == 22) {
			try {
				if (loader != null && loader.isAlive()) {
					loader.interrupt();
				}
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			MP.midlet.commandAction(MP.backCmd, this);
			toDraw = null;
			return;
		}
//		if (!canDraw()) {
//			repaint();
//			return;
//		}

		if (!resizing) {
			if (k == KEY_NUM9) {
				rotate = !rotate;
				MP.midlet.start(MP.RUN_ZOOM_VIEW, this);
				repaint();
				return;
			}
			// zooming via *0#
			if (k == KEY_STAR) {
				zoom = 1;
				MP.midlet.start(MP.RUN_ZOOM_VIEW, this);
			}
			if (k == KEY_NUM0) {
				zoom = 2;
				MP.midlet.start(MP.RUN_ZOOM_VIEW, this);
			}
			if (k == KEY_POUND) {
				zoom = 3;
				MP.midlet.start(MP.RUN_ZOOM_VIEW, this);
			}
	
			// zoom is active
			if (k == -5 || getGameAction(k) == FIRE) {
				if (zoom == 1) {
					zoom = 2;
					x = 0;
					y = 0;
				} else if (++zoom > 3) zoom = 1;
				MP.midlet.start(MP.RUN_ZOOM_VIEW, this);
			} else if (zoom != 1) {
				if (k == -1 || k == KEY_NUM2 || k == 'w') {
					// up
					y += getHeight() / 4;
				} else if (k == -2 || k == KEY_NUM8 || k == 's') {
					y -= getHeight() / 4;
				} else if (k == -3 || k == KEY_NUM4 || k == 'a') {
					x += getWidth() / 4;
				} else if (k == -4 || k == KEY_NUM6 || k == 'd') {
					x -= getWidth() / 4;
				}
			}
		}

		repaint();
	}

	protected final void keyRepeated(int k) {
		k = qwertyToNum(k);
		if (toDraw == null) {
			repaint();
			return;
		}
		// zoom is active
		if (zoom != 1) {
			if (k == -1 || k == KEY_NUM2 || k == 'w') {
				// up
				y += getHeight() / 4;
			} else if (k == -2 || k == KEY_NUM8 || k == 's') {
				y -= getHeight() / 4;
			} else if (k == -3 || k == KEY_NUM4 || k == 'a') {
				x += getWidth() / 4;
			} else if (k == -4 || k == KEY_NUM6 || k == 'd') {
				x -= getWidth() / 4;
			}
		}

		repaint();
	}

	/**
	 * <ul>
	 * <li>0 - nothing
	 * <li>1 - zoom x1
	 * <li>2 - zoom x2
	 * <li>3 - zoom x3
	 * <li>4 - prev
	 * <li>5 - goto
	 * <li>6 - next
	 * <li>7 - return
	 * <li>8 - zoom slider
	 * </ul>
	 */
	int touchHoldPos = 0;
	int lx, ly;
	int sx, sy;

	protected final void pointerPressed(int tx, int ty) {
		if (toDraw == null && ty > getHeight() - 50 && tx > getWidth() * 2 / 3) {
			keyPressed(-7);
			return;
		}
		touchHoldPos = 0;
		lx = (sx = tx);
		ly = (sy = ty);
		if (!touchCtrlShown)
			return;
		if (ty < 50) {
			int b;
			if (tx < getWidth() / 3) {
				b = 1;
			} else if (tx < getWidth() * 2 / 3) {
				b = 2;
			} else {
				b = 3;
			}
			touchHoldPos = b;
		} else if (ty > getHeight() - 50) {
			int b;
			if (tx < getWidth() / 4) {
				b = 4;
			} else if (tx < getWidth() * 2 / 4) {
				b = 5;
			} else if (tx < getWidth() * 3 / 4) {
				b = 6;
			} else {
				b = 7;
			}
			touchHoldPos = b;
		}
		repaint();
	}

	protected final void pointerDragged(int tx, int ty) {
		if (touchHoldPos == 8) {
			repaint();
			return;
		}
		if (touchHoldPos != 0)
			return;
		x += (tx - lx);
		y += (ty - ly);
		lx = tx;
		ly = ty;
		repaint();
	}

	protected final void pointerReleased(int tx, int ty) {
		if (!touchCtrlShown || touchHoldPos == 0) {
			if (Math.abs(sx - tx) < 10 && Math.abs(sy - ty) < 10) {
				touchCtrlShown = !touchCtrlShown;
			}
		}
		if (touchHoldPos == 8) {
			touchHoldPos = 0;
			repaint();
			return;
		}
		int zone = 0;
		if (ty < 50) {
			int b;
			if (tx < getWidth() / 3) {
				b = 1;
			} else if (tx < getWidth() * 2 / 3) {
				b = 2;
			} else {
				b = 3;
			}
			zone = b;
		} else if (ty > getHeight() - 50) {
			int b;
			if (tx < getWidth() / 4) {
				b = 4;
			} else if (tx < getWidth() * 2 / 4) {
				b = 5;
			} else if (tx < getWidth() * 3 / 4) {
				b = 6;
			} else {
				b = 7;
			}
			zone = b;
		}
		if (zone == touchHoldPos) {
			if (zone >= 1 && zone <= 3 && !resizing) {
				zoom = zone;
				MP.midlet.start(MP.RUN_ZOOM_VIEW, this);
			} else if (zone == 7) {
				keyPressed(-7);
			}
		}
		touchHoldPos = 0;
		repaint();
	}

	/**
	 * Fills an opaque gradient on the canvas.
	 * 
	 * @param g  Graphics object to draw in.
	 * @param x  X.
	 * @param y  Y.
	 * @param w  Width.
	 * @param h  Height.
	 * @param c1 Top color.
	 * @param c2 Bottom color.
	 */
	public static void fillGrad(Graphics g, int x, int y, int w, int h, int c1, int c2) {
		for (int i = 0; i < h; i++) {
//			g.setColor(blend(c2, c1, i * 255 / h));
			int v = (i * 255 / h) & 0xFF;
			final int c2_RB = c2 & 0x00FF00FF;
			final int c2_AG_org = c2 & 0xFF00FF00;
			g.setColor((((c2_AG_org) + ((((c1 >>> 8) & 0x00FF00FF) - ((c2_AG_org) >>> 8)) * v)) & 0xFF00FF00)
					| ((c2_RB + ((((c1 & 0x00FF00FF) - c2_RB) * v) >> 8)) & 0x00FF00FF));
			g.drawLine(x, y + i, x + w, y + i);
		}
	}

	/**
	 * Converts qwerty key code to corresponding 12k key code.
	 * 
	 * @param k Original key code.
	 * @return Converted key code.
	 */
	public static int qwertyToNum(int k) {
		char c = (char) k;
		switch (c) {
		case 'r':
		case 'R':
		case 'к':
			return Canvas.KEY_NUM1;

		case 't':
		case 'T':
		case 'е':
			return Canvas.KEY_NUM2;

		case 'y':
		case 'Y':
		case 'н':
			return Canvas.KEY_NUM3;

		case 'f':
		case 'F':
		case 'а':
			return Canvas.KEY_NUM4;

		case 'g':
		case 'G':
		case 'п':
			return Canvas.KEY_NUM5;

		case 'h':
		case 'H':
		case 'р':
			return Canvas.KEY_NUM6;

		case 'v':
		case 'V':
		case 'м':
			return Canvas.KEY_NUM7;

		case 'b':
		case 'B':
		case 'и':
			return Canvas.KEY_NUM8;

		case 'n':
		case 'N':
		case 'т':
			return Canvas.KEY_NUM9;

		case 'm':
		case 'M':
		case 'ь':
			return Canvas.KEY_NUM0;
		
		case 'o':
		case 'O':
		case 'p':
		case 'P':
			return -7;

		default:
			return k;
		}
	}
}
