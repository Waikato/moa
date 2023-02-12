package moa.sorts;

/**
 * Interface for sorting algorithms.
 */
public interface Sort<T extends Comparable<? super T>> {

    /**
     * Sorts the array in place.
     *
     * @param array
     */
    public void sort(T[] array);

}
