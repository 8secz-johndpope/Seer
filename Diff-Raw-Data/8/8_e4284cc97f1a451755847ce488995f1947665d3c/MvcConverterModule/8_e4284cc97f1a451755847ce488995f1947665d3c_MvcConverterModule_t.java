 package org.zdevra.guice.mvc;
 
 import org.zdevra.guice.mvc.ConversionService.ConverterFactory;
 
 import com.google.inject.AbstractModule;
 
 
 /**
  * It's regular Guice's abstract module but it's enriched 
  * by methods which provides converter registration.
  *
  */
 public abstract class MvcConverterModule extends AbstractModule {
 	
 // ------------------------------------------------------------------------
 	
 	private MultibinderBuilder<ConverterFactory> conversionServiceBuilder;
 	
 // ------------------------------------------------------------------------
 	
 	/**
	 * In this method you will setup your converters
 	 */
	protected abstract void configureConverters();
 	
 
 // ------------------------------------------------------------------------
 	
 	@Override
 	protected final void configure() {
 		try {
 			conversionServiceBuilder = new MultibinderBuilder<ConverterFactory>(binder(), ConverterFactory.class);
			configureConverters();
 		} finally {
 			conversionServiceBuilder = null;
 		}
 	}
 
 // ------------------------------------------------------------------------
 	
 	/**
 	 * The method registers a custom converter which converts strings to the
 	 * concrete types. These converters are used for conversions from a HTTP request 
 	 * to the method's parameters.
 	 * 
 	 * The all predefined default converters are placed in the 'converters' sub-package.  
 	 */
 	protected final void registerConverter(ConverterFactory converterFactory) {
 		this.conversionServiceBuilder.registerInstance(converterFactory);
 	}
 	
 	
 	/**
 	 * The method registers a custom converter which converts strings to the
 	 * concrete types. These converters are used for conversions from a HTTP request 
 	 * to the method's parameters.
 	 * 
 	 * The all predefined default convertors are placed in the 'converters' sub-package.  
 	 */
 	protected final void registerConverter(Class<? extends ConverterFactory> convertorFactoryClazz) {
 		this.conversionServiceBuilder.registerClass(convertorFactoryClazz);
 	}
 
 	
 // ------------------------------------------------------------------------
 }
