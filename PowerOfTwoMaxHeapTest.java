import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * Tests for {@link PowerOfTwoMaxHeap}.
 *
 * Covers:
 *  - Edge cases: empty heap, single element, duplicate values
 *  - Small powers:  0, 1, 2, 3
 *  - Large powers: 10, 20, 30
 *  - Correctness: sorted output must match descending sort of inserted values
 *  - Stress test: large random input with multiple power values
 *  - Memory / resize: insert-then-pop cycling to exercise grow + shrink paths
 *  - Negative and extreme int values (Integer.MIN_VALUE, Integer.MAX_VALUE)
 */
public class PowerOfTwoMaxHeapTest {

    // -------------------------------------------------------------------------
    // Minimal test framework (no JUnit dependency)
    // -------------------------------------------------------------------------

    private static int passed = 0;
    private static int failed = 0;

    private static void assertTrue(String testName, boolean condition) {
        if (condition) {
            System.out.println("  PASS: " + testName);
            passed++;
        } else {
            System.out.println("  FAIL: " + testName);
            failed++;
        }
    }

    private static void assertThrows(String testName, Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
            System.out.println("  FAIL: " + testName + " (no exception thrown)");
            failed++;
        } catch (Throwable t) {
            if (expected.isInstance(t)) {
                System.out.println("  PASS: " + testName);
                passed++;
            } else {
                System.out.println("  FAIL: " + testName + " (wrong exception: " + t.getClass().getName() + ")");
                failed++;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Test helpers
    // -------------------------------------------------------------------------

    /**
     * Inserts all values into a fresh heap with the given power, then pops
     * every element and returns the pop order (should be descending sorted).
     */
    private static int[] insertAndDrainAll(int power, int[] values) {
        PowerOfTwoMaxHeap heap = new PowerOfTwoMaxHeap(power);
        for (int v : values) heap.insert(v);
        int[] result = new int[values.length];
        for (int i = 0; i < result.length; i++) result[i] = heap.popMax();
        return result;
    }

    /**
     * Returns true iff {@code arr} is in non-increasing order.
     */
    private static boolean isDescending(int[] arr) {
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[i - 1]) return false;
        }
        return true;
    }

    /**
     * Returns the expected descending-sorted version of values.
     */
    private static int[] expectedDescending(int[] values) {
        int[] sorted = values.clone();
        Arrays.sort(sorted);
        // reverse
        for (int l = 0, r = sorted.length - 1; l < r; l++, r--) {
            int tmp = sorted[l]; sorted[l] = sorted[r]; sorted[r] = tmp;
        }
        return sorted;
    }

    // -------------------------------------------------------------------------
    // Test suites
    // -------------------------------------------------------------------------

    static void testConstructorValidation() {
        System.out.println("\n--- Constructor validation ---");
        assertThrows("negative power throws", IllegalArgumentException.class,
                () -> new PowerOfTwoMaxHeap(-1));
        assertThrows("power > 30 throws", IllegalArgumentException.class,
                () -> new PowerOfTwoMaxHeap(31));
        // boundary values must not throw
        new PowerOfTwoMaxHeap(0);
        new PowerOfTwoMaxHeap(30);
        assertTrue("power=0 constructs OK", true);
        assertTrue("power=30 constructs OK", true);
    }

    static void testEmptyHeap() {
        System.out.println("\n--- Empty heap ---");
        for (int power : new int[]{0, 1, 2, 10}) {
            PowerOfTwoMaxHeap heap = new PowerOfTwoMaxHeap(power);
            assertTrue("isEmpty on new heap (power=" + power + ")", heap.isEmpty());
            assertTrue("size=0 on new heap (power=" + power + ")", heap.size() == 0);
            assertThrows("popMax on empty throws (power=" + power + ")",
                    NoSuchElementException.class, heap::popMax);
            assertThrows("peekMax on empty throws (power=" + power + ")",
                    NoSuchElementException.class, heap::peekMax);
        }
    }

