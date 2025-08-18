
public interface KeyboardConstants {

	// режимы клавы
	
	/**
	 * Обычная кверти для общего пользования
	 */
	public static final int KEYBOARD_DEFAULT = 0;
	/**
	 * урл
	 */
	public static final int KEYBOARD_URL = 1;
	/**
	 * Только цифры
	 */
	public static final int KEYBOARD_NUMERIC = 2;
	/**
	 * Цифры с минусом и точкой
	 */
	public static final int KEYBOARD_DECIMAL = 3;
	/**
	 * Номер телефона
	 */
	public static final int KEYBOARD_PHONE_NUMBER = 4;

	// дефолтные настройки стиля
	
	public static final int DEFAULT_BACKGROUND_COLOR = 0x000000;
	public static final int DEFAULT_BUTTON_COLOR = 0x404040;
	public static final int DEFAULT_BUTTON_HOVER_COLOR = 0x606060;
	public static final int DEFAULT_BUTTON_OUTLINE_COLOR = 0x131313;
	public static final int DEFAULT_KEY_TEXT_COLOR = 0xCDCDCD;
	public static final int DEFAULT_KEY_TEXT_SHADOW_COLOR = 0x2E2E2E;
	public static final int DEFAULT_CARET_COLOR = 0xFFFFFF;
	public static final int DEFAULT_TEXT_COLOR = 0xFFFFFF;
	public static final int DEFAULT_TEXT_HINT_COLOR = 0x5D5D5D;
	
	public static final boolean DEFAULT_BUTTONS = true;
	public static final boolean DEFAULT_TEXT_SHADOWS = false;
	public static final boolean DEFAULT_ROUND_BUTTONS = true;
	
	public static final int DEFAULT_BUTTON_PADDING = 2;
	
	/**
	 * папка в ресурсах, где будут храниться раскладки
	 */
	public static final String KEYBOARD_LAYOUTS_DIR = "/keyboard_layouts/";
	public static final String DEFAULT_LAYOUT_PACK = "default_layout_pack.json";
	
	// коды раскладки

	public static final int KEY_EMPTY = 0;
	public static final int KEY_UNDEFINED = -1;
	public static final int KEY_SHIFT = 1;
	public static final int KEY_LANG = 2;
	public static final int KEY_MODE = 3;
	public static final int KEY_BACKSPACE = 8;
	public static final int KEY_RETURN = '\n';
	public static final int KEY_SPACE = ' ';
	

	public static final int KEY_REPEAT_TICKS = 10;
	
	public static final int PHYSICAL_KEYBOARD_NONE = 0;
	public static final int PHYSICAL_KEYBOARD_PHONE_KEYPAD = 1;
	public static final int PHYSICAL_KEYBOARD_QWERTY = 2;
}
