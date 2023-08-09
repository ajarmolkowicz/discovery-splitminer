package beamline.miners.splitminer.streaming;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MemoryUsageResponseSink implements SinkFunction<MemoryUsageResponse> {
    private final String sourceFile;
    private final String outputDirectory;
    private final String mode;

    public MemoryUsageResponseSink(String sourceFile, String outputDirectory, String mode) {
        this.sourceFile = sourceFile;
        this.outputDirectory = outputDirectory;
        this.mode = mode;
    }

    @Override
    public void invoke(MemoryUsageResponse value, Context context) {
        log.info("Creating output file: {}", outputDirectory + sourceFile + "-memory-" + mode + ".json");
        final var json = new JSONObject(value.getUsage());
        try (var out = new PrintWriter(new FileWriter(outputDirectory + sourceFile + "-memory-" + mode + ".json"))) {
            out.write(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
