package beamline.miners.splitminer.view;

import beamline.miners.splitminer.bpmn.BPMNTemplate;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.plugins.graphviz.visualisation.DotPanel;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class PaliaLikeBPMNDiagramGenerator {
    public static void fromBPMNTemplate(String processName, BPMNTemplate template, String outputFilePath) throws IOException {
        final var dot = new Dot();
        dot.setOption("rankdir", "LR");
        dot.setOption("outputorder", "edgesfirst");

        final var nodeToDiagramNode = new HashMap<String, DotNode>();

        final var diagramStartEvent = makeEventNode(dot, true);
        nodeToDiagramNode.put(template.getStartEvent().getId(), diagramStartEvent);

        for (var activity : template.getActivities()) {
            var node = makeActivityNode(dot, activity.getName());
            nodeToDiagramNode.put(activity.getId(), node);
        }

        for (var gateway : template.getGateways()) {
            if (gateway.isXorSplit() || gateway.isXorJoin()) {
                var diagramGateway = makeGatewayNode(dot, "&times;");
                nodeToDiagramNode.put(gateway.getId(), diagramGateway);
            }
            if (gateway.isAndSplit() || gateway.isAndJoin()) {
                var diagramGateway = makeGatewayNode(dot, "+");
                nodeToDiagramNode.put(gateway.getId(), diagramGateway);
            }
            if (gateway.isOrJoin()) {
                var diagramGateway = makeGatewayNode(dot, "o");
                nodeToDiagramNode.put(gateway.getId(), diagramGateway);
            }
        }

        final var diagramEndEvent = makeEventNode(dot, false);
        nodeToDiagramNode.put(template.getEndEvent().getId(), diagramEndEvent);

        for (var association : template.getAssociations()) {
            makeEdge(dot, nodeToDiagramNode.get(association.getLeft().getId()), nodeToDiagramNode.get(association.getRight().getId()));
        }

        DotPanel p = new DotPanel(dot);
        p.setBackground(Color.white);

        Graphviz.fromString(dot.toString()).render(Format.SVG).toFile(new File(outputFilePath));
    }

    private static DotEdge makeEdge(Dot dot, DotNode source, DotNode target) {
        DotEdge edge = dot.addEdge(source, target);
        edge.setOption("tailclip", "false");
        return edge;
    }

    private static DotNode makeActivityNode(Dot dot, String name) {
        DotNode dotNode = dot.addNode(name);
        dotNode.setOption("shape", "box");
        dotNode.setOption("style", "rounded,filled");
        dotNode.setOption("fillcolor", "#FFFFCC");
        dotNode.setOption("fontsize", "8");
        return dotNode;
    }

    private static DotNode makeEventNode(Dot dot, boolean isStart) {
        DotNode dotNode = dot.addNode("");
        dotNode.setOption("shape", "circle");
        dotNode.setOption("style", "filled");
        dotNode.setOption("fillcolor", "white");
        dotNode.setOption("fontcolor", "white");
        dotNode.setOption("width", "0.3");
        dotNode.setOption("height", "0.3");
        dotNode.setOption("fixedsize", "true");
        if (!isStart) {
            dotNode.setOption("penwidth", "3");
        }
        return dotNode;
    }

    private static DotNode makeGatewayNode(Dot dot, String name) {
        DotNode dotNode = dot.addNode(
                "<<table border='0'><tr><td></td></tr><tr><td valign='bottom'>" + name + "</td></tr></table>>");
        dotNode.setOption("shape", "diamond");
        dotNode.setOption("style", "filled");
        dotNode.setOption("fillcolor", "white");
        dotNode.setOption("fontcolor", "black");

        dotNode.setOption("width", "0.4");
        dotNode.setOption("height", "0.4");
        dotNode.setOption("fontsize", "30");
        dotNode.setOption("fixedsize", "true");

        return dotNode;
    }
}
