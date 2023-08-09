package beamline.miners.splitminer.streaming.slidingwindow;

import beamline.events.BEvent;
import beamline.miners.splitminer.bpmn.BPMNTemplate;
import beamline.miners.splitminer.dfg.DFG;
import beamline.miners.splitminer.dfg.FilteredPDFG;
import beamline.miners.splitminer.dfg.PrunedDFG;
import beamline.miners.splitminer.streaming.BPMNTemplateResponse;
import beamline.miners.splitminer.streaming.MemoryUsageResponse;
import beamline.models.algorithms.StreamMiningAlgorithm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SlidingWindowMemoryEvaluator extends StreamMiningAlgorithm<MemoryUsageResponse> {

    // SLIDING WINDOW PARAMETERS
    private final int maxCapacity;

    // DFG
    private final LinkedList<BEvent> events;

    private final int logSize;
    private final Map<Long, Integer> memoryUsage;

    public SlidingWindowMemoryEvaluator(int maxCapacity, int logSize) {
        this.maxCapacity = maxCapacity;
        this.logSize = logSize;
        this.events = new LinkedList<>();
        this.memoryUsage = new HashMap<>();
    }

    @Override
    public MemoryUsageResponse ingest(BEvent bEvent) {
        if (events.size() == maxCapacity) {
            events.pollFirst();
        }
        events.addLast(bEvent);
        memoryUsage.put(getProcessedEvents(), events.size());

        if (getProcessedEvents() % logSize == 0) {
            return new MemoryUsageResponse(memoryUsage);
        }

        return null;
    }
}
