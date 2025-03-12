package moa.classifiers.trees.plastic_util;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.core.AttributeSplitSuggestion;
import moa.classifiers.core.attributeclassobservers.NominalAttributeClassObserver;
import moa.classifiers.core.splitcriteria.SplitCriterion;
import moa.core.DoubleVector;

import java.util.LinkedList;
import java.util.List;

public class CustomHTNode extends CustomEFDTNode {

    public CustomHTNode(SplitCriterion splitCriterion,
                        int gracePeriod,
                        Double confidence,
                        Double adaptiveConfidence,
                        boolean useAdaptiveConfidence,
                        String leafPrediction,
                        Integer depth,
                        Integer maxDepth,
                        Double tau,
                        boolean binaryOnly,
                        boolean noPrePrune,
                        NominalAttributeClassObserver nominalObserverBlueprint,
                        DoubleVector observedClassDistribution,
                        List<Integer> usedNominalAttributes,
                        int blockedAttributeIndex) {
        super(splitCriterion, gracePeriod, confidence, adaptiveConfidence, useAdaptiveConfidence, leafPrediction,
                Integer.MAX_VALUE, depth, maxDepth, tau, 0.0, 0.0, binaryOnly, noPrePrune,
                nominalObserverBlueprint, observedClassDistribution, usedNominalAttributes, blockedAttributeIndex);

    }

    @Override
    protected void reevaluateSplit(Instance instance) {
    }

    @Override
    protected CustomHTNode newNode(int depth, DoubleVector classDistribution, List<Integer> usedNominalAttributes) {
        return new CustomHTNode(
                splitCriterion, gracePeriod, confidence, adaptiveConfidence, useAdaptiveConfidence,
                leafPrediction, depth, maxDepth,
                tau, binaryOnly, noPrePrune, nominalObserverBlueprint,
                classDistribution, new LinkedList<>(), -1  // we don't block attributes in HT
        );
    }

    @Override
    protected boolean shouldSplitLeaf(AttributeSplitSuggestion[] suggestions,
                                    double confidence,
                                    DoubleVector observedClassDistribution
    ) {
        boolean shouldSplit;
        if (suggestions.length < 2) {
            shouldSplit = suggestions.length > 0;
        } else {
            AttributeSplitSuggestion bestSuggestion = suggestions[suggestions.length - 1];
            AttributeSplitSuggestion secondBestSuggestion = suggestions[suggestions.length - 2];

            double bestSuggestionAverageMerit = bestSuggestion.merit;
            double currentAverageMerit = secondBestSuggestion.merit;
            double eps = computeHoeffdingBound();

            shouldSplit = bestSuggestionAverageMerit - currentAverageMerit > eps || eps < tau;
            if (bestSuggestion.merit < 1e-10)
                shouldSplit = false; // we don't use average here

            if (shouldSplit) {
                for (Integer i : usedNominalAttributes) {
                    if (bestSuggestion.splitTest.getAttsTestDependsOn()[0] == i) {
                        shouldSplit = false;
                        break;
                    }
                }
            }
        }
        return shouldSplit;
    }
}
