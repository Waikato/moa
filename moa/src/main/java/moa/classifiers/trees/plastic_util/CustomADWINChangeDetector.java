package moa.classifiers.trees.plastic_util;

import moa.classifiers.core.driftdetection.ADWINChangeDetector;

public class CustomADWINChangeDetector extends ADWINChangeDetector {
    private boolean hadChange = false;

    @Override
    public boolean getChange() {
        boolean hadChangeCpy = hadChange;
        hadChange = false;
        return hadChangeCpy;
    }

    @Override
    public void input(double inputValue) {
        super.input(inputValue);
        if (!hadChange)
            hadChange = super.getChange();
    }

    public int getWidth() {
        if (adwin == null)
            return 0;
        return adwin.getWidth();
    }
}