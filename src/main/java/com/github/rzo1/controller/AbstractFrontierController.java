package com.github.rzo1.controller;

import com.github.rzo1.crawler.FrontierWebCrawler;
import crawlercommons.filters.basic.BasicURLNormalizer;
import de.hhn.mi.ConsoleProgressBar;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.frontier.Frontier;
import edu.uci.ics.crawler4j.frontier.FrontierConfiguration;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public abstract class AbstractFrontierController {

    private static final String CONTROLLER_KEY = "controller";
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(AbstractFrontierController.class);
    private static boolean skipFirst = true;
    private final Scheduler scheduler;
    private final int terminateAfterXMinutes;
    private final Path storagePath;

    public AbstractFrontierController(Scheduler scheduler, Path storagePath, int terminateAfterXMinutes) {
        this.scheduler = scheduler;
        this.terminateAfterXMinutes = terminateAfterXMinutes;
        this.storagePath = storagePath;
    }

    private CrawlConfig configure() {
        final CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(storagePath.toAbsolutePath().toString());
        config.setIncludeHttpsPages(true);
        config.setPolitenessDelay(800);
        config.setMaxDepthOfCrawling(3); // we only aim for a max depth of 3
        config.setMaxPagesToFetch(-1);
        config.setIncludeBinaryContentInCrawling(false);
        config.setResumableCrawling(true);
        config.setHaltOnError(false);
        return config;

    }

    protected CrawlController init() throws Exception {
        final CrawlConfig config = configure();
        final BasicURLNormalizer normalizer = BasicURLNormalizer.newBuilder().idnNormalization(BasicURLNormalizer.IdnNormalization.NONE).build();
        final PageFetcher pageFetcher = new PageFetcher(config, normalizer);
        final RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        robotstxtConfig.setEnabled(true);
        robotstxtConfig.setCacheSize(10000); // we have enough memory available, so we cache 10k hosts
        robotstxtConfig.setSkipCheckForSeeds(true); // we skip the robots checks for adding seeds (will be checked later on demand)
        final FrontierConfiguration frontierConfiguration = getFrontierConfiguration(config);
        final RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher, frontierConfiguration.getWebURLFactory());
        return new CrawlController(config, normalizer, pageFetcher, robotstxtServer, frontierConfiguration);
    }

    protected abstract FrontierConfiguration getFrontierConfiguration(CrawlConfig config) throws Exception;

    public long start(int numberOfCrawlers, String... seeds) {
        try {
            final CrawlController controller = init();

            long start = System.currentTimeMillis();

            ConsoleProgressBar consoleProgressBar = new ConsoleProgressBar(0.025, "Inserting Seeds");
            consoleProgressBar.update(0);

            long i = 0;
            for (String seed : seeds) {
                controller.addSeed(seed);
                consoleProgressBar.update((double) i / seeds.length);
                i++;
            }
            consoleProgressBar.update(1);

            long end = System.currentTimeMillis() - start;

            logger.info("#### Took {} ms to insert seeds", end);

            final CrawlController.WebCrawlerFactory<FrontierWebCrawler> factory = FrontierWebCrawler::new;

            watchDogs(controller);

            controller.start(factory, numberOfCrawlers);

            final List<Object> data = controller.getCrawlersLocalData();

            long crawled = 0;

            for (Object d : data) {
                crawled += (long) d;
            }

            return crawled;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void watchDogs(CrawlController controller) throws SchedulerException {
        final JobDataMap m = new JobDataMap();
        m.put(CONTROLLER_KEY, controller);

        final JobDetail stopJob = newJob(StopJob.class)
                .withDescription("StopJob")
                .build();

        final JobDetail statsJob = newJob(StatsJob.class)
                .withDescription("StatsJob")
                .build();

        // Trigger the job to run now, and then repeat every 40 seconds
        final Trigger stopTrigger = newTrigger()
                .withIdentity("StopTrigger")
                .startNow()
                .withSchedule(simpleSchedule()
                        .withIntervalInMinutes(terminateAfterXMinutes)
                        .repeatForever())
                .usingJobData(m)
                .build();

        final Trigger statsTrigger = newTrigger()
                .withIdentity("StatsTrigger")
                .startNow()
                .withSchedule(simpleSchedule()
                        .withIntervalInSeconds(30)
                        .repeatForever())
                .usingJobData(m)
                .build();

        // Tell quartz to schedule the job using our trigger
        scheduler.scheduleJob(stopJob, stopTrigger);
        scheduler.scheduleJob(statsJob, statsTrigger);
    }

    public static class StopJob implements Job {

        @Override
        public void execute(JobExecutionContext context) {

            final JobDataMap map = context.getMergedJobDataMap();

            if (skipFirst) {
                skipFirst = false;
                return;
            }

            final CrawlController controller = (CrawlController) map.get(CONTROLLER_KEY);

            if (!controller.isShuttingDown()) {
                logger.info("Shutting down...");
                controller.shutdown();
            }
        }
    }

    public static class StatsJob implements Job {

        @Override
        public void execute(JobExecutionContext context) {

            final JobDataMap map = context.getMergedJobDataMap();
            final CrawlController controller = (CrawlController) map.get(CONTROLLER_KEY);
            final Frontier frontier = controller.getFrontier();
            final Path store = Paths.get(controller.getConfig().getCrawlStorageFolder() + File.separator + "stats.txt");

            logger.info("Scheduled pages: {}, Processed pages: {}",
                    frontier.getNumberOfScheduledPages(),
                    frontier.getNumberOfProcessedPages());

            try {
                Files.writeString(store, frontier.getNumberOfScheduledPages() + ";" + frontier.getNumberOfProcessedPages() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                logger.warn(e.getLocalizedMessage(), e);
            }
        }
    }
}
