package moa.classifiers.trees.plastic_util;

import com.yahoo.labs.samoa.instances.Attribute;
import moa.classifiers.core.conditionaltests.InstanceConditionalTest;

import java.util.*;

class MappedTree implements Iterator<PlasticBranch> {

    private LinkedList<PlasticBranch> branchQueue;
    private final LinkedList<PlasticBranch> finishedBranches = new LinkedList<>();

    private final Attribute splitAttribute;
    private final int splitAttributeIndex;
    private final Double splitValue;
    private final int maxBranchLength;

    public MappedTree(PlasticNode root, Attribute splitAttribute, int splitAttributeIndex, Double splitValue, int maxBranchLength) {
        branchQueue = disconnectRoot(root);

        this.splitAttribute = splitAttribute;
        this.splitAttributeIndex = splitAttributeIndex;
        this.splitValue = splitValue;
        this.maxBranchLength = maxBranchLength;
    }


    @Override
    public boolean hasNext() {
        return !(branchQueue.size() == 0 && finishedBranches.size() == 0);
    }

    @Override
    public PlasticBranch next() {
        if (finishedBranches.size() > 0) {
            return finishedBranches.removeFirst();
        }
        else {
            while (finishedBranches.size() == 0) {
                mapBranches(branchQueue, splitAttribute, splitAttributeIndex, splitValue, maxBranchLength);
            }
            return finishedBranches.removeFirst();
        }
    }

    private void mapBranches(
            LinkedList<PlasticBranch> branches,
            Attribute swapAttribute,
            int swapAttributeIndex,
            Double splitValue,
            int maxBranchLength
    ) {
        int numBranches = branches.size();
        for (int i = 0; i < numBranches; i++) {
            PlasticBranch branch = branches.removeFirst();
            boolean branchIsFinished = getEndConditionForBranch(branch, swapAttribute, swapAttributeIndex, splitValue, maxBranchLength);
            if (branchIsFinished) {
                expandBranch(branch, swapAttribute, swapAttributeIndex, splitValue);
                List<PlasticBranch> decoupledBranches = decoupleLastNode(branch);
                decoupledBranches.forEach(b -> modifyBranch(b, swapAttribute));
                finishedBranches.addAll(decoupledBranches);
                branchQueue = branches;
                return;
            }
            PlasticTreeElement lastElement = branch.getLast();
            PlasticBranch branchExtensions = disconnectSuccessors(lastElement, swapAttribute);
            for (PlasticTreeElement extension: branchExtensions.getBranchRef()) {
                PlasticBranch extendedBranch = new PlasticBranch((LinkedList<PlasticTreeElement>) branch.getBranchRef().clone());
                extendedBranch.getBranchRef().add(extension);
                branches.add(extendedBranch);
            }
        }
        branchQueue = branches;
    }

    private boolean getEndConditionForBranch(PlasticBranch branch, Attribute swapAttribute, int swapAttributeIndex, Double splitValue, int maxBranchLength) {
        CustomEFDTNode lastNodeOfBranch = branch.getLast().getNode();
        boolean isSwapAttribute = lastNodeOfBranch.getSplitAttribute() == swapAttribute;
        boolean isLeaf = lastNodeOfBranch.isLeaf();
        if (isSwapAttribute || isLeaf)
            return true;
        return branch.getBranchRef().size() >= maxBranchLength;
    }

    private PlasticBranch disconnectSuccessors(PlasticTreeElement ancestor, Attribute swapAttribute) {
        PlasticBranch branchExtensions = new PlasticBranch();
        PlasticNode ancestorNode = ancestor.getNode();
        SuccessorIdentifier key = ancestor.getKey();
        PlasticNode successor = (PlasticNode) ancestorNode.getSuccessors().getSuccessorNode(key);

        boolean endCondition = successor.isLeaf() || successor.getSplitAttribute() == swapAttribute;

        if (endCondition) {
            PlasticTreeElement element = new PlasticTreeElement(successor, null);
            branchExtensions.getBranchRef().add(element);
        }
        else {
            for (SuccessorIdentifier successorSuccessorKey: successor.getSuccessors().getKeyset()) {
                PlasticTreeElement element = new PlasticTreeElement(successor, successorSuccessorKey);
                branchExtensions.getBranchRef().add(element);
            }
        }
        return branchExtensions;
    }

