package beamline.miners.splitminer.bpmn;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BPMNTestUtils {
    public static Set<String> simplifyNodes(Set<? extends BPMNNode> nodes) {
        return nodes.stream().map(BPMNNode::simplify).collect(Collectors.toSet());
    }
    public static List<Pair<String, String >> simplifyAssociations(Set<Pair<? extends BPMNNode, ? extends BPMNNode>> associations) {
        return associations.stream()
                .map(association -> Pair.of(association.getLeft().simplify(), association.getRight().simplify()))
                .collect(Collectors.toList());
    }
}
