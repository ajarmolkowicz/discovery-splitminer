package beamline.miners.splitminer.streaming.lossycounting;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.stream.Collectors;

class LossyCountingRelations extends LossyCounting<Pair<String, String>> {
    void addObservation(String latestActivity, String activity, int currentBucket) {
        addObservation(Pair.of(latestActivity, activity), currentBucket);
    }

    Map<Pair<String, String>, Integer> getRelations() {
        return entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getKey()));
    }
}
