package beamline.miners.splitminer.dfg;

import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class FilteredPDFG {
    private final Set<String> sources;
    private final Set<String> sinks;
    private final Set<String> nodes;
    private final Set<Pair<String, String>> edges;
    private final Set<Pair<String, String>> concurrentEdges;

    public FilteredPDFG(Set<String> sources, Set<String> sinks, Set<String> nodes, Set<Pair<String, String>> edges, Set<Pair<String, String>> concurrentEdges) {
        this.sources = sources;
        this.sinks = sinks;
        this.nodes = nodes;
        this.edges = edges;
        this.concurrentEdges = concurrentEdges;
    }

    public FilteredPDFG(PrunedDFG pdfg, double percentile /* 0 - 1*/) {
        final var sources = pdfg.getSources();
        final var sinks = pdfg.getSinks();

        final var forwardCapacities = new HashMap<String, Integer>();
        final var backwardCapacities = new HashMap<String, Integer>();
        final var frequencies = new LinkedList<Integer>();
        sources.forEach(source -> forwardCapacities.put(source, Integer.MAX_VALUE));
        sinks.forEach(sink -> backwardCapacities.put(sink, Integer.MAX_VALUE));

        for (final var node : pdfg.getNodes()) {
            if (!sources.contains(node)) {
                forwardCapacities.put(node, 0);
            }
            if (!sinks.contains(node)) {
                backwardCapacities.put(node, 0);
            }
            var highestIncomingFrequency = 0;
            var highestOutgoingFrequency = 0;
            for (final var entry : pdfg.getEdges().entrySet()) {
                if (entry.getKey().getRight().equals(node) && entry.getValue() > highestIncomingFrequency) {
                    highestIncomingFrequency = entry.getValue();
                }
                if (entry.getKey().getLeft().equals(node) && entry.getValue() > highestOutgoingFrequency) {
                    highestOutgoingFrequency = entry.getValue();
                }
            }
            frequencies.add(highestIncomingFrequency);
            frequencies.add(highestOutgoingFrequency);
        }

        Collections.sort(frequencies);
        var index = (int) (frequencies.size() * percentile) - 1;
        if (index < 0) {
            index = 0;
        }
        final var fth = !frequencies.isEmpty() ? frequencies.get(index) : 0;

        var bestIncomingEdges = new HashMap<String, Pair<String, String>>();
        var bestOutgoingEdges = new HashMap<String, Pair<String, String>>();
        discoverBestIncomingEdges(pdfg, sources, forwardCapacities, bestIncomingEdges);
        discoverBestOutgoingEdges(pdfg, sinks, backwardCapacities, bestOutgoingEdges);

        var filteredEdges = new HashMap<Pair<String, String>, Integer>();
        for (final var entry : pdfg.getEdges().entrySet()) {
            if (bestIncomingEdges.containsKey(entry.getKey().getLeft()) || sources.contains(entry.getKey().getLeft())) {
                if (bestIncomingEdges.containsValue(entry.getKey()) || bestOutgoingEdges.containsValue(entry.getKey()) || entry.getValue() > fth) {
                    filteredEdges.put(entry.getKey(), entry.getValue());
                }
            }
        }

        final var nodesWithNoOutGoingEdges = filteredEdges.keySet().stream()
                .filter(edge -> filteredEdges.keySet().stream().noneMatch(edge2 -> edge.getLeft().equals(edge2.getRight())))
                .map(Pair::getLeft).collect(Collectors.toSet());
        final var nodesWithNoIncomingEdges = filteredEdges.keySet().stream()
                .filter(edge -> filteredEdges.keySet().stream().noneMatch(edge2 -> edge.getRight().equals(edge2.getLeft())))
                .map(Pair::getRight).collect(Collectors.toSet());
        this.nodes = filteredEdges.keySet().stream().flatMap(edge -> Stream.of(edge.getLeft(), edge.getRight())).collect(Collectors.toSet());
        this.edges = new HashSet<>(filteredEdges.keySet());
        this.concurrentEdges = pdfg.getConcurrentEdges();
        this.sources = sources.stream().filter(nodes::contains).collect(Collectors.toSet());
        this.sources.addAll(nodesWithNoOutGoingEdges);
        this.sinks = sinks.stream().filter(nodes::contains).collect(Collectors.toSet());
        this.sinks.addAll(nodesWithNoIncomingEdges);
    }

    void discoverBestIncomingEdges(PrunedDFG pdfg, Set<String> sources,
                                   Map<String, Integer> forwardCapacities,
                                   Map<String, Pair<String, String>> bestIncomingEdges) {
        final var queue = new LinkedList<String>();
        sources.forEach(queue::addLast);

        final var unexploredNodes = new HashSet<>(pdfg.getNodes());
        sources.forEach(unexploredNodes::remove);

        while (!queue.isEmpty()) {
            final var node = queue.pollFirst();

            for (final var entry : pdfg.getOutgoingEdges(node).entrySet()) {
                final var target = entry.getKey().getRight();
                final var frequency = entry.getValue();
                final var cmax = Math.min(forwardCapacities.get(node), frequency);

                if (cmax > forwardCapacities.get(target)) {
                    forwardCapacities.put(target, cmax);
                    bestIncomingEdges.put(target, entry.getKey());
                    if (!queue.contains(target) || !unexploredNodes.contains(target)) {
                        unexploredNodes.add(target);
                    }
                }
                if (unexploredNodes.contains(target)) {
                    unexploredNodes.remove(target);
                    queue.add(target);
                }
            }
        }
    }

    void discoverBestOutgoingEdges(PrunedDFG pdfg, Set<String> sinks,
                                   Map<String, Integer> backwardCapacities,
                                   Map<String, Pair<String, String>> bestOutgoingEdges) {
        final var queue = new LinkedList<String>();
        sinks.forEach(queue::addLast);

        final var unexploredNodes = new HashSet<>(pdfg.getNodes());
        sinks.forEach(unexploredNodes::remove);

        while (!queue.isEmpty()) {
            final var node = queue.pollFirst();

            for (final var entry : pdfg.getIncomingEdges(node).entrySet()) {
                final var source = entry.getKey().getLeft();
                final var frequency = entry.getValue();
                final var cmax = Math.min(backwardCapacities.get(node), frequency);
                if (cmax > backwardCapacities.get(source)) {
                    backwardCapacities.put(source, cmax);
                    bestOutgoingEdges.put(source, entry.getKey());
                    if (!queue.contains(source) || !unexploredNodes.contains(source)) {
                        unexploredNodes.add(source);
                    }
                }
                if (unexploredNodes.contains(source)) {
                    unexploredNodes.remove(source);
                    queue.add(source);
                }
            }
        }
    }
}
