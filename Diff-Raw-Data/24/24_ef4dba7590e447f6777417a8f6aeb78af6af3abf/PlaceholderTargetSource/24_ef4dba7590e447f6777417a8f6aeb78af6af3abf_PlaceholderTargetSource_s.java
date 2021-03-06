 /*
  * Copyright 2006-2007 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package org.springframework.batch.core.scope.util;
 
 import org.springframework.aop.TargetSource;
 import org.springframework.aop.target.SimpleBeanTargetSource;
 import org.springframework.beans.BeanWrapper;
 import org.springframework.beans.BeanWrapperImpl;
 import org.springframework.beans.BeansException;
 import org.springframework.beans.TypeConverter;
 import org.springframework.beans.TypeMismatchException;
 import org.springframework.beans.factory.InitializingBean;
 import org.springframework.beans.factory.config.BeanDefinitionVisitor;
 import org.springframework.beans.factory.config.TypedStringValue;
 import org.springframework.beans.factory.support.DefaultListableBeanFactory;
 import org.springframework.beans.factory.support.GenericBeanDefinition;
 import org.springframework.core.MethodParameter;
 import org.springframework.util.Assert;
 import org.springframework.util.StringValueResolver;
 
 /**
  * A {@link TargetSource} that lazily initializes its target, replacing bean
  * definition properties dynamically if they are marked as placeholders. String
  * values with embedded <code>#{key}</code> patterns will be replaced with the
  * corresponding value from the injected context (which must also be a String).
  * This includes dynamically locating a bean reference (e.g.
  * <code>ref="#{foo}"</code>), and partial replacement of patterns (e.g.
  * <code>value="#{foo}-bar-#{spam}"</code>). These replacements work for context
  * values that are primitive (String, Long, Integer). You can also replace
  * non-primitive values directly by making the whole bean property value into a
  * placeholder (e.g. <code>value="#{foo}"</code> where <code>foo</code> is a
  * property in the context).
  * 
  * @author Dave Syer
  * 
  */
 public class PlaceholderTargetSource extends SimpleBeanTargetSource implements InitializingBean {
 
 	/**
 	 * Key for placeholders to be replaced from the properties provided.
 	 */
 	private static final String PLACEHOLDER_PREFIX = "#{";
 
 	private static final String PLACEHOLDER_SUFFIX = "}";
 
 	private ContextFactory contextFactory;
 
 	/**
 	 * Public setter for the context factory. Used to construct the context root
 	 * whenever placeholders are replaced in a bean definition.
 	 * 
 	 * @param contextFactory the {@link ContextFactory}
 	 */
 	public void setContextFactory(ContextFactory contextFactory) {
 		this.contextFactory = contextFactory;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see
 	 * org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
 	 */
 	public void afterPropertiesSet() throws Exception {
 		Assert.notNull(contextFactory, "The ContextFactory must be set.");
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.springframework.aop.target.LazyInitTargetSource#getTarget()
 	 */
 	@Override
 	public synchronized Object getTarget() throws BeansException {
 
 		DefaultListableBeanFactory listableBeanFactory = (DefaultListableBeanFactory) getBeanFactory();
 
 		final TypeConverter typeConverter = listableBeanFactory.getTypeConverter();
 
 		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(listableBeanFactory);
 		beanFactory.copyConfigurationFrom(listableBeanFactory);
 
 		beanFactory.setTypeConverter(new TypeConverter() {
 			@SuppressWarnings("unchecked")
 			public Object convertIfNecessary(Object value, Class requiredType, MethodParameter methodParam)
 					throws TypeMismatchException {
 				Object result = null;
 				if (value instanceof String) {
 					String key = (String) value;
 					if (key.startsWith(PLACEHOLDER_PREFIX) && key.endsWith(PLACEHOLDER_SUFFIX)) {
 						key = extractKey(key);
 						result = convertFromContext(key, requiredType);
 					}
 				}
 				return result != null ? result : typeConverter.convertIfNecessary(value, requiredType, methodParam);
 			}
 
 			@SuppressWarnings("unchecked")
 			public Object convertIfNecessary(Object value, Class requiredType) throws TypeMismatchException {
 				return convertIfNecessary(value, requiredType, null);
 			}
 		});
 
 		String beanName = getTargetBeanName() + "#" + contextFactory.getContextId();
 
 		try {
 
 			/*
 			 * Need to use the merged bean definition here, otherwise it gets
 			 * cached and "frozen" in and the "regular" bean definition does not
 			 * come back when getBean() is called later on
 			 */
 			String targetBeanName = getTargetBeanName();
 			GenericBeanDefinition beanDefinition = new GenericBeanDefinition(listableBeanFactory
 					.getMergedBeanDefinition(targetBeanName));
 			logger.debug("Rehydrating scoped target: [" + targetBeanName + "]");
 
 			BeanDefinitionVisitor visitor = new BeanDefinitionVisitor(new StringValueResolver() {
 				public String resolveStringValue(String strVal) {
 					if (!strVal.contains(PLACEHOLDER_PREFIX)) {
 						return strVal;
 					}
 					return replacePlaceholders(strVal, typeConverter);
 				}
 			}) {
 				protected Object resolveValue(Object value) {
 					if (value instanceof TypedStringValue) {
 						TypedStringValue typedStringValue = (TypedStringValue) value;
 						String stringValue = typedStringValue.getValue();
 						if (stringValue != null) {
 							String visitedString = resolveStringValue(stringValue);
 							value = new TypedStringValue(visitedString);
 						}
 					}
 					else {
 						value = super.resolveValue(value);
 					}
 					return value;
 				}
 
 			};
 
 			beanFactory.registerBeanDefinition(beanName, beanDefinition);
 			// Make the replacements before the target is hydrated
 			visitor.visitBeanDefinition(beanDefinition);
 			return beanFactory.getBean(beanName);
 
 		}
 		finally {
 			beanFactory.removeBeanDefinition(beanName);
 			beanFactory = null;
 			// Anything else we can do to clean it up?
 		}
 
 	}
 
 	/**
 	 * @param value
 	 * @param requiredType
 	 * @return
 	 */
 	private Object convertFromContext(String key, Class<?> requiredType) {
 		Object result = null;
 		BeanWrapper wrapper = new BeanWrapperImpl(contextFactory.getContext());
 		if (wrapper.isReadableProperty(key)) {
 			Object property = wrapper.getPropertyValue(key);
			if (property==null || requiredType.isAssignableFrom(property.getClass())) {
 				result = property;
 			}
 		}
 		return result;
 	}
 
 	private String extractKey(String value) {
 		if (value.startsWith(PLACEHOLDER_PREFIX)) {
 			value = value.substring(PLACEHOLDER_PREFIX.length());
 			value = value.substring(0, value.length() - PLACEHOLDER_SUFFIX.length());
 		}
 		return value;
 	}
 
 	/**
 	 * @param typeConverter
 	 * @param strVal
 	 * @return
 	 */
 	private String replacePlaceholders(String value, TypeConverter typeConverter) {
 
 		StringBuilder result = new StringBuilder(value);
 
 		int first = result.indexOf(PLACEHOLDER_PREFIX);
 		int next = result.indexOf(PLACEHOLDER_SUFFIX, first + 1);
 
 		while (first >= 0) {
 
 			Assert.state(next > 0, String.format("Placeholder key incorrectly specified: use %skey%s (in %s)",
 					PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, value));
 
 			String key = result.substring(first + PLACEHOLDER_PREFIX.length(), next);
 
 			replaceIfTypeMatches(result, first, next, key, String.class, typeConverter);
 			replaceIfTypeMatches(result, first, next, key, Long.class, typeConverter);
 			replaceIfTypeMatches(result, first, next, key, Integer.class, typeConverter);
 			// Spring cannot convert from String to Date, so there is an error
 			// here.
 			// replaceIfTypeMatches(result, first, next, key, Date.class,
 			// typeConverter);

			first = result.indexOf(PLACEHOLDER_PREFIX, next + 1);
			next = result.indexOf(PLACEHOLDER_SUFFIX, first + 1);
 
 		}
 
 		logger.debug(String.format("Replaced [%s] with [%s]", value, result));
 		return result.toString();
 
 	}
 
 	private void replaceIfTypeMatches(StringBuilder result, int first, int next, String key, Class<?> requiredType,
 			TypeConverter typeConverter) {
 		Object property = convertFromContext(key, requiredType);
 		if (property != null) {
 			result.replace(first, next + 1, (String) typeConverter.convertIfNecessary(property, String.class));
 		}
 	}
 
 }
