package moa.clusterers.clustree.util;

/**
 * A simple implementation of a <code>Budget</code> with relative
 * cost constants for each operation.
 * @author reidl
 * @see Budget
 */
public class SimpleBudget implements Budget {

    public static final int INT_ADD = 1;
    public static final int INT_MULT = 1;
    public static final int INT_DIV = 1;
    public static final int DOUBLE_ADD = 1;
    public static final int DOUBLE_MULT = 1;
    public static final int DOUBLE_DIV = 10;
    private int time;

    public SimpleBudget(int time) {
        assert (time >= 0);
        this.time = time;
    }

    @Override
    public boolean hasMoreTime() {
        return time > 0;
    }

    @Override
    public void integerAddition() {
        time -= INT_ADD;
    }

    @Override
    public void integerAddition(int number) {
        time -= INT_ADD * number;
    }

    @Override
    public void doubleAddition() {
        time -= DOUBLE_ADD;
    }

    @Override
    public void doubleAddition(int number) {
        time -= DOUBLE_ADD * number;
    }

    @Override
    public void integerMultiplication() {
        time -= INT_MULT;
    }

    @Override
    public void integerMultiplication(int number) {
        time -= INT_MULT * number;
    }

    @Override
    public void doubleMultiplication() {
        time -= DOUBLE_MULT;
    }

    @Override
    public void doubleMultiplication(int number) {
        time -= DOUBLE_MULT * number;
    }

    @Override
    public void integerDivision() {
        time -= INT_DIV;
    }

    @Override
    public void integerDivision(int number) {
        time -= INT_DIV * number;
    }

    @Override
    public void doubleDivision() {
        time -= DOUBLE_DIV;
    }

    @Override
    public void doubleDivision(int number) {
        time -= DOUBLE_DIV * number;
    }
}
