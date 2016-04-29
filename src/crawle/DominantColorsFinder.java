package crawle;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class DominantColorsFinder {

	File img;
	File output;
	HashMap<Integer, Integer> count = new HashMap<Integer, Integer>();
	HashMap<Integer, Double> ratio = new HashMap<Integer, Double>();
	int totalNum = 0;
	int maxColorNum = 10;
	ArrayList<DColor> dominantList = new ArrayList<DColor>();
	int granularity = 8;
	int halfGranularity = granularity / 2;

	final public static int[] BASIC_COLOR = { 0xFF, 0xCC, 0xCC, 0xFF, 0xE5, 0xCC, 0xFF, 0xFF, 0xCC, 0xE5, 0xFF, 0xCC,
			0xCC, 0xFF, 0xCC, 0xCC, 0xFF, 0xE5, 0xCC, 0xFF, 0xFF, 0xCC, 0xE5, 0xFF, 0xCC, 0xCC, 0xFF, 0xE5, 0xCC, 0xFF,
			0xFF, 0xCC, 0xFF, 0xFF, 0xCC, 0xE5, 0xFF, 0xFF, 0xFF, 0xFF, 0x00, 0x00, 0xFF, 0x80, 0x00, 0xFF, 0xFF, 0x00,
			0x80, 0xFF, 0x00, 0x00, 0xFF, 0x00, 0x00, 0xFF, 0x80, 0x00, 0xFF, 0xFF, 0x00, 0x80, 0xFF, 0x00, 0x00, 0xFF,
			0x7F, 0x00, 0xFF, 0xFF, 0x00, 0xFF, 0xFF, 0x00, 0x7F, 0xC0, 0xC0, 0xC0, 0x99, 0x00, 0x00, 0x99, 0x4C, 0x00,
			0x99, 0x99, 0x00, 0x4C, 0x99, 0x00, 0x00, 0x99, 0x00, 0x00, 0x99, 0x4C, 0x00, 0x99, 0x99, 0x00, 0x4C, 0x99,
			0x00, 0x00, 0x99, 0x4C, 0x00, 0x99, 0x99, 0x00, 0x99, 0x99, 0x00, 0x4C, 0x40, 0x40, 0x40, };

	public DominantColorsFinder(File img_file, File out_file) {
		this.img = img_file;
		this.output = out_file;
	}

	public class DColor {
		int rgbValue;
		double ratio;

		public DColor(int rgb, double rat) {
			this.rgbValue = rgb;
			this.ratio = rat;
		}
	}

	private int getColorIntValue(int[] color) {
		int rgb = 0;
		int r = (color[0] << 16) & 0xFF0000;
		int g = (color[1] << 8) & 0xFF00;
		int b = color[2] & 0xFF;
		rgb = r + g + b;
		return rgb;
	}

	private int[] getRGBValue(int rgb) {
		int[] color = new int[3];
		color[0] = (rgb >> 16) & 0xFF; // red
		color[1] = (rgb >> 8) & 0xFF; // green
		color[2] = rgb & 0xFF; // blue
		return color;
	}

	public double[] getCIEXYZValue(int[] orgb) {
		double[] rgb = new double[3];
		double[] xyz = { 0.0, 0.0, 0.0 };
		// System.out.println(" rgb : " + orgb[0] + " " + orgb[1] + " " +
		// orgb[2]);
		for (int i = 0; i < 3; i++) {
			rgb[i] = (double) orgb[i] / 255.0;
			if (rgb[i] <= 0.04045)
				rgb[i] /= 12;
			else
				rgb[i] = Math.pow((rgb[i] + 0.055) / 1.055, 2.4);
		}
		xyz[0] = (0.4124564 * rgb[0] + 0.3575761 * rgb[1] + 0.1804375 * rgb[2]) * 100;
		xyz[1] = (0.2126729 * rgb[0] + 0.7151522 * rgb[1] + 0.0721750 * rgb[2]) * 100;
		xyz[2] = (0.0193339 * rgb[0] + 0.1191920 * rgb[1] + 0.9503041 * rgb[2]) * 100;
		// System.out.println(" xyz : " + xyz[0] + " " + xyz[1] + " " + xyz[2]);
		return xyz;
	}

	private double fFunction(double t) {
		double result = 0;

		double th = 8.85645E-3;
		if (t > th)
			result = Math.cbrt(t);
		else {
			result = 7.78704 * t + 0.13793;
		}
		return result;
	}

	private double[] getCIELABValue(double[] xyz) {
		double[] lab = new double[3];
		double xn = 95.047;
		double yn = 100.000;
		double zn = 108.883;
		lab[0] = 116.0 * fFunction(xyz[1] / yn) - 16.0;
		lab[1] = 500.0 * (fFunction(xyz[0] / xn) - fFunction(xyz[1] / yn));
		lab[2] = 200.0 * (fFunction(xyz[1] / yn) - fFunction(xyz[2] / zn));
		// System.out.println(" lab : " + lab[0] + " " + lab[1] + " " + lab[2]);
		return lab;
	}

	private double getCIE76Distance(int[] c1, int[] c2) {
		double distance = 0;
		double[] xyz1 = getCIEXYZValue(c1);
		double[] lab1 = getCIELABValue(xyz1);
		double[] xyz2 = getCIEXYZValue(c2);
		double[] lab2 = getCIELABValue(xyz2);
		distance = Math
				.sqrt(Math.pow(lab1[0] - lab2[0], 2) + Math.pow(lab1[1] - lab2[1], 2) + Math.pow(lab1[2] - lab2[2], 2));
		// System.out.println("dis " + distance);
		return distance;
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
		if (isBackGroundColor(aColor))
			return null;
		return aColor;
	}

	private int approximateToBasicColor(int rgb) {
		int appr = -1;
		int[] tmp = new int[3];
		int[] c = getRGBValue(rgb);
		// System.out.println(" #RGB : " + c[0] + " " + c[1] + " " + c[2]);
		double minDistance = Double.MAX_VALUE;
		int minIndex = -1;
		for (int i = 0; i < BASIC_COLOR.length; i += 3) {
			tmp[0] = BASIC_COLOR[i];
			tmp[1] = BASIC_COLOR[i + 1];
			tmp[2] = BASIC_COLOR[i + 2];
			double dis = getCIE76Distance(c, tmp);
			// System.out.println(i + " dis : " + dis + " " + tmp[0] + " " +
			// tmp[1] + " " + tmp[2]);
			if (dis < minDistance) {
				minDistance = dis;
				minIndex = i;
			}
		}
		// System.out.println("********************* " + minIndex + " " +
		// minDistance);
		tmp[0] = BASIC_COLOR[minIndex];
		tmp[1] = BASIC_COLOR[minIndex + 1];
		tmp[2] = BASIC_COLOR[minIndex + 2];
		appr = getColorIntValue(tmp);
		return appr;
	}

	private String toHexString(int rgb) {
		int[] color = getRGBValue(rgb);
		String r = Integer.toHexString(color[0]);
		if (r.length() == 1)
			r = "0" + r;
		String g = Integer.toHexString(color[1]);
		if (g.length() == 1)
			g = "0" + g;
		String b = Integer.toHexString(color[2]);
		if (b.length() == 1)
			b = "0" + b;
		return "#" + r + g + b;
	}

	public void getDominantColors() {
		Iterator<Integer> itr = count.keySet().iterator();
		double maxRatio = Double.MIN_VALUE;
		while (itr.hasNext()) {
			int key = itr.next();
			double rat = (double) count.get(key) / totalNum;
			int[] cc = getRGBValue(key);
			// System.out.println("% " + key + " " + cc[0] + " " + cc[1] + " " +
			// cc[2] + " " + count.get(key));
			ratio.put(key, rat);
			if (rat > maxRatio)
				maxRatio = rat;
		}
		itr = ratio.keySet().iterator();

		ArrayList<DColor> list = new ArrayList<DColor>();
		while (itr.hasNext()) {
			int key = itr.next();
			double newValue = ratio.get(key) / maxRatio;
			ratio.replace(key, newValue);
			DColor dc = new DColor(key, newValue);
			list.add(dc);
		}
		Comparator<DColor> comparator = new Comparator<DColor>() {

			@Override
			public int compare(DColor c1, DColor c2) {
				if (c1.ratio > c2.ratio)
					return -1;
				else if (c1.ratio < c2.ratio)
					return 1;
				else
					return 0;
			}
		};
		Collections.sort(list, comparator);

		double threshold = 0.5;
		for (int i = 0; i < list.size(); i++) {
			DColor dc = list.get(i);
			if (dc.ratio > threshold) {
				// System.out.println(" #ratio " + dc.ratio);
				DColor dcolor = new DColor(dc.rgbValue, dc.ratio);
				dominantList.add(dcolor);
			}
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

			FileWriter fw = new FileWriter(output, true);

			BufferedImage image = imageReader.read(0);
			int height = image.getHeight();
			int width = image.getWidth();
			System.out.println(img.getName());
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					int rgb = image.getRGB(i, j);
					// background color white
					if (rgb == -1)
						continue;
					int[] cl = getApproximateColor(rgb);
					// also is the background
					if (cl != null) {
						int apprRgb = approximateToBasicColor(rgb);
						if (!count.containsKey(apprRgb))
							count.put(apprRgb, 1);
						else
							count.replace(apprRgb, count.get(apprRgb) + 1);
						totalNum++;
					}
				}
			}
			this.getDominantColors();
			for (int i = 0; i < dominantList.size(); i++) {
				// int[] c = getRGBValue(dominantList.get(i).rgbValue);
				System.out.println((dominantList.get(i).rgbValue + " " + dominantList.get(i).ratio + " "
						+ Integer.toHexString(dominantList.get(i).rgbValue)));
				fw.write(toHexString(dominantList.get(i).rgbValue) + "\r\n");
			}
			fw.close();
		} catch (Exception e) {
			System.out.println("DominantColorsFinder.processThePic(): fail to create input stream of the image");
			e.printStackTrace();
		}
	}

	public static void main(String args[]) throws Exception {
		// Document doc = Jsoup.parse(new File("./rawData/RGBcolor.html"),
		// "UTF-8", "http://www.rapidtables.com/");
		// Elements tabTag = doc.getElementsByClass("table1");
		// Element tabel = tabTag.first();
		// Elements rows = tabel.getElementsByTag("tr");
		// for (Element row : rows) {
		// Elements data = row.getElementsByTag("td");
		// for (Element e : data) {
		// String backColor = e.attr("style");
		// String color = backColor.substring(12);
		// String r = color.substring(0, 2);
		// String g = color.substring(2, 4);
		// String b = color.substring(4, 6);
		// System.out.print("0x" + r + "," + "0x" + g + "," + "0x" + b + ",");
		// }
		// System.out.println();
		// }
		String root = "./rawData/htmls";
		File htmls = new File(root);
		File[] list = htmls.listFiles();
		for (File f : list) {
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String ostr = br.readLine();
			String s = "article=";
			String str = ostr.substring(ostr.indexOf(s) + s.length());
			System.out.println(" " + str);
			File nf = new File("./rawData/htmlf/" + str + ".html");
			if (!nf.exists())
				nf.createNewFile();
			FileWriter fWriter = new FileWriter(nf);
			fWriter.write(ostr + "\r\n");
			while ((str = br.readLine()) != null) {
				fWriter.write(str + "\r\n");
			}
			fWriter.close();
			br.close();
			fr.close();
		}

	}
}