    private LinkedList<PlasticBranch> disconnectRoot(PlasticNode root) {
        Successors successors = root.getSuccessors();
        if (successors == null) {
            return null;
        }
        if (successors.size() == 0) {
            return null;
        }
        Set<SuccessorIdentifier> keys = successors.getKeyset();
        LinkedList<PlasticBranch> branches = new LinkedList<>();
        for (SuccessorIdentifier key: keys) {
            PlasticTreeElement newElement = new PlasticTreeElement(root, key);
            PlasticBranch newBranch = new PlasticBranch();
            newBranch.getBranchRef().add(newElement);
            branches.add(newBranch);
        }
        return branches;
    }

    private void expandBranch(PlasticBranch branch, Attribute splitAttribute, int splitAttributeIndex, Double splitValue) {
            if (splitAttribute.isNominal())
                splitLastNodeIfRequiredCategorical(branch, splitAttribute, splitAttributeIndex, splitValue);
            else
                splitLastNodeIfRequiredNumeric(branch, splitAttribute, splitAttributeIndex, splitValue);
    }

    private LinkedList<PlasticBranch> decoupleLastNode(PlasticBranch branch) {
        PlasticNode lastNode = branch.getLast().getNode();
        LinkedList<PlasticBranch> decoupledBranches = new LinkedList<>();
        Successors lastNodeSuccessors = lastNode.getSuccessors();

        for (SuccessorIdentifier key: lastNodeSuccessors.getKeyset()) {
            PlasticBranch branchCopy = branch.copy();  //TODO: Doublecheck if this is fine.
            PlasticTreeElement replacedEndElement = new PlasticTreeElement(branchCopy.getLast().getNode(), key);
            branchCopy.getBranchRef().removeLast();
            branchCopy.getBranchRef().add(replacedEndElement);
            PlasticTreeElement finalElement = new PlasticTreeElement((PlasticNode) lastNodeSuccessors.getSuccessorNode(key), null);
            branchCopy.getBranchRef().add(finalElement);
            decoupledBranches.add(branchCopy);
        }

        return decoupledBranches;
    }

