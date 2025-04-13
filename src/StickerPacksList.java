
public class StickerPacksList extends MPList {

	public StickerPacksList(String title) {
		super(title);
		// TODO Auto-generated constructor stub
	}

	void loadInternal(Thread thread) throws Exception {
		// TODO Auto-generated method stub

	}

	void select(int i) {
		// TODO Auto-generated method stub
		MP.openLoad(new StickerPackForm(null));
	}

}