    static void testSingleElement() {
        System.out.println("\n--- Single element ---");
        for (int power : new int[]{0, 1, 2, 5}) {
            PowerOfTwoMaxHeap heap = new PowerOfTwoMaxHeap(power);
            heap.insert(42);
            assertTrue("size=1 after one insert (power=" + power + ")", heap.size() == 1);
            assertTrue("peekMax=42 (power=" + power + ")", heap.peekMax() == 42);
            assertTrue("popMax=42 (power=" + power + ")", heap.popMax() == 42);
            assertTrue("isEmpty after popMax (power=" + power + ")", heap.isEmpty());
        }
    }

    static void testDuplicates() {
        System.out.println("\n--- Duplicate values ---");
        for (int power : new int[]{1, 2, 3}) {
            int[] values = {5, 5, 5, 5, 5};
            int[] result = insertAndDrainAll(power, values);
            assertTrue("all dupes pop in non-increasing order (power=" + power + ")",
                    isDescending(result));
            assertTrue("all dupes equal 5 (power=" + power + ")",
                    Arrays.stream(result).allMatch(v -> v == 5));
        }
    }

    static void testExtremeIntValues() {
        System.out.println("\n--- Extreme int values ---");
        for (int power : new int[]{1, 2, 10}) {
            int[] values = {Integer.MAX_VALUE, Integer.MIN_VALUE, 0, -1, 1,
                            Integer.MAX_VALUE, Integer.MIN_VALUE};
            int[] result = insertAndDrainAll(power, values);
            int[] expected = expectedDescending(values);
            assertTrue("extreme values sorted correctly (power=" + power + ")",
                    Arrays.equals(result, expected));
        }
    }

    static void testAllNegatives() {
        System.out.println("\n--- All negative values ---");
        for (int power : new int[]{1, 3}) {
            int[] values = {-1, -100, -50, -3, -999};
            int[] result = insertAndDrainAll(power, values);
            int[] expected = expectedDescending(values);
            assertTrue("all-negative sorted correctly (power=" + power + ")",
                    Arrays.equals(result, expected));
        }
    }

    static void testAlreadySortedInput() {
        System.out.println("\n--- Already sorted / reverse sorted input ---");
        for (int power : new int[]{1, 2, 4}) {
            int n = 100;
            int[] ascending  = new int[n];
            int[] descending = new int[n];
            for (int i = 0; i < n; i++) { ascending[i] = i; descending[i] = n - 1 - i; }

            int[] r1 = insertAndDrainAll(power, ascending);
            int[] r2 = insertAndDrainAll(power, descending);
            int[] exp = expectedDescending(ascending);
            assertTrue("ascending input → correct order (power=" + power + ")", Arrays.equals(r1, exp));
            assertTrue("descending input → correct order (power=" + power + ")", Arrays.equals(r2, exp));
        }
    }

    static void testSmallPowers() {
        System.out.println("\n--- Small power values (0, 1, 2, 3) ---");
        int[] values = {9, 4, 7, 1, 8, 3, 6, 2, 5, 0};
        int[] expected = expectedDescending(values);
        for (int power : new int[]{0, 1, 2, 3}) {
            int[] result = insertAndDrainAll(power, values);
            assertTrue("power=" + power + " produces correct sorted output",
                    Arrays.equals(result, expected));
        }
    }

    static void testLargePowers() {
        System.out.println("\n--- Large power values (10, 20, 30) ---");
        // With large powers the heap becomes very flat; correctness still required.
        Random rng = new Random(0xDEADBEEFL);
        int n = 200;
        int[] values = new int[n];
        for (int i = 0; i < n; i++) values[i] = rng.nextInt();
        int[] expected = expectedDescending(values);

        for (int power : new int[]{10, 20, 30}) {
            int[] result = insertAndDrainAll(power, values);
            assertTrue("power=" + power + " produces correct sorted output",
                    Arrays.equals(result, expected));
        }
    }

