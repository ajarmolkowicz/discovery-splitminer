package beamline.miners.splitminer.streaming.lossycounting;

import beamline.events.BEvent;
import beamline.miners.splitminer.bpmn.BPMNTemplate;
import beamline.miners.splitminer.dfg.DFG;
import beamline.miners.splitminer.dfg.FilteredPDFG;
import beamline.miners.splitminer.dfg.PrunedDFG;
import beamline.miners.splitminer.streaming.BPMNTemplateResponse;
import beamline.models.algorithms.StreamMiningAlgorithm;

public class LossyCountingSplitMiner extends StreamMiningAlgorithm<BPMNTemplateResponse> {

    // LOSSY COUNTING PARAMETERS
    private final int bucketWidth;

    // DFG
    private final LossyCountingCases cases = new LossyCountingCases();
    private final LossyCountingRelations relations = new LossyCountingRelations();

    // SPLIT MINER PARAMETERS
    private final double concurrencyThreshold;
    private final double frequencyPercentile;

    private final int modelRefreshRate;

    public LossyCountingSplitMiner(double maxApproximationError, double concurrencyThreshold, double frequencyPercentile, int modelRefreshRate) {
        this.bucketWidth = (int) (1.0 / maxApproximationError);
        this.concurrencyThreshold = concurrencyThreshold;
        this.frequencyPercentile = frequencyPercentile;
        this.modelRefreshRate = modelRefreshRate;
    }

    @Override
    public BPMNTemplateResponse ingest(BEvent bEvent) {
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
