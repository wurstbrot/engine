package io.securecodebox.scanprocess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.securecodebox.constants.DefaultFields;
import io.securecodebox.model.execution.Target;
import io.securecodebox.model.findings.Finding;
import io.securecodebox.scanprocess.listener.TransformFindingsToTargetsListener;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CombinedTransformListener implements JavaDelegate {

    protected static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CombinedTransformListener.class);

    Map<String, String> defaultValues = new HashMap() {{
        put("DEFAULT_SSH_SERVICE", "ssh");
        put("DEFAULT_SSH_PORT", "22");
        put("DEFAULT_SSLYZE_SERVICE", "");
        put("DEFAULT_SSLYZE_PORT", "443");
    }};

    private enum ScannerEnum {
        AMASS,
        ARACHNI,
        NIKTO,
        SSH,
        SSLYZE,
        WPSCAN,
        ZAP
    }

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception{
        ObjectMapper objectMapper = new ObjectMapper();

        List<Target> targets = new LinkedList<>();
        Target initialTarget = new LinkedList<>(ProcessVariableHelper.readListFromValue(
                (String) delegateExecution.getVariable(DefaultFields.PROCESS_TARGETS.name()), Target.class)).get(0);

        List<Finding> findings = new LinkedList<>(ProcessVariableHelper.readListFromValue(
                (String) delegateExecution.getVariable(DefaultFields.PROCESS_FINDINGS.name()), Finding.class));

        ScannerEnum[] scannerNames = {ScannerEnum.SSH, ScannerEnum.SSLYZE};

        for (ScannerEnum scannerName: scannerNames) {
            targets.addAll(
                createTargetsFor(scannerName, findings, initialTarget)
            );
        }

        LOG.debug("Created Targets out of Findings");

        Collection<ObjectValue> objectValueCollection = new LinkedList<>();
        for (Target target: targets) {
            objectValueCollection.add(Variables.objectValue(objectMapper.writeValueAsString(target))
                    .serializationDataFormat(Variables.SerializationDataFormats.JSON)
                    .create());
        }
        ObjectValue objectValue = Variables.objectValue(objectMapper.writeValueAsString(targets))
                .serializationDataFormat(Variables.SerializationDataFormats.JSON)
                .create();
        delegateExecution.setVariable(DefaultFields.PROCESS_TARGETS.name(), objectValue);
        delegateExecution.setVariable("PROCESS_TARGETS_COLLECTION", objectValueCollection);
    }

    private List<Target> createTargetsFor (ScannerEnum scannerName, List<Finding> findings, Target initialTarget) {
        //Setting default values if none were provided
        Map<String, Object> targetAttributes = initialTarget.getAttributes();

        //TODO: Port & Service as Comma separated Lists
        String port = (String) targetAttributes.getOrDefault(scannerName + "_PORT", defaultValues.get("DEFAULT_" + scannerName + "_PORT"));
        String service = (String) targetAttributes.getOrDefault(scannerName + "_SERVICE", defaultValues.get("DEFAULT_" + scannerName + "_SERVICE"));

        List<Target> nextTargets = new LinkedList<>();

        for (Finding finding : findings) {
            Map<String, Object>findingAttributes = finding.getAttributes();

            String actualPort = findingAttributes.get("port").toString();
            String actualService = (String) findingAttributes.get("service");


            if (targetAttributes.get("DO_" + scannerName).equals(true)) {
                if (port.equals(actualPort) || service.equals(actualService)) {
                    Target target = new Target();
                    switch (scannerName){
                        case SSH:
                            target.setLocation(findingAttributes.get("hostname") + ":" + findingAttributes.get("port"));
                            break;
                        case SSLYZE:
                            target.setLocation(findingAttributes.get("hostname") + ":" + findingAttributes.get("port"));
                            break;
                    }
                    target.appendOrUpdateAttribute("SECOND_SCAN", scannerName);
                }
            }
        }

        return nextTargets;
    }
}
