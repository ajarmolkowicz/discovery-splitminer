package beamline.miners.splitminer.bpmn;

import beamline.miners.splitminer.dfg.FilteredPDFG;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.jbpt.algo.tree.rpst.IRPSTNode;
import org.jbpt.algo.tree.rpst.RPST;
import org.jbpt.graph.DirectedEdge;
import org.jbpt.graph.DirectedGraph;
import org.jbpt.hypergraph.abs.Vertex;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@Getter
public class BPMNTemplate {
    private final BPMNEvent startEvent;
    private final BPMNEvent endEvent;
    private final Set<BPMNActivity> activities;
    private final Set<BPMNGateway> gateways;
    private final Set<Pair<? extends BPMNNode, ? extends BPMNNode>> associations;

    public BPMNTemplate(FilteredPDFG fpdfg) {
        final var startEvent = new BPMNEvent();
        this.startEvent = startEvent;
        final var endEvent = new BPMNEvent();
        this.endEvent = endEvent;
        final var dfgEdges = new HashSet<>(fpdfg.getEdges());
        final var dfgNodes = new HashSet<>(fpdfg.getNodes());
        dfgNodes.add(startEvent.getId());
        dfgNodes.add(endEvent.getId());
        fpdfg.getSources().forEach(source -> dfgEdges.add(Pair.of(startEvent.getId(), source)));
        fpdfg.getSinks().forEach(sink -> dfgEdges.add(Pair.of(sink, endEvent.getId())));

        discoverSplits(dfgNodes, dfgEdges, fpdfg.getConcurrentEdges());

        discoverJoins(dfgNodes, dfgEdges);

        replaceORJoins(startEvent.getId(), dfgNodes, dfgEdges);

        final var bpmnNodeMap = new HashMap<String, BPMNNode>();
        bpmnNodeMap.put(startEvent.getId(), startEvent);
        bpmnNodeMap.put(endEvent.getId(), endEvent);
        this.activities = dfgNodes.stream()
                .filter(node -> !node.equals(startEvent.getId()))
                .filter(node -> !node.equals(endEvent.getId()))
                .filter(node -> !isGateway(node))
                .map(BPMNActivity::new)
                .peek(activity -> bpmnNodeMap.put(activity.getName(), activity))
                .collect(Collectors.toSet());
        this.gateways = dfgNodes.stream()
                .filter(node -> !node.equals(startEvent.getId()))
                .filter(node -> !node.equals(endEvent.getId()))
                .filter(BPMNTemplate::isGateway)
                .map(BPMNGateway::new)
                .peek(gateway -> bpmnNodeMap.put(gateway.toString(), gateway))
                .collect(Collectors.toSet());
        this.associations = dfgEdges.stream()
                .map(edge -> Pair.of(bpmnNodeMap.get(edge.getLeft()), bpmnNodeMap.get(edge.getRight())))
                .collect(Collectors.toSet());
    }

