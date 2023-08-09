package beamline.miners.splitminer.view;

import au.edu.qut.bpmn.io.impl.BPMNDiagramExporterImpl;
import beamline.miners.splitminer.bpmn.BPMNTemplate;
import org.apache.commons.io.FileUtils;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramImpl;
import org.processmining.models.graphbased.directed.bpmn.elements.Event;
import org.processmining.models.graphbased.directed.bpmn.elements.Gateway;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class BPMNDiagramGenerator {
    public static void fromBPMNTemplate(String processName, BPMNTemplate template, String outputFilePath) throws Exception {
        final var diagram = new BPMNDiagramImpl(processName);

        final var nodeToDiagramNode = new HashMap<String, org.processmining.models.graphbased.directed.bpmn.BPMNNode>();

        final var diagramStartEvent = diagram.addEvent("", Event.EventType.START, Event.EventTrigger.NONE, Event.EventUse.CATCH, true, null);
        nodeToDiagramNode.put(template.getStartEvent().getId(), diagramStartEvent);

        for (final var activity : template.getActivities()) {
            final var diagramActivity = diagram.addActivity(activity.getName(), false, false, false, false, false);
            nodeToDiagramNode.put(activity.getId(), diagramActivity);
        }

        for (final var gateway : template.getGateways()) {
            if (gateway.isXorSplit() || gateway.isXorJoin()) {
                final var diagramGateway = diagram.addGateway("", Gateway.GatewayType.DATABASED);
                nodeToDiagramNode.put(gateway.getId(), diagramGateway);
            }
            if (gateway.isAndSplit() || gateway.isAndJoin()) {
                final var diagramGateway = diagram.addGateway("", Gateway.GatewayType.PARALLEL);
                nodeToDiagramNode.put(gateway.getId(), diagramGateway);
            }
            if (gateway.isOrJoin()) {
                final var diagramGateway = diagram.addGateway("", Gateway.GatewayType.INCLUSIVE);
                nodeToDiagramNode.put(gateway.getId(), diagramGateway);
            }
        }

        final var diagramEndEvent = diagram.addEvent("", Event.EventType.END, Event.EventTrigger.NONE, Event.EventUse.CATCH, true, null);
        nodeToDiagramNode.put(template.getEndEvent().getId(), diagramEndEvent);

        for (final var association : template.getAssociations()) {
            diagram.addFlow(nodeToDiagramNode.get(association.getLeft().getId()), nodeToDiagramNode.get(association.getRight().getId()), "");
        }

        BPMNDiagramExporterImpl impl = new BPMNDiagramExporterImpl();

        FileUtils.writeStringToFile(new File(outputFilePath), impl.exportBPMNDiagram(diagram), StandardCharsets.UTF_8);
    }
}
