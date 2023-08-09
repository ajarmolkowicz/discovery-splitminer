package beamline.miners.splitminer.streaming.lossycounting;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;

abstract class LossyCounting<V> extends HashMap<V, Pair<Integer, Integer>> {
    protected void addObservation(V newValue, int currentBucket) {
        if (containsKey(newValue)) {
            final var value = get(newValue);
            put(newValue, Pair.of(value.getLeft() + 1, value.getRight()));
        } else {
            put(newValue, Pair.of(1, currentBucket - 1));
        }
    }

    void cleanup(int currentBucket) {
        final var toClean = new HashSet<V>();
        for (var entry : entrySet()) {
            if (entry.getValue().getLeft() + entry.getValue().getRight() < currentBucket) {
                toClean.add(entry.getKey());
            }
        }
        for (var entry : toClean) {
            remove(entry);
        }
    }
}
