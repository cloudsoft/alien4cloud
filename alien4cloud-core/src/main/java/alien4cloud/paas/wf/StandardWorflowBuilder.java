package alien4cloud.paas.wf;

import alien4cloud.paas.wf.exception.BadWorkflowOperationException;
import alien4cloud.paas.wf.exception.InconsistentWorkflowException;
import alien4cloud.paas.wf.util.WorkflowUtils;


public abstract class StandardWorflowBuilder extends AbstractWorkflowBuilder {

    @Override
    public void removeStep(Workflow wf, String stepId, boolean force) {
        AbstractStep step = wf.getSteps().get(stepId);
        if (step == null) {
            throw new InconsistentWorkflowException(String.format(
                    "Inconsistent workflow: a step named '%s' can not be found while it's referenced else where ...", stepId));
        }
        if (!force && WorkflowUtils.isStateStep(step)) {
            throw new BadWorkflowOperationException("State steps can not be removed from standard workflows");
        }
        
        super.removeStep(wf, stepId, force);
    }

    @Override
    protected NodeActivityStep addActivityStep(Workflow wf, String nodeId, AbstractActivity activity) {
        if (activity instanceof SetStateActivity) {
            throw new BadWorkflowOperationException("State steps can not be added to standard workflows");
        }
        return super.addActivityStep(wf, nodeId, activity);
    }

}
