package beamline.miners.splitminer.streaming.lossycountingbudget;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.stream.Collectors;

class LossyCountingBudgetRelations extends LossyCountingBudget<Pair<String, String>> {

    public LossyCountingBudgetRelations(int budget) {
        this.budget = budget;
    }

    void addObservation(String latestActivity, String activity) {
        addObservation(Pair.of(latestActivity, activity));
    }

    Map<Pair<String, String>, Integer> getRelations() {
        return entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getKey()));
    }
}
