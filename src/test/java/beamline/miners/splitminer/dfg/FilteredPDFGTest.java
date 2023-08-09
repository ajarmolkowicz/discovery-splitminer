package beamline.miners.splitminer.dfg;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FilteredPDFGTest {
    @Test
    void filter() {
        //GIVEN
        final var edges = new HashMap<Pair<String, String>, Integer>();
        edges.put(Pair.of("A", "B"), 60);
        edges.put(Pair.of("A", "C"), 20);
        edges.put(Pair.of("A", "D"), 20);
        edges.put(Pair.of("B", "E"), 40);
        edges.put(Pair.of("B", "F"), 20);
        edges.put(Pair.of("C", "F"), 10);
        edges.put(Pair.of("C", "G"), 20);
        edges.put(Pair.of("D", "G"), 20);
        edges.put(Pair.of("E", "C"), 10);
        edges.put(Pair.of("E", "H"), 20);
        edges.put(Pair.of("F", "G"), 30);
        edges.put(Pair.of("G", "H"), 80);

        final var pdfg = new PrunedDFG(new DFG(Set.of("A"), Set.of("H"), edges, new HashSet<>(), new HashSet<>(), 0.5));


        //WHEN
        var filtered = new FilteredPDFG(pdfg, 0.6);

        //THEN
        assertThat(filtered.getEdges()).containsExactlyInAnyOrder(
                Pair.of("A", "B"),
                Pair.of("A", "C"),
                Pair.of("A", "D"),
                Pair.of("B", "E"),
                Pair.of("B", "F"),
                Pair.of("C", "G"),
                Pair.of("D", "G"),
                Pair.of("E", "H"),
                Pair.of("F", "G"),
                Pair.of("G", "H")
        );
    }

}