package moa.sorts.quicksort;

import java.util.ArrayList;

public class FloatQuickSortTest extends AbstractQuickSortTest<Float> {

    public FloatQuickSortTest(String name) {
        super(name);
    }

    @Override
    protected Iterable<TestCase> getTestCases() {
        ArrayList<TestCase> testCases = new ArrayList<>();
        // Empty array
        testCases.add(new TestCase(
                new Float[] {},
                new Float[] {}));
        // Single element
        testCases.add(new TestCase(
                new Float[] { -1.0f },
                new Float[] { -1.0f }));
        // Nominal cases
        testCases.add(new TestCase(
                new Float[] { 2.0f, 3.0f, 1.0f, 4.0f, 5.0f },
                new Float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }));
        testCases.add(new TestCase(
                new Float[] { -23.0f, 43.0f, 100.0f, -500.0f, 0.0f, 1.0f },
                new Float[] { -500.0f, -23.0f, 0.0f, 1.0f, 43.0f, 100.0f }));
        // Already sorted
        testCases.add(new TestCase(
                new Float[] { 6.0f, 7.0f, 8.0f, 9.0f, 10.0f },
                new Float[] { 6.0f, 7.0f, 8.0f, 9.0f, 10.0f }));
        return testCases;
    }

}
