package beamline.miners.splitminer.view;

import beamline.miners.splitminer.bpmn.BPMNTemplate;
import beamline.miners.splitminer.dfg.FilteredPDFG;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static beamline.miners.splitminer.view.CamundaBPMNDiagramGenerator.fromBPMNTemplate;

class CamundaBPMNGeneratorTest {
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

        //WHEN, THEN
        final var bpmnTemplate = new BPMNTemplate(fpdgf);
    }

}