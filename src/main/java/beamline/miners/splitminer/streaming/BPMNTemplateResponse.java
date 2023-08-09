package beamline.miners.splitminer.streaming;

import beamline.miners.splitminer.bpmn.BPMNTemplate;
import beamline.models.responses.Response;

public class BPMNTemplateResponse extends Response {
    private static final long serialVersionUID = 803561441785918416L;
    private final BPMNTemplate bpmnTemplate;

    public BPMNTemplateResponse(BPMNTemplate bpmnTemplate) {
        this.bpmnTemplate = bpmnTemplate;
    }

    public BPMNTemplate getBpmnTemplate() {
        return bpmnTemplate;
    }
}
