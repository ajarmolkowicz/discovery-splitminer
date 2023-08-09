package beamline.miners.splitminer.main;

import beamline.events.BEvent;
import beamline.miners.splitminer.streaming.BPMNTemplateResponseSink;
import beamline.miners.splitminer.streaming.lossycounting.LossyCountingSplitMiner;
import beamline.miners.splitminer.streaming.lossycountingbudget.LossyCountingBudgetSplitMiner;
import beamline.miners.splitminer.streaming.slidingwindow.SlidingWindowSplitMiner;
import beamline.sources.BeamlineAbstractSource;
import beamline.sources.CSVLogSource;
import beamline.sources.XesLogSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static java.lang.String.format;

@Slf4j
public class Main implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = "Source file directory")
    private String sourceFileDirectory;

    @CommandLine.Parameters(index = "1", description = "Source file name")
    private String sourceFileName;

    @CommandLine.Parameters(index = "2", description = "The output directory")
    private String outputDirectory;

    @CommandLine.Parameters(index = "3", description = "Concurrency threshold")
    private Double concurrencyThreshold;

    @CommandLine.Parameters(index = "4", description = "Frequency percentile")
    private Double frequencyPercentile;

    @CommandLine.Option(names = {"-s", "--source"}, description = "Source file input type: CSV, XES", required = true)
    private String sourceType;

    @CommandLine.Option(names = {"-m", "--mode"}, description = "Online split miner approach: SW, LC, LCB", required = true)
    private String mode;

    @CommandLine.Option(names = {"-r", "--refresh"}, description = "Model refresh rate", required = true)
    private Integer refreshRate;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Output type: SVG, BPMN, CAMUNDA_BPMN", required = true)
    private String outputType;

    // CSV source options
    @CommandLine.Option(names = {"--caseid_column"}, description = "Case ID column")
    private Integer caseIdColumn;

    @CommandLine.Option(names = {"--activity_column"}, description = "Activity column")
    private Integer activityNameColumn;

    @CommandLine.Option(names = {"--separator"}, description = "Column separator")
    private Character columnSeparator;

    // Sliding window options
    @CommandLine.Option(names = {"--max_capacity"}, description = "Sliding window max capacity", defaultValue = "100")
    private Integer maxCapacity = 100;

    // Lossy counting options
    @CommandLine.Option(names = {"--approximation_error"}, description = "Lossy counting max approximation error", defaultValue = "0.01")
    private Double maxApproximationError = 0.01;

    // Lossy counting budget options
    @CommandLine.Option(names = {"--cases_budget"}, description = "Lossy counting cases budget", defaultValue = "100")
    private int casesBudget = 100;
    @CommandLine.Option(names = {"--relations_budget"}, description = "Lossy counting cases budget", defaultValue = "100")
    private int relationsBudget = 100;


    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        BeamlineAbstractSource source = null;
        final var file = format("%s%s", sourceFileDirectory, sourceFileName);
        log.info("Reading file: {}", file);
        switch (sourceType) {
            case "CSV":
                if (caseIdColumn == null || activityNameColumn == null || columnSeparator == null) {
                    throw new RuntimeException("CSV input requires to provide following opions: --caseid_column, --activity_column, --separator");
                }
                source = new CSVLogSource(file, caseIdColumn, activityNameColumn, new CSVLogSource.ParserConfiguration().withSeparator(columnSeparator));
                break;
            case "XES":
                source = new XesLogSource(file);
                break;
            default:
                throw new RuntimeException(format("Unsupported source type: %s", sourceType));
        }
        final var env = StreamExecutionEnvironment.getExecutionEnvironment();
        final var sink = new BPMNTemplateResponseSink(outputType, sourceFileName, outputDirectory, mode);
        switch (mode) {
            case "SW":
                log.info("Starting split miner sliding window");
                env
                        .addSource(source).keyBy(BEvent::getProcessName)
                        .flatMap(new SlidingWindowSplitMiner(maxCapacity, concurrencyThreshold, frequencyPercentile, refreshRate))
                        .addSink(sink);
                env.execute();
                break;
            case "LC":
                log.info("Starting split miner lossy counting");
                env
                        .addSource(source).keyBy(BEvent::getProcessName)
                        .flatMap(new LossyCountingSplitMiner(maxApproximationError, concurrencyThreshold, frequencyPercentile, refreshRate))
                        .addSink(sink);
                env.execute();
                break;
            case "LCB":
                log.info("Starting split miner lossy counting budget");
                env
                        .addSource(source).keyBy(BEvent::getProcessName)
                        .flatMap(new LossyCountingBudgetSplitMiner(
                                casesBudget,
                                relationsBudget,
                                concurrencyThreshold,
                                frequencyPercentile,
                                refreshRate))
                        .addSink(sink);
                env.execute();
                break;
            default:
                throw new RuntimeException(format("Unsupported split miner mode: %s", mode));
        }
        return null;
    }
}
