package com.github.rzo1.controller;

import de.hshn.mi.crawler4j.frontier.URLFrontierConfiguration;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.frontier.FrontierConfiguration;
import org.quartz.Scheduler;

import java.nio.file.Path;

public class URLFrontierController extends AbstractFrontierController {

    public URLFrontierController(Scheduler scheduler, Path storagePath, int terminateAfterXMinutes) {
        super(scheduler, storagePath, terminateAfterXMinutes);
    }

    @Override
    protected FrontierConfiguration getFrontierConfiguration(CrawlConfig config) {
        return new URLFrontierConfiguration(config, 10, "localhost", 7071);
    }
}
