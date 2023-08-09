package beamline.miners.splitminer.view;

import beamline.miners.splitminer.bpmn.BPMNTemplate;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.create.bpmndi.BPMNDiagramGenerator;

import java.util.HashMap;

public class CamundaBPMNDiagramGenerator {
    public static void fromBPMNTemplate(String processName, BPMNTemplate template, String outputFilePath) {
        final var modelInstance = Bpmn.createEmptyModel();
        final var definitions = modelInstance.newInstance(Definitions.class);
        definitions.setTargetNamespace("http://camunda.org/examples");
        modelInstance.setDefinitions(definitions);

        final var process = createElement(definitions, processName, Process.class);

        final var nodeToDiagramElement = new HashMap<String, FlowNode>();

        final var startEvent = createElement(process, "START-EVENT", StartEvent.class);
        nodeToDiagramElement.put(template.getStartEvent().getId(), startEvent);

        for (final var activity : template.getActivities()) {
            final var bpmnActivity = createElement(process, "NC" + activity.getId(), Task.class);
            bpmnActivity.setName(activity.getName());
            nodeToDiagramElement.put(activity.getId(), bpmnActivity);
        }

        for (var gateway : template.getGateways()) {
            if (gateway.isXorSplit() || gateway.isXorJoin()) {
                final var bpmnGateway = createElement(process, "XOR-" + gateway.getId(), ExclusiveGateway.class);
                nodeToDiagramElement.put(gateway.getId(), bpmnGateway);
            }
            if (gateway.isAndSplit() || gateway.isAndJoin()) {
                final var bpmnGateway = createElement(process, "AND-" + gateway.getId(), ParallelGateway.class);
                nodeToDiagramElement.put(gateway.getId(), bpmnGateway);
            }
            if (gateway.isOrJoin()) {
                final var bpmnGateway = createElement(process, "OR-" + gateway.getId(), InclusiveGateway.class);
                nodeToDiagramElement.put(gateway.getId(), bpmnGateway);
            }
        }

        final var endEvent = createElement(process, "END-EVENT", EndEvent.class);
        nodeToDiagramElement.put(template.getEndEvent().getId(), endEvent);

        for (var association : template.getAssociations()) {
            FlowNode from = nodeToDiagramElement.get(association.getLeft().getId());
            FlowNode to = nodeToDiagramElement.get(association.getRight().getId());
            createSequenceFlow(process, from, to);
        }

        BPMNDiagramGenerator.generate(template, modelInstance, outputFilePath);
    }

    private static <T extends BpmnModelElementInstance> T createElement(BpmnModelElementInstance parentElement, String id, Class<T> elementClass) {
        var element = parentElement.getModelInstance().newInstance(elementClass);
        element.setAttributeValue("id", id, true);
        parentElement.addChildElement(element);
        return element;
    }

    private static SequenceFlow createSequenceFlow(Process process, FlowNode from, FlowNode to) {
        final var identifier = from.getId() + "-" + to.getId();
        final var sequenceFlow = createElement(process, identifier, SequenceFlow.class);
        process.addChildElement(sequenceFlow);
        sequenceFlow.setSource(from);
        from.getOutgoing().add(sequenceFlow);
        sequenceFlow.setTarget(to);
        to.getIncoming().add(sequenceFlow);
        return sequenceFlow;
    }
}
