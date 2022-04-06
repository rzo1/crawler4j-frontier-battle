# crawler4j-frontier-battle

## Requirements

- Maven 3.8.0 +
- Java 8 +

## Build

```bash

mvn clean install

```

## Run

A `mvn clean install` will build a shaded executable JAR file (ready for usage):

```bash
java -jar frontier-battle-runtime-LATEST.jar
```

It will require some command line options:

```bash

Usage: <main class> [-c=<crawlers>] -f=<frontier> [-l=<storagePath>]
                    -s=<seedPath> [-t=<terminateAfterXMinutes>]
  -c, --crawlers=<crawlers>
                           Number of crawlers
  -f, --frontier=<frontier>
                           The frontier impl to use
  -l, --location=<storagePath>
                           Storage Location
  -s, --seeds=<seedPath>   File with seeds
  -t, --time=<terminateAfterXMinutes>
                           Time in Minutes

```
You can choose between different frontier impls: `SLEEPYCAT`,`HSQLDB`, `URLFRONTIER`. The crawlers will behave polite.

The behaviour can be changed in `AbstractFrontierController`:

```bash
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
````
