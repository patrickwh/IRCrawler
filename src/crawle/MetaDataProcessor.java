package crawle;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MetaDataProcessor {

	public static String HTML_FOLDER = CrawlerController.HTML_DIR;
	public static String IMG_FOLDER = CrawlerController.IMG_DIR;
	public static String PIC_LIST_ID = "product-thumbs";
	public static String CATEGORY_LIST_ID = "product-breadcrumbs";
	public static String ARTTRIBUTES_FOLDER = CrawlerController.ARTTRIBUTES_DIR;
	public static String SPARE_IMG_FOLDER = CrawlerController.SPARE_IMG_DIR;

	public String[] expectCat = { "herr", "dam" };

	private boolean isCategoryExpected(String cat) {
		boolean bool = false;
		for (String str : this.expectCat) {
			if (cat.toLowerCase().equals(str)) {
				bool = true;
				break;
			}
		}
		return bool;
	}

	public MetaDataProcessor() {
		File dir = new File(IMG_FOLDER);
		if (!dir.exists())
			dir.mkdirs();
		dir = new File(SPARE_IMG_FOLDER);
		if (!dir.exists())
			dir.mkdirs();
		dir = new File(ARTTRIBUTES_FOLDER);
		if (!dir.exists())
			dir.mkdirs();
	}

	public void parseAllHtml() {
		File root = new File(HTML_FOLDER);
		File[] files = root.listFiles();
		int processedFileNum = 0;
		for (File f : files) {
			try {
				System.out.println("num: " + processedFileNum + " start");
				processedFileNum++;
				FileReader fr = new FileReader(f);
				BufferedReader br = new BufferedReader(fr);
				// get the first line of the html, it is the parent url of the
				// images
				String title = br.readLine();
				String pre = "article=";
				int start = title.indexOf(pre);
				String productID = title.substring(start + pre.length());
				fr.close();
				br.close();
				Document doc = Jsoup.parse(f, "UTF-8", "http://www.hm.com/se/");
				// retrieve category information about the cloth
				Element categoryList = doc.getElementById(CATEGORY_LIST_ID);
				Elements categories = categoryList.getElementsByTag("a");

				String[] catArray = new String[20];
				int catNum = 0;
				for (Element cat : categories) {
					catArray[catNum] = cat.text();
					catNum++;
				}
				Elements subcat = categoryList.getElementsByTag("strong");
				for (Element cat : subcat) {
					catArray[catNum] = cat.text();
					catNum++;
				}
				// only record products that are in expected categories
				if (!isCategoryExpected(catArray[1]))
					continue;
				Elements nameTag = doc.select("div.product-markers + h1");
				Elements priceTag = doc.select("span#text-price");
				Iterator<Element> iterator = priceTag.iterator();
				for (Element h : nameTag) {
					String productName = h.text();
					String productPrice = iterator.next().text();
					catArray[catNum] = productName.substring(0, (productName.length() - productPrice.length() - 1));
					catNum++;
				}
				String attrFileName = ARTTRIBUTES_FOLDER + "/" + productID + ".txt";
				File attrFile = new File(attrFileName);
				if (!attrFile.exists())
					attrFile.createNewFile();
				FileWriter fw = new FileWriter(attrFile);
				for (int i = 1; i < catNum - 1; i++) {
					fw.write(catArray[i]);
					fw.write("\r\n");
				}
				fw.close();
				// get out all the target images
				Element picList = doc.getElementById(PIC_LIST_ID);
				Elements linkTags = picList.getElementsByTag("a");
				int num = 0;
				for (Element link : linkTags) {
					String protocolHead = "http:";
					String imgUrl = link.attr("href");
					// due to UTF-8 stuff we have to turn empty spaces into %20
					imgUrl = imgUrl.replaceAll(" ", "%20");
					String fullUrl = protocolHead + imgUrl;
					URL url = new URL(fullUrl);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.setRequestMethod("GET");
					connection.setDoOutput(true);
					connection.connect();
					InputStream in = connection.getInputStream();
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					byte[] buf = new byte[1024];
					int n = 0;
					while ((n = in.read(buf)) != -1) {
						out.write(buf, 0, n);
					}
					out.close();
					in.close();
					byte[] imgBtyeArray = out.toByteArray();
					String pathname;
					if (num != 1)
						pathname = SPARE_IMG_FOLDER + "/" + productID + "-" + num + ".jpg";
					else
						pathname = IMG_FOLDER + "/" + productID + ".jpg";
					num++;
					File imgFile = new File(pathname);
					if (imgFile.exists())
						break;
					FileOutputStream fos = new FileOutputStream(imgFile);
					fos.write(imgBtyeArray);
					fos.close();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		MetaDataProcessor dataProcessor = new MetaDataProcessor();
		dataProcessor.parseAllHtml();
		File root = new File(IMG_FOLDER);
		File[] files = root.listFiles();
		int n = 0;
		for (File f : files) {
			String name = f.getName();
			String ext = ".jpg";
			File attrf = new File(ARTTRIBUTES_FOLDER + "/" + name.substring(0, name.length() - ext.length()) + ".txt");
			if (!attrf.exists())
				System.err.println("no such file" + attrf.getName());
			DominantColorsFinder dcf = new DominantColorsFinder(f, attrf);
			dcf.processThePic();
			System.out.println(n + " finished");
			n++;
		}
	}

}
