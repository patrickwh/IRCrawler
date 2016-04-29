package crawle;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import com.google.common.io.Files;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

public class IRCrawler extends WebCrawler {

	private static final Pattern filters = Pattern.compile(
			".*(\\.(css|js|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");

	private static final Pattern imgPatterns = Pattern.compile(".*(\\.(bmp|gif|jpe?g|png|tiff?))$");
	private static String[] DOMAINS;
	private static String HTML_FOLDER;
	private static String[] KEY_WORD;
	public static int PAGE_NUM = 0;

	public static void ConfigCrawler(String[] domains, String imgDir, String[] keyword) {
		DOMAINS = domains;
		HTML_FOLDER = imgDir;
		KEY_WORD = keyword;
	}

	/**
	 * whether the referred pages should be visit or not
	 */

	private boolean isStartInDomains(String href) {
		boolean bool = false;
		for (String domain : DOMAINS) {
			if (href.startsWith(domain)) {
				bool = true;
				break;
			}
		}
		return bool;
	}

	private boolean containsKeyWord(String url) {
		for (String kw : KEY_WORD) {
			CharSequence kwcs = kw;
			if (url.contains(kwcs))
				return true;
		}
		return false;
	}

	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		String href = url.getURL().toLowerCase();
		if (imgPatterns.matcher(href).matches())
			return true;
		if (filters.matcher(href).matches())
			return false;
		if (isStartInDomains(href))
			return true;
		return false;
	}

	/**
	 * This function is called when a page is fetched and ready to be processed
	 * by your program.
	 */
	@Override
	public void visit(Page page) {
		String url = page.getWebURL().getURL();
		if (!isStartInDomains(url) || !containsKeyWord(url))
			return;
		System.out.println(PAGE_NUM + "  URL: " + url);
		PAGE_NUM++;

		if (page.getParseData() instanceof HtmlParseData) {
			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
			String html = htmlParseData.getHtml();

			String extension = ".html";
			String s = "article=";
			String str = url.substring(url.indexOf(s) + s.length());
			String id = str + extension;

			String fileName = HTML_FOLDER + "/" + id;
			File root = new File(HTML_FOLDER);
			if (!(root.exists()))
				root.mkdirs();
			String data = url + "\r\n" + html;
			try {
				File file = new File(fileName);
				if (file.exists())
					return;
				Files.write(data.getBytes(), file);
				logger.info("Stored: {}", url);
			} catch (IOException iox) {
				System.err.println("Failed to write file: " + fileName);
				logger.error("Failed to write file: " + fileName, iox);
			}

		}

	}
}
