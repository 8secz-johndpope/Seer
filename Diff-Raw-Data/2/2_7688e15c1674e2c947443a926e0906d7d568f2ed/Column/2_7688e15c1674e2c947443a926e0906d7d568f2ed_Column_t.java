 package com.michaldabski.msqlite.models;
 
 import java.io.NotSerializableException;
 import java.io.Serializable;
 import java.lang.reflect.Field;
 import java.util.Locale;
 
 import android.util.Log;
 
 import com.michaldabski.msqlite.Annotations.ColumnName;
 import com.michaldabski.msqlite.Annotations.DataType;
 import com.michaldabski.msqlite.DataTypes;
 
 /**
  * Represents Table column
  * @author Michal
  *
  */
 public class Column
 {
 	// Real underlying type
 	
 	private final Field field;
 	protected boolean 
 		NOT_NULL, 
 		PRIMARY_KEY;
 	
 	protected final String 
 		name,
 		dataType,
 		// table.field
 		uniqueName;
 	
 	protected final Class<?> fieldClass;
 	private final int fieldType;
 	
 	public Column(Field field)
 	{
 		this.field =field;
 		
 		if (field.isAnnotationPresent(ColumnName.class)) this.name = field.getAnnotation(ColumnName.class).value();
 		else this.name = field.getName();
 		
 		this.fieldClass = field.getType();
 		
 		this.uniqueName = String.format("%s.%s", field.getDeclaringClass().getSimpleName(), name);
 		
 		fieldType = DataTypes.getFieldType(fieldClass);
 		if (fieldType == DataTypes.TYPE_OTHER)
 		{
 			Log.w("MSQLite", fieldClass.getSimpleName()+" is not supported as a table field yet. Consider making this field transient.");
 			dataType = null;
 			return;
 		}
 		else if (fieldType == DataTypes.TYPE_SERIALIZABLE)
 		{
			if (Serializable.class.isAssignableFrom(field.getType()) == false)
 			{
 				NotSerializableException e = new NotSerializableException(field.getType().getSimpleName()+" does not implement Serializable.")
 				{
 					private static final long serialVersionUID = 1L;
 
 					@Override
 					public String getMessage() 
 					{
 						return String.format(Locale.ENGLISH, "Cannot convert %s into database table definition, because its field '%s' does not implement Serializable Interface. You should implement Serializable or make the field transient.",  
 								Column.this.field.getDeclaringClass().getSimpleName(),
 								Column.this.field.getName());
 					}
 				};
 				throw new RuntimeException(e);
 			}
 		}	
 		
 		if (field.isAnnotationPresent(DataType.class))
 			this.dataType = field.getAnnotation(DataType.class).value();
 		else 
 			this.dataType = DataTypes.getDataType(fieldType);
 		
 		this.PRIMARY_KEY = field.isAnnotationPresent(com.michaldabski.msqlite.Annotations.PrimaryKey.class);
 		this.NOT_NULL = field.isAnnotationPresent(com.michaldabski.msqlite.Annotations.NotNull.class);
 	}
 	
 	public String getDataType()
 	{
 		return dataType;
 	}
 	
 	public CharSequence getBuilder()
 	{
 		StringBuilder builder = new StringBuilder()
 			.append('`')
 			.append(name)
 			.append('`')
 			.append(' ')
 			.append(this.dataType);
 		
 		
 		if (NOT_NULL == true) builder.append(" NOT");
 		builder.append(" NULL");
 		
 		return builder;
 	}
 	
 	@Override
 	public String toString()
 	{
 		return uniqueName;
 	}
 	
 	public String getName()
 	{
 		return name;
 	}
 	
 	@Override
 	public int hashCode()
 	{
 		return uniqueName.hashCode();
 	}
 	
 	@Override
 	public boolean equals(Object obj)
 	{
 		return uniqueName.equals(obj);
 	}
 	
 	public Object getValue(Object object) throws IllegalArgumentException, NoSuchFieldException
 	{
 //		return fieldClass.cast(object.getClass().getField(fieldName).get(object))
 		try
 		{
 			return field.get(object);
 		} catch (IllegalAccessException e)
 		{
 			field.setAccessible(true);
 			Object value = getValue(object);
 			field.setAccessible(false);
 			return value;
 		}
 	}
 	
 	/**
 	 * Sets raw value without interpreting or converting
 	 */
 	public void setValue(Object object, Object value)
 	{
 		try
 		{
 			field.set(object, value);
 		} catch (IllegalAccessException e)
 		{
 			field.setAccessible(true);
 			setValue(object, value);
 			field.setAccessible(false);
 		}
 	}
 	
 	public void setValue(Object object, Long value) throws IllegalArgumentException
 	{
 		try
 		{
 			switch (fieldType)
 			{
 				case DataTypes.TYPE_INT:
 					field.set(object, value.intValue());
 					break;
 				case DataTypes.TYPE_LONG:
 					field.set(object, value);
 					break;
 				case DataTypes.TYPE_BYTE:
 					field.set(object, value.byteValue());
 					break;
 			}
 		} catch (IllegalAccessException e)
 		{
 			field.setAccessible(true);
 			setValue(object, value);
 			field.setAccessible(false);
 		}
 	}
 	
 	public int getFieldType()
 	{
 		return fieldType;
 	}
 	
 	public void setValueFromString(Object object, String value) throws IllegalArgumentException
 	{
 		try
 		{
 			if (value == null)
 				field.set(object, null);
 			switch (fieldType)
 			{
 				case DataTypes.TYPE_STRING:
 					field.set(object, value);
 					break;
 
 				case DataTypes.TYPE_INT:
 					field.set(object, Integer.valueOf(value));
 					break;
 					
 				case DataTypes.TYPE_LONG:
 					field.set(object, Long.valueOf(value));
 					break;
 					
 				case DataTypes.TYPE_DOUBLE:
 					field.set(object, Double.valueOf(value));
 					break;
 					
 				case DataTypes.TYPE_FLOAT:
 					field.set(object, Float.valueOf(value));
 					break;
 					
 				case DataTypes.TYPE_BYTE:
 					field.set(object, Byte.valueOf(value));
 					break;
 					
 				case DataTypes.TYPE_CHAR:
 					field.set(object, value.charAt(0));
 					break;					
 					
 				case DataTypes.TYPE_SHORT:
 					field.set(object, Short.valueOf(value));
 					break;
 			}				
 		} 
 		catch (IllegalAccessException e)
 		{
 			field.setAccessible(true);
 			setValueFromString(object, value);
 			field.setAccessible(false);
 		}
 	}
 }
