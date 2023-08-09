package beamline.miners.splitminer.streaming;

import beamline.models.responses.Response;

import java.util.Map;

public class MemoryUsageResponse extends Response {
    private static final long serialVersionUID = 234897463529846532L;
    private final Map<Long, Integer> usage;

    public MemoryUsageResponse(Map<Long, Integer> usage) {
        this.usage = usage;
    }

    public Map<Long, Integer> getUsage() {
        return usage;
    }
}
