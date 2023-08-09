package beamline.miners.splitminer.streaming.lossycounting;

import beamline.events.BEvent;
import beamline.miners.splitminer.bpmn.BPMNTemplate;
import beamline.miners.splitminer.dfg.DFG;
import beamline.miners.splitminer.dfg.FilteredPDFG;
import beamline.miners.splitminer.dfg.PrunedDFG;
import beamline.miners.splitminer.streaming.BPMNTemplateResponse;
import beamline.miners.splitminer.streaming.MemoryUsageResponse;
import beamline.models.algorithms.StreamMiningAlgorithm;

import java.util.HashMap;
import java.util.Map;

public class LossyCountingMemoryEvaluator extends StreamMiningAlgorithm<MemoryUsageResponse> {

    // LOSSY COUNTING PARAMETERS
    private final int bucketWidth;

    // DFG
    private final LossyCountingCases cases = new LossyCountingCases();
    private final LossyCountingRelations relations = new LossyCountingRelations();

    private final int logSize;
    private final Map<Long, Integer> memoryUsage;

    public LossyCountingMemoryEvaluator(double maxApproximationError, int logSize) {
        this.bucketWidth = (int) (1.0 / maxApproximationError);
        this.logSize = logSize;
        this.memoryUsage = new HashMap<>();
    }

    @Override
    public MemoryUsageResponse ingest(BEvent bEvent) {
        final var currentBucket = (int) (getProcessedEvents() / bucketWidth);

        final var activityName = bEvent.getEventName();
        final var caseId = bEvent.getTraceName();

        final var latestActivity = cases.addObservation(activityName, caseId, currentBucket);
        if (latestActivity != null) {
            relations.addObservation(latestActivity, activityName, currentBucket);
        }

        if (getProcessedEvents() % bucketWidth == 0) {
            cases.cleanup(currentBucket);
            relations.cleanup(currentBucket);
        }

        memoryUsage.put(getProcessedEvents(), getConsumedMemory());

        if (getProcessedEvents() == logSize) {
            return new MemoryUsageResponse(memoryUsage);
        }

        return null;
    }

    int getConsumedMemory() {
        return cases.keySet().size() + relations.keySet().size();
    }
}
