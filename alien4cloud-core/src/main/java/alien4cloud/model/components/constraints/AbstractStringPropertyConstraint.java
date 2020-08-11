package alien4cloud.model.components.constraints;

import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractStringPropertyConstraint extends AbstractPropertyConstraint {
    protected abstract void doValidate(String propertyValue) throws ConstraintViolationException;

    @Override
    public void validate(Object propertyValue) throws ConstraintViolationException {
        if (propertyValue == null) {
            throw new ConstraintViolationException("Value to validate is null");
        }
        if (!(propertyValue instanceof String)) {
            throw new ConstraintViolationException("This constraint can only be applied on String value");
        }
        doValidate((String) propertyValue);
    }

    @Override
    public void initialize(IPropertyType<?> propertyType) throws ConstraintValueDoNotMatchPropertyTypeException {
        ConstraintUtil.checkStringType(propertyType);
    }
}
