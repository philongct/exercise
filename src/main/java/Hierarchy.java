import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// The task:
// 1. Read and understand the Hierarchy data structure described in this file.
// 2. Implement filter() method.
// 3. Implement more test cases.
//
// The task should take 30-90 minutes.
//
// When assessing the submission, we will pay attention to:
// - correctness, efficiency, and clarity of the code;
// - the test cases.

/**
 * A {@code Hierarchy} stores an arbitrary <i>forest</i> (an ordered collection of ordered trees)
 * as an array of node IDs in the order of DFS traversal, combined with a parallel array of node depths.
 *
 * <p>Parent-child relationships are identified by the position in the array and the associated depth.
 * Each tree root has depth 0, its children have depth 1 and follow it in the array, their children have depth 2 and follow them, etc.
 *
 * <p>Example:
 * <pre>
 * nodeIds: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
 * depths:  0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2
 * </pre>
 *
 * <p>the forest can be visualized as follows:
 * <pre>
 * 1
 * - 2
 * - - 3
 * - - - 4
 * - 5
 * 6
 * - 7
 * 8
 * - 9
 * - 10
 * - - 11
 * </pre>
 * 1 is a parent of 2 and 5, 2 is a parent of 3, etc. Note that depth is equal to the number of hyphens for each node.
 *
 * <p>Invariants on the depths array:
 * <ul>
 *   <li>Depth of the first element is 0.</li>
 *   <li>If the depth of a node is {@code D}, the depth of the next node in the array can be:
 *     <ul>
 *       <li>{@code D + 1} if the next node is a child of this node;</li>
 *       <li>{@code D} if the next node is a sibling of this node;</li>
 *       <li>{@code d < D} - in this case the next node is not related to this node.</li>
 *     </ul>
 *   </li>
 * </ul>
 */
interface Hierarchy {
    /** The number of nodes in the hierarchy. */
    int size();

    /**
     * Returns the unique ID of the node identified by the hierarchy index. The depth for this node will be {@code depth(index)}.
     * @param index must be non-negative and less than {@link #size()}
     */
    int nodeId(int index);

    /**
     * Returns the depth of the node identified by the hierarchy index. The unique ID for this node will be {@code nodeId(index)}.
     * @param index must be non-negative and less than {@link #size()}
     */
    int depth(int index);

    default String formatString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(nodeId(i)).append(":").append(depth(i));
        }
        sb.append("]");
        return sb.toString();
    }
}

/**
 * A node is present in the filtered hierarchy iff its node ID passes the predicate and all of its ancestors pass it as well.
 */
class HierarchyFilter {
    public static Hierarchy filter(Hierarchy hierarchy, java.util.function.IntPredicate nodeIdPredicate) {
        // Assumed that this is a bounded array
        int n = hierarchy.size();
        int[] resultIds = new int[n];
        int[] resultDepths = new int[n];
        int count = 0;

        // Assumed that the first depth is always 0
        if (n > 0 && hierarchy.depth(0) != 0) {
            throw new RuntimeException("Invalid forest");
        }

        if (n==0) {
            return new ArrayBasedHierarchy(new int[0], new int[0]);
        }

        int blockedAtDepth = -1;
        int previousDepth = hierarchy.depth(0);

        // Assumed that we don't have any negative depth value
        for (int i = 0; i < n; i++) {
            int id = hierarchy.nodeId(i);
            int depth = hierarchy.depth(i);

            if (depth > previousDepth + 1) {
                throw new RuntimeException("Invalid depth");
            }
            previousDepth = depth;

            // A node with the same or shallower depth clears any active block
            if (blockedAtDepth >= 0 && depth <= blockedAtDepth) {
                blockedAtDepth = -1;
            }

            // Skip descendants of a failed ancestor
            if (blockedAtDepth >= 0) {
                continue;
            }

            if (nodeIdPredicate.test(id)) {
                resultIds[count] = id;
                resultDepths[count] = depth;
                count++;
            } else {
                blockedAtDepth = depth;
            }
        }

        return new ArrayBasedHierarchy(
                Arrays.copyOf(resultIds, count),
                Arrays.copyOf(resultDepths, count)
        );
    }
}

