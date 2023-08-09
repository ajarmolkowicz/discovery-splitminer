package beamline.miners.splitminer.dfg;

import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class PrunedDFG {
    private final Set<String> sources;
    private final Set<String> sinks;
    private final Set<String> nodes;
    private final Map<Pair<String, String>, Integer> edges;
    private final Set<Pair<String, String>> concurrentEdges;

    public PrunedDFG(DFG dfg) {
        this.edges = dfg.getEdges().entrySet().stream()
                .filter(edge -> !dfg.getSelfLoops().contains(edge.getKey()))
                .filter(edge -> !dfg.getShortLoops().contains(edge.getKey()) && !dfg.getShortLoops().contains(Pair.of(edge.getKey().getRight(), edge.getKey().getLeft())))
                .filter(edge -> !dfg.getConcurrentEdges().containsKey(edge.getKey()))
                .filter(edge -> !dfg.getInfrequentEdges().containsKey(edge.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.nodes = edges.keySet().stream().flatMap(edge -> Stream.of(edge.getLeft(), edge.getRight())).collect(Collectors.toSet());
        this.concurrentEdges = dfg.getConcurrentEdges().keySet();
        this.sources = dfg.getSources().stream().filter(nodes::contains).collect(Collectors.toSet());
        this.sinks = dfg.getSinks().stream().filter(nodes::contains).collect(Collectors.toSet());
    }

    public Map<Pair<String, String>, Integer> getIncomingEdges(String node) {
        return edges.entrySet().stream()
                .filter(entry -> entry.getKey().getRight().equals(node))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<Pair<String, String>, Integer> getOutgoingEdges(String node) {
        return edges.entrySet().stream()
                .filter(entry -> entry.getKey().getLeft().equals(node))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
