package beamline.miners.splitminer.bpmn;

import lombok.Value;

import java.util.UUID;

@Value
public class BPMNActivity implements BPMNNode {
    String id;
    String name;

    public BPMNActivity(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    @Override
    public String simplify() {
        return name;
    }
}