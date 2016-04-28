package crawle;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class CrawlerController {

	public static String STOREAGE_FOLDER = "./rawData";
	public static String IMG_DIR = "./rawData/imgs";
	public static String SPARE_IMG_DIR = "./rawData/spare_imgs";
	public static String HTML_DIR = "./rawData/htmls";
	public static String ARTTRIBUTES_DIR = "./rawData/attrs";

	public static void main(String[] args) throws Exception {

		// initialize some variables

		// number of crawlers
		int crawlNum = 20;
		String[] startDomains = { "http://www.hm.com/se/", "http://www.hm.com/se/department/LADIES",
				"http://www.hm.com/se/department/MEN" };
		String[] crawlDomains = { "http://www.hm.com/se", "http://lp.hm.com/hmprod?set=key" };
		String[] keyword = { "article" };

		CrawlConfig config = new CrawlConfig();
		// set the data storage folder
		config.setCrawlStorageFolder(STOREAGE_FOLDER);
		// image is binary contents so we have to set this parameter to be true
		config.setIncludeBinaryContentInCrawling(true);

		PageFetcher pageFetcher = new PageFetcher(config);
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);

		CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
		// add some start points for the crawler to start with, then it will
		// follow the links in the fetched pages
		for (String domain : startDomains)
			controller.addSeed(domain);
		IRCrawler.ConfigCrawler(crawlDomains, HTML_DIR, keyword);
		controller.start(IRCrawler.class, crawlNum);
	}

}
