package beamline.miners.splitminer.dfg;

import beamline.events.BEvent;
import beamline.exceptions.EventException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DFGTest {
    @Test
    void edges() throws EventException {
        //GIVEN
        final var events = List.of(
                BEvent.create("Process1", "Case1", "A"),
                BEvent.create("Process1", "Case1", "B"),
                BEvent.create("Process1", "Case1", "C"),
                BEvent.create("Process1", "Case1", "D"),

                BEvent.create("Process1", "Case2", "A"),
                BEvent.create("Process1", "Case2", "C"),
                BEvent.create("Process1", "Case2", "B"),
                BEvent.create("Process1", "Case2", "D"),

                BEvent.create("Process1", "Case3", "A"),
                BEvent.create("Process1", "Case3", "B"),
                BEvent.create("Process1", "Case3", "C"),
                BEvent.create("Process1", "Case3", "D"),

                BEvent.create("Process1", "Case4", "A"),
                BEvent.create("Process1", "Case4", "C"),
                BEvent.create("Process1", "Case4", "B"),
                BEvent.create("Process1", "Case4", "D"),

                BEvent.create("Process1", "Case5", "E"),
                BEvent.create("Process1", "Case5", "F")
        );

        //WHEN
        final var dfg = new DFG(events, 0.08);
        dfg.buildEdges();

        //THEN
        var expected = new HashMap<Pair<String, String>, Integer>();
        expected.put(Pair.of("A", "B"), 2);
        expected.put(Pair.of("A", "C"), 2);
        expected.put(Pair.of("B", "C"), 2);
        expected.put(Pair.of("C", "B"), 2);
        expected.put(Pair.of("C", "D"), 2);
        expected.put(Pair.of("B", "D"), 2);
        expected.put(Pair.of("E", "F"), 1);
        assertThat(dfg.getEdges()).containsExactlyInAnyOrderEntriesOf(expected);
    }


    @Test
    void loops() throws EventException {
        //GIVEN
        final var events = List.of(
                BEvent.create("Process1", "Case1", "A"),
                BEvent.create("Process1", "Case1", "A"),
                BEvent.create("Process1", "Case1", "B"),
                BEvent.create("Process1", "Case1", "A"),
                BEvent.create("Process1", "Case1", "C"),
                BEvent.create("Process1", "Case1", "D"),
                BEvent.create("Process1", "Case1", "C"),
                BEvent.create("Process1", "Case1", "D"),
                BEvent.create("Process1", "Case1", "C"),
                BEvent.create("Process1", "Case1", "D"),
                BEvent.create("Process1", "Case1", "C"),
                BEvent.create("Process1", "Case1", "D"),

                BEvent.create("Process1", "Case2", "A"),
                BEvent.create("Process1", "Case2", "C"),
                BEvent.create("Process1", "Case2", "B"),
                BEvent.create("Process1", "Case2", "D"),
                BEvent.create("Process1", "Case2", "B"),
                BEvent.create("Process1", "Case2", "D"),
                BEvent.create("Process1", "Case2", "B"),

                BEvent.create("Process1", "Case3", "A"),
                BEvent.create("Process1", "Case3", "A"),
                BEvent.create("Process1", "Case3", "B"),
                BEvent.create("Process1", "Case3", "A"),
                BEvent.create("Process1", "Case3", "C"),
                BEvent.create("Process1", "Case3", "D"),

                BEvent.create("Process1", "Case4", "A"),
                BEvent.create("Process1", "Case4", "A"),
                BEvent.create("Process1", "Case4", "C"),
                BEvent.create("Process1", "Case4", "B"),
                BEvent.create("Process1", "Case4", "D"),

                BEvent.create("Process1", "Case5", "E"),
                BEvent.create("Process1", "Case5", "E"),
                BEvent.create("Process1", "Case5", "F")
        );

        //WHEN
        final var dfg = new DFG(events, 0.08);
        dfg.buildEdges();
        dfg.findLoops();

        //THEN
        assertThat(dfg.getSelfLoops()).containsExactlyInAnyOrder(
                Pair.of("A", "A"),
                Pair.of("E", "E")
        );

        assertThat(dfg.getShortLoops()).containsExactlyInAnyOrder(
                Pair.of("A", "B"),
                Pair.of("C", "D"),
                Pair.of("D", "C"),
                Pair.of("B", "D"),
                Pair.of("D", "B")
        );
    }

    @Test
    void concurrency() throws EventException {
        //GIVEN
        final var events = List.of(
                BEvent.create("Process1", "Case1", "A"),
                BEvent.create("Process1", "Case1", "A"),
                BEvent.create("Process1", "Case1", "B"),
                BEvent.create("Process1", "Case1", "C"),
                BEvent.create("Process1", "Case1", "D"),
                BEvent.create("Process1", "Case1", "E"),

                BEvent.create("Process1", "Case2", "A"),
                BEvent.create("Process1", "Case2", "C"),
                BEvent.create("Process1", "Case2", "B"),
                BEvent.create("Process1", "Case2", "D"),
                BEvent.create("Process1", "Case2", "E"),

                BEvent.create("Process1", "Case3", "A"),
                BEvent.create("Process1", "Case3", "B"),
                BEvent.create("Process1", "Case3", "C"),
                BEvent.create("Process1", "Case3", "D"),
                BEvent.create("Process1", "Case3", "E"),

                BEvent.create("Process1", "Case4", "A"),
                BEvent.create("Process1", "Case4", "A"),
                BEvent.create("Process1", "Case4", "C"),
                BEvent.create("Process1", "Case4", "B"),
                BEvent.create("Process1", "Case4", "D"),

                BEvent.create("Process1", "Case5", "E"),
                BEvent.create("Process1", "Case5", "D"),
                BEvent.create("Process1", "Case5", "F"),

                BEvent.create("Process1", "Case6", "X"),
                BEvent.create("Process1", "Case6", "Y"),
                BEvent.create("Process1", "Case6", "X")
        );

        //WHEN
        final var dfg = new DFG(events, 0.08);
        dfg.buildEdges();
        dfg.findLoops();
        dfg.findConcurrentAndInfrequentEdges(0.5);

        //THEN
        assertThat(dfg.getSelfLoops()).containsExactlyInAnyOrder(
                Pair.of("A", "A")
        );
        assertThat(dfg.getShortLoops()).containsExactlyInAnyOrder(
                Pair.of("X", "Y")
        );

        assertThat(dfg.getConcurrentEdges()).containsExactlyInAnyOrderEntriesOf(Map.of(
                Pair.of("B", "C"), 2,
                Pair.of("C", "B"), 2
        ));
        assertThat(dfg.getInfrequentEdges()).containsExactlyInAnyOrderEntriesOf(Map.of(
                Pair.of("E", "D"), 1
        ));
    }
}