/**
 * A max-heap where every parent node has exactly 2^power children.
 *
 * <p>Examples:
 * <ul>
 *   <li>power = 0 → 1 child per parent  (degenerate linked list)</li>
 *   <li>power = 1 → 2 children per parent (classic binary max-heap)</li>
 *   <li>power = 2 → 4 children per parent (4-ary max-heap)</li>
 *   <li>power = 3 → 8 children per parent (8-ary max-heap)</li>
 * </ul>
 *
 * <p>All heap elements are stored in a single {@code int[]} array for
 * cache-friendly access and minimal allocation overhead.
 *
 * <p>Time complexity (n = number of elements, k = 2^power):
 * <ul>
 *   <li>insert  – O(log_k n) comparisons, O(1) amortised allocation</li>
 *   <li>popMax  – O(k · log_k n) comparisons</li>
 * </ul>
 */
public class PowerOfTwoMaxHeap {

    /** 2^power – the number of children every parent node must have. */
    private final int childCount;

    /** Backing store; heap occupies indices [0, size). */
    private int[] data;

    /** Number of elements currently in the heap. */
    private int size;

    /** Initial and minimum capacity of the backing array. */
    private static final int INITIAL_CAPACITY = 16;

    /**
     * Constructs a PowerOfTwoMaxHeap.
     *
     * @param power the exponent that defines child-count per parent (2^power).
     *              Must be >= 0. A value of 0 gives 1 child per parent;
     *              1 gives the classic binary heap.
     * @throws IllegalArgumentException if power is negative
     */
    public PowerOfTwoMaxHeap(int power) {
        if (power < 0) {
            throw new IllegalArgumentException(
                    "power must be >= 0, but was: " + power);
        }
        // 1 << power == 2^power; cap shift at 30 to stay within int range
        if (power > 30) {
            throw new IllegalArgumentException(
                    "power must be <= 30, but was: " + power);
        }
        this.childCount = 1 << power;          // 2^power
        this.data       = new int[INITIAL_CAPACITY];
        this.size       = 0;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns the number of elements currently in the heap. */
    public int size() {
        return size;
    }

    /** Returns {@code true} if the heap contains no elements. */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Inserts {@code value} into the heap.
     *
     * <p>The element is appended at the next available leaf position and then
     * sifted upward until the max-heap property is restored.
     *
     * @param value the integer to insert
     */
    public void insert(int value) {
        ensureCapacity();
        data[size] = value;
        siftUp(size);
        size++;
    }

    /**
     * Removes and returns the maximum element (the root).
     *
     * <p>The last element is moved to the root and then sifted downward,
     * always swapping with the largest child, until the heap property is
     * restored.
     *
     * @return the maximum integer in the heap
     * @throws java.util.NoSuchElementException if the heap is empty
     */
    public int popMax() {
        if (isEmpty()) {
            throw new java.util.NoSuchElementException("Heap is empty");
        }
        final int max = data[0];
        size--;
        if (size > 0) {
            data[0] = data[size]; // move last element to root
            siftDown(0);
        }
        shrinkIfNeeded();
        return max;
    }

    /**
     * Returns (without removing) the maximum element.
     *
     * @return the maximum integer in the heap
     * @throws java.util.NoSuchElementException if the heap is empty
     */
    public int peekMax() {
        if (isEmpty()) {
            throw new java.util.NoSuchElementException("Heap is empty");
        }
        return data[0];
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Sifts the element at {@code index} upward until the heap property holds.
     * Index arithmetic: parent of node i is (i - 1) / childCount.
     */
    private void siftUp(int index) {
        final int value = data[index];
        while (index > 0) {
            final int parentIndex = (index - 1) / childCount;
            if (data[parentIndex] >= value) {
                break; // heap property satisfied
            }
            data[index] = data[parentIndex]; // pull parent down
            index = parentIndex;
        }
        data[index] = value;
    }

    /**
     * Sifts the element at {@code index} downward until the heap property
     * holds. At each step the child with the largest value is chosen.
     * First-child index: i * childCount + 1.
     */
    private void siftDown(int index) {
        final int value = data[index];
        while (true) {
            // Use long arithmetic to avoid int overflow when childCount is large
            // (e.g. power=30 → childCount=2^30; index*childCount can exceed Integer.MAX_VALUE)
            final long firstChildLong = (long) index * childCount + 1;
            if (firstChildLong >= size) {
                break; // no children — we are at a leaf
            }
            final int firstChild = (int) firstChildLong;

            // Find the index of the largest child in [firstChild, lastChild]
            final long lastChildLong = Math.min(firstChildLong + childCount, size); // exclusive
            final int lastChild = (int) lastChildLong;
            int maxChildIndex = firstChild;
            for (int c = firstChild + 1; c < lastChild; c++) {
                if (data[c] > data[maxChildIndex]) {
                    maxChildIndex = c;
                }
            }

            if (data[maxChildIndex] <= value) {
                break; // heap property satisfied
            }
            data[index] = data[maxChildIndex]; // pull largest child up
            index = maxChildIndex;
        }
        data[index] = value;
    }

    /** Doubles the backing array when it is full. */
    private void ensureCapacity() {
        if (size == data.length) {
            data = java.util.Arrays.copyOf(data, data.length * 2);
        }
    }

    /**
     * Halves the backing array when it is less than 25% full and larger than
     * the initial capacity, preventing unbounded memory retention.
     */
    private void shrinkIfNeeded() {
        if (data.length > INITIAL_CAPACITY && size <= data.length / 4) {
            data = java.util.Arrays.copyOf(data, Math.max(data.length / 2, INITIAL_CAPACITY));
        }
    }
}
