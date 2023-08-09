package beamline.miners.splitminer.streaming.lossycountingbudget;

import beamline.events.BEvent;
import beamline.miners.splitminer.bpmn.BPMNTemplate;
import beamline.miners.splitminer.dfg.DFG;
import beamline.miners.splitminer.dfg.FilteredPDFG;
import beamline.miners.splitminer.dfg.PrunedDFG;
import beamline.miners.splitminer.streaming.BPMNTemplateResponse;
import beamline.models.algorithms.StreamMiningAlgorithm;

public class LossyCountingBudgetSplitMiner extends StreamMiningAlgorithm<BPMNTemplateResponse> {
    // DFG
    private final LossyCountingBudgetCases cases;
    private final LossyCountingBudgetRelations relations;

    // SPLIT MINER PARAMETERS
    private final double concurrencyThreshold;
    private final double frequencyPercentile;

    private final int modelRefreshRate;

    public LossyCountingBudgetSplitMiner(int casesBudget, int relationsBudget, double concurrencyThreshold, double frequencyPercentile, int modelRefreshRate) {
        this.concurrencyThreshold = concurrencyThreshold;
        this.frequencyPercentile = frequencyPercentile;
        this.modelRefreshRate = modelRefreshRate;
        this.cases = new LossyCountingBudgetCases(casesBudget);
        this.relations = new LossyCountingBudgetRelations(relationsBudget);
    }

    @Override
    public BPMNTemplateResponse ingest(BEvent bEvent) {
        final var activityName = bEvent.getEventName();
        final var caseId = bEvent.getTraceName();

        final var latestActivity = cases.addObservation(activityName, caseId);
        if (latestActivity != null) {
            relations.addObservation(latestActivity, activityName);
        }

        if (getProcessedEvents() % modelRefreshRate == 0) {
            return updateModel();
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
