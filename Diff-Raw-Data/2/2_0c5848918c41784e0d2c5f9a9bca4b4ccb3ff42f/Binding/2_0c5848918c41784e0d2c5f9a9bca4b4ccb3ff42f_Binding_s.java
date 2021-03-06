 /*
  * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
  * subject to license terms.
  */
 
 package org.jdesktop.beansbinding;
 
 import java.util.List;
 import java.util.ArrayList;
 import java.beans.*;
 
 /**
  * @author Shannon Hickey
  */
 public abstract class Binding<SS, SV, TS, TV> {
 
     private String name;
     private SS sourceObject;
     private TS targetObject;
     private Property<SS, SV> sourceProperty;
     private Property<TS, TV> targetProperty;
     private Validator<? super SV> validator;
     private Converter<SV, TV> converter;
     private TV sourceNullValue;
     private SV targetNullValue;
     private TV sourceUnreadableValue;
     private List<BindingListener> listeners;
     private PropertyStateListener psl;
     private boolean hasEditedSource;
     private boolean hasEditedTarget;
     private boolean ignoreChange;
     private boolean isManaged;
     private boolean isBound;
     private PropertyChangeSupport changeSupport;
 
     /**
      * An enumeration representing the reasons a sync ({@code save} or {@code refresh})
      * can fail on a {@code Binding}.
      *
      * @see Binding#refresh
      * @see Binding#save
      */
     public enum SyncFailureType {
         
         /**
          * A {@code refresh} failed because the {@code Binding's} target property is unwriteable
          * for the {@code Binding's} target object.
          */
         TARGET_UNWRITEABLE,
         
         /**
          * A {@code save} failed because the {@code Binding's} source property is unwriteable
          * for the {@code Binding's} source object.
          */
         SOURCE_UNWRITEABLE,
         
         /**
          * A {@code save} failed because the {@code Binding's} target property is unreadable
          * for the {@code Binding's} target object.
          */
         TARGET_UNREADABLE,
         
         /**
          * A {@code save} failed due to a conversion failure on the value
          * returned by the {@code Binding's} target property for the {@code Binding's}
          * target object.
          */
         CONVERSION_FAILED,
         
         /**
          * A {@code save} failed due to a validation failure on the value
          * returned by the {@code Binding's} target property for the {@code Binding's}
          * target object.
          */
         VALIDATION_FAILED
     }
 
     public static final class SyncFailure {
         private SyncFailureType type;
         private Object reason;
 
         private static SyncFailure TARGET_UNWRITEABLE = new SyncFailure(SyncFailureType.TARGET_UNWRITEABLE);
         private static SyncFailure SOURCE_UNWRITEABLE = new SyncFailure(SyncFailureType.SOURCE_UNWRITEABLE);
         private static SyncFailure TARGET_UNREADABLE = new SyncFailure(SyncFailureType.TARGET_UNREADABLE);
 
         private static SyncFailure conversionFailure(RuntimeException rte) {
             return new SyncFailure(rte);
         }
 
         private static SyncFailure validationFailure(Validator.Result result) {
             return new SyncFailure(result);
         }
 
         private SyncFailure(SyncFailureType type) {
             if (type == SyncFailureType.CONVERSION_FAILED || type == SyncFailureType.VALIDATION_FAILED) {
                 throw new IllegalArgumentException();
             }
 
             this.type = type;
         }
 
         private SyncFailure(RuntimeException exception) {
             this.type = SyncFailureType.CONVERSION_FAILED;
             this.reason = exception;
         }
 
         private SyncFailure(Validator.Result result) {
             this.type = SyncFailureType.VALIDATION_FAILED;
             this.reason = result;
         }
 
         public SyncFailureType getType() {
             return type;
         }
         
         public RuntimeException getConversionException() {
             if (type != SyncFailureType.CONVERSION_FAILED) {
                 throw new UnsupportedOperationException();
             }
             
             return (RuntimeException)reason;
         }
 
         public Validator.Result getValidationResult() {
             if (type != SyncFailureType.VALIDATION_FAILED) {
                 throw new UnsupportedOperationException();
             }
             
             return (Validator.Result)reason;
         }
 
         public String toString() {
             return type + (reason == null ? "" : ": " + reason.toString());
         }
     }
 
     public static final class ValueResult<V> {
         private V value;
         private SyncFailure failure;
 
         private ValueResult(V value) {
             this.value = value;
         }
 
         private ValueResult(SyncFailure failure) {
             if (failure == null) {
                 throw new AssertionError();
             }
 
             this.failure = failure;
         }
 
         public boolean failed() {
             return failure != null;
         }
 
         public V getValue() {
             if (failed()) {
                 throw new UnsupportedOperationException();
             }
 
             return value;
         }
 
         public SyncFailure getFailure() {
             if (!failed()) {
                 throw new UnsupportedOperationException();
             }
             
             return failure;
         }
 
         public String toString() {
             return value == null ? "failure: " + failure : "value: " + value;
         }
     }
 
     protected Binding(SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty, String name) {
         setSourceProperty(sourceProperty);
         setTargetProperty(targetProperty);
 
         this.sourceObject = sourceObject;
         this.sourceProperty = sourceProperty;
         this.targetObject = targetObject;
         this.targetProperty = targetProperty;
         this.name = name;
     }
 
     protected final void setSourceProperty(Property<SS, SV> sourceProperty) {
         throwIfBound();
         if (sourceProperty == null) {
             throw new IllegalArgumentException("source property can't be null");
         }
         this.sourceProperty = sourceProperty;
     }
     
     protected final void setTargetProperty(Property<TS, TV> targetProperty) {
         throwIfBound();
         if (targetProperty == null) {
            throw new IllegalArgumentException("source property can't be null");
         }
         this.targetProperty = targetProperty;
     }
     
     public final String getName() {
         return name;
     }
 
     public final Property<SS, SV> getSourceProperty() {
         return sourceProperty;
     }
 
     public final Property<TS, TV> getTargetProperty() {
         return targetProperty;
     }
 
     public final SS getSourceObject() {
         return sourceObject;
     }
 
     public final TS getTargetObject() {
         return targetObject;
     }
 
     public final void setSourceObject(SS sourceObject) {
         throwIfManaged();
         setSourceObjectUnmanaged(sourceObject);
     }
 
     protected final void setSourceObjectUnmanaged(SS sourceObject) {
         throwIfBound();
         this.sourceObject = sourceObject;
     }
 
     public final void setTargetObject(TS targetObject) {
         throwIfManaged();
         setTargetObjectUnmanaged(targetObject);
     }
 
     protected final void setTargetObjectUnmanaged(TS targetObject) {
         throwIfBound();
         this.targetObject = targetObject;
     }
 
     public final void setValidator(Validator<? super SV> validator) {
         throwIfBound();
         this.validator = validator;
     }
 
     public final Validator<? super SV> getValidator() {
         return validator;
     }
 
     public final void setConverter(Converter<SV, TV> converter) {
         throwIfBound();
         this.converter = converter;
     }
 
     public final Converter<SV, TV> getConverter() {
         return converter;
     }
 
     public final void setSourceNullValue(TV value) {
         throwIfBound();
         sourceNullValue = value;
     }
 
     public final TV getSourceNullValue() {
         return sourceNullValue;
     }
 
     public final void setTargetNullValue(SV value) {
         throwIfBound();
         targetNullValue = value;
     }
 
     public final SV getTargetNullValue() {
         return targetNullValue;
     }
 
     public final void setSourceUnreadableValue(TV value) {
         throwIfBound();
         sourceUnreadableValue = value;
     }
 
     public final TV getSourceUnreadableValue() {
         return sourceUnreadableValue;
     }
 
     public final void addBindingListener(BindingListener listener) {
         if (listeners == null) {
             listeners = new ArrayList<BindingListener>();
         }
 
         listeners.add(listener);
     }
 
     public final void removeBindingListener(BindingListener listener) {
         if (listeners != null) {
             listeners.remove(listener);
         }
     }
 
     public final BindingListener[] getBindingListeners() {
         if (listeners == null) {
             return new BindingListener[0];
         }
 
         BindingListener[] ret = new BindingListener[listeners.size()];
         ret = listeners.toArray(ret);
         return ret;
     }
 
     public final ValueResult<TV> getSourceValueForTarget() {
         if (!targetProperty.isWriteable(targetObject)) {
             return new ValueResult<TV>(SyncFailure.TARGET_UNWRITEABLE);
         }
 
         TV value;
 
         if (sourceProperty.isReadable(sourceObject)) {
             SV rawValue = sourceProperty.getValue(sourceObject);
 
             if (rawValue == null) {
                 value = sourceNullValue;
             } else {
                 // may throw ClassCastException or other RuntimeException here;
                 // allow it to be propogated back to the user of Binding
                 value = convertForward(rawValue);
             }
         } else {
             value = sourceUnreadableValue;
         }
 
         return new ValueResult<TV>((TV)value);
     }
 
     public final ValueResult<SV> getTargetValueForSource() {
         if (!targetProperty.isReadable(targetObject)) {
             return new ValueResult<SV>(SyncFailure.TARGET_UNREADABLE);
         }
 
         if (!sourceProperty.isWriteable(sourceObject)) {
             return new ValueResult<SV>(SyncFailure.SOURCE_UNWRITEABLE);
         }
 
         SV value = null;
         TV rawValue = targetProperty.getValue(targetObject);
 
         if (rawValue == null) {
             value = targetNullValue;
         } else {
             try {
                 value = convertReverse(rawValue);
             } catch (ClassCastException cce) {
                 throw cce;
             } catch (RuntimeException rte) {
                 return new ValueResult<SV>(SyncFailure.conversionFailure(rte));
             }
 
             if (validator != null) {
                 Validator.Result vr = validator.validate(value);
                 if (vr != null) {
                     return new ValueResult<SV>(SyncFailure.validationFailure(vr));
                 }
             }
         }
 
         return new ValueResult<SV>((SV)value);
     }
 
     public final void bind() {
         throwIfManaged();
         bindUnmanaged();
     }
     
     protected final void bindUnmanaged() {
         throwIfBound();
 
         hasEditedSource = false;
         hasEditedTarget = false;
         isBound = true;
 
         psl = new PSL();
         sourceProperty.addPropertyStateListener(sourceObject, psl);
         targetProperty.addPropertyStateListener(targetObject, psl);
 
         bindImpl();
 
         if (listeners != null) {
             for (BindingListener listener : listeners) {
                 listener.bindingBecameBound(this);
             }
         }
     }
 
     protected abstract void bindImpl();
 
     public final void unbind() {
         throwIfManaged();
         unbindUnmanaged();
     }
     
     protected final void unbindUnmanaged() {
         throwIfUnbound();
 
         unbindImpl();
 
         sourceProperty.removePropertyStateListener(sourceObject, psl);
         targetProperty.removePropertyStateListener(targetObject, psl);
         psl = null;
 
         isBound = false;
         hasEditedSource = false;
         hasEditedTarget = false;
 
         if (listeners != null) {
             for (BindingListener listener : listeners) {
                 listener.bindingBecameUnbound(this);
             }
         }
     }
     
     protected abstract void unbindImpl();
 
     public final boolean isBound() {
         return isBound;
     }
     
     public final boolean getHasEditedSource() {
         throwIfUnbound();
         return hasEditedSource;
     }
 
     public final boolean getHasEditedTarget() {
         throwIfUnbound();
         return hasEditedTarget;
     }
 
     protected final void setManaged(boolean isManaged) {
         this.isManaged = isManaged;
     }
 
     protected final boolean isManaged() {
         return isManaged;
     }
 
     protected final void notifySynced() {
         if (listeners == null) {
             return;
         }
 
         for (BindingListener listener : listeners) {
             listener.synced(this);
         }
     }
 
     protected final void notifySyncFailed(SyncFailure... failures) {
         if (listeners == null) {
             return;
         }
 
         for (BindingListener listener : listeners) {
             listener.syncFailed(this, failures);
         }
     }
 
     private final SyncFailure notifyAndReturn(SyncFailure failure) {
         if (failure == null) {
             notifySynced();
         } else {
             notifySyncFailed(failure);
         }
 
         return failure;
     }
 
     public final SyncFailure refreshAndNotify() {
         return notifyAndReturn(refresh());
     }
 
     public final SyncFailure saveAndNotify() {
         return notifyAndReturn(save());
     }
 
     public final SyncFailure refresh() {
         throwIfManaged();
         return refreshUnmanaged();
     }
     
     protected final SyncFailure refreshUnmanaged() {
         throwIfUnbound();
 
         ValueResult<TV> vr = getSourceValueForTarget();
         if (vr.failed()) {
             return vr.getFailure();
         }
 
         try {
             ignoreChange = true;
             targetProperty.setValue(targetObject, vr.getValue());
         } finally {
             ignoreChange = false;
         }
 
         notifySourceEdited(false);
         notifyTargetEdited(false);
         return null;
     }
 
     public final SyncFailure save() {
         throwIfManaged();
         return saveUnmanaged();
     }
     
     protected final SyncFailure saveUnmanaged() {
         throwIfUnbound();
 
         ValueResult<SV> vr = getTargetValueForSource();
         if (vr.failed()) {
             return vr.getFailure();
         }
 
         try {
             ignoreChange = true;
             sourceProperty.setValue(sourceObject, vr.getValue());
         } finally {
             ignoreChange = false;
         }
 
         notifySourceEdited(false);
         notifyTargetEdited(false);
         return null;
     }
 
     private final Class<?> noPrimitiveType(Class<?> klass) {
         if (!klass.isPrimitive()) {
             return klass;
         }
 
         if (klass == Byte.TYPE) {
             return Byte.class;
         } else if (klass == Short.TYPE) {
             return Short.class;
         } else if (klass == Integer.TYPE) {
             return Integer.class;
         } else if (klass == Long.TYPE) {
             return Long.class;
         } else if (klass == Boolean.TYPE) {
             return Boolean.class;
         } else if (klass == Character.TYPE) {
             return Character.class;
         } else if (klass == Float.TYPE) {
             return Float.class;
         } else if (klass == Double.TYPE) {
             return Double.class;
         }
 
         throw new AssertionError();
     }
 
     private final TV convertForward(SV value) {
         if (converter == null) {
             Class<?> targetType = noPrimitiveType(targetProperty.getWriteType(targetObject));
             return (TV)targetType.cast(Converter.defaultConvert(value, targetType));
         }
 
         return converter.convertForward(value);
     }
 
     private final SV convertReverse(TV value) {
         if (converter == null) {
             Class<?> sourceType = noPrimitiveType(sourceProperty.getWriteType(sourceObject));
             return (SV)sourceType.cast(Converter.defaultConvert(value, sourceType));
         }
 
         return converter.convertReverse(value);
     }
 
     protected final void throwIfManaged() {
         if (isManaged()) {
             throw new IllegalStateException("Can not call this method on a managed binding");
         }
     }
     
     protected final void throwIfBound() {
         if (isBound()) {
             throw new IllegalStateException("Can not call this method on a bound binding");
         }
     }
 
     protected final void throwIfUnbound() {
         if (!isBound()) {
             throw new IllegalStateException("Can not call this method on an unbound binding");
         }
     }
 
     public String toString() {
         return getClass().getName() + " [" + paramString() + "]";
     }
 
     private String paramString() {
         return "name=" + getName() +
                ", sourceObject=" + sourceObject +
                ", sourceProperty=" + sourceProperty +
                ", targetObject=" + targetObject +
                ", targetProperty" + targetProperty +
                ", validator=" + validator +
                ", converter=" + converter +
                ", sourceNullValue=" + sourceNullValue +
                ", targetNullValue=" + targetNullValue +
                ", sourceUnreadableValue=" + sourceUnreadableValue +
                ", hasChangedSource=" + hasEditedSource +
                ", hasChangedTarget=" + hasEditedTarget +
                ", bound=" + isBound();
     }
     
     private void sourceChanged(PropertyStateEvent pse) {
         if (!pse.getValueChanged()) {
             return;
         }
 
         notifySourceEdited(true);
 
         if (listeners != null) {
             for (BindingListener listener : listeners) {
                 listener.sourceEdited(this);
             }
         }
 
         sourceChangedImpl(pse);
     }
 
     protected void sourceChangedImpl(PropertyStateEvent pse) {
     }
 
     private void targetChanged(PropertyStateEvent pse) {
         if (!pse.getValueChanged()) {
             return;
         }
 
         notifyTargetEdited(true);
 
         if (listeners != null) {
             for (BindingListener listener : listeners) {
                 listener.targetEdited(this);
             }
         }
 
         targetChangedImpl(pse);
     }
 
     protected void targetChangedImpl(PropertyStateEvent pse) {
     }
 
     private void notifySourceEdited(boolean newValue) {
         boolean old = hasEditedSource;
         hasEditedSource = newValue;
         if (changeSupport != null) {
             changeSupport.firePropertyChange("hasEditedSource", old, hasEditedSource);
         }
     }
     
     private void notifyTargetEdited(boolean newValue) {
         boolean old = hasEditedTarget;
         hasEditedTarget = newValue;
         if (changeSupport != null) {
             changeSupport.firePropertyChange("hasEditedTarget", old, hasEditedTarget);
         }
     }
 
     public final void addPropertyChangeListener(PropertyChangeListener listener) {
         if (changeSupport == null) {
             changeSupport = new PropertyChangeSupport(this);
         }
 
         changeSupport.addPropertyChangeListener(listener);
     }
 
     public final void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
         if (changeSupport == null) {
             changeSupport = new PropertyChangeSupport(this);
         }
 
         changeSupport.addPropertyChangeListener(propertyName, listener);
     }
 
     public final void removePropertyChangeListener(PropertyChangeListener listener) {
         if (changeSupport == null) {
             return;
         }
 
         changeSupport.removePropertyChangeListener(listener);
     }
 
     public final void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
         if (changeSupport == null) {
             return;
         }
 
         changeSupport.removePropertyChangeListener(propertyName, listener);
     }
 
     public final PropertyChangeListener[] getPropertyChangeListeners() {
         if (changeSupport == null) {
             return new PropertyChangeListener[0];
         }
         
         return changeSupport.getPropertyChangeListeners();
     }
 
     public final PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
         if (changeSupport == null) {
             return new PropertyChangeListener[0];
         }
         
         return changeSupport.getPropertyChangeListeners(propertyName);
     }
     
     private class PSL implements PropertyStateListener {
         public void propertyStateChanged(PropertyStateEvent pse) {
             if (ignoreChange) {
                 return;
             }
 
             if (pse.getSourceProperty() == sourceProperty && pse.getSourceObject() == sourceObject) {
                 sourceChanged(pse);
             } else {
                 targetChanged(pse);
             }
         }
     }
 
 }
