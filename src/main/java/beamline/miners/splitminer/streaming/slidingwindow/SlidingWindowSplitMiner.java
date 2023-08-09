package beamline.miners.splitminer.streaming.slidingwindow;

import beamline.events.BEvent;
import beamline.miners.splitminer.bpmn.BPMNTemplate;
import beamline.miners.splitminer.dfg.DFG;
import beamline.miners.splitminer.dfg.FilteredPDFG;
import beamline.miners.splitminer.dfg.PrunedDFG;
import beamline.miners.splitminer.streaming.BPMNTemplateResponse;
import beamline.models.algorithms.StreamMiningAlgorithm;

import java.util.LinkedList;

public class SlidingWindowSplitMiner extends StreamMiningAlgorithm<BPMNTemplateResponse> {

    // SLIDING WINDOW PARAMETERS
    private final int maxCapacity;

    // DFG
    private final LinkedList<BEvent> events;

    // SPLIT MINER PARAMETERS
    private final double concurrencyThreshold;
    private final double frequencyPercentile;

    private final int modelRefreshRate;

    public SlidingWindowSplitMiner(int maxCapacity, double concurrencyThreshold, double frequencyPercentile, int modelRefreshRate) {
        this.maxCapacity = maxCapacity;
        this.concurrencyThreshold = concurrencyThreshold;
        this.frequencyPercentile = frequencyPercentile;
        this.modelRefreshRate = modelRefreshRate;
        this.events = new LinkedList<>();
    }

    @Override
    public BPMNTemplateResponse ingest(BEvent bEvent) {
        if (events.size() == maxCapacity) {
            events.pollFirst();
        }
        events.addLast(bEvent);

        if (getProcessedEvents() % modelRefreshRate == 0) {
            return updateModel();
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
