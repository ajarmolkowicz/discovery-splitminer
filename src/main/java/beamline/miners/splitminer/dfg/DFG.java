package beamline.miners.splitminer.dfg;

import beamline.events.BEvent;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class DFG {
    private List<BEvent> events;
    private Map<String, List<Pair<String, String>>> cases;

    private Set<String> sources;
    private Set<String> sinks;
    private Map<Pair<String, String>, Integer> edges;

    private Set<Pair<String, String>> selfLoops;
    private Set<Pair<String, String>> shortLoops;

    private Map<Pair<String, String>, Integer> concurrentEdges;
    private Map<Pair<String, String>, Integer> infrequentEdges;

    public DFG(List<BEvent> events, double concurrencyThreshold) {
        prepareNodes(events);
        findSourcesAndSinks();
        buildEdges();
        findLoops();
        findConcurrentAndInfrequentEdges(concurrencyThreshold);
    }

    public void prepareNodes(List<BEvent> events) {
        this.events = events;
        this.cases = new HashMap<>();

        final var latestActivityInCase = new HashMap<String, String>();

        events.forEach(event -> {
            final var caseId = event.getTraceName();
            final var activity = event.getEventName();
            if (cases.containsKey(caseId)) {
                final var caze = cases.get(caseId);
                caze.add(Pair.of(latestActivityInCase.get(caseId), activity));
            } else {
                cases.put(caseId, new LinkedList<>());
            }
            latestActivityInCase.put(caseId, activity);
        });
    }

    public DFG(Set<String> sources, Set<String> sinks,
               Map<Pair<String, String>, Integer> edges,
               Set<Pair<String, String>> selfLoops,
               Set<Pair<String, String>> shortLoops,
               double concurrencyThreshold) {
        this.sources = sources;
        this.sinks = sinks;
        this.edges = edges;
        this.selfLoops = selfLoops;
        this.shortLoops = shortLoops;
        adjustSourcesAndSinks();
        findConcurrentAndInfrequentEdges(concurrencyThreshold);
    }

    void buildEdges() {
        final var edges = new HashMap<Pair<String, String>, Integer>();
        cases.forEach((caseId, transitions) ->
                transitions.forEach(transition ->
                        edges.put(transition, edges.getOrDefault(transition, 0) + 1)));
        this.edges = edges;
    }

    void findSourcesAndSinks() {
        final var sources = new HashMap<String, Integer>();
        final var sinks = new HashMap<String, Integer>();
        cases.forEach((caseId, transitions) -> {
            if (transitions.size() > 1) { // Case with one event
                final var firstNode = transitions.get(0);
                final var lastNode = transitions.get(transitions.size() - 1);
                sources.put(firstNode.getLeft(), sources.getOrDefault(firstNode.getLeft(), 0) + 1);
                sinks.put(lastNode.getRight(), sinks.getOrDefault(lastNode.getRight(), 0) + 1);
            }
        });
        this.sources = sources.keySet();
        this.sinks = sinks.keySet();
    }

    void findLoops() {
        // --- SELF LOOPS
        final var selfLoops = new HashMap<Pair<String, String>, Integer>();

        edges.forEach((edge, frequency) -> {
            if (edge.getLeft().equals(edge.getRight())) {
                selfLoops.put(edge, frequency);
            }
        });
        this.selfLoops = selfLoops.keySet();

        // --- SHORT LOOPS
        final var shortLoops = new HashMap<Pair<String, String>, Integer>();

        cases.forEach((caseId, transitions) -> {
            final var iterator = transitions.listIterator();
            while (iterator.hasNext()) {
                final var transition = iterator.next();
                if (iterator.hasNext()) {
                    var nextTransition = iterator.next();
                    if (transition.getLeft().equals(nextTransition.getRight())) {
                        if (shortLoops.containsKey(transition)) {
                            shortLoops.put(transition, shortLoops.get(transition) + 1);
                        } else {
                            shortLoops.put(transition, 1);
                        }
                    }
                    iterator.previous();
                }
            }
        });
        this.shortLoops = shortLoops.keySet();
    }

    void adjustSourcesAndSinks() {
        this.sources = this.sources.stream()
                .filter(source -> edges.keySet().stream().anyMatch(edge2 -> edge2.getLeft().equals(source)))
                .collect(Collectors.toSet());
        this.sinks = this.sinks.stream()
                .filter(source -> edges.keySet().stream().anyMatch(edge2 -> edge2.getRight().equals(source)))
                .collect(Collectors.toSet());

        final var extraSources = edges.keySet().stream()
                .filter(edge -> edges.keySet().stream().noneMatch(edge2 -> edge2.getRight().equals(edge.getLeft())))
                .map(Pair::getLeft).collect(Collectors.toSet());
        final var extraSinks = edges.keySet().stream()
                .filter(edge -> edges.keySet().stream().noneMatch(edge2 -> edge2.getLeft().equals(edge.getRight())))
                .map(Pair::getRight).collect(Collectors.toSet());

        this.sources.addAll(extraSources);
        this.sinks.addAll(extraSinks);
    }

    void findConcurrentAndInfrequentEdges(double threshold) {
        final var concurrentEdges = new HashMap<Pair<String, String>, Integer>();
        final var infrequentEdges = new HashMap<Pair<String, String>, Integer>();

        for (final var entry : edges.entrySet()) {
            final var edge = entry.getKey();
            final var frequency = entry.getValue();
            if (!selfLoops.contains(edge)) {
                if (!shortLoops.contains(edge) && !shortLoops.contains(Pair.of(edge.getRight(), edge.getLeft()))) {
                    if (edges.containsKey(Pair.of(edge.getRight(), edge.getLeft()))) {
                        final var frequency2 = edges.get(Pair.of(edge.getRight(), edge.getLeft()));
                        final var diff = frequency - frequency2;
                        if ((Math.abs(diff) / (double) (frequency + frequency2)) < threshold) {
                            concurrentEdges.put(edge, frequency);
                        } else {
                            if (diff < 0) {
                                infrequentEdges.put(edge, frequency);
                            }
                        }
                    }
                }
            }
        }

        this.concurrentEdges = concurrentEdges;
        this.infrequentEdges = infrequentEdges;
    }
}
