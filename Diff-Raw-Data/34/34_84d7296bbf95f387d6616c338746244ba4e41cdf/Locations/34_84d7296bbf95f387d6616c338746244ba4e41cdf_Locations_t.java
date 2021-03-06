 package com.sun.javafx.runtime.location;
 
 import java.lang.ref.WeakReference;
 import java.util.Collection;
 import java.util.Iterator;
 
 import com.sun.javafx.runtime.sequence.Sequence;
 import com.sun.javafx.runtime.sequence.SequencePredicate;
 
 /**
  * Factory methods for wrapping Locations: unmodifiable locations, ObjectLocation-typed views of primitive locations, etc
  *
  * @author Brian Goetz
  */
 public class Locations {
     // non-instantiable
     private Locations() {
     }
 
     public static ObjectLocation<Integer> asObjectLocation(IntLocation loc) {
         return loc instanceof MutableLocation
                 ? new IntObjectMutableLocation(loc)
                 : new IntObjectLocation(loc);
     }
 
     public static ObjectLocation<Double> asObjectLocation(DoubleLocation loc) {
         return loc instanceof MutableLocation
                 ? new DoubleObjectMutableLocation(loc)
                 : new DoubleObjectLocation(loc);
     }
 
     public static ObjectLocation<Boolean> asObjectLocation(BooleanLocation loc) {
         return loc instanceof MutableLocation
                 ? new BooleanObjectMutableLocation(loc)
                 : new BooleanObjectLocation(loc);
     }
 
     public static DoubleLocation asDoubleLocation(IntLocation loc) {
         return new IntDoubleLocation(loc);
     }
 
 
     public static IntLocation unmodifiableLocation(IntLocation loc) {
         return new UnmodifiableIntLocation(loc);
     }
 
     public static DoubleLocation unmodifiableLocation(DoubleLocation loc) {
         return new UnmodifiableDoubleLocation(loc);
     }
 
     public static BooleanLocation unmodifiableLocation(BooleanLocation loc) {
         return new UnmodifiableBooleanLocation(loc);
     }
 
     public static <T> ObjectLocation<T> unmodifiableLocation(ObjectLocation<T> loc) {
         return new UnmodifiableObjectLocation<T>(loc);
     }
 
     public static <T> SequenceLocation<T> unmodifiableLocation(SequenceLocation<T> loc) {
         return new UnmodifiableSequenceLocation<T>(loc);
     }
 
     public static<T, V extends T> ObjectLocation<T> upcast(ObjectLocation<V> loc) {
         return new UpcastLocation<T, V>(loc);
     }
 
 
     private static abstract class LocationWrapper implements Location {
         protected abstract Location getLocation();
 
         public boolean isValid() {
             return getLocation().isValid();
         }
 
         public boolean isNull() {
             return getLocation().isNull();
         }
 
         public boolean isLazy() {
             return getLocation().isLazy();
         }
 
         public void invalidate() {
             getLocation().invalidate();
         }
 
         public void update() {
             getLocation().update();
         }
 
         public void addChangeListener(ChangeListener listener) {
             getLocation().addChangeListener(listener);
         }
 
         public void addWeakListener(ChangeListener listener) {
             getLocation().addWeakListener(listener);
         }
 
         public Collection<ChangeListener> getListeners() {
             return getLocation().getListeners();
         }
 
         public void addDependencies(Location... location) {
             getLocation().addDependencies(location);
         }
 
         public void addDynamicDependency(Location location) {
             getLocation().addDynamicDependency(location);
         }
 
         public void clearDynamicDependencies() {
             getLocation().clearDynamicDependencies();
         }
 
         public void addDependentLocation(WeakReference<Location> location) {
             getLocation().addDependentLocation(location);
         }
     }
 
     /**
      * Wrapper class that creates an ObjectLocation<Integer> view of an IntLocation
      */
     private static class IntObjectLocation extends LocationWrapper implements ObjectLocation<Integer>, ViewLocation {
         private final IntLocation location;
 
         protected IntLocation getLocation() {
             return location;
         }
 
         public IntObjectLocation(IntLocation location) {
             this.location = location;
         }
 
         public Integer get() {
             return location.getAsInt();
         }
 
         public Integer set(Integer value) {
             return location.setAsInt(value);
         }
 
         public Location getUnderlyingLocation() {
             return location;
         }
 
         public void setDefault() {
             location.setDefault();
         }
 
         public void addChangeListener(ObjectChangeListener<Integer> listener) {
             location.addChangeListener(listener);
         }
     }
 
     /**
      * Wrapper class that creates a DoubleLocation view of an IntLocation
      */
     private static class IntDoubleLocation extends LocationWrapper implements DoubleLocation, ViewLocation {
         private final IntLocation location;
 
         protected IntLocation getLocation() {
             return location;
         }
 
         public IntDoubleLocation(IntLocation location) {
             this.location = location;
         }
 
         public double getAsDouble() {
             return location.getAsInt();
         }
 
         public double setAsDouble(double value) {
             throw new UnsupportedOperationException();
         }
 
         public void setDefault() {
             throw new UnsupportedOperationException();
         }
 
         public Double get() {
             return getAsDouble();
         }
 
         public Double set(Double value) {
             throw new UnsupportedOperationException();
         }
 
         public Location getUnderlyingLocation() {
             return location;
         }
 
         public void addChangeListener(final DoubleChangeListener listener) {
             location.addChangeListener(new IntChangeListener() {
                 public void onChange(int oldValue, int newValue) {
                     listener.onChange(oldValue, newValue);
                 }
             });
         }
 
         public void addChangeListener(final ObjectChangeListener<Double> listener) {
             location.addChangeListener(new IntChangeListener() {
                 public void onChange(int oldValue, int newValue) {
                     listener.onChange((double) oldValue, (double) newValue);
                 }
             });
         }
     }
 
     /**
      * Wrapper class that creates an ObjectLocation<Integer> view of a mutable IntLocation
      */
     private static class IntObjectMutableLocation extends IntObjectLocation implements MutableLocation {
         public IntObjectMutableLocation(IntLocation location) {
             super(location);
         }
     }
 
     /**
      * Wrapper class that wraps an IntLocation so it cannot be modified
      */
     private static class UnmodifiableIntLocation extends LocationWrapper implements IntLocation {
         private final IntLocation location;
 
         protected Location getLocation() {
             return location;
         }
 
         public UnmodifiableIntLocation(IntLocation location) {
             this.location = location;
         }
 
         public int getAsInt() {
             return location.getAsInt();
         }
 
         public void addChangeListener(IntChangeListener listener) {
             location.addChangeListener(listener);
         }
 
         public void addChangeListener(ObjectChangeListener<Integer> listener) {
             location.addChangeListener(listener);
         }
 
         public int setAsInt(int value) {
             throw new UnsupportedOperationException();
         }
 
         public void setDefault() {
             throw new UnsupportedOperationException();
         }
 
         public void invalidate() {
             throw new UnsupportedOperationException();
         }
 
         public Integer get() {
             return getAsInt();
         }
 
         public Integer set(Integer value) {
             throw new UnsupportedOperationException();
         }
     }
 
 
     /**
      * Wrapper class that creates an ObjectLocation<Double> view of a DoubleLocation
      */
     private static class DoubleObjectLocation extends LocationWrapper implements ObjectLocation<Double>, ViewLocation {
         private final DoubleLocation location;
 
         protected Location getLocation() {
             return location;
         }
 
         public DoubleObjectLocation(DoubleLocation location) {
             this.location = location;
         }
 
         public Double get() {
             return location.getAsDouble();
         }
 
         public Double set(Double value) {
             return location.setAsDouble(value);
         }
 
         public void setDefault() {
             location.setDefault();
         }
 
         public Location getUnderlyingLocation() {
             return location;
         }
 
         public void addChangeListener(ObjectChangeListener<Double> listener) {
             location.addChangeListener(listener);
         }
     }
 
     /**
      * Wrapper class that creates an ObjectLocation<Double> view of a mutable DoubleLocation
      */
     private static class DoubleObjectMutableLocation extends DoubleObjectLocation implements MutableLocation {
         public DoubleObjectMutableLocation(DoubleLocation location) {
             super(location);
         }
     }
 
     /**
      * Wrapper class that wraps a DoubleLocation so it cannot be modified
      */
     private static class UnmodifiableDoubleLocation extends LocationWrapper implements DoubleLocation {
         private final DoubleLocation location;
 
         protected Location getLocation() {
             return location;
         }
 
         public UnmodifiableDoubleLocation(DoubleLocation location) {
             this.location = location;
         }
 
         public double getAsDouble() {
             return location.getAsDouble();
         }
 
         public double setAsDouble(double value) {
             throw new UnsupportedOperationException();
         }
 
         public void setDefault() {
             throw new UnsupportedOperationException();
         }
 
         public Double get() {
             return getAsDouble();
         }
 
         public Double set(Double value) {
             throw new UnsupportedOperationException();
         }
 
         public void invalidate() {
             throw new UnsupportedOperationException();
         }
 
         public void addChangeListener(DoubleChangeListener listener) {
             location.addChangeListener(listener);
         }
 
         public void addChangeListener(ObjectChangeListener<Double> listener) {
             location.addChangeListener(listener);
         }
     }
 
 
     /**
      * Wrapper class that creates an ObjectLocation<Boolean> view of a BooleanLocation
      */
     private static class BooleanObjectLocation extends LocationWrapper implements ObjectLocation<Boolean>, ViewLocation {
         private final BooleanLocation location;
 
         protected Location getLocation() {
             return location;
         }
 
         public BooleanObjectLocation(BooleanLocation location) {
             this.location = location;
         }
 
         public Boolean get() {
             return location.getAsBoolean();
         }
 
         public Boolean set(Boolean value) {
             return location.setAsBoolean(value);
         }
 
         public void setDefault() {
             location.setDefault();
         }
 
         public Location getUnderlyingLocation() {
             return location;
         }
 
         public void addChangeListener(ObjectChangeListener<Boolean> listener) {
             location.addChangeListener(listener);
         }
     }
 
     /**
      * Wrapper class that creates an ObjectLocation<Boolean> view of a mutable BooleanLocation
      */
     private static class BooleanObjectMutableLocation extends BooleanObjectLocation implements MutableLocation {
         public BooleanObjectMutableLocation(BooleanLocation location) {
             super(location);
         }
     }
 
     /**
      * Wrapper class that wraps a BooleanLocation so it cannot be modified
      */
     private static class UnmodifiableBooleanLocation extends LocationWrapper implements BooleanLocation {
         private final BooleanLocation location;
 
         protected Location getLocation() {
             return location;
         }
 
         public UnmodifiableBooleanLocation(BooleanLocation location) {
             this.location = location;
         }
 
         public boolean getAsBoolean() {
             return location.getAsBoolean();
         }
 
         public boolean setAsBoolean(boolean value) {
             throw new UnsupportedOperationException();
         }
 
         public void setDefault() {
             throw new UnsupportedOperationException();
         }
 
         public Boolean get() {
             return getAsBoolean();
         }
 
         public Boolean set(Boolean value) {
             throw new UnsupportedOperationException();
         }
 
         public void invalidate() {
             throw new UnsupportedOperationException();
         }
 
         public void addChangeListener(BooleanChangeListener listener) {
             location.addChangeListener(listener);
         }
 
         public void addChangeListener(ObjectChangeListener<Boolean> listener) {
             location.addChangeListener(listener);
         }
     }
 
     /**
      * Wrapper class that wraps an ObjectLocation so it cannot be modified
      */
     private static class UnmodifiableObjectLocation<T> extends LocationWrapper implements ObjectLocation<T> {
         private final ObjectLocation<T> location;
 
         public UnmodifiableObjectLocation(ObjectLocation<T> location) {
             this.location = location;
         }
 
         public ObjectLocation<T> getLocation() {
             return location;
         }
 
         public T get() {
             return location.get();
         }
 
         public T set(T value) {
             throw new UnsupportedOperationException();
         }
 
         public void setDefault() {
             throw new UnsupportedOperationException();
         }
 
         public void invalidate() {
             throw new UnsupportedOperationException();
         }
 
         public void addChangeListener(ObjectChangeListener<T> listener) {
             location.addChangeListener(listener);
         }
     }
 
     private static class UpcastLocation<T, V extends T> extends LocationWrapper implements ObjectLocation<T> {
         private final ObjectLocation<V> location;
 
         public UpcastLocation(ObjectLocation<V> location) {
             this.location = location;
         }
 
         protected Location getLocation() {
             return location;
         }
 
         public V get() {
             return location.get();
         }
 
         public V set(T value) {
             // Alternately; allow the set to proceed after passing a runtime type check; would require T.class
             throw new UnsupportedOperationException();
         }
 
         public void setDefault() {
             throw new UnsupportedOperationException();
         }
 
         public void addChangeListener(final ObjectChangeListener<T> listener) {
             location.addChangeListener(new ObjectChangeListener<V>() {
                 public void onChange(V oldValue, V newValue) {
                     listener.onChange(oldValue, newValue);
                 }
             });
         }
     }
 
     /**
      * Wrapper class that wraps a SequenceLocation so it cannot be modified
      */
     private static class UnmodifiableSequenceLocation<T> extends LocationWrapper implements SequenceLocation<T> {
         private final SequenceLocation<T> location;
 
         public UnmodifiableSequenceLocation(SequenceLocation<T> location) {
             this.location = location;
         }
 
         public SequenceLocation<T> getLocation() {
             return location;
         }
 
         public void invalidate() {
             throw new UnsupportedOperationException();
         }
 
         public T get(int position) {
             return location.get(position);
         }
 
         public Sequence<T> getAsSequence() {
             return location.getAsSequence();
         }
 
         public Iterator<T> iterator() {
             return location.iterator();
         }
 
         public Sequence<T> setAsSequence(Sequence<? extends T> value) {
             throw new UnsupportedOperationException();
         }
 
         public void addChangeListener(SequenceReplaceListener<T> sequenceReplaceListener) {
             location.addChangeListener(sequenceReplaceListener);
         }
 
         public void removeChangeListener(SequenceReplaceListener<T> sequenceReplaceListener) {
             location.removeChangeListener(sequenceReplaceListener);
         }
 
         public void addChangeListener(SequenceChangeListener<T> listener) {
             location.addChangeListener(listener);
         }
 
         public T set(int position, T value) {
             throw new UnsupportedOperationException();
         }
 
         @Override
         public Sequence<T> getSlice(int startPos, int endPos) {
             return getAsSequence().getSlice(startPos, endPos);
         }
 
        public Sequence<? extends T> replaceSlice(int startPos, int endPos, Sequence<? extends T> newValues) {
             throw new UnsupportedOperationException();
         }
 
         public void delete(int position) {
             throw new UnsupportedOperationException();
         }
 
         public void deleteSlice(int startPos, int endPos) {
             throw new UnsupportedOperationException();
         }
 
         public void deleteAll() {
             throw new UnsupportedOperationException();
         }
 
         public void deleteValue(T value) {
             throw new UnsupportedOperationException();
         }
 
         public void delete(SequencePredicate<T> sequencePredicate) {
             throw new UnsupportedOperationException();
         }
 
         public void insert(T value) {
             throw new UnsupportedOperationException();
         }
 
         public void insert(Sequence<? extends T> values) {
             throw new UnsupportedOperationException();
         }
 
         public void insertFirst(T value) {
             throw new UnsupportedOperationException();
         }
 
         public void insertFirst(Sequence<? extends T> values) {
             throw new UnsupportedOperationException();
         }
 
         public void insertBefore(T value, int position) {
             throw new UnsupportedOperationException();
         }
 
         public void insertBefore(T value, SequencePredicate<T> sequencePredicate) {
             throw new UnsupportedOperationException();
         }
 
         public void insertBefore(Sequence<? extends T> values, int position) {
             throw new UnsupportedOperationException();
         }
 
         public void insertBefore(Sequence<? extends T> values, SequencePredicate<T> sequencePredicate) {
             throw new UnsupportedOperationException();
         }
 
         public void insertAfter(T value, int position) {
             throw new UnsupportedOperationException();
         }
 
         public void insertAfter(T value, SequencePredicate<T> sequencePredicate) {
             throw new UnsupportedOperationException();
         }
 
         public void insertAfter(Sequence<? extends T> values, int position) {
             throw new UnsupportedOperationException();
         }
 
         public void insertAfter(Sequence<? extends T> values, SequencePredicate<T> sequencePredicate) {
             throw new UnsupportedOperationException();
         }
     }
 }