    static void discoverSplits(Set<String> dfgNodes, Set<Pair<String, String>> dfgEdges, Set<Pair<String, String>> concurrentEdges) {
        final var xorSplits = new HashSet<String>();
        final var andSplits = new HashSet<String>();
        final var xorSplitsEdges = new HashSet<Pair<String, String>>();
        final var andSplitEdges = new HashSet<Pair<String, String>>();
        final var leftoverSplitEdges = new HashSet<Pair<String, String>>();

        for (final var node : dfgNodes) {
            final var outgoingEdges = getDfgNodeOutgoingEdges(node, dfgEdges);

            if (outgoingEdges.size() > 1) {
                final var directSuccessors = new HashSet<String>();
                for (final var dSuccessor : outgoingEdges) {
                    directSuccessors.add(dSuccessor.getRight());
                }

                final var covers = new HashMap<String, Set<String>>();
                final var futures = new HashMap<String, Set<String>>();
                for (final var s1 : directSuccessors) {
                    covers.put(s1, new HashSet<>(new LinkedList<>(List.of(s1))));
                    final var futureCoverSet = new HashSet<String>();
                    for (final var s2 : directSuccessors) {
                        if (!s2.equals(s1) && (concurrentEdges.contains(Pair.of(s2, s1)) || concurrentEdges.contains(Pair.of(s1, s2)))) {
                            futureCoverSet.add(s2);
                        }
                    }
                    futures.put(s1, futureCoverSet);
                }

                var toRemove = new HashSet<Pair<String, String>>();
                for (final var edge : dfgEdges) {
                    if (edge.getLeft().equals(node)) {
                        toRemove.add(edge);
                    }
                }
                for (final var edge : toRemove) {
                    dfgEdges.remove(edge);
                }

                while (directSuccessors.size() > 1) {
                    boolean foundSplits = false;
                    // --- Discover XOR splits
                    Set<String> X;
                    do {
                        X = new HashSet<>();
                        String S = null;
                        Set<String> cu = null;
                        for (final var s1 : directSuccessors) {
                            S = s1;
                            cu = new HashSet<>(covers.get(s1));

                            for (final var s2 : directSuccessors) {
                                if (!s1.equals(s2) && futures.get(s1).equals(futures.get(s2))) {
                                    X.add(s2);
                                    cu.addAll(covers.get(s2));
                                }
                            }

                            if (!X.isEmpty()) {
                                X.add(s1);
                                break;
                            }
                        }

                        if (!X.isEmpty()) {
                            final var xorGateway = new BPMNGateway(BPMNGateway.Type.XOR, BPMNGateway.Category.SPLIT).toString();
                            xorSplits.add(xorGateway);
                            for (final var s : X) {
                                xorSplitsEdges.add(Pair.of(xorGateway, s));
                                directSuccessors.remove(s);
                            }

                            directSuccessors.add(xorGateway);
                            futures.put(xorGateway, futures.get(S));
                            covers.put(xorGateway, cu);
                            foundSplits = true;
                        }
                    } while (!X.isEmpty());

                    // --- Discover AND splits
                    Set<String> A;
                    do {
                        A = new HashSet<>();
                        Set<String> cu = null;
                        Set<String> fi = null;
                        for (final var s1 : directSuccessors) {
                            cu = new HashSet<>(covers.get(s1));
                            fi = new HashSet<>(futures.get(s1));

                            var cufi = new HashSet<>();
                            cufi.addAll(cu);
                            cufi.addAll(fi);

                            for (final var s2 : directSuccessors) {
                                var cufi2 = new HashSet<>();
                                cufi2.addAll(covers.get(s2));
                                cufi2.addAll(futures.get(s2));

                                if (!s1.equals(s2) && cufi.equals(cufi2)) {
                                    A.add(s2);
                                    cu.addAll(covers.get(s2));
                                    fi.retainAll(futures.get(s2));
                                }
                            }

                            if (!A.isEmpty()) {
                                A.add(s1);
                                break;
                            }
                        }

                        if (!A.isEmpty()) {
                            final var andGateway = new BPMNGateway(BPMNGateway.Type.AND, BPMNGateway.Category.SPLIT).toString();
                            andSplits.add(andGateway);

                            for (final var s : A) {
                                andSplitEdges.add(Pair.of(andGateway, s));
                                directSuccessors.remove(s);
                            }

                            directSuccessors.add(andGateway);
                            covers.put(andGateway, cu);
                            futures.put(andGateway, fi);
                            foundSplits = true;
                        }
                    } while (!A.isEmpty());

                    if (!foundSplits) {
                        break;
                    }
                }

                if (directSuccessors.size() > 1) {
                    final var xorGateway = new BPMNGateway(BPMNGateway.Type.XOR, BPMNGateway.Category.SPLIT).toString();
                    xorSplits.add(xorGateway);
                    directSuccessors.forEach(s -> xorSplitsEdges.add(Pair.of(xorGateway, s)));
                    directSuccessors.clear();
                    directSuccessors.add(xorGateway);
                }
                final var s = directSuccessors.stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("No direct successor left after splits discovery"));
                leftoverSplitEdges.add(Pair.of(node, s));
            }
        }

        dfgNodes.addAll(xorSplits);
        dfgNodes.addAll(andSplits);
        dfgEdges.addAll(xorSplitsEdges);
        dfgEdges.addAll(andSplitEdges);
        dfgEdges.addAll(leftoverSplitEdges);
    }

    static void discoverJoins(Set<String> dfgNodes, Set<Pair<String, String>> dfgEdges) {
        final var graph = new DirectedGraph();
        final var vertices = new HashMap<String, Vertex>();
        dfgNodes.forEach($ -> vertices.put($, new Vertex($)));

        for (final var edge : dfgEdges) {
            graph.addEdge(vertices.get(edge.getLeft()), vertices.get(edge.getRight()));
        }

        final var rpst = new RPST<>(graph);
        final var queue = new LinkedList<IRPSTNode<DirectedEdge, Vertex>>();

        var rpstNode = rpst.getRoot();
        if (rpstNode != null) {
            var rpstNodeChildren = rpst.getChildren(rpstNode);
            queue.addFirst(rpstNode);

            while (!rpstNodeChildren.isEmpty()) {
                rpstNodeChildren.forEach(queue::addFirst); // --- BOTTOM UP EXPLORATION
                rpstNodeChildren = rpstNodeChildren.stream().flatMap(child -> rpst.getChildren(child).stream()).collect(Collectors.toSet());
            }
        }

        while (!queue.isEmpty()) {
            final var node = queue.pollFirst();

            final var cTasks = node.getFragment().stream().flatMap($ -> Stream.of($.getSource().getName(), $.getTarget().getName())).collect(Collectors.toSet());
            final var cEdges = node.getFragment().stream().map($ -> Pair.of($.getSource().getName(), $.getTarget().getName())).collect(Collectors.toSet());

            for (final var t : cTasks) {
                final var incomingEdges = getDfgNodeIncomingEdges(t, dfgEdges);
                incomingEdges.retainAll(cEdges);

                if (incomingEdges.size() > 1) {
                    String gateway = null;

                    for (final var edge : incomingEdges) {
                        if (isBackedge(edge, dfgEdges)) {
                            gateway = new BPMNGateway(BPMNGateway.Type.XOR, BPMNGateway.Category.JOIN).toString();
                        }
                    }

                    boolean isXorHomogenous = node.getFragment().stream().noneMatch(
                            $ -> $.getSource().getName().startsWith(BPMNGateway.Type.AND.toString()) ||
                                    $.getTarget().getName().startsWith(BPMNGateway.Type.AND.toString()));

                    boolean isAndHomogenous = node.getFragment().stream().noneMatch(
                            $ -> $.getSource().getName().startsWith(BPMNGateway.Type.XOR.toString()) ||
                                    $.getTarget().getName().startsWith(BPMNGateway.Type.XOR.toString()));

                    if (gateway == null) {
                        if (isXorHomogenous) {
                            gateway = new BPMNGateway(BPMNGateway.Type.XOR, BPMNGateway.Category.JOIN).toString();
                        } else if (isAndHomogenous) {
                            gateway = new BPMNGateway(BPMNGateway.Type.AND, BPMNGateway.Category.JOIN).toString();
                        } else {
                            gateway = new BPMNGateway(BPMNGateway.Type.OR, BPMNGateway.Category.JOIN).toString();
                        }
                    }

                    dfgNodes.add(gateway);
                    dfgEdges.add(Pair.of(gateway, t));

                    for (final var edge : incomingEdges) {
                        dfgEdges.add(Pair.of(edge.getLeft(), gateway));
                        dfgEdges.remove(edge);
                    }
                }
            }
        }

        final var nodes = new ArrayList<>(dfgNodes);
        for (var i = 0; i < nodes.size(); i++) {
            final var node = nodes.get(i);
            final var incomingEdges = getDfgNodeIncomingEdges(node, dfgEdges);

            if (incomingEdges.size() <= 1
                    || node.startsWith(BPMNGateway.Type.XOR.toString())
                    || node.startsWith(BPMNGateway.Type.AND.toString())
                    || node.startsWith(BPMNGateway.Type.OR.toString())
            ) {
                continue;
            }

            final var gateway = new BPMNGateway(BPMNGateway.Type.OR, BPMNGateway.Category.JOIN).toString();
            nodes.add(gateway);
            dfgEdges.add(Pair.of(gateway, node));

            for (final var edge : incomingEdges) {
                dfgEdges.add(Pair.of(edge.getLeft(), gateway));
                dfgEdges.remove(edge);
            }
        }
        dfgNodes.clear();
        dfgNodes.addAll(nodes);
    }

    static void replaceORJoins(String source, Set<String> dfgNodes, Set<Pair<String, String>> dfgEdges) {
        final var paths = new HashMap<String, List<List<String>>>();
        final var orJoins = dfgNodes.stream()
                .filter($ -> $.startsWith(BPMNGateway.Type.OR.toString()))
                .collect(Collectors.toSet());

        for (final var orJoin : orJoins) {

            final var toExplore = new LinkedList<Pair<String, LinkedList<String>>>();
            toExplore.add(Pair.of(orJoin, new LinkedList<>(List.of(orJoin))));
            final var joinPaths = new LinkedList<List<String>>();
            paths.put(orJoin, joinPaths);
            while (!toExplore.isEmpty()) {
                final var pathPair = toExplore.pollFirst();
                final var visiting = pathPair.getLeft();
                final var path = pathPair.getRight();
                final var incomingEdges = getDfgNodeIncomingEdges(visiting, dfgEdges);
                if (incomingEdges.size() == 1) {
                    final var edge = incomingEdges.stream().findFirst().orElseThrow(() -> new RuntimeException("Visiting node has no incoming edges"));
                    if (source.equals(edge.getLeft())) {
                        path.addFirst(edge.getLeft());
                        paths.get(orJoin).add(path);
                    } else {
                        if (path.contains(edge.getLeft())) {
                            // Visiting node already visited - loop
                            continue;
                        } else {
                            path.addFirst(edge.getLeft());
                            toExplore.addFirst(Pair.of(edge.getLeft(), path));
                        }
                    }
                } else {
                    for (final var e : incomingEdges) {
                        final var newPath = new LinkedList<>(path);
                        if (path.contains(e.getLeft())) {
                            // Visiting node already visited - loop
                            continue;
                        } else {
                            newPath.addFirst(e.getLeft());
                            toExplore.add(Pair.of(e.getLeft(), newPath));
                        }
                    }
                }

            }

            final var candidates = new HashSet<Pair<String, Integer>>();
            for (final var path : joinPaths) {
                for (var i = 0; i < path.size(); i++) {
                    final var el = path.get(i);
                    if (el.contains(BPMNGateway.Category.SPLIT.toString())) {
                        if (joinPaths.stream().allMatch(p -> p.contains(el))) {
                            candidates.add(Pair.of(el, i));
                        }
                    }
                }
            }

            final var minimalDominator = candidates.stream()
                    .max(Comparator.comparingInt(Pair::getRight))
                    .map(Pair::getLeft)
                    .orElse(null);

            if (minimalDominator != null) {
                final var gs = new HashSet<String>();
                gs.add(minimalDominator);
                for (final var path : joinPaths) {
                    boolean dominatorMet = false;
                    for (final var el : path) {
                        if (dominatorMet && el.contains(BPMNGateway.Category.SPLIT.toString())) {
                            gs.add(el);
                        } else if (el.equals(minimalDominator)) {
                            dominatorMet = true;
                        }
                    }
                }

                final var es = new HashSet<Pair<String, String>>();
                for (final var edge : dfgEdges) {
                    if (gs.contains(edge.getLeft())) {
                        es.add(edge);
                    }
                }

                final var t = new HashMap<Pair<String, String>, Set<Pair<String, String>>>();

                final var semantic = checkOrJoinSemantic(dfgEdges, joinPaths, gs, es, t);
                if (semantic != null && !semantic.equals(BPMNGateway.Type.OR.toString())) {
                    final var newSemanticGateway = new BPMNGateway(BPMNGateway.Type.valueOf(semantic), BPMNGateway.Category.JOIN).toString();
                    final var oldSemanticEdges = new HashSet<Pair<String, String>>();
                    final var newSemanticEdges = new HashSet<Pair<String, String>>();
                    for (final var edge : dfgEdges) {
                        if (edge.getLeft().equals(orJoin)) {
                            newSemanticEdges.add(Pair.of(newSemanticGateway, edge.getRight()));
                            oldSemanticEdges.add(edge);
                        }
                        if (edge.getRight().equals(orJoin)) {
                            newSemanticEdges.add(Pair.of(edge.getLeft(), newSemanticGateway));
                            oldSemanticEdges.add(edge);
                        }
                    }
                    dfgNodes.remove(orJoin);
                    dfgNodes.add(newSemanticGateway);
                    dfgEdges.removeAll(oldSemanticEdges);
                    dfgEdges.addAll(newSemanticEdges);
                }
            }
        }
    }

    static String checkOrJoinSemantic(Set<Pair<String, String>> dfgEdges,
                                      List<List<String>> joinPaths,
                                      Set<String> gs,
                                      Set<Pair<String, String>> es,
                                      Map<Pair<String, String>, Set<Pair<String, String>>> t) {
        String semantic = null;
        for (final var e : es) {
            final var incomingGatewayEdges = new HashSet<Pair<String, String>>();
            for (final var path : joinPaths) {
                var reachesGateway = false;
                for (var i = 0; i < path.size() - 1; i++) {
                    if (path.get(i).equals(e.getLeft()) && path.get(i + 1).equals(e.getRight())) {
                        reachesGateway = true;
                        break;
                    }
                }
                if (reachesGateway) {
                    incomingGatewayEdges.add(Pair.of(path.get(path.size() - 2), path.get(path.size() - 1)));
                }
            }
            t.put(e, incomingGatewayEdges);
            if (incomingGatewayEdges.isEmpty() && isXorSplit(e.getLeft())) {
                semantic = BPMNGateway.Type.XOR.toString();
            }
        }

        for (final var g : gs) {
            final var outgoingEdges = getDfgNodeOutgoingEdges(g, dfgEdges);

            for (final var e1 : outgoingEdges) {
                for (final var e2 : outgoingEdges) {
                    final var i = new HashSet<>(t.get(e1));
                    i.retainAll(t.get(e2));

                    final var s1 = new HashSet<>(t.get(e1));
                    s1.removeAll(i);

                    final var s2 = new HashSet<>(t.get(e2));
                    s2.removeAll(i);

                    if (!s1.isEmpty() && !s2.isEmpty()) {
                        final var semanticOfG = checkOrJoinSemantic(g);
                        if (semantic != null && !semantic.equals(semanticOfG)) {
                            return BPMNGateway.Type.OR.toString();
                        } else {
                            semantic = semanticOfG;
                        }
                    }
                }
            }
        }
        return semantic;
    }

    static Set<Pair<String, String>> getDfgNodeIncomingEdges(String node, Set<Pair<String, String>> edges) {
        return edges.stream().filter(entry -> entry.getRight().equals(node)).collect(Collectors.toSet());
    }

    static Set<Pair<String, String>> getDfgNodeOutgoingEdges(String node, Set<Pair<String, String>> edges) {
        return edges.stream().filter(entry -> entry.getLeft().equals(node)).collect(Collectors.toSet());
    }

    static boolean isXorSplit(String node) {
        return node.startsWith(format("%s:%s", BPMNGateway.Type.XOR, BPMNGateway.Category.SPLIT));
    }

    static boolean isAndSplit(String node) {
        return node.startsWith(format("%s:%s", BPMNGateway.Type.AND, BPMNGateway.Category.SPLIT));
    }

    static boolean isGateway(String node) {
        return node.startsWith(BPMNGateway.Type.XOR.toString()) || node.startsWith(BPMNGateway.Type.AND.toString()) || node.startsWith(BPMNGateway.Type.OR.toString());
    }

    static boolean isBackedge(Pair<String, String> edge, Set<Pair<String, String>> dfgEdges) {
        final var entry = edge.getRight();
        final var exit = edge.getLeft();
        final var toVisit = new Stack<String>();
        final var visited = new HashSet<String>();
        toVisit.push(entry);

        while (!toVisit.isEmpty()) {
            final var current = toVisit.pop();
            if (current.equals(exit)) {
                return true;
            }
            visited.add(current);
            for (final var outgoing : getDfgNodeOutgoingEdges(current, dfgEdges)) {
                var successor = outgoing.getRight();
                if (!visited.contains(successor)) {
                    toVisit.push(successor);
                }
            }
        }
        return false;
    }

    static String checkOrJoinSemantic(String node) {
        if (node.startsWith(BPMNGateway.Type.XOR.toString())) {
            return BPMNGateway.Type.XOR.toString();
        }
        if (node.startsWith(BPMNGateway.Type.AND.toString())) {
            return BPMNGateway.Type.AND.toString();
        }
        throw new RuntimeException(String.format("Unknown semantic of node: %s", node));
    }
}
