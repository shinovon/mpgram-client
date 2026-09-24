import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import cc.nnproject.json.JSONStream;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;

public class Main {
	public static void main(String[] args) {
		try {
			JSONArray list = JSONStream.getArray(new FileInputStream("./emoji.json"));
			BufferedImage sheet = ImageIO.read(new File("./sheet_apple_16.png"));
			int l = list.size();
			for (int i = 0; i < l; ++i) {
				JSONObject e = list.getObject(i);
				BufferedImage img = sheet.getSubimage(e.getInt("sheet_x") * 18 + 1,
						e.getInt("sheet_y") * 18 + 1,
						16, 16);
				StringBuffer sb = new StringBuffer();
				String[] c = e.getString("unified").split("-");
				for (String u: c) {
					sb.append(Character.toChars(Integer.parseInt(u, 16)));
				}
				byte[] b = sb.toString().getBytes("UTF-8");
				sb.setLength(0);
				sb.append("./out/");
				int h = b.length;
				for (int k = 0; k < h; k++) {
					sb.append(Integer.toHexString(b[k] >> 4 & 0xf));
					sb.append(Integer.toHexString(b[k] & 0xf));
				};
				sb.append(".png");
				ImageIO.write(img, "png", new File(sb.toString()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}