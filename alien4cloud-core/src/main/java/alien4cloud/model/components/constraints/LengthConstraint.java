package alien4cloud.model.components.constraints;

import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = { "length" })
@SuppressWarnings({ "PMD.UnusedPrivateField" })
public class LengthConstraint extends AbstractStringPropertyConstraint {
    @NotNull
    private Integer length;

    @Override
    protected void doValidate(String propertyValue) throws ConstraintViolationException {
        if (propertyValue.length() != length) {
            throw new ConstraintViolationException("The length of the value is not equals to [" + length + "]");
        }
    }
}