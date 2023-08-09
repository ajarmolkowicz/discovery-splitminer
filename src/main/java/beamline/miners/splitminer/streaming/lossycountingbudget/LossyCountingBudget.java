package beamline.miners.splitminer.streaming.lossycountingbudget;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;

abstract class LossyCountingBudget<V> extends HashMap<V, Pair<Integer, Integer>> {
    private int currentBucket = 0;
    protected int budget = -1;

    protected void addObservation(V newValue) {
        if (containsKey(newValue)) {
            final var value = get(newValue);
            put(newValue, Pair.of(value.getLeft() + 1, value.getRight()));
        } else {
            if (size() == budget) {
                cleanup();
            }
            put(newValue, Pair.of(1, currentBucket));
        }
    }

    void cleanup() {
        currentBucket++;
        final var toClean = new HashSet<V>();
        for (var entry : entrySet()) {
            if (entry.getValue().getLeft() + entry.getValue().getRight() < currentBucket) {
                toClean.add(entry.getKey());
            }
        }
        if (toClean.isEmpty()) {
            var newBucket = Integer.MAX_VALUE;
            for (var entry : entrySet()) {
                final var candidate = entry.getValue().getLeft() + entry.getValue().getRight();
                if (candidate < newBucket) {
                    newBucket = candidate;
                }
            }
            currentBucket = newBucket;
            cleanup();
        } else {
            for (var entry : toClean) {
                remove(entry);
            }
        }
    }
}
