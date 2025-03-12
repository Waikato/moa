package moa.classifiers.trees.plastic_util;

import moa.AbstractMOAObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

class Successors extends AbstractMOAObject {
    private Double referenceValue;
    private HashMap<SuccessorIdentifier, CustomEFDTNode> successors = new HashMap<>();

    public Successors(Successors other, boolean transferNodes) {
        isBinarySplit = other.isBinary();
        isNumericSplit = !other.isNominal();
        referenceValue = other.getReferenceValue();
        if (transferNodes) {
            successors = new HashMap<>(other.successors);
        }
    }

    public Successors(boolean isBinarySplit, boolean isNumericSplit, Double splitValue) {
        this.isBinarySplit = isBinarySplit;
        this.isNumericSplit = isNumericSplit;
        this.referenceValue = splitValue;
    }

    private final boolean isBinarySplit;
    private final boolean isNumericSplit;


    protected boolean addSuccessor(CustomEFDTNode node, SuccessorIdentifier key) {
        if (node == null)
            return false;
        if (isNumericSplit != key.isNumeric())
            return false;
        if (successors.size() >= 2 && isBinary()) {
            return false;
        }
        if (successors.containsKey(key)) {
            return false;
        }
        successors.put(key, node);
        return true;
    }


    public boolean addSuccessorNumeric(Double attValue, CustomEFDTNode n, boolean isLower) {
        if (n == null)
            return false;
        if (!isNumericSplit)
            return false;
        if (successors.size() >= 2)
            return false;
        if (referenceValue != null && !referenceValue.equals(attValue))
            return false;

        SuccessorIdentifier id = new SuccessorIdentifier(true, attValue, attValue, isLower);
        if (successors.containsKey(id))
            return false;

        referenceValue = attValue;
        successors.put(id, n);
        return true;
    }


    public boolean addSuccessorNominalBinary(Double attValue, CustomEFDTNode n) {
        if (n == null)
            return false;
        if (isNumericSplit)
            return false;
        if (!isBinarySplit)
            return false;
        if (successors.size() >= 2)
            return false;
        if (successors.size() == 1) {
            SuccessorIdentifier key = (SuccessorIdentifier) successors.keySet().toArray()[0];  // get key of existing successor
            if (key.getValue() == SuccessorIdentifier.DEFAULT_NOMINAL_VALUE) { // check if the key is the default key for nominal values
                if (!referenceValue.equals(attValue)) // if the key is the default key, only add the successor if the referenceValue of the split matches the provided value
                    return false;
            }
        }

        SuccessorIdentifier id = new SuccessorIdentifier(false, attValue, attValue, false);
        if (successors.containsKey(id))
            return false;

        referenceValue = attValue;
        successors.put(id, n);
        return true;
    }

    public boolean addDefaultSuccessorNominalBinary(CustomEFDTNode n) {
        if (n == null)
            return false;
        if (isNumericSplit)
            return false;
        if (!isBinarySplit)
            return false;
        if (successors.size() >= 2)
            return false;

        SuccessorIdentifier id = new SuccessorIdentifier(false, referenceValue, SuccessorIdentifier.DEFAULT_NOMINAL_VALUE, false);
        if (successors.containsKey(id))
            return false;

        successors.put(id, n);
        return true;
    }

    public boolean addSuccessorNominalMultiway(Double attValue, CustomEFDTNode n) {
        if (n == null)
            return false;
        if (isNumericSplit)
            return false;
        if (isBinarySplit)
            return false;

        SuccessorIdentifier id = new SuccessorIdentifier(false, attValue, attValue, false);
        if (successors.containsKey(id))
            return false;

        successors.put(id, n);
        return true;
    }

    public CustomEFDTNode getSuccessorNode(SuccessorIdentifier key) {
        return successors.get(key);
    }

    public CustomEFDTNode getSuccessorNode(Double attributeValue) {
        for (SuccessorIdentifier s : successors.keySet()) {
            if (s.equals(attributeValue))
                return successors.get(s);
        }
        return null;
    }

    public SuccessorIdentifier getSuccessorKey(Object key) {
        //TODO: Looping over a set is probably not the best way to do this.
        for (SuccessorIdentifier successorKey : successors.keySet()) {
            if (successorKey.equals(key)) {
                return successorKey;
            }
        }
        return null;
    }

    public boolean isNominal() {
        return !isNumericSplit;
    }

    public boolean isBinary() {
        return isBinarySplit;
    }

    public boolean contains(Object key) {
        return successors.containsKey((SuccessorIdentifier) key);
    }

    public Double getReferenceValue() {
        return referenceValue;
    }

    public int size() {
        return successors.size();
    }

    public SuccessorIdentifier getMissingKey() {
        if (successors.size() > 1)
            return null;
        SuccessorIdentifier someKey = (SuccessorIdentifier) successors.keySet().toArray()[0];
        return someKey.getOther();
    }

    public boolean lowerIsMissing() {
        SuccessorIdentifier key = getMissingKey();
        if (key == null)
            return false;
        return key.isLower();
    }

    public boolean upperIsMissing() {
        SuccessorIdentifier key = getMissingKey();
        if (key == null)
            return false;
        return !key.isLower();
    }

    public void adjustThreshold(double newThreshold) {
        HashMap<SuccessorIdentifier, CustomEFDTNode> newSuccessors = new HashMap<>();
        for (SuccessorIdentifier oldId: successors.keySet()) {
            SuccessorIdentifier newId = new SuccessorIdentifier(true, newThreshold, newThreshold, oldId.isLower());
            newSuccessors.put(newId, successors.get(oldId));
        }
        referenceValue = newThreshold;
        successors = newSuccessors;
    }

    public Collection<CustomEFDTNode> getAllSuccessors() {
        return successors.values();
    }

    public Set<SuccessorIdentifier> getKeyset() {
        return successors.keySet();
    }

    protected void forceSuccessorForKey(SuccessorIdentifier key, CustomEFDTNode node) {
        successors.put(key, node);
    }

    protected CustomEFDTNode removeSuccessor(SuccessorIdentifier key) {
        return successors.remove(key);
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {

    }
}
