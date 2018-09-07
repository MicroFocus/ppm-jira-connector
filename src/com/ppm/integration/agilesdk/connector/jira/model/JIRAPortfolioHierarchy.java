package com.ppm.integration.agilesdk.connector.jira.model;

import com.ppm.integration.agilesdk.connector.jira.service.JIRAService;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * This class holds the structure of a Jira Portfolio Hierarchy of issues, as well as some extra information such as all Epics.
 *
 * It is build based on the Jira Portfolio Parent field.
 */
public class JIRAPortfolioHierarchy {

    private Map<String, Node> nodesByKey = new HashMap<>();

    // Map<Parent key, Keys of issues with this parent key>
    private Map<String, Set<String>> indexedParents = new HashMap<>();

    /**
     * You build a portfolio hierarchy from a list of "base" issues. It will then rebuild the hierarchy, and retrieve missing parent issues from Jira if they are not in the initial list.
     * @param issues
     */
    public JIRAPortfolioHierarchy(Collection<JIRASubTaskableIssue> issues, JIRAService service) {
        buildHierarchy(issues, service);
    }

    private void buildHierarchy(Collection<JIRASubTaskableIssue> issues, JIRAService service) {
        Set<String> missingIssues = new HashSet<>();

        do {
            missingIssues.clear();

            // Processing issues and putting them in the hierarchy, noting missing issues along the way.
            for (JIRASubTaskableIssue issue : issues) {
                missingIssues.remove(issue.getKey());
                String missingParentKey = processNewIssue(issue);
                if (missingParentKey != null) {
                    missingIssues.add(missingParentKey);
                }
            }

            if (!missingIssues.isEmpty()) {
                // Following commented block is for if you want to build the hierarchy upward to the top.
                // Retrieve all missing issues and go for one more round.
                // issues = service.getAllIssuesByKeys(missingIssues);

                // For now We want to not go upward in the hierarchy, so we stop here.
                missingIssues.clear();

            }

        } while (!missingIssues.isEmpty());
    }

    /**
     * Turns a new issue into a Node in its right position in the hierarchy.
     * @return The parent key if it's not been loaded yet.
     */
    private String processNewIssue(JIRASubTaskableIssue issue) {
        // If issue has different portfolio parent and owner Epic, then Portfolio gets priority.
        String parentKey = issue.getPortfolioParentKey();
        if (StringUtils.isBlank(parentKey) || "null".equalsIgnoreCase(parentKey)) {
            parentKey = issue.getEpicKey();
        }
        if (StringUtils.isBlank(parentKey) || "null".equalsIgnoreCase(parentKey)) {
            parentKey = null;
        }

        Node parentNode = parentKey == null ? null : nodesByKey.get(parentKey);
        Node issueNode = new Node(issue, parentNode);

        nodesByKey.put(issue.getKey(), issueNode);

        // If any previously processed node has this node as parent, set it.
        Set<String> children = indexedParents.get(issue.getKey());
        if (children != null) {
            for (String childKey: children) {
                Node child = nodesByKey.get(childKey);
                child.setParent(issueNode);
            }
        }

        if (StringUtils.isBlank(parentKey)) {
            // This issue doesn't have any parent, it's a standalone issue or a root issue.
            return null;
        }

        Set<String> siblings = indexedParents.get(parentKey);

        if (siblings == null) {
            siblings = new HashSet<>();
            indexedParents.put(parentKey, siblings);
        }
        siblings.add(issue.getKey());

        if (parentNode == null) {
            // Parent hasn't been loaded yet.
            return parentKey;
        } else {
            // Parent was already loaded and has been set on this processed node.
            return null;
        }
    }

    /**
     * Standalone issues have no parent (orphan) and no Child (not root)
     */
    public List<Node> getStandaloneNodes() {
        List<Node> standaloneNodes = new ArrayList<>();

        for (Node node : nodesByKey.values()) {
            if (node.getParent() == null & node.getChildren().isEmpty()) {
                standaloneNodes.add(node);
            }
        }

        // We want to sort standalone nodes by issue type & name.
        Collections.sort(standaloneNodes, new Comparator<Node>() {
            @Override public int compare(Node o1, Node o2) {
                int compare = o1.getIssue().getType().compareToIgnoreCase(o2.getIssue().getType());
                if (compare == 0) {
                    return o1.getIssue().getName().compareToIgnoreCase(o2.getIssue().getName());
                }
                return compare;
            }
        });

        return standaloneNodes;
    }

    public List<Node> getRootNodes() {

        List<Node> rootNodes = new ArrayList<>();

        for (Node node : nodesByKey.values()) {
            if (node.getParent() == null & !node.getChildren().isEmpty()) {
                rootNodes.add(node);
            }
        }

        // We want to sort root nodes from the deepest to shallowest.
        Collections.sort(rootNodes, new Comparator<Node>() {
            @Override public int compare(Node o1, Node o2) {
                int compare = new Integer(o1.getLevel()).compareTo(o2.getLevel());
                if (compare == 0) {
                    return o1.getIssue().getName().compareToIgnoreCase(o2.getIssue().getName());
                }
                return compare;
            }
        });

        return rootNodes;
    }

    public List<JIRAEpic> getEpics() {
        List<JIRAEpic> epics = new ArrayList<>();
        for (Node node : nodesByKey.values()) {
            if (node.getIssue() instanceof JIRAEpic) {
                epics.add((JIRAEpic)node.getIssue());
            }
        }



        return epics;
    }

    public List<List<Node>> findLoops() {
        List<List<Node>> loops = new ArrayList<>();

        for (Node node: nodesByKey.values()) {
            List<Node> loop = node.findLoop();
            if (loop != null) {
                loops.add(loop);
            }
        }

        return loops;
    }

    public class Node {

        private JIRASubTaskableIssue issue;

        private Node parent;

        private List<Node> children = new ArrayList<>();

        public Node(JIRASubTaskableIssue issue, Node parent) {
            this.issue = issue;
            setParent(parent);
        }

        public void setParent(Node parent) {
            this.parent = parent;
            if (parent != null) {
                parent.addChild(this);
            }
        }

        public void addChild(Node child) {
            children.add(child);
        }

        public List<Node> getChildren() {
            return children;
        }

        public Node getParent() {
            return parent;
        }

        public JIRASubTaskableIssue getIssue() {
            return issue;
        }
        
        public int getLevel() {
            if (children.isEmpty()) {
                return 0;
            }
            
            int maxChildLevel = 0;
            
            for (Node child : getChildren()) {
                int childLevel = child.getLevel();
                if (childLevel > maxChildLevel) {
                    maxChildLevel = childLevel;
                }
            }
            
            return maxChildLevel + 1;
        }

        private long getStoryPoints() {
            Long sp = issue.getStoryPoints();
            return sp == null? 0 : sp.longValue();
        }

        public long getAggregatedStoryPoints() {
            long sp = 0;
            for (Node child : children) {
                sp +=child.getStoryPoints();
            }
            return sp;
        }

        public List<Node> findLoop() {
            return this.findFirstPathTo(issue.getKey());
        }

        private List<Node> findFirstPathTo(String key) {
            // Direct child
            for (Node child: getChildren()) {
                if (key.equals(child.getIssue().getKey())) {
                    List<Node> path = new ArrayList<>();
                    path.add(this);
                    path.add(child);
                    return path;
                }
            }

            // grand+ child.
            for (Node child: getChildren()) {
                List<Node> path = child.findFirstPathTo(key);
                if (path != null) {
                    path.add(0, this);
                    return path;
                }
            }

            return null;
        }
    }

}
