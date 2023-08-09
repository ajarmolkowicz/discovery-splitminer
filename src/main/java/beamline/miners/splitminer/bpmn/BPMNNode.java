package beamline.miners.splitminer.bpmn;

public interface BPMNNode {
    String getId();

    String simplify();

    default boolean isAndSplit() {
        if (this instanceof BPMNGateway) {
            var andSplit = (BPMNGateway) this;
            return andSplit.getCategory().equals(BPMNGateway.Category.SPLIT) && andSplit.getType().equals(BPMNGateway.Type.AND);
        }
        return false;
    }

    default boolean isXorSplit() {
        if (this instanceof BPMNGateway) {
            var xorSplit = (BPMNGateway) this;
            return xorSplit.getCategory().equals(BPMNGateway.Category.SPLIT) && xorSplit.getType().equals(BPMNGateway.Type.XOR);
        }
        return false;
    }

    default boolean isAndJoin() {
        if (this instanceof BPMNGateway) {
            var andJoin = (BPMNGateway) this;
            return andJoin.getCategory().equals(BPMNGateway.Category.JOIN) && andJoin.getType().equals(BPMNGateway.Type.AND);
        }
        return false;
    }

    default boolean isXorJoin() {
        if (this instanceof BPMNGateway) {
            var xorJoin = (BPMNGateway) this;
            return xorJoin.getCategory().equals(BPMNGateway.Category.JOIN) && xorJoin.getType().equals(BPMNGateway.Type.XOR);
        }
        return false;
    }

    default boolean isOrJoin() {
        if (this instanceof BPMNGateway) {
            var xor = (BPMNGateway) this;
            return xor.getCategory().equals(BPMNGateway.Category.JOIN) && xor.getType().equals(BPMNGateway.Type.OR);
        }
        return false;
    }
}
