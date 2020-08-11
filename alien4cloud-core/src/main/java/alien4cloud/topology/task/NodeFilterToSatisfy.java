package alien4cloud.topology.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class NodeFilterToSatisfy {
    private String relationshipName;
    private String targetName;
    // propertyName, list of violated constraints.
    private List<String> missingCapabilities;
    private Map<String, List<NodeFilterConstraintViolation>> violatedConstraints;
}