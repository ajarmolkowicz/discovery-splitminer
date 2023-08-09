package beamline.miners.splitminer.streaming.slidingwindow;

import beamline.events.BEvent;
import beamline.miners.splitminer.bpmn.BPMNTemplate;
import beamline.miners.splitminer.dfg.DFG;
import beamline.miners.splitminer.dfg.FilteredPDFG;
import beamline.miners.splitminer.dfg.PrunedDFG;
import beamline.miners.splitminer.streaming.BPMNTemplateResponse;
import beamline.miners.splitminer.streaming.ProcessingTimeResponse;
import beamline.models.algorithms.StreamMiningAlgorithm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SlidingWindowProcessingTimeEvaluator extends StreamMiningAlgorithm<ProcessingTimeResponse> {

    // SLIDING WINDOW PARAMETERS
    private final int maxCapacity;

    // DFG
    private final LinkedList<BEvent> events;

    // SPLIT MINER PARAMETERS
    private final double concurrencyThreshold;
    private final double frequencyPercentile;

    private final int logSize;
    private final Map<Long, Integer> processingTime;

    public SlidingWindowProcessingTimeEvaluator(int maxCapacity, double concurrencyThreshold, double frequencyPercentile, int logSize) {
        this.maxCapacity = maxCapacity;
        this.concurrencyThreshold = concurrencyThreshold;
        this.frequencyPercentile = frequencyPercentile;
        this.logSize = logSize;
        this.events = new LinkedList<>();
        this.processingTime = new HashMap<>();
    }

    @Override
    public ProcessingTimeResponse ingest(BEvent bEvent) {
        final var start = System.nanoTime();
        if (events.size() == maxCapacity) {
            events.pollFirst();
        }

        events.addLast(bEvent);

        updateModel();

        final var stop = System.nanoTime();

        processingTime.put(getProcessedEvents(), (int) (stop - start));

        if (getProcessedEvents() % logSize == 0) {
            return new ProcessingTimeResponse(processingTime);
        }

        return null;
    }

    public BPMNTemplateResponse updateModel() {
        final var dfg = new DFG(events, concurrencyThreshold);
        final var pdfg = new PrunedDFG(dfg);
        final var fpdfg = new FilteredPDFG(pdfg, frequencyPercentile);
        final var bpmnTemplate = new BPMNTemplate(fpdfg);
        return new BPMNTemplateResponse(bpmnTemplate);
    }
}
