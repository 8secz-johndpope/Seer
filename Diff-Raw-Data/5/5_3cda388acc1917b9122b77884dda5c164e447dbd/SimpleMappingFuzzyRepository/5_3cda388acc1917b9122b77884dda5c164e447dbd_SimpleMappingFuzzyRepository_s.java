 package com.wwm.db.spring.repository;
 
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.ObjectInputStream;
 import java.io.ObjectOutputStream;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.Map.Entry;
 
 import org.springframework.beans.DirectFieldAccessor;
 import org.springframework.beans.factory.annotation.Autowired;
 
 import com.wwm.attrs.AttributeDefinitionService;
 import com.wwm.attrs.converters.WhirlwindConversionService;
 import com.wwm.attrs.enums.EnumExclusiveValue;
 import com.wwm.attrs.enums.EnumMultipleValue;
 import com.wwm.attrs.search.SearchSpecImpl;
 import com.wwm.attrs.userobjects.BlobStoringWhirlwindItem;
 import com.wwm.db.GenericRef;
 import com.wwm.db.internal.ResultImpl;
 import com.wwm.db.query.Result;
 import com.wwm.db.query.ResultIterator;
 import com.wwm.db.query.ResultSet;
 import com.wwm.db.whirlwind.SearchSpec;
 import com.wwm.db.whirlwind.internal.IAttribute;
 import com.wwm.model.attributes.EnumAttribute;
 import com.wwm.model.attributes.MultiEnumAttribute;
 
 /**
  * A simple (PoC) Repository implementation that performs a minimal conversion to get attributes
  * in and out of the database
  *
  * Fuller support will come in time. This is a starting point to get a walking-skeleton 
  * up and err... walking.
  * 
  * @author Neale Upstone
  *
  * @param <T> the type being stored (Must contain a field: Map<String,Object> attributes for the fuzzy data)
  */
 public class SimpleMappingFuzzyRepository<T> extends AbstractConvertingRepository<BlobStoringWhirlwindItem, T, GenericRef<T>> {
 
 	@Autowired
 	private WhirlwindConversionService converter; 
 	
 	@Autowired
 	private AttributeDefinitionService attrDefinitionService;
 
 	public SimpleMappingFuzzyRepository(Class<T> type) {
 		super(type);
 	}
 
 	@Override
 	protected T fromInternal(BlobStoringWhirlwindItem internal) {
 		T result = createInstance(internal);
 		Map<String,Object> externalMap = getAttrsField(result);
 		
 		for( IAttribute attr : internal.getAttributeMap()) {
 			addConvertedAttribute(externalMap, attr);
 		}
 		
 		return result;
 	}
 
 	private void addConvertedAttribute(Map<String, Object> externalMap, IAttribute attr) {
 		
 		String key = attrDefinitionService.getAttrName(attr.getAttrId());
 		Object value = converter.convert(attr, attrDefinitionService.getExternalClass(attr.getAttrId()));
 		externalMap.put(key, value);
 	}
 
 	@Override
 	protected BlobStoringWhirlwindItem toInternal(T external) {
 		Map<String,Object> externalMap = getAttrsField(external);
 		BlobStoringWhirlwindItem result = new BlobStoringWhirlwindItem(null);
 		for (Entry<String, Object> item : externalMap.entrySet()) {
 			addConvertedAttribute(result, item.getKey(), item.getValue());
 		}
 		
 		ByteArrayOutputStream baos = new ByteArrayOutputStream();
 		try {
 			ObjectOutputStream oos = new ObjectOutputStream(baos);
 			oos.writeObject(external);
 			oos.close();
 		} catch (IOException e) {
 			throw new RuntimeException(e);
 		}
 		byte[] bytes = baos.toByteArray();
 		result.setBlob(bytes);
 		return result;
 	}
 
 	private void addConvertedAttribute(BlobStoringWhirlwindItem result,
 			String key, Object value) {
 
 		int id = attrDefinitionService.getAttrId(key, value.getClass());
 		Class<? extends IAttribute> dbClass = attrDefinitionService.getDbClass(id);
 		// If can't convert as is, wrap the value based on the db class
 		if (!converter.canConvert(value.getClass(), dbClass)) {
 			// TODO: Generalize this as part of using TypeDescriptor base converters
 			value = wrapValue(key, value, dbClass);
 		}
 		IAttribute attr = converter.convert(value, dbClass);
 		result.getAttributeMap().put(id, attr);
 	}
 
 	
 	private Object wrapValue(String key, Object value, Class<? extends IAttribute> dbClass) {
 		if (dbClass.equals(EnumExclusiveValue.class)) {
			return new EnumAttribute(key, null, (String)value);
 		}
 		if (dbClass.equals(EnumMultipleValue.class)) {
			return new MultiEnumAttribute(key, null, (String[])value);
 		}
 		return value;
 	}
 
 	@SuppressWarnings("unchecked")
 	private Map<String, Object> getAttrsField(T external) {
 		Object attrs = new DirectFieldAccessor(external).getPropertyValue("attributes"); // TODO: make annotated
 		return (Map<String, Object>) attrs;
 	}
 
 	@SuppressWarnings("unchecked")
 	private T createInstance(BlobStoringWhirlwindItem internal) {
 		try {
 		if (internal.getBlob() != null) {
 			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(internal.getBlob()));
 			return (T) ois.readObject();
 		}
 			return type.newInstance();
 		} catch (Exception e) {
 			throw new RuntimeException(e);
 		}
 	}
 
 	@SuppressWarnings("unchecked")
 	@Override
 	protected final GenericRef<BlobStoringWhirlwindItem> toInternalId(GenericRef<T> id) {
 		// Externally we ref as GenericRef<T>  and we are using the real ref here
 		return (GenericRef<BlobStoringWhirlwindItem>) id;
 	}
 	
 	@Override
 	protected Iterator<Result<T>> findMatchesInternal(BlobStoringWhirlwindItem internal, String matchStyle, int maxResults) {
 		SearchSpec spec = new SearchSpecImpl(BlobStoringWhirlwindItem.class, matchStyle);
 		spec.setTargetNumResults(maxResults);
 		spec.setAttributes(internal);
 		ResultSet<Result<BlobStoringWhirlwindItem>> resultsInternal = getPersister().query(BlobStoringWhirlwindItem.class, spec);
 		final ResultIterator<Result<BlobStoringWhirlwindItem>> resultIterator = resultsInternal.iterator();
 
 		Iterator<Result<T>> iterator = new Iterator<Result<T>>() {
 			public boolean hasNext() {
 				return resultIterator.hasNext();
 			}
 			public Result<T> next() {
 				Result<BlobStoringWhirlwindItem> resultInternal = resultIterator.next();
 				
 				Result<T> result = new ResultImpl<T>(fromInternal(resultInternal.getItem()), resultInternal.getScore());
 				return result;
 			}
 
 			public void remove() {
 				resultIterator.remove(); // Generally we'd not expect this to be supported
 			}
 		};
 		return iterator;
 	}
 }