    private void splitLastNodeIfRequiredCategorical(PlasticBranch branch,
                                                    Attribute swapAttribute,
                                                    int swapAttributeIndex,
                                                    Double splitValue) {
        PlasticNode lastNode = branch.getLast().getNode();

        boolean shouldBeBinary = splitValue != null;
        boolean splitAttributesMatch = lastNode.splitAttribute == swapAttribute;

        if (lastNode.isLeaf() || !splitAttributesMatch) { // Option 1: the split attributes don't match
            lastNode.forceSplit(
                    swapAttribute,
                    swapAttributeIndex,
                    splitValue,
                    shouldBeBinary
            );
            if (lastNode.isLeaf()) {
                System.out.println("Error");
            }
            lastNode.successors.getAllSuccessors().forEach(s -> ((PlasticNode) s).setIsArtificial());
            return;
        }

        boolean isBinary = lastNode.successors.isBinary();

        if (!isBinary && !shouldBeBinary) // Option 2: the split attributes match and the splits are multiway (this is really the best case possible)
            // do nothing
            return;

        if (isBinary && shouldBeBinary) { // Option 3: the split attributes match and also both splits should be binary
            if (splitValue.equals(lastNode.successors.getReferenceValue()))
                // do nothing
                return;
            Successors lastNodeSuccessors = new Successors(lastNode.getSuccessors(), true);

            // Corresponds to transformation 3 in the paper
            Attribute lastNodeSplitAttribute = lastNode.getSplitAttribute();
            int lastNodeSplitAttributeIndex = lastNode.getSplitAttributeIndex();
            InstanceConditionalTest splitTest = lastNode.getSplitTest();
            lastNode.successors = null;
            lastNode.forceSplit(
                    swapAttribute,
                    swapAttributeIndex,
                    splitValue,
                    shouldBeBinary
            );
            PlasticNode defaultSuccessor = (PlasticNode) lastNode.getSuccessors().getSuccessorNode(SuccessorIdentifier.DEFAULT_NOMINAL_VALUE);
            defaultSuccessor.transferSplit(
                    lastNodeSuccessors, lastNodeSplitAttribute, lastNodeSplitAttributeIndex, splitTest
            );
            return;
        }

        if (!isBinary && shouldBeBinary) { // Option 4: The split attributes match and the current split is multiway while the old one was binary. In this case, we do something similar to the numeric splits
            // Corresponds to transformation 2 in the paper
            Successors lastNodeSuccessors = new Successors(lastNode.getSuccessors(), true);
            Attribute lastNodeSplitAttribute = lastNode.getSplitAttribute();
            int lastNodeSplitAttributeIndex = lastNode.getSplitAttributeIndex();
            InstanceConditionalTest splitTest = lastNode.getSplitTest();
            lastNode.successors = null;
            lastNode.forceSplit(
                    swapAttribute,
                    swapAttributeIndex,
                    splitValue,
                    shouldBeBinary
            );
            PlasticNode defaultSuccessor = (PlasticNode) lastNode.getSuccessors().getSuccessorNode(SuccessorIdentifier.DEFAULT_NOMINAL_VALUE);
//            if (lastNodeSuccessors.contains(splitValue)) {
//                SuccessorIdentifier foundKey = null;
//                for (SuccessorIdentifier k: lastNodeSuccessors.getKeyset()) {
//                    if (k.getSelectorValue() == splitValue) {
//                        foundKey = k;
//                        break;
//                    }
//                }
//                if (foundKey != null)
//                    lastNodeSuccessors.removeSuccessor(foundKey);
//            }
            defaultSuccessor.transferSplit(
                    lastNodeSuccessors, lastNodeSplitAttribute, lastNodeSplitAttributeIndex, splitTest
            );
            return;
        }

        if (isBinary && !shouldBeBinary) { // Option 5: The split is binary but should be multiway. In this case, we force the split and then use the old subtree of the left branch of the old subtree.
            // Corresponds to transformation 1 in the paper
            SuccessorIdentifier keyToPreviousSuccessor = new SuccessorIdentifier(false, splitValue, splitValue, false);
            PlasticNode previousSuccessor = (PlasticNode) lastNode.getSuccessors().getSuccessorNode(keyToPreviousSuccessor);

            lastNode.successors = null;
            lastNode.forceSplit(
                    swapAttribute,
                    swapAttributeIndex,
                    splitValue,
                    shouldBeBinary
            );
            lastNode.successors.removeSuccessor(keyToPreviousSuccessor);
            lastNode.successors.addSuccessor(previousSuccessor, keyToPreviousSuccessor);
            return;
        }

        else if (lastNode.isLeaf()) {
            System.out.println("Do nothing");
        }
    }

    private void splitLastNodeIfRequiredNumeric(PlasticBranch branch,
                                                Attribute swapAttribute,
                                                int swapAttributeIndex,
                                                Double splitValue) {
        assert splitValue != null;
        PlasticNode lastNode = branch.getLast().getNode();
        boolean forceSplit = lastNode.isLeaf() || lastNode.splitAttribute != swapAttribute;

        if (forceSplit) {
            lastNode.forceSplit(
                    swapAttribute,
                    swapAttributeIndex,
                    splitValue,
                    true
            );
            lastNode.getSuccessors().getAllSuccessors().forEach(s -> ((PlasticNode) s).setIsArtificial());
            return;
        }

        Double oldThreshold = lastNode.getSuccessors().getReferenceValue();
        if (splitValue.equals(oldThreshold))
            return; // do nothing

        if (lastNode.getSuccessors().size() > 1) {
            updateThreshold(lastNode, swapAttributeIndex, splitValue);
            return;
        }

        lastNode.forceSplit(
                swapAttribute,
                swapAttributeIndex,
                splitValue,
                true
        );
        lastNode.getSuccessors().getAllSuccessors().forEach(s -> ((PlasticNode) s).setIsArtificial());
    }

