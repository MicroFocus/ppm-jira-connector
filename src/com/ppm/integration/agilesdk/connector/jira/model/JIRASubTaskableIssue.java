package com.ppm.integration.agilesdk.connector.jira.model;

import java.util.ArrayList;
import java.util.List;

/**
 * This represents JIRA issues that can have sub-tasks: EPICS, FEATURES, STORIES, TASKS. Everything but sub-tasks.
 */
public abstract class JIRASubTaskableIssue extends JIRAIssue {

    private List<JIRASubTask> subTasks = new ArrayList<JIRASubTask>();

    public List<JIRASubTask> getSubTasks() {
        return subTasks;
    }

    public void addSubTask(JIRASubTask st) {
        this.subTasks.add(st);
    }

    @Override
    public boolean hasWork() {
        if (super.hasWork()) {
            return true;
        }

        if (getSubTasks() == null) {
            return false;
        }

        for (JIRASubTask subTask : getSubTasks() ) {

            if (subTask != null &&  subTask.hasWork()) {
                return true;
            }
        }

        return false;
    }
}
