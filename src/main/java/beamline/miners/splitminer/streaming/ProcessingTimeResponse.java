package beamline.miners.splitminer.streaming;

import beamline.models.responses.Response;

import java.util.Map;

public class ProcessingTimeResponse extends Response {
    private static final long serialVersionUID = 703561441785918416L;
    private final Map<Long, Integer> usage;

    public ProcessingTimeResponse(Map<Long, Integer> usage) {
        this.usage = usage;
    }

    public Map<Long, Integer> getUsage() {
        return usage;
    }
}