    private void updateThreshold(PlasticNode node, int splitAttributeIndex, double splitValue) {
        Double oldThreshold = node.getSuccessors().getReferenceValue();

        SuccessorIdentifier leftKey = new SuccessorIdentifier(true, oldThreshold, oldThreshold, true);
        SuccessorIdentifier rightKey = new SuccessorIdentifier(true, oldThreshold, oldThreshold, false);
        PlasticNode succ1 = (PlasticNode) node.getSuccessors().getSuccessorNode(leftKey);
        PlasticNode succ2 = (PlasticNode) node.getSuccessors().getSuccessorNode(rightKey);
        Successors newSuccessors = new Successors(true, true, splitValue);
        if (succ1 != null)
            newSuccessors.addSuccessorNumeric(splitValue, succ1, true);
        if (succ2 != null)
            newSuccessors.addSuccessorNumeric(splitValue, succ2, false);
        node.successors = newSuccessors;

        if (node.isLeaf())
            return;

        for (SuccessorIdentifier key: node.getSuccessors().getKeyset()) {
            PlasticNode s = (PlasticNode) node.getSuccessors().getSuccessorNode(key);
            removeUnreachableSubtree(s, splitAttributeIndex, splitValue, key.isLower());
        }
    }

    private void removeUnreachableSubtree(PlasticNode node, int splitAttributeIndex, double threshold, boolean isLower) {
        if (node.isLeaf())
            return;

        if (node.getSplitAttributeIndex() != splitAttributeIndex) {
            for (CustomEFDTNode successor: node.getSuccessors().getAllSuccessors()) {
                removeUnreachableSubtree((PlasticNode) successor, splitAttributeIndex, threshold, isLower);
            }
            return;
        }

        Set<SuccessorIdentifier> keysToRemove = new HashSet<>();
        for (SuccessorIdentifier key: node.getSuccessors().getKeyset()) {
            assert key.isNumeric();
            if (isLower) {
                if (!key.isLower() && key.getSelectorValue() >= threshold) {
                    keysToRemove.add(key);
                }
            }
            else {
                if (key.isLower() && key.getSelectorValue() <= threshold) {
                    keysToRemove.add(key);
                }
            }
        }
        for (SuccessorIdentifier key: keysToRemove) {
            node.getSuccessors().removeSuccessor(key);
        }

        if (!node.isLeaf()) {
            node.getSuccessors().getAllSuccessors().forEach(s -> removeUnreachableSubtree((PlasticNode) s, splitAttributeIndex, threshold, isLower));
        }
    }

    private void modifyBranch(PlasticBranch branch, Attribute splitAttribute) {
        putLastElementToFront(branch, splitAttribute);
        resetSuccessorsInBranch(branch);
        setRestructuredFlagInBranch(branch);
        setDepth(branch);
    }

    private void putLastElementToFront(PlasticBranch branch, Attribute splitAttribute) {
        PlasticTreeElement oldFirstBranchElement = branch.getBranchRef().getFirst();
        PlasticTreeElement newFirstBranchElement = branch.getBranchRef().remove(branch.getBranchRef().size() - 2);

        branch.getBranchRef().addFirst(newFirstBranchElement);
        if (splitAttribute != newFirstBranchElement.getNode().splitAttribute) {
            System.out.println(branch.getDescription());
        }

        PlasticNode oldFirstNode = oldFirstBranchElement.getNode();
        PlasticNode newFirstNode = newFirstBranchElement.getNode();

        //TODO not sure this is actually required! I think it could be sufficient to just change the successors when building the tree in a later step.
        newFirstNode.observedClassDistribution = oldFirstNode.observedClassDistribution;
        newFirstNode.depth = oldFirstNode.depth;
        newFirstNode.attributeObservers = oldFirstNode.attributeObservers;
    }

    private void resetSuccessorsInBranch(PlasticBranch branch) {
        if (branch.getBranchRef().size() == 1)
            return;
        int i = 0;
        for (PlasticTreeElement item: branch.getBranchRef()) {
            if (i == branch.getBranchRef().size() - 1)
                break;
            item.getNode().successors = new Successors(item.getNode().getSuccessors(), false);
            i++;
        }
    }

    private void setRestructuredFlagInBranch(PlasticBranch branch) {
        if (branch.getBranchRef().size() <= 2)
            return;
        int i = 0;
        for (PlasticTreeElement item: branch.getBranchRef()) {
            if (i == 0 || i == branch.getBranchRef().size() - 1) {
                i++;
                continue;
            }
            item.getNode().setRestructuredFlag();
            i++;
        }
    }

    private void setDepth(PlasticBranch branch) {
        PlasticNode firstNode = branch.getBranchRef().getFirst().getNode();
        int i = 0;
        for (PlasticTreeElement item: branch.getBranchRef()) {
            item.getNode().depth = firstNode.getDepth() + i;
            i++;
        }
    }
}
