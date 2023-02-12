package moa.sorts.quicksort;

import java.util.ArrayList;

public class IntegerQuickSortTest extends AbstractQuickSortTest<Integer> {

    public IntegerQuickSortTest(String name) {
        super(name);
    }

    @Override
    protected Iterable<TestCase> getTestCases() {
        ArrayList<TestCase> testCases = new ArrayList<>();
        // Empty array
        testCases.add(new TestCase(
                new Integer[] {},
                new Integer[] {}));
        // Single element
        testCases.add(new TestCase(
                new Integer[] { -1 },
                new Integer[] { -1 }));
        // Nominal cases
        testCases.add(new TestCase(
                new Integer[] { 2, 3, 1, 4, 5 },
                new Integer[] { 1, 2, 3, 4, 5 }));
        testCases.add(new TestCase(
                new Integer[] { -23, 43, 100, -500, 0, 1 },
                new Integer[] { -500, -23, 0, 1, 43, 100 }));
        // Already sorted
        testCases.add(new TestCase(
                new Integer[] { 6, 7, 8, 9, 10 },
                new Integer[] { 6, 7, 8, 9, 10 }));
        return testCases;
    }

}
