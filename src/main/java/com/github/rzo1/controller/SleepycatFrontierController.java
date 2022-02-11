package com.github.rzo1.controller;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.frontier.FrontierConfiguration;
import edu.uci.ics.crawler4j.frontier.SleepycatFrontierConfiguration;
import org.quartz.Scheduler;

import java.nio.file.Path;

public class SleepycatFrontierController extends AbstractFrontierController {

    public SleepycatFrontierController(Scheduler scheduler, Path storagePath, int terminateAfterXMinutes) {
        super(scheduler, storagePath, terminateAfterXMinutes);
    }

    @Override
    protected FrontierConfiguration getFrontierConfiguration(CrawlConfig config) throws Exception {
        return new SleepycatFrontierConfiguration(config);
    }
}
