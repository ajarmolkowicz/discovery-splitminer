package beamline.miners.splitminer.streaming.lossycounting;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class LossyCountingCases extends HashMap<String, Pair<Pair<LossyCountingCases.Case, Integer>, Integer>> {
    String addObservation(String activity, String caseId, int currentBucket) {
        if (containsKey(caseId)) {
            final var value = get(caseId);
            final var caze = value.getLeft().getLeft();
            caze.shift(activity);
            put(caseId, Pair.of(Pair.of(caze, value.getLeft().getRight() + 1), value.getRight()));
            return caze.getSecondToLastActivity();
        } else {
            put(caseId, Pair.of(Pair.of(Case.initial(activity), 1), currentBucket - 1));
        }
        return null;
    }

    void cleanup(int currentBucket) {
        final var toClean = new HashSet<>();
        for (final var entry : entrySet()) {
            if (entry.getValue().getLeft().getRight() + entry.getValue().getRight() < currentBucket) {
                toClean.add(entry.getKey());
            }
        }
        for (final var entry : toClean) {
            remove(entry);
        }
    }

    Set<String> getStartingActivities() {
        return values().stream().map(value -> value.getLeft().getLeft().getStartingActivity()).collect(Collectors.toSet());
    }

    Set<String> getFinishingActivities() {
        return values().stream().map(value -> value.getLeft().getLeft().getLatestActivity()).collect(Collectors.toSet());
    }

    Set<Pair<String, String>> getSelfLoops() {
        return values().stream().flatMap(value -> value.getLeft().getLeft().getSelfLoops().stream()).collect(Collectors.toSet());
    }

    Set<Pair<String, String>> getShortLoops() {
        return values().stream().flatMap(value -> value.getLeft().getLeft().getShortLoops().stream()).collect(Collectors.toSet());
    }
    static class Case {
        String startingActivity;
        String secondToLastActivity;
        String latestActivity;

        Set<Pair<String, String>> shortLoops;
        Set<Pair<String, String>> selfLoops;

        public Case(String startingActivity, String secondToLastActivity, String latestActivity, Set<Pair<String, String>> shortLoops, Set<Pair<String, String>> selfLoops) {
            this.startingActivity = startingActivity;
            this.secondToLastActivity = secondToLastActivity;
            this.latestActivity = latestActivity;
            this.shortLoops = shortLoops;
            this.selfLoops = selfLoops;
        }

        static Case initial(String firstActivityInCase) {
            return new Case(firstActivityInCase, null, firstActivityInCase, new HashSet<>(), new HashSet<>());
        }

        void shift(String newActivity) {
            if (secondToLastActivity != null && secondToLastActivity.equals(newActivity)) {
                shortLoops.add(Pair.of(secondToLastActivity, latestActivity));
            }
            if (latestActivity != null && latestActivity.equals(newActivity)) {
                selfLoops.add(Pair.of(latestActivity, newActivity));
            }
            this.secondToLastActivity = latestActivity;
            this.latestActivity = newActivity;
        }

        String getStartingActivity() {
            return startingActivity;
        }

        public String getSecondToLastActivity() {
            return secondToLastActivity;
        }

        String getLatestActivity() {
            return latestActivity;
        }

        Set<Pair<String, String>> getShortLoops() {
            return shortLoops;
        }

        Set<Pair<String, String>> getSelfLoops() {
            return selfLoops;
        }
    }
}
