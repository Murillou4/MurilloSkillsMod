package com.murilloskills.skills;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UltmineDropCollectorTest {
    private static final UltmineDropCollector.StackAdapter<FakeStack> ADAPTER =
            new UltmineDropCollector.StackAdapter<>() {
                @Override
                public boolean isEmpty(FakeStack stack) {
                    return stack == null || stack.count() <= 0;
                }

                @Override
                public boolean isTrash(FakeStack stack) {
                    return stack.trash();
                }

                @Override
                public int getCount(FakeStack stack) {
                    return stack.count();
                }

                @Override
                public int getMaxCount(FakeStack stack) {
                    return stack.maxCount();
                }

                @Override
                public boolean canMerge(FakeStack existing, FakeStack source) {
                    return existing.id().equals(source.id());
                }

                @Override
                public void increment(FakeStack stack, int amount) {
                    stack.increment(amount);
                }

                @Override
                public FakeStack copyWithCount(FakeStack stack, int count) {
                    return stack.copyWithCount(count);
                }
            };

    @Test
    void mergesCompatibleStacks() {
        List<FakeStack> merged = new ArrayList<>();
        UltmineDropCollector.addMerged(merged, new FakeStack("cobblestone", 40), ADAPTER);
        UltmineDropCollector.addMerged(merged, new FakeStack("cobblestone", 40), ADAPTER);

        assertEquals(2, merged.size());
        assertEquals("cobblestone", merged.get(0).id());
        assertEquals(64, merged.get(0).count());
        assertEquals(16, merged.get(1).count());
    }

    @Test
    void discardsTrashBeforeMerging() {
        List<FakeStack> merged = new ArrayList<>();
        UltmineDropCollector.addMerged(merged, new FakeStack("cobblestone", 64, true), ADAPTER);
        UltmineDropCollector.addMerged(merged, new FakeStack("diamond", 3), ADAPTER);

        assertEquals(1, merged.size());
        assertEquals("diamond", merged.getFirst().id());
        assertEquals(3, merged.getFirst().count());
    }

    @Test
    void keepsOverflowAsCompactStacks() {
        List<FakeStack> merged = new ArrayList<>();
        UltmineDropCollector.addMerged(merged, new FakeStack("cobblestone", 64), ADAPTER);
        UltmineDropCollector.addMerged(merged, new FakeStack("cobblestone", 64), ADAPTER);
        UltmineDropCollector.addMerged(merged, new FakeStack("cobblestone", 2), ADAPTER);

        assertEquals(3, merged.size());
        assertEquals(64, merged.get(0).count());
        assertEquals(64, merged.get(1).count());
        assertEquals(2, merged.get(2).count());
    }

    @Test
    void mergesIntoExistingPartialStackBeforeAddingOverflow() {
        List<FakeStack> destination = new ArrayList<>();
        destination.add(new FakeStack("cobblestone", 60));

        UltmineDropCollector.addMerged(destination, new FakeStack("cobblestone", 10), ADAPTER);

        assertEquals(2, destination.size());
        assertEquals(64, destination.get(0).count());
        assertEquals(6, destination.get(1).count());
    }

    private static final class FakeStack {
        private final String id;
        private final int maxCount;
        private final boolean trash;
        private int count;

        private FakeStack(String id, int count) {
            this(id, count, false);
        }

        private FakeStack(String id, int count, boolean trash) {
            this(id, count, 64, trash);
        }

        private FakeStack(String id, int count, int maxCount, boolean trash) {
            this.id = id;
            this.count = count;
            this.maxCount = maxCount;
            this.trash = trash;
        }

        private String id() {
            return id;
        }

        private int count() {
            return count;
        }

        private int maxCount() {
            return maxCount;
        }

        private boolean trash() {
            return trash;
        }

        private void increment(int amount) {
            count += amount;
        }

        private FakeStack copyWithCount(int newCount) {
            return new FakeStack(id, newCount, maxCount, trash);
        }
    }
}
