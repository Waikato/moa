package moa.sorts.quicksort;

import java.util.ArrayList;

public class StringQuickSortTest extends AbstractQuickSortTest<String> {

    public StringQuickSortTest(String name) {
        super(name);
    }

    @Override
    protected Iterable<TestCase> getTestCases() {
        ArrayList<TestCase> testCases = new ArrayList<>();
        // Empty array
        testCases.add(new TestCase(
                new String[] {},
                new String[] {}));
        // Single element
        testCases.add(new TestCase(
                new String[] { "test case" },
                new String[] { "test case" }));
        // Nominal cases
        testCases.add(new TestCase(
                new String[] { "test", "case", "this", "is", "a" },
                new String[] { "a", "case", "is", "test", "this" }));
        // with capital letters
        testCases.add(new TestCase(
                new String[] { "Test", "case", "this", "Is", "a" },
                new String[] { "Is", "Test", "a", "case", "this" }));
        // with numbers
        testCases.add(new TestCase(
                new String[] { "test", "3", "case", "2", "this", "is", "a", "1" },
                new String[] { "1", "2", "3", "a", "case", "is", "test", "this" }));
        // Already sorted
        testCases.add(new TestCase(
                new String[] { "a", "b", "c", "d", "e" },
                new String[] { "a", "b", "c", "d", "e" }));
        return testCases;
    }

}