class ArrayBasedHierarchy implements Hierarchy {
    private final int[] nodeIds;
    private final int[] depths;

    public ArrayBasedHierarchy(int[] nodeIds, int[] depths) {
        this.nodeIds = nodeIds;
        this.depths = depths;
    }

    @Override
    public int size() {
        return depths.length;
    }

    @Override
    public int nodeId(int index) {
        return nodeIds[index];
    }

    @Override
    public int depth(int index) {
        return depths[index];
    }
}

class FilterTest {

    static class TestValueProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                // Case 0: Extra test data
                // node id=6 fails at depth 2, all nodeids=2 (decendants) must fail,
                // then it get reset at node id=4, nodeids=5 (decendants) pass
                // then it continue to fail at nodeid=9, nodeid=8 fail, then come back to 1 and pass
                Arguments.of(
                    new ArrayBasedHierarchy(
                        new int[]{1, 1, 1, 1, 1, 6, 2, 2, 2, 2, 2, 4, 5, 5, 9, 8, 11, 11},
                        new int[]{0, 1, 2, 3, 4, 2, 3, 4, 5, 3, 4, 2, 3, 4, 2, 3, 1, 2}),
                    new ArrayBasedHierarchy(
                        new int[]{1, 1, 1, 1, 1, 4, 5, 5, 11, 11},
                        new int[]{0, 1, 2, 3, 4, 2, 3, 4, 1, 2})
                ),
                // Case 1: Extra test data 2
                // it fail at nodeid=3, depth=5 then nodeids=2 must pass
                // Then reset depth=4 at nodeid=4, after that decrease depth and increase again
                Arguments.of(
                    new ArrayBasedHierarchy(
                        new int[]{1, 1, 1, 1, 1, 3, 2, 2, 4, 7, 1, 3, 3, 5, 7, 8},
                        new int[]{0, 1, 2, 3, 4, 5, 3, 4, 5, 4, 3, 2, 2, 1, 2, 3}),
                    new ArrayBasedHierarchy(
                        new int[]{1, 1, 1, 1, 1, 2, 2, 4, 7, 1, 5, 7, 8},
                        new int[]{0, 1, 2, 3, 4, 3, 4, 5, 4, 3, 1, 2, 3})
                ),
                // Case 1: Node ids decreasing
                Arguments.of(
                    new ArrayBasedHierarchy(
                        new int[]{11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1},
                        new int[]{0,   1, 2, 3, 1, 0, 1, 0, 1, 1, 2}),
                    new ArrayBasedHierarchy(
                        new int[]{11, 10, 7, 4, 2, 1},
                        new int[]{0,   1, 1, 0, 1, 2})
                ),
                // Case 2: Random node ids distribution
                Arguments.of(
                    new ArrayBasedHierarchy(
                            new int[]{0, 5, 2, 8, 3, 4, 7, 1, 6, 11, 9, 10},
                            new int[]{0, 0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2}),
                    new ArrayBasedHierarchy(
                            new int[]{5, 2, 8, 4, 7, 1},
                            new int[]{0, 1, 2, 1, 0, 1})
                ),
                // Case 3: Same node id
                Arguments.of(
                    new ArrayBasedHierarchy(
                            new int[]{6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6},
                            new int[]{0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2}),
                    new ArrayBasedHierarchy(
                            new int[0],
                            new int[0])
                ),
                // Case 4: All root pass
                Arguments.of(
                    new ArrayBasedHierarchy(
                        new int[]{1, 2, 4, 2, 5, 7, 8, 11, 13, 17, 22},
                        new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
                    new ArrayBasedHierarchy(
                        new int[]{1, 2, 4, 2, 5, 7, 8, 11, 13, 17, 22},
                        new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0})
                ),
                // Case 5: All root fail
                Arguments.of(
                        new ArrayBasedHierarchy(
                                new int[]{3, 6, 9, 15, 6, 3, 27},
                                new int[]{0, 0, 0, 0, 0, 0, 0}),
                        new ArrayBasedHierarchy(
                                new int[0],
                                new int[0])
                ),
                // Case 7: All node pass
                Arguments.of(
                    new ArrayBasedHierarchy(
                        new int[]{1, 2, 2, 4, 5, 8, 7},
                        new int[]{0, 1, 2, 3, 4, 5, 6}),
                    new ArrayBasedHierarchy(
                        new int[]{1, 2, 2, 4, 5, 8, 7},
                        new int[]{0, 1, 2, 3, 4, 5, 6})
                ),
                // Case 8: All node fail
                Arguments.of(
                    new ArrayBasedHierarchy(
                        new int[]{3, 6, 9, 15, 6, 3, 27},
                        new int[]{0, 1, 2, 3, 4, 5, 6}),
                    new ArrayBasedHierarchy(
                        new int[0],
                        new int[0])
                ),
                // Case 9: Root 1 fails -> entire subtree (2, 3) excluded. Root 4 remained.
                Arguments.of(
                    new ArrayBasedHierarchy(
                        new int[]{3, 2, 3, 4},
                        new int[]{0, 1, 2, 0}),
                    new ArrayBasedHierarchy(
                        new int[]{4},
                        new int[]{0})
                ),
                // Case 10: Single node pass
                Arguments.of(
                    new ArrayBasedHierarchy(
                        new int[]{2},
                        new int[]{0}),
                    new ArrayBasedHierarchy(
                        new int[]{2},
                        new int[]{0})
                ),
                // Case 11: Single node failed
                Arguments.of(
                    new ArrayBasedHierarchy(
                        new int[]{3},
                        new int[]{0}),
                    new ArrayBasedHierarchy(
                        new int[0],
                        new int[0])
                ),
                // Case 12: Empty hierarchy
                Arguments.of(
                    new ArrayBasedHierarchy(
                        new int[0],
                        new int[0]),
                    new ArrayBasedHierarchy(
                        new int[0],
                        new int[0])
                )
            );
        }
    }


    @Test
    void testFilter() {
        Hierarchy unfiltered = new ArrayBasedHierarchy(
                new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11},
                new int[]{0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2}
        );
        Hierarchy filteredActual = HierarchyFilter.filter(unfiltered, nodeId -> nodeId % 3 != 0);
        Hierarchy filteredExpected = new ArrayBasedHierarchy(
                new int[]{1, 2, 5, 8, 10, 11},
                new int[]{0, 1, 1, 0, 1, 2}
        );
        assertEquals(filteredExpected.formatString(), filteredActual.formatString());
    }

    @ParameterizedTest
    @ArgumentsSource(TestValueProvider.class)
    void testMoreForests(ArrayBasedHierarchy unfiltered, ArrayBasedHierarchy expected) {
        Hierarchy filteredActual = HierarchyFilter.filter(unfiltered, nodeId -> nodeId % 3 != 0);
        assertEquals(expected.formatString(), filteredActual.formatString());
    }

    @Test
    void testFirstElementNotEqualsToZero() {
        Hierarchy empty = new ArrayBasedHierarchy(new int[]{10}, new int[]{1});
        assertThrows(RuntimeException.class, () -> HierarchyFilter.filter(empty, nodeId -> nodeId % 3 != 0));
    }

    @Test
    void testTreeNotInSequence() {
        Hierarchy empty = new ArrayBasedHierarchy(new int[]{1, 2, 3, 4, 5}, new int[]{0, 2, 0 , 1, 2});
        assertThrows(RuntimeException.class, () -> HierarchyFilter.filter(empty, nodeId -> nodeId % 3 != 0));
    }

    @Test
    void testVeryLargeHierarchy() {
        int n = 1000_000;
        int[] ids = new int[n];
        int[] depths = new int[n];

        Arrays.fill(ids, 5);
        Arrays.fill(ids, 0);

        Hierarchy input = new ArrayBasedHierarchy(ids, depths);
        assertEquals("[]", HierarchyFilter.filter(input, id -> false).formatString());
    }
}