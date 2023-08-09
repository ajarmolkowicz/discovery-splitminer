package beamline.miners.splitminer.bpmn;

import beamline.miners.splitminer.dfg.DFG;
import beamline.miners.splitminer.dfg.FilteredPDFG;
import beamline.miners.splitminer.dfg.PrunedDFG;
import beamline.miners.splitminer.view.CamundaBPMNDiagramGenerator;
import beamline.miners.splitminer.view.PaliaLikeBPMNDiagramGenerator;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BPMNTemplateTest {

    @Test
    void test() {
        //GIVEN
        final var fpdgf = new FilteredPDFG(
                Set.of("A"),
                Set.of("H"),
                Set.of("A", "B", "C", "D", "E", "F", "G", "H"),
                Set.of(Pair.of("A", "B"),
                        Pair.of("A", "C"),
                        Pair.of("A", "D"),
                        Pair.of("B", "E"),
                        Pair.of("B", "F"),
                        Pair.of("C", "G"),
                        Pair.of("D", "G"),
                        Pair.of("E", "H"),
                        Pair.of("F", "G"),
                        Pair.of("G", "H")
                ),
                Set.of(
                        Pair.of("B", "C"),
                        Pair.of("C", "B"),
                        Pair.of("B", "D"),
                        Pair.of("D", "B"),
                        Pair.of("D", "E"),
                        Pair.of("E", "D"),
                        Pair.of("E", "G"),
                        Pair.of("G", "E")
                )
        );

        //WHEN
        final var bpmnTemplate = new BPMNTemplate(fpdgf);

        //THEN
        assertThat(BPMNTestUtils.simplifyNodes(bpmnTemplate.getActivities())).containsExactlyInAnyOrder("A", "B", "C", "D", "E", "F", "G", "H");
        assertThat(bpmnTemplate.getAssociations()).hasSize(18);
        assertThat(BPMNTestUtils.simplifyAssociations(bpmnTemplate.getAssociations())).containsExactlyInAnyOrder(
                Pair.of(bpmnTemplate.getStartEvent().simplify(), "A"),
                Pair.of("A", "AND:SPLIT"),
                Pair.of("AND:SPLIT", "B"),
                Pair.of("AND:SPLIT", "XOR:SPLIT"),
                Pair.of("B", "XOR:SPLIT"),
                Pair.of("XOR:SPLIT", "E"),
                Pair.of("XOR:SPLIT", "F"),
                Pair.of("E", "XOR:JOIN"),
                Pair.of("F", "OR:JOIN"),
                Pair.of("XOR:SPLIT", "C"),
                Pair.of("XOR:SPLIT", "D"),
                Pair.of("C", "XOR:JOIN"),
                Pair.of("D", "XOR:JOIN"),
                Pair.of("XOR:JOIN", "OR:JOIN"),
                Pair.of("OR:JOIN", "G"),
                Pair.of("XOR:JOIN", "H"),
                Pair.of("G", "XOR:JOIN"),
                Pair.of("H", bpmnTemplate.getEndEvent().simplify())

        );
    }

    @Test
    void splits() {
        //GIVEN
        final var fpdfg = new FilteredPDFG(
                Set.of("z"),
                Set.of("a", "b", "c", "d", "e", "f", "g"),
                Set.of("z", "a", "b", "c", "d", "e", "f", "g"),
                Set.of(
                        Pair.of("z", "a"),
                        Pair.of("z", "b"),
                        Pair.of("z", "c"),
                        Pair.of("z", "d"),
                        Pair.of("z", "e"),
                        Pair.of("z", "f"),
                        Pair.of("z", "g")
                ),
                Set.of(
                        Pair.of("e", "c"),
                        Pair.of("e", "d"),
                        Pair.of("e", "f"),

                        Pair.of("f", "e"),
                        Pair.of("f", "c"),
                        Pair.of("f", "d"),

                        Pair.of("c", "e"),
                        Pair.of("c", "d"),
                        Pair.of("c", "f"),
                        Pair.of("c", "g"),

                        Pair.of("d", "e"),
                        Pair.of("d", "c"),
                        Pair.of("d", "f"),
                        Pair.of("d", "g"),

                        Pair.of("g", "c"),
                        Pair.of("g", "d")

                )
        );

        //WHEN
        final var bpmnTemplate = new BPMNTemplate(fpdfg);

        //THEN
        assertThat(BPMNTestUtils.simplifyAssociations(bpmnTemplate.getAssociations())).contains(
                Pair.of("z", "XOR:SPLIT"),
                Pair.of("XOR:SPLIT", "XOR:SPLIT"),
                Pair.of("XOR:SPLIT", "AND:SPLIT"),
                Pair.of("XOR:SPLIT", "a"),
                Pair.of("XOR:SPLIT", "b"),
                Pair.of("AND:SPLIT", "AND:SPLIT"),
                Pair.of("AND:SPLIT", "XOR:SPLIT"),
                Pair.of("AND:SPLIT", "c"),
                Pair.of("AND:SPLIT", "d"),
                Pair.of("XOR:SPLIT", "AND:SPLIT"),
                Pair.of("AND:SPLIT", "e"),
                Pair.of("AND:SPLIT", "f"),
                Pair.of("XOR:SPLIT", "g")
        );
    }

    @Test
    void cycle() {
        //GIVEN
        final var fpdgf = new FilteredPDFG(
                Set.of("A"),
                Set.of("H"),
                Set.of("A", "B", "C", "D", "E", "F", "G", "H"),
                Set.of(Pair.of("A", "B"),
                        Pair.of("A", "C"),
                        Pair.of("A", "D"),
                        Pair.of("B", "E"),
                        Pair.of("B", "F"),
                        Pair.of("C", "G"),
                        Pair.of("D", "G"),
                        Pair.of("E", "H"),
                        Pair.of("F", "G"),
                        Pair.of("G", "B"), // -- forms cycle
                        Pair.of("G", "H")
                ),
                Set.of(
                        Pair.of("B", "C"),
                        Pair.of("C", "B"),
                        Pair.of("B", "D"),
                        Pair.of("D", "B"),
                        Pair.of("D", "E"),
                        Pair.of("E", "D"),
                        Pair.of("E", "G"),
                        Pair.of("G", "E")
                )
        );

        //WHEN
        final var bpmnTemplate = new BPMNTemplate(fpdgf);

        //THEN
        assertThat(BPMNTestUtils.simplifyNodes(bpmnTemplate.getActivities())).containsExactlyInAnyOrder("A", "B", "C", "D", "E", "F", "G", "H");
        assertThat(bpmnTemplate.getAssociations()).hasSize(21);
        assertThat(BPMNTestUtils.simplifyAssociations(bpmnTemplate.getAssociations())).containsExactlyInAnyOrder(
                Pair.of(bpmnTemplate.getStartEvent().simplify(), "A"),
                Pair.of("A", "AND:SPLIT"),
                Pair.of("AND:SPLIT", "XOR:JOIN"), // --- because of cycle
                Pair.of("XOR:JOIN", "B"), // --- because of cycle
                Pair.of("AND:SPLIT", "XOR:SPLIT"),
                Pair.of("B", "XOR:SPLIT"),
                Pair.of("XOR:SPLIT", "E"),
                Pair.of("XOR:SPLIT", "F"),
                Pair.of("E", "XOR:JOIN"),
                Pair.of("F", "OR:JOIN"),
                Pair.of("XOR:SPLIT", "C"),
                Pair.of("XOR:SPLIT", "D"),
                Pair.of("C", "XOR:JOIN"),
                Pair.of("D", "XOR:JOIN"),
                Pair.of("XOR:JOIN", "OR:JOIN"),
                Pair.of("OR:JOIN", "G"),
                Pair.of("XOR:JOIN", "H"),
                Pair.of("G", "XOR:SPLIT"), // --- because of cycle
                Pair.of("XOR:SPLIT", "XOR:JOIN"), // --- because of cycle
                Pair.of("XOR:SPLIT", "XOR:JOIN"), // --- because of cycle
                Pair.of("H", bpmnTemplate.getEndEvent().simplify())

        );
    }

    @Test
    void replaceORJoins() {
        //GIVEN
        final var xorSplit = new BPMNGateway(BPMNGateway.Type.XOR, BPMNGateway.Category.SPLIT).toString();
        final var andSplit1 = new BPMNGateway(BPMNGateway.Type.AND, BPMNGateway.Category.SPLIT).toString();
        final var andSplit2 = new BPMNGateway(BPMNGateway.Type.AND, BPMNGateway.Category.SPLIT).toString();
        final var orJoin1 = new BPMNGateway(BPMNGateway.Type.OR, BPMNGateway.Category.JOIN).toString();
        final var orJoin2 = new BPMNGateway(BPMNGateway.Type.OR, BPMNGateway.Category.JOIN).toString();
        final var orJoin3 = new BPMNGateway(BPMNGateway.Type.OR, BPMNGateway.Category.JOIN).toString();

        final var dfgNodes = new HashSet<>(Set.of("A", "B", "C", "D", "E", "F", "G", "H", xorSplit, andSplit1, andSplit2, orJoin1, orJoin2, orJoin3));

        final var dfgEdges = new HashSet<>(Set.of(
                Pair.of("A", xorSplit),
                Pair.of(xorSplit, "B"),
                Pair.of(xorSplit, "C"),

                Pair.of("B", andSplit1),
                Pair.of(andSplit1, orJoin2),
                Pair.of(andSplit1, "D"),
                Pair.of("D", orJoin1),
                Pair.of(orJoin1, "F"),

                Pair.of("C", andSplit2),
                Pair.of(andSplit2, orJoin1),
                Pair.of(andSplit2, "E"),
                Pair.of("E", orJoin2),
                Pair.of(orJoin2, "G"),

                Pair.of("F", orJoin3),
                Pair.of("G", orJoin3),
                Pair.of(orJoin3, "H")
        ));

        //WHEN
        BPMNTemplate.replaceORJoins("A", dfgNodes, dfgEdges);

        //THEN
        final var andJoin = dfgNodes.stream()
                .filter(node -> node.startsWith("AND:JOIN"))
                .findFirst().orElseThrow(() -> new RuntimeException("No AND JOIN found"));
        final var xorJoin1 = dfgNodes.stream()
                .filter(node -> node.startsWith("XOR:JOIN"))
                .filter(node -> dfgEdges.contains(Pair.of("D", node)))
                .findFirst().orElseThrow(() -> new RuntimeException("No AND JOIN found"));
        final var xorJoin2 = dfgNodes.stream()
                .filter(node -> node.startsWith("XOR:JOIN"))
                .filter(node -> dfgEdges.contains(Pair.of("E", node)))
                .findFirst().orElseThrow(() -> new RuntimeException("No AND JOIN found"));
        assertThat(dfgEdges).containsExactlyInAnyOrder(
                Pair.of("A", xorSplit),
                Pair.of(xorSplit, "B"),
                Pair.of(xorSplit, "C"),

                Pair.of("B", andSplit1),
                Pair.of(andSplit1, xorJoin2),
                Pair.of(andSplit1, "D"),
                Pair.of("D", xorJoin1),
                Pair.of(xorJoin1, "F"),

                Pair.of("C", andSplit2),
                Pair.of(andSplit2, xorJoin1),
                Pair.of(andSplit2, "E"),
                Pair.of("E", xorJoin2),
                Pair.of(xorJoin2, "G"),

                Pair.of("F", andJoin),
                Pair.of("G", andJoin),
                Pair.of(andJoin, "H")
        );
    }

    @Test
    void holistic() {
        //GIVEN
        final var edges = new HashMap<Pair<String, String>, Integer>();
        edges.put(Pair.of("A", "B"), 60);
        edges.put(Pair.of("A", "C"), 20);
        edges.put(Pair.of("A", "D"), 20);
        edges.put(Pair.of("B", "C"), 20);
        edges.put(Pair.of("B", "D"), 20);
        edges.put(Pair.of("B", "E"), 40);
        edges.put(Pair.of("B", "F"), 20);
        edges.put(Pair.of("C", "B"), 20);
        edges.put(Pair.of("C", "F"), 10);
        edges.put(Pair.of("C", "G"), 20);
        edges.put(Pair.of("D", "B"), 20);
        edges.put(Pair.of("D", "E"), 10);
        edges.put(Pair.of("D", "G"), 20);
        edges.put(Pair.of("E", "C"), 10);
        edges.put(Pair.of("E", "D"), 10);
        edges.put(Pair.of("E", "G"), 30);
        edges.put(Pair.of("E", "H"), 20);
        edges.put(Pair.of("F", "G"), 30);
        edges.put(Pair.of("G", "E"), 20);
        edges.put(Pair.of("G", "H"), 80);
        final var pdfg = new PrunedDFG(new DFG(Set.of("A"), Set.of("H"), edges, new HashSet<>(), new HashSet<>(), 1.0));

        //WHEN
        final var bpmnTemplate = new BPMNTemplate(new FilteredPDFG(pdfg, 1));

        //THEN
        assertThat(BPMNTestUtils.simplifyNodes(bpmnTemplate.getActivities())).containsExactlyInAnyOrder("A", "B", "C", "D", "E", "F", "G", "H");
        assertThat(bpmnTemplate.getAssociations()).hasSize(18);
        assertThat(BPMNTestUtils.simplifyAssociations(bpmnTemplate.getAssociations())).containsExactlyInAnyOrder(
                Pair.of(bpmnTemplate.getStartEvent().simplify(), "A"),
                Pair.of("A", "AND:SPLIT"),
                Pair.of("AND:SPLIT", "B"),
                Pair.of("AND:SPLIT", "XOR:SPLIT"),
                Pair.of("B", "XOR:SPLIT"),
                Pair.of("XOR:SPLIT", "E"),
                Pair.of("XOR:SPLIT", "F"),
                Pair.of("E", "XOR:JOIN"),
                Pair.of("F", "OR:JOIN"),
                Pair.of("XOR:SPLIT", "C"),
                Pair.of("XOR:SPLIT", "D"),
                Pair.of("C", "XOR:JOIN"),
                Pair.of("D", "XOR:JOIN"),
                Pair.of("XOR:JOIN", "OR:JOIN"),
                Pair.of("OR:JOIN", "G"),
                Pair.of("XOR:JOIN", "H"),
                Pair.of("G", "XOR:JOIN"),
                Pair.of("H", bpmnTemplate.getEndEvent().simplify())
        );
    }
}