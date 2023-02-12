package moa.sorts.quicksort;

import moa.sorts.AbstractSortTest;
import moa.sorts.QuickSort;
import moa.sorts.Sort;

public abstract class AbstractQuickSortTest<T extends Comparable<? super T>> extends AbstractSortTest<T> {

    public AbstractQuickSortTest(String name) {
        super(name);
    }

    @Override
    protected Sort<T> newSort() {
        return new QuickSort<>();
    }

}
