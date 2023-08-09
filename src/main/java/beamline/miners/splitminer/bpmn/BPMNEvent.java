package beamline.miners.splitminer.bpmn;

import lombok.Value;

import java.util.UUID;

@Value
public class BPMNEvent implements BPMNNode {
    String id;

    public BPMNEvent() {
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public String simplify() {
        return id;
    }
}
