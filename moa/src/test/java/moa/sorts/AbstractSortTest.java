package moa.sorts;

import static org.junit.Assert.assertArrayEquals;

import moa.test.MoaTestCase;

public abstract class AbstractSortTest<T extends Comparable<? super T>> extends MoaTestCase {

    public class TestCase {

        private final T[] array;
        private final T[] expected;

        public TestCase(T[] array, T[] expected) {
            this.array = array;
            this.expected = expected;
        }

        public T[] getArray() {
            return array;
        }

        public T[] getExpected() {
            return expected;
        }

    }

    private Sort<T> sort;

    public AbstractSortTest(String name) {
        super(name);
    }

    protected abstract Sort<T> newSort();

    protected abstract Iterable<TestCase> getTestCases();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sort = newSort();
    }

    public void testSort() {
        for (var testCase : getTestCases()) {
            sort.sort(testCase.getArray());
            assertArrayEquals(testCase.getExpected(), testCase.getArray());
        }
    }
}
