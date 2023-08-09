package beamline.miners.splitminer.dfg;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PrunedDFGTest {
    @Test
    void fromDFG() {
        //GIVEN
        final var edges = new HashMap<Pair<String, String>, Integer>();
        edges.put(Pair.of("A", "A"), 2);
        edges.put(Pair.of("A", "B"), 2);
        edges.put(Pair.of("A", "C"), 1);
        edges.put(Pair.of("B", "C"), 2);
        edges.put(Pair.of("C", "B"), 2);
        edges.put(Pair.of("B", "D"), 2);
        edges.put(Pair.of("C", "D"), 2);
        edges.put(Pair.of("D", "E"), 3);
        edges.put(Pair.of("E", "D"), 1);
        edges.put(Pair.of("D", "F"), 1);
        edges.put(Pair.of("E", "F"), 1);
        edges.put(Pair.of("X", "Y"), 1);
        edges.put(Pair.of("Y", "X"), 1);

        final var selfLoops = new HashSet<Pair<String, String>>();
        selfLoops.add(Pair.of("A", "A"));

        final var shortLoops = new HashSet<Pair<String, String>>();
        shortLoops.add(Pair.of("X", "Y"));

        final var dfg = new DFG(Set.of("MOCK-SOURCE"), Set.of("MOCK-SINK"), edges, selfLoops, shortLoops, 0.5);

        //WHEN
        final var pdfg = new PrunedDFG(dfg);

        //THEN
        final var expected = new HashMap<Pair<String, String>, Integer>();
        expected.put(Pair.of("A", "B"), 2);
        expected.put(Pair.of("A", "C"), 1);
        expected.put(Pair.of("B", "D"), 2);
        expected.put(Pair.of("C", "D"), 2);
        expected.put(Pair.of("D", "E"), 3);
        expected.put(Pair.of("D", "F"), 1);
        expected.put(Pair.of("E", "F"), 1);

        assertThat(pdfg.getEdges()).containsExactlyInAnyOrderEntriesOf(expected);
    }

}