package com.github.rzo1;


import com.github.rzo1.controller.HSQLDBFrontierController;
import com.github.rzo1.controller.SleepycatFrontierController;
import com.github.rzo1.controller.URLFrontierController;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

public class Main implements Callable<Integer> {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(Main.class);

    @CommandLine.Option(names = {"-f", "--frontier"}, description = "The frontier impl to use", required = true)
    private Frontier frontier;

    @CommandLine.Option(names = {"-c", "--crawlers"}, description = "Number of crawlers", required = false)
    private int crawlers = 8;

    @CommandLine.Option(names = {"-t", "--time"}, description = "Time in Minutes", required = false)
    private int terminateAfterXMinutes = 60;

    @CommandLine.Option(names = {"-s", "--seeds"}, description = "File with seeds", required = true)
    private Path seedPath;


    public static void main(String... args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    @Override
    public Integer call() throws SchedulerException, IOException {
        logger.info("Starting crawling process...");

        final Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();

        long c;

        String[] seeds = Files.readAllLines(seedPath).toArray(new String[0]);
        switch (this.frontier) {
            case SLEEPYCAT -> c = new SleepycatFrontierController(scheduler, terminateAfterXMinutes).start(crawlers, seeds);
            case HSQLDB -> c = new HSQLDBFrontierController(scheduler, terminateAfterXMinutes).start(crawlers, seeds);
            case URLFRONTIER -> c = new URLFrontierController(scheduler, terminateAfterXMinutes).start(crawlers, seeds);
            default -> throw new RuntimeException("Unknown frontier");
        }

        logger.info("Completed crawl after {} minutes, crawled {} pages.", terminateAfterXMinutes, c);

        scheduler.shutdown();

        return 0;
    }

}
