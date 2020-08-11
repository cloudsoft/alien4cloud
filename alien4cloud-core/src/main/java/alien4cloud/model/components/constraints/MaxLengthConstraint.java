package alien4cloud.model.components.constraints;

import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, of = { "maxLength" })
@Slf4j
public class MaxLengthConstraint extends AbstractStringPropertyConstraint {
    @NotNull
    private Integer maxLength;

    public MaxLengthConstraint(Integer maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    protected void doValidate(String propertyValue) throws ConstraintViolationException {
        if (propertyValue.length() > maxLength) {
            throw new ConstraintViolationException("The length of the value is greater than [" + maxLength + "]");
        }
    }
}