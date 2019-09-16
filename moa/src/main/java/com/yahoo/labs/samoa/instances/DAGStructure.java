package com.yahoo.labs.samoa.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DAGStructure implements AttributeStructure {
	public Node root;

	public Map<String, Node> nodes;

	public int type;

	public static int HIERARCHY_TYPE = 0;
	public static int DAG_TYPE = 1;

	public DAGStructure() {
		this.type = DAGStructure.HIERARCHY_TYPE;
		this.nodes = new HashMap<>();
		this.root = null;
	}

	public boolean hasNode(String description) {
		return nodes.containsKey(description);
	}

	public void addNode(String description) {
		nodes.put(description, new Node(description));
	}

	public void addChild(String parent, String child) {
		Node p = nodes.get(parent);
		Node c = nodes.get(child);
		p.addChild(c);
		c.addAncestor(p);
		if (c.ancestors.size() > 1) {
			this.type = DAGStructure.DAG_TYPE;
		}
	}

	public void setRoot(String description) {
		if (this.nodes.containsKey(description)) {
			this.root = this.nodes.get(description);
		}
	}

	public class Node {
		public String description;

		public List<Node> ancestors;
		public List<Node> children;

		public Node(String description) {
			this.description = description;
			this.ancestors = new ArrayList<>();
			this.children = new ArrayList<>();
		}

		public void addAncestor(Node ancestor) {
			this.ancestors.add(ancestor);
		}

		public void addChild(Node child) {
			this.ancestors.add(child);
		}

		public boolean isRoot() {
			// Should only be called when the structure is built
			return this.ancestors.isEmpty();
		}

		public double getLevel() {
			// Check how this is done when a DAG is used instead of a hierarchy
			if (this.isRoot())
				return 0;
			else {
				double total = Integer.MAX_VALUE;
				for (Node ancestor : this.ancestors) {
					total = total + ancestor.getLevel();
				}
				return total / ancestors.size() + 1;
			}
		}
	}

}
