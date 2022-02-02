package com.github.rzo1.crawler;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.url.WebURL;

public class FrontierWebCrawler extends WebCrawler {

    private long localData;

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return true;
    }

    @Override
    public void visit(Page page) {
        localData++;
    }

    public Object getMyLocalData() {
        return localData;
    }

}
