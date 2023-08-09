package beamline.miners.splitminer.streaming.lossycountingbudget;

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

public class LossyCountingBudgetMemoryEvaluator extends StreamMiningAlgorithm<MemoryUsageResponse> {
    // DFG
    private final LossyCountingBudgetCases cases;
    private final LossyCountingBudgetRelations relations;

    private final int logSize;
    private final Map<Long, Integer> memoryUsage;

    public LossyCountingBudgetMemoryEvaluator(int casesBudget, int relationsBudget, int logSize) {
        this.logSize = logSize;
        this.cases = new LossyCountingBudgetCases(casesBudget);
        this.relations = new LossyCountingBudgetRelations(relationsBudget);
        this.memoryUsage = new HashMap<>();
    }

    @Override
    public MemoryUsageResponse ingest(BEvent bEvent) {
        final var activityName = bEvent.getEventName();
        final var caseId = bEvent.getTraceName();

        final var latestActivity = cases.addObservation(activityName, caseId);
        if (latestActivity != null) {
            relations.addObservation(latestActivity, activityName);
        }

        memoryUsage.put(getProcessedEvents(), getConsumedMemory());

        if (getProcessedEvents() % logSize == 0) {
            return new MemoryUsageResponse(memoryUsage);
        }

        return null;
    }

    int getConsumedMemory() {
        return cases.keySet().size() + relations.keySet().size();
    }
}
