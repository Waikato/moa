package moa.classifiers.trees.brim_util;

class PlasticTreeElement {
    private PlasticNode node;
    private SuccessorIdentifier key;

    public PlasticTreeElement(PlasticNode node, SuccessorIdentifier key) {
        this.node = node;
        this.key = key;
    }

    public PlasticTreeElement(PlasticTreeElement other) {
        this.node = other.node;
        this.key = other.key;
    }

    public PlasticNode getNode() {
        return node;
    }

    public SuccessorIdentifier getKey() {
        return key;
    }

    public String getDescription() {
        String blueprint = "%s%s -- %s";
        return String.format(blueprint,
                node.splitAttribute != null ? node.splitAttribute.toString() : "L",
                node.isArtificial() ? "*" : "",
                key != null ? Double.toString(key.getReferencevalue()) : (node.isLeaf() ? "X" : "...")
        );
    }

    public PlasticTreeElement copy() {
        PlasticNode nodeCpy;
        // copyNode keeps the type of the node (e.g., a regression node), unlike the PlasticNode copy constructor
        nodeCpy = node.copyNode();
        SuccessorIdentifier keyCpy = key != null ? new SuccessorIdentifier(key) : null;
        return new PlasticTreeElement(nodeCpy, keyCpy);
    }
}