    static void testStressRandom() {
        System.out.println("\n--- Stress: large random input ---");
        Random rng = new Random(12345L);
        int n = 100_000;
        int[] values = new int[n];
        for (int i = 0; i < n; i++) values[i] = rng.nextInt();
        int[] expected = expectedDescending(values);

        for (int power : new int[]{1, 2, 3, 4, 8}) {
            long start = System.nanoTime();
            int[] result = insertAndDrainAll(power, values);
            long ms = (System.nanoTime() - start) / 1_000_000;
            boolean correct = Arrays.equals(result, expected);
            assertTrue("stress n=100000 power=" + power + " correct (took " + ms + " ms)", correct);
        }
    }

    static void testInterleavedInsertPop() {
        System.out.println("\n--- Interleaved insert / popMax ---");
        for (int power : new int[]{1, 2, 3}) {
            PowerOfTwoMaxHeap heap = new PowerOfTwoMaxHeap(power);
            // Push 1..10, pop 5, push 11..15, drain
            for (int i = 1; i <= 10; i++) heap.insert(i);
            int firstPop = heap.popMax();
            assertTrue("first pop is 10 (power=" + power + ")", firstPop == 10);
            for (int i = 11; i <= 15; i++) heap.insert(i);
            // Now heap contains {1..9, 11..15}; drain and verify order
            List<Integer> order = new ArrayList<>();
            while (!heap.isEmpty()) order.add(heap.popMax());
            boolean desc = true;
            for (int i = 1; i < order.size(); i++) {
                if (order.get(i) > order.get(i - 1)) { desc = false; break; }
            }
            assertTrue("interleaved insert/pop stays sorted (power=" + power + ")", desc);
            assertTrue("correct element count after interleaved ops (power=" + power + ")",
                    order.size() == 14); // 9 remaining from first batch + 5 from second
        }
    }

    static void testMemoryCycling() {
        System.out.println("\n--- Memory cycling (grow + shrink) ---");
        // Insert a large number then pop all — exercises both resize paths
        for (int power : new int[]{1, 4}) {
            PowerOfTwoMaxHeap heap = new PowerOfTwoMaxHeap(power);
            int n = 1024;
            for (int i = 0; i < n; i++) heap.insert(i);
            int prev = heap.popMax();
            boolean ok = true;
            for (int i = 1; i < n; i++) {
                int cur = heap.popMax();
                if (cur > prev) { ok = false; break; }
                prev = cur;
            }
            assertTrue("grow+shrink cycle preserves order (power=" + power + ")", ok);
            assertTrue("heap empty after full drain (power=" + power + ")", heap.isEmpty());
        }
    }

    static void testPeekDoesNotRemove() {
        System.out.println("\n--- peekMax does not remove element ---");
        PowerOfTwoMaxHeap heap = new PowerOfTwoMaxHeap(2);
        heap.insert(100);
        heap.insert(50);
        int peek1 = heap.peekMax();
        int peek2 = heap.peekMax();
        assertTrue("peekMax is idempotent", peek1 == peek2 && peek1 == 100);
        assertTrue("size still 2 after two peeks", heap.size() == 2);
    }

    // -------------------------------------------------------------------------
    // main
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println(" PowerOfTwoMaxHeap Test Suite");
        System.out.println("========================================");

        testConstructorValidation();
        testEmptyHeap();
        testSingleElement();
        testDuplicates();
        testExtremeIntValues();
        testAllNegatives();
        testAlreadySortedInput();
        testSmallPowers();
        testLargePowers();
        testStressRandom();
        testInterleavedInsertPop();
        testMemoryCycling();
        testPeekDoesNotRemove();

        System.out.println("\n========================================");
        System.out.printf(" Results: %d passed, %d failed%n", passed, failed);
        System.out.println("========================================");
        if (failed > 0) System.exit(1);
    }
}
