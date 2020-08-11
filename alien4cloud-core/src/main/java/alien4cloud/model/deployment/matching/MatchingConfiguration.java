package alien4cloud.model.deployment.matching;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Matching configuration is associated to types provided by a plugin so alien knows how things should be matched.
 *
 * Alien ships with default implementations of matcher configurations for TOSCA normative types but this can be extended by the locations providers.
 */
@Getter
@Setter
public class MatchingConfiguration extends MatchingFilterDefinition {
    /** Sort ordering is used to sort templates in order to find best matches. */
    private List<String> sortOrdering;
    /** Constraints to be applied to properties capabilities to match the type. */
    private Map<String, MatchingFilterDefinition> capabilities;
}