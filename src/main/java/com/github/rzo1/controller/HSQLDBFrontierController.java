package com.github.rzo1.controller;

import de.hshn.mi.crawler4j.frontier.HSQLDBFrontierConfiguration;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.frontier.FrontierConfiguration;
import org.quartz.Scheduler;

public class HSQLDBFrontierController extends AbstractFrontierController {
    public HSQLDBFrontierController(Scheduler scheduler, int terminateAfterXMinutes) {
        super(scheduler, terminateAfterXMinutes);
    }

    @Override
    protected FrontierConfiguration getFrontierConfiguration(CrawlConfig config) {
        return new HSQLDBFrontierConfiguration(config, 10);
    }
}
