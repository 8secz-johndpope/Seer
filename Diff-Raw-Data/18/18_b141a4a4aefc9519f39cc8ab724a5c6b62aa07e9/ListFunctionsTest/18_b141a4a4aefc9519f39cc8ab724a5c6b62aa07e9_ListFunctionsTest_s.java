 package nodebox.function;
 
 import com.google.common.collect.ImmutableList;
 import com.google.common.collect.Iterables;
 import nodebox.node.polygraph.Point;
 import org.junit.Test;
 
 import static junit.framework.Assert.assertEquals;
 
 public class ListFunctionsTest {
 
     @Test
     public void testCount() {
         assertEquals(0, ListFunctions.count(null));
         assertEquals(0, ListFunctions.count(ImmutableList.of()));
         assertEquals(3, ListFunctions.count(ImmutableList.of(1, 2, 3)));
     }
 
     @Test
     public void testFirst() {
         assertElements(ListFunctions.first(ImmutableList.of()));
         assertElements(ListFunctions.first(ImmutableList.of(1)), 1);
         assertElements(ListFunctions.first(ImmutableList.of(1, 2)), 1);
         assertElements(ListFunctions.first(ImmutableList.of(1, 2, 3)), 1);
     }
 
     @Test
     public void testSecond() {
         assertElements(ListFunctions.second(ImmutableList.of()));
         assertElements(ListFunctions.second(ImmutableList.of(1)));
         assertElements(ListFunctions.second(ImmutableList.of(1, 2)), 2);
         assertElements(ListFunctions.second(ImmutableList.of(1, 2, 3)), 2);
     }
 
     @Test
     public void testRest() {
         assertElements(ListFunctions.rest(ImmutableList.of()));
         assertElements(ListFunctions.rest(ImmutableList.of(1)));
         assertElements(ListFunctions.rest(ImmutableList.of(1, 2)), 2);
         assertElements(ListFunctions.rest(ImmutableList.of(1, 2, 3)), 2, 3);
     }
 
     @Test
     public void testLast() {
         assertElements(ListFunctions.last(ImmutableList.of()));
         assertElements(ListFunctions.last(ImmutableList.of(1)), 1);
         assertElements(ListFunctions.last(ImmutableList.of(1, 2)), 2);
         assertElements(ListFunctions.last(ImmutableList.of(1, 2, 3)), 3);
     }
 
     @Test
     public void testCombine() {
         assertElements(ListFunctions.combine(ImmutableList.of(), ImmutableList.of(), null));
         assertElements(ListFunctions.combine(ImmutableList.of(1), ImmutableList.of(), null), 1);
         assertElements(ListFunctions.combine(ImmutableList.of(1), ImmutableList.of(2), null), 1, 2);
         assertElements(ListFunctions.combine(ImmutableList.of(1), ImmutableList.of(2), ImmutableList.of(3)), 1, 2, 3);
     }
 
     @Test
     public void testSubList() {
         assertElements(ListFunctions.slice(ImmutableList.of(1, 2, 3, 4), 0, 100), 1, 2, 3, 4);
         assertElements(ListFunctions.slice(ImmutableList.of(), 100, 100));
         assertElements(ListFunctions.slice(ImmutableList.of(1, 2, 3, 4), 1, 2), 2, 3);
         assertElements(ListFunctions.slice(ImmutableList.of(1, 2, 3, 4), 100, 2));
     }
 
     @Test
     public void testShift() {
         assertElements(ListFunctions.shift(ImmutableList.of(), 0));
         assertElements(ListFunctions.shift(ImmutableList.of(), 10));
         assertElements(ListFunctions.shift(ImmutableList.of(1), 10), 1);
         assertElements(ListFunctions.shift(ImmutableList.of(1, 2, 3), 1), 2, 3, 1);
         assertElements(ListFunctions.shift(ImmutableList.of(1, 2, 3), 2), 3, 1, 2);
         assertElements(ListFunctions.shift(ImmutableList.of(1, 2, 3), 3), 1, 2, 3);
         assertElements(ListFunctions.shift(ImmutableList.of(1, 2, 3), 4), 2, 3, 1);
     }
 
     @Test
     public void testReverse() {
         assertElements(ListFunctions.reverse(ImmutableList.of()));
         assertElements(ListFunctions.reverse(ImmutableList.of(1)), 1);
         assertElements(ListFunctions.reverse(ImmutableList.of(1, 2)), 2, 1);
         assertElements(ListFunctions.reverse(ImmutableList.of(1, 2, 3)), 3, 2, 1);
     }
 
     @Test
     public void testSort() {
         assertElements(ListFunctions.sort(ImmutableList.of("c", "b", "a"), null), "a", "b", "c");
         assertElements(ListFunctions.sort(ImmutableList.of(9, 3, 5), null), 3, 5, 9);
     }
     
     @Test
     public void testSortByKey() {
         Point p1 = new Point(1, 9);
         Point p2 = new Point(10, 4);
         Point p3 = new Point(4, 7);
         Point p4 = new Point(8, 6);
         assertElements(ListFunctions.sort(ImmutableList.of(p1, p2, p3, p4), "x"), p1, p3, p4, p2);
         assertElements(ListFunctions.sort(ImmutableList.of(p1, p2, p3, p4), "y"), p2, p4, p3, p1);
     }
 
     @Test
     public void testShuffle() {
         // Shuffling is stable: the same seed always returns the same sort order.
         assertElements(ListFunctions.shuffle(ImmutableList.of(), 42));
         assertElements(ListFunctions.shuffle(ImmutableList.of(1), 42), 1);
        assertElements(ListFunctions.shuffle(ImmutableList.of(1, 2, 3, 4, 5), 42), 2, 3, 4, 5, 1);
        assertElements(ListFunctions.shuffle(ImmutableList.of(1, 2, 3, 4, 5), 33), 2, 1, 5, 3, 4);
     }
 
     @Test
     public void testFilter() {
         assertElements(ListFunctions.filter(ImmutableList.of(), ImmutableList.<Boolean>of()));
         assertElements(ListFunctions.filter(ImmutableList.of(1, 2, 3, 4, 5), ImmutableList.of(true, false, false, true, true)), 1, 4, 5);
         assertElements(ListFunctions.filter(ImmutableList.of(1, 2, 3, 4, 5), ImmutableList.of(true, false, true, false, true, false, true)), 1, 3, 5);
         assertElements(ListFunctions.filter(ImmutableList.of(1, 2, 3), ImmutableList.<Boolean>of()), 1, 2, 3);
         assertElements(ListFunctions.filter(ImmutableList.of(1, 2, 3), ImmutableList.of(true)), 1, 2, 3);
         assertElements(ListFunctions.filter(ImmutableList.of(1, 2, 3), ImmutableList.of(false)));
     }
 
     @Test
     public void testCull() {
         assertElements(ListFunctions.cull(ImmutableList.of(), ImmutableList.<Boolean>of()));
         assertElements(ListFunctions.cull(ImmutableList.of(1, 2, 3), ImmutableList.<Boolean>of()), 1, 2, 3);
         assertElements(ListFunctions.cull(ImmutableList.of(1, 2, 3), ImmutableList.<Boolean>of(true)), 1, 2, 3);
         assertElements(ListFunctions.cull(ImmutableList.of(1, 2, 3), ImmutableList.<Boolean>of(false)));
         assertElements(ListFunctions.cull(ImmutableList.of(1, 2, 3, 4), ImmutableList.<Boolean>of(false, true)), 2, 4);
         assertElements(ListFunctions.cull(ImmutableList.of(1, 2, 3, 4), ImmutableList.<Boolean>of(true, false)), 1, 3);
         assertElements(ListFunctions.cull(ImmutableList.of(1, 2, 3, 4), ImmutableList.<Boolean>of(true, true, false)), 1, 2, 4);
         assertElements(ListFunctions.cull(ImmutableList.of(1, 2, 3, 4), ImmutableList.<Boolean>of(true, false, true, true, true)), 1, 3, 4);
     }
 
     @Test
     public void testDistinct() {
         // Distinct is stable: the same seed always returns the same sort order.
         assertElements(ListFunctions.distinct(ImmutableList.of()));
         assertElements(ListFunctions.distinct(ImmutableList.of(1, 2, 3, 4)), 1, 2, 3, 4);
         assertElements(ListFunctions.distinct(ImmutableList.of(4, 3, 2, 1)), 4, 3, 2, 1);
         assertElements(ListFunctions.distinct(ImmutableList.of(3, 4, 3, 1, 2, 1)), 3, 4, 1, 2);
         assertElements(ListFunctions.distinct(ImmutableList.of(3, 4, 3, 2, 1)), 3, 4, 2, 1);
     }
 
     @Test
     public void testRepeat() {
         assertElements(ListFunctions.repeat(ImmutableList.of(), 0));
         assertElements(ListFunctions.repeat(ImmutableList.of(), 10));
         assertElements(ListFunctions.repeat(ImmutableList.of(1, 2, 3), -1));
         assertElements(ListFunctions.repeat(ImmutableList.of(1, 2, 3), 0));
         assertElements(ListFunctions.repeat(ImmutableList.of(1, 2, 3), 1), 1, 2, 3);
         assertElements(ListFunctions.repeat(ImmutableList.of(1, 2, 3), 2), 1, 2, 3, 1, 2, 3);
         assertElements(ListFunctions.repeat(ImmutableList.of(1), 5), 1, 1, 1, 1, 1);
     }
 
     @Test(expected = IllegalArgumentException.class)
     public void testSortDisparateElements() {
         // You can't sort elements of different types. This error is caught and wrapped in an illegal argument exception.
         ListFunctions.sort(ImmutableList.of("hello", 42, 15.0), null);
     }
 
     @Test
     public void testCycle() {
         assertFirstElements(ListFunctions.cycle(ImmutableList.of()));
         assertFirstElements(ListFunctions.cycle(ImmutableList.of(1)), 1, 1, 1, 1, 1);
         assertFirstElements(ListFunctions.cycle(ImmutableList.of(1, 2)), 1, 2, 1, 2, 1);
         assertFirstElements(ListFunctions.cycle(ImmutableList.of(1, 2, 3)), 1, 2, 3, 1, 2);
     }
 
     @Test
     public void testTakeEvery() {
         assertElements(ListFunctions.takeEvery(ImmutableList.of(), 1));
         assertElements(ListFunctions.takeEvery(ImmutableList.of(1, 2, 3, 4, 5), 1), 1, 2, 3, 4, 5);
         assertElements(ListFunctions.takeEvery(ImmutableList.of(1, 2, 3, 4, 5), 2), 1, 3, 5);
         assertElements(ListFunctions.takeEvery(ImmutableList.of(1, 2, 3, 4, 5), 3), 1, 4);
     }
 
     private void assertElements(Iterable<?> iterable, Object... items) {
        assertEquals(ImmutableList.copyOf(iterable), ImmutableList.copyOf(items));
     }
 
     private void assertFirstElements(Iterable<?> iterable, Object... items) {
         assertElements(Iterables.limit(iterable, items.length), items);
     }
 
 }
