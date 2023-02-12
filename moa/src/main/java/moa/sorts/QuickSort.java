package moa.sorts;

/**
 * Quick sort implementation.
 *
 * @param <T> the type of the elements to be sorted
 */
public class QuickSort<T extends Comparable<? super T>> implements Sort<T> {

    @Override
    public void sort(T[] array) {
        quickSort(array, 0, array.length - 1);
    }

    private void quickSort(T[] array, int left, int right) {
        if (left >= right || left < 0) {
            return;
        }
        int pivot = partition(array, left, right);
        quickSort(array, left, pivot - 1);
        quickSort(array, pivot + 1, right);
    }

    /**
     * Partitions the array around the pivot.
     *
     * @param array
     * @param left
     * @param right
     * @return
     */
    private int partition(T[] array, int left, int right) {
        T pivot = array[right];
        int i = left;
        // move all elements smaller than the pivot to the left
        for (int j = left; j < right; j++) {
            if (array[j].compareTo(pivot) < 0) {
                swap(array, i, j);
                i++;
            }
        }
        swap(array, i, right);
        return i;
    }

    /**
     * Swaps the elements at the given indices.
     *
     * @param array
     * @param i
     * @param j
     */
    private void swap(T[] array, int i, int j) {
        T temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

}
