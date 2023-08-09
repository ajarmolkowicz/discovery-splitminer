package beamline.miners.splitminer.bpmn;

import lombok.Value;

import java.util.UUID;

import static java.lang.String.format;

@Value
public class BPMNGateway implements BPMNNode {
    String id;
    Type type;
    Category category;

    public BPMNGateway(Type type, Category category) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.category = category;
    }

    public BPMNGateway(String dfgNode) {
        final var splits = dfgNode.split(":");
        this.id = splits[2];
        this.type = Type.valueOf(splits[0]);
        this.category = Category.valueOf(splits[1]);
    }

    @Override
    public String toString() {
        return format("%s:%s:%s", type.toString(), category.toString(), id);
    }

    @Override
    public String simplify() {
        return format("%s:%s", type.toString(), category.toString());
    }

    enum Type {
        AND, XOR, OR
    }

    enum Category {
        JOIN, SPLIT
    }
}