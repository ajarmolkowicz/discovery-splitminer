package beamline.miners.splitminer.streaming;

import beamline.miners.splitminer.view.BPMNDiagramGenerator;
import beamline.miners.splitminer.view.CamundaBPMNDiagramGenerator;
import beamline.miners.splitminer.view.PaliaLikeBPMNDiagramGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.io.Serializable;

import static java.lang.String.format;

@Slf4j
public class BPMNTemplateResponseSink implements SinkFunction<BPMNTemplateResponse>, Serializable {
    private final String outputType;
    private final String sourceFile;
    private final String outputDirectory;
    private final String mode;
    private Integer modelCount;

    public BPMNTemplateResponseSink(String outputType, String sourceFile, String outputDirectory, String mode) {
        this.outputType = outputType;
        this.sourceFile = sourceFile;
        this.outputDirectory = outputDirectory;
        this.mode = mode;
        this.modelCount = 0;
    }

    @Override
    public void invoke(BPMNTemplateResponse value, Context context) throws Exception {
        switch (outputType) {
            case "SVG":
                final var outputFileNamePalia = format("%s%s-%d-%s.svg", outputDirectory, sourceFile, modelCount, mode);
                log.info("Creating output file: {}", outputFileNamePalia);
                PaliaLikeBPMNDiagramGenerator.fromBPMNTemplate(sourceFile, value.getBpmnTemplate(), outputFileNamePalia);
                break;
            case "BPMN":
                String outputFileNameBPMN = format("%s%s-%d-%s.bpmn", outputDirectory, sourceFile, modelCount, mode);
                log.info("Creating output file: {}", outputFileNameBPMN);
                BPMNDiagramGenerator.fromBPMNTemplate(sourceFile, value.getBpmnTemplate(), outputFileNameBPMN);
                break;
            case "CAMUNDA_BPMN":
                String outputFileNameCamunda= format("%s%s-%d-%s.bpmn", outputDirectory, sourceFile, modelCount, mode);
                log.info("Creating output file: {}", outputFileNameCamunda);
                CamundaBPMNDiagramGenerator.fromBPMNTemplate(sourceFile, value.getBpmnTemplate(), outputFileNameCamunda);
                break;
            default:
                throw new RuntimeException(format("Unsupported output type: %s", outputType));
        }
        modelCount++;
    }
}
