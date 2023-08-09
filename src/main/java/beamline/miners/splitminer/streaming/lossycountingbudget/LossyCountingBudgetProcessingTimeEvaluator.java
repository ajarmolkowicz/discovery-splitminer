package beamline.miners.splitminer.streaming.lossycountingbudget;

import beamline.events.BEvent;
import beamline.miners.splitminer.bpmn.BPMNTemplate;
import beamline.miners.splitminer.dfg.DFG;
import beamline.miners.splitminer.dfg.FilteredPDFG;
import beamline.miners.splitminer.dfg.PrunedDFG;
import beamline.miners.splitminer.streaming.BPMNTemplateResponse;
import beamline.miners.splitminer.streaming.ProcessingTimeResponse;
import beamline.models.algorithms.StreamMiningAlgorithm;

import java.util.HashMap;
import java.util.Map;

public class LossyCountingBudgetProcessingTimeEvaluator extends StreamMiningAlgorithm<ProcessingTimeResponse> {
    // DFG
    private final LossyCountingBudgetCases cases;
    private final LossyCountingBudgetRelations relations;

    // SPLIT MINER PARAMETERS
    private final double concurrencyThreshold;
    private final double frequencyPercentile;

    private final int logSize;
    private final Map<Long, Integer> processingTime;

    public LossyCountingBudgetProcessingTimeEvaluator(int casesBudget, int relationsBudget, double concurrencyThreshold, double frequencyPercentile, int logSize) {
        this.concurrencyThreshold = concurrencyThreshold;
        this.frequencyPercentile = frequencyPercentile;
        this.logSize = logSize;
        this.cases = new LossyCountingBudgetCases(casesBudget);
        this.relations = new LossyCountingBudgetRelations(relationsBudget);
        this.processingTime = new HashMap<>();
    }

    @Override
    public ProcessingTimeResponse ingest(BEvent bEvent) {
        final var start = System.nanoTime();

        final var activityName = bEvent.getEventName();
        final var caseId = bEvent.getTraceName();


        final var latestActivity = cases.addObservation(activityName, caseId);
        if (latestActivity != null) {
            relations.addObservation(latestActivity, activityName);
        }

        updateModel();

        final var stop = System.nanoTime();

        processingTime.put(getProcessedEvents(), (int) (stop - start));

        if (getProcessedEvents() % logSize == 0) {
            return new ProcessingTimeResponse(processingTime);
        }

        return null;
    }

    public BPMNTemplateResponse updateModel() {
        final var dfg = new DFG(
                cases.getStartingActivities(),
                cases.getFinishingActivities(),
                relations.getRelations(),
                cases.getSelfLoops(),
                cases.getShortLoops(),
                concurrencyThreshold
        );
        final var pdfg = new PrunedDFG(dfg);
        final var fpdfg = new FilteredPDFG(pdfg, frequencyPercentile);
        final var bpmnTemplate = new BPMNTemplate(fpdfg);
        return new BPMNTemplateResponse(bpmnTemplate);
    }
}
