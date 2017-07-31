package com.ppm.integration.agilesdk.connector.jira.util;

import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalTaskActuals;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * The default Agile SDK doesn't make any assumption as to how % complete should be computed for summary tasks, and by default the PPM duration-based % complete rollup will take effect.
 * However, we do not want this for an Agile tool: We want % complete to be based on either work complete (in terms or logged hours in the agile tool), or use % of Story Points done.
 * <br/> This class will take care of this by abstracting away the work (could be either hours of work or Story Points) and computing the % complete based on this.
 * <br/> It will also read the work info from the children to compute % complete of a parent.
 * <p>You must always put instances of {@link WorkDrivenPercentCompleteExternalTask} as children of an instance of this class.</p>
 */
public class WorkDrivenPercentCompleteExternalTask extends ExternalTask {

    private Double workDone;

    private Double workRemaining;

    private ExternalTask wrappedTask;

    private String nameOverride = null;

    private WorkDrivenPercentCompleteExternalTask(ExternalTask wrappedTask, Double workDone, Double workRemaining) {
        this.wrappedTask = wrappedTask;
        this.workDone = workDone;
        this.workRemaining = workRemaining;
    }

    /**
     * Use this constructor for a summary task, when you want to get work info from children.
     */
    public static WorkDrivenPercentCompleteExternalTask forSummaryTask(ExternalTask wrappedTask) {
        return new WorkDrivenPercentCompleteExternalTask(wrappedTask, null, null);
    }

    /**
     * Use this constructor for Summary tasks, that have some values for work done & work remaining.
     * If passing null, we consider it to be zero.
     */
    public static WorkDrivenPercentCompleteExternalTask forLeafTask(ExternalTask wrappedTask, double workDone, double workRemaining) {
        return new WorkDrivenPercentCompleteExternalTask(wrappedTask, workDone, workRemaining);
    }

    @Override public Double getPercentCompleteOverrideValue() {
        double done = getWorkDone();
        double remaining = getWorkRemaining();

        if (done == 0d && remaining == 0d) {
            // Could be seen as either 0% or 100% since we have no info; no info = not yet estimated, so let's consider the task status.
            if (TaskStatus.COMPLETED.equals(getStatus()) || TaskStatus.CANCELLED.equals(getStatus())) {
                return 100d;
            } else {
                return 0d;
            }
        }

        // % complete = work done / total work
        // Total work = work done + remaining work
        double percentComplete = 100d * done / (done + remaining);

        // In PPM 9.41, a percent complete override value of 0 will be ignored.
        // This is fixed in 9.42, but we want this connector to be compatible with 9.41.
        if (percentComplete <= 0d) {
            percentComplete = 0.001d;
        }

        return  percentComplete;

    }

    public double getWorkDone() {
        if (workDone != null) {
            return workDone;
        } else {
            double childrenWorkDone = 0d;
            for (ExternalTask child : getChildren()) {
                childrenWorkDone += ((WorkDrivenPercentCompleteExternalTask)child).getWorkDone();
            }
            return childrenWorkDone;
        }
    }

    public double getWorkRemaining() {
        if (workRemaining != null) {
            return workRemaining;
        } else {
            double childrenWorkRemaining = 0d;
            for (ExternalTask child : getChildren()) {
                childrenWorkRemaining += ((WorkDrivenPercentCompleteExternalTask)child).getWorkRemaining();
            }
            return childrenWorkRemaining;
        }
    }


    ///// Delegating everything else to the wrapped task

    public String getId() {
        return wrappedTask.getId();
    }


    public String getName() {
        return nameOverride == null ? wrappedTask.getName() : nameOverride;
    }

    public TaskStatus getStatus() {
        return wrappedTask.getStatus();
    }

    public Date getScheduledStart() {
        return wrappedTask.getScheduledStart();
    }

    public Date getScheduledFinish() {
        return wrappedTask.getScheduledFinish();
    }

    public Double getScheduledDurationOverrideValue() {
        return wrappedTask.getScheduledDurationOverrideValue();
    }

    public String getOwnerRole() {
        return wrappedTask.getOwnerRole();
    }

    public List<ExternalTaskActuals> getActuals() {
        return wrappedTask.getActuals();
    }

    public long getOwnerId() {
        return wrappedTask.getOwnerId();
    }

    public boolean isMilestone() {
        return wrappedTask.isMilestone();
    }

    public boolean isMajorMilestone() {
        return wrappedTask.isMajorMilestone();
    }

    public List<ExternalTask> getChildren() {
        return wrappedTask.getChildren();
    }

    public Map<Integer, UserData> getUserDataFields() {
        return wrappedTask.getUserDataFields();
    }

    public void setNameOverride(String nameOverride) {
        this.nameOverride = nameOverride;
    }
}
