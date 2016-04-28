package crawle;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DominantColorsFinder {

	File img;
	File output;
	HashMap<Integer, Integer> count = new HashMap<Integer, Integer>();
	HashMap<Integer, Double> ratio = new HashMap<Integer, Double>();
	int totalNum = 0;
	int maxColorNum = 10;
	int dominantNum = 0;
	int[] dominant = new int[maxColorNum];
	int granularity = 8;
	int halfGranularity = granularity / 2;

	public class DColor {
		int rgbValue;
		double ratio;

		public DColor(int rgb, double rat) {
			this.rgbValue = rgb;
			this.ratio = rat;
		}
	}

	public boolean isBackGroundColor(int[] color) {
		boolean bool = true;
		for (int c : color) {
			if (c < 247)
				bool = false;
		}
		return bool;
	}

	public int[] getApproximateColor(int rgb) {
		int[] color = new int[3];
		int[] aColor = new int[3];
		color = getRGBValue(rgb);
		for (int i = 0; i < 3; i++) {
			int level = (color[i] + halfGranularity) / granularity;
			aColor[i] = level * granularity - 1;
			if (aColor[i] > 255)
				aColor[i] = 255;
		}
		// System.out.println(color[0] + " " + color[1] + " " + color[2] + " " +
		// Integer.toHexString(rgb));
		// System.out.println("a " + aColor[0] + " " + aColor[1] + " " +
		// aColor[2] + " "
		// + Integer.toHexString(getColorIntValue(aColor)));
		if (isBackGroundColor(aColor))
			return null;
		return aColor;
	}

	public void getDominantColors() {
		Iterator<Integer> itr = count.keySet().iterator();
		double maxRatio = Double.MIN_VALUE;
		while (itr.hasNext()) {
			int key = itr.next();
			double rat = (double) count.get(key) / totalNum;
			ratio.put(key, rat);
			if (rat > maxRatio)
				maxRatio = rat;
		}
		itr = ratio.keySet().iterator();
		double threshold = 0.7;
		ArrayList<DColor> list = new ArrayList<DColor>();
		while (itr.hasNext()) {
			int key = itr.next();
			double newValue = ratio.get(key) / maxRatio;
			ratio.replace(key, newValue);
			DColor dc = new DColor(key, newValue);
			list.add(dc);
		}
		Collections.sort(list, new Comparator<DColor>() {

			@Override
			public int compare(DColor c1, DColor c2) {
				if (c1.ratio > c2.ratio)
					return -1;
				else if (c1.ratio < c2.ratio)
					return 1;
				else
					return 0;
			}
		});

		for (int i = 0; i < list.size(); i++) {
			DColor dc = list.get(i);
			int[] c = getRGBValue(dc.rgbValue);

			if (dc.ratio > threshold) {
				dominant[dominantNum] = dc.rgbValue;
				dominantNum++;
				// at most 10 dominant colors
				if (dominantNum >= maxColorNum)
					break;
			}
		}
	}

	public int getColorIntValue(int[] color) {
		int rgb = 0;
		int r = (color[0] << 16) & 0xFF0000;
		int g = (color[1] << 8) & 0xFF00;
		int b = color[2] & 0xFF;
		rgb = r + g + b;
		return rgb;
	}

	public DominantColorsFinder(File img_file, File out_file) {
		this.img = img_file;
		this.output = out_file;
	}

	private int[] getRGBValue(int rgb) {
		int[] color = new int[3];
		color[0] = (rgb >> 16) & 0xFF; // red
		color[1] = (rgb >> 8) & 0xFF; // green
		color[2] = rgb & 0xFF; // blue
		return color;
	}

	public void writeAnArray(int[] array, String divider, FileWriter fw) {
		try {
			fw.write("[");
			for (int t : array) {
				fw.write(t + divider);
			}
			fw.write("]");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void processThePic() {
		if (!this.output.exists()) {
			System.out.println("DominantColorsFinder.processThePic(): output file does not exist");
			return;
		}
		if (!this.img.exists()) {
			System.out.println("DominantColorsFinder.processThePic(): image file does not exist");
			return;
		}
		try {
			ImageInputStream imageInputStream = ImageIO.createImageInputStream(img);
			Iterator<ImageReader> iterator = ImageIO.getImageReaders(imageInputStream);
			if (!iterator.hasNext())
				System.out.println("DominantColorsFinder.processThePic(): iterator fails");
			ImageReader imageReader = (ImageReader) iterator.next();
			imageReader.setInput(imageInputStream);

			FileWriter fw = new FileWriter(output);

			BufferedImage image = imageReader.read(0);
			int height = image.getHeight();
			int width = image.getWidth();
			System.out.println(img.getName());
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					int rgb = image.getRGB(i, j);
					fw.write(Integer.toHexString(rgb) + " ");
					// background color white
					if (rgb == -1)
						continue;
					int[] cl = getApproximateColor(rgb);
					// also is the background
					if (cl != null) {
						int apprRgb = getColorIntValue(cl);
						if (!count.containsKey(apprRgb))
							count.put(apprRgb, 0);
						else
							count.replace(apprRgb, count.get(apprRgb) + 1);
						totalNum++;
					}
				}
				fw.write("\r\n");
			}
			this.getDominantColors();
			for (int i = 0; i < dominantNum; i++) {
				int[] c = getRGBValue(dominant[i]);
				System.out.println(
						dominant[i] + " " + c[0] + " " + c[1] + " " + c[2] + " " + Integer.toHexString(dominant[i]));
			}
			fw.close();
		} catch (Exception e) {
			System.out.println("DominantColorsFinder.processThePic(): fail to create input stream of the image");
			e.printStackTrace();
		}
	}

	public static void main(String args[]) throws Exception {
		Document doc = Jsoup.parse(new File("./rawData/RGBcolor.html"), "UTF-8", "http://www.rapidtables.com/");
		Elements tabTag = doc.getElementsByClass("table1");
		Element tabel = tabTag.first();
		Elements rows = tabel.getElementsByTag("tr");
		for (Element row : rows) {
			Elements data = row.getElementsByTag("td");
			for (Element e : data) {
				String backColor = e.attr("style");
				String color = backColor.substring(12);
				String r = color.substring(0, 2);
				String g = color.substring(2, 4);
				String b = color.substring(4, 6);
				System.out.print("0x" + r + "," + "0x" + g + "," + "0x" + b + ",");
			}
			System.out.println();
		}
	}
}
