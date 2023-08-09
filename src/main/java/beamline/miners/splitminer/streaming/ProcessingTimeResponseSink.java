package beamline.miners.splitminer.streaming;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.PrintWriter;

@Slf4j
public class ProcessingTimeResponseSink implements SinkFunction<ProcessingTimeResponse> {
    private final String sourceFile;
    private final String outputDirectory;
    private final String mode;


    public ProcessingTimeResponseSink(String sourceFile, String outputDirectory, String mode) {
        this.sourceFile = sourceFile;
        this.outputDirectory = outputDirectory;
        this.mode = mode;
    }

    @Override
    public void invoke(ProcessingTimeResponse value, Context context) {
        log.info("Creating output file: {}", outputDirectory + sourceFile + "-time-" + mode + ".json");
        final var json = new JSONObject(value.getUsage());
        try (var out = new PrintWriter(new FileWriter(outputDirectory + sourceFile + "-time-" + mode + ".json"))) {
            out.write(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
