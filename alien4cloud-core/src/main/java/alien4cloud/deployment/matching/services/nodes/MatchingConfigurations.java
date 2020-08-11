package alien4cloud.deployment.matching.services.nodes;

import alien4cloud.model.deployment.matching.MatchingConfiguration;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class MatchingConfigurations {
    private Map<String, MatchingConfiguration> matchingConfigurations = Maps.newHashMap();
}