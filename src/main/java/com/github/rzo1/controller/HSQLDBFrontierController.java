package com.github.rzo1.controller;

import de.hshn.mi.crawler4j.frontier.HSQLDBFrontierConfiguration;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.frontier.FrontierConfiguration;
import org.quartz.Scheduler;

import java.nio.file.Path;

public class HSQLDBFrontierController extends AbstractFrontierController {
    public HSQLDBFrontierController(Scheduler scheduler, Path storagePath, int terminateAfterXMinutes) {
        super(scheduler, storagePath, terminateAfterXMinutes);
    }

    @Override
    protected FrontierConfiguration getFrontierConfiguration(CrawlConfig config) {
        return new HSQLDBFrontierConfiguration(config, 10);
    }
}
