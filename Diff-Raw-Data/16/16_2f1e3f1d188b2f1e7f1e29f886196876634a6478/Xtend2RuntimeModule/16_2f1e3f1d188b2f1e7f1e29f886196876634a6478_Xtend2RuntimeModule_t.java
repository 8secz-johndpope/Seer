 /*
  * generated by Xtext
  */
 package org.eclipse.xtext.xtend2;
 
 import org.eclipse.xtext.common.types.JvmTypeReference;
 import org.eclipse.xtext.common.types.util.IJvmTypeConformanceComputer;
import org.eclipse.xtext.common.types.util.TypeArgumentContext;
 import org.eclipse.xtext.conversion.IValueConverterService;
 import org.eclipse.xtext.scoping.IScopeProvider;
 import org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider;
 import org.eclipse.xtext.xbase.scoping.XbaseImportedNamespaceScopeProvider;
import org.eclipse.xtext.xbase.typing.XbaseTypeArgumentContextProvider;
import org.eclipse.xtext.xbase.typing.XbaseTypeConformanceComputer;
 import org.eclipse.xtext.xbase.typing.XbaseTypeProvider;
 import org.eclipse.xtext.xtend2.conversion.Xtend2ValueConverterService;
 import org.eclipse.xtext.xtend2.typing.Xtend2ExpectedTypeProvider;
 import org.eclipse.xtext.xtend2.typing.Xtend2TypeProvider;
 
 import com.google.inject.Binder;
 import com.google.inject.name.Names;
 
 /**
  * Use this class to register components to be used at runtime / without the Equinox extension registry.
  */
 public class Xtend2RuntimeModule extends org.eclipse.xtext.xtend2.AbstractXtend2RuntimeModule {
 	public Class<? extends org.eclipse.xtext.typing.ITypeConformanceComputer<JvmTypeReference>> bindITypeService() {
 		return IJvmTypeConformanceComputer.class;
 	}
 	
	public Class<? extends IJvmTypeConformanceComputer> bindIJvmTypeConformanceComputer() {
		return XbaseTypeConformanceComputer.class;
	}
	
 	public Class<? extends org.eclipse.xtext.typing.ITypeProvider<JvmTypeReference>> bindITypeProvider() {
 		return XbaseTypeProvider.class;
 	}
 	
 	public Class<? extends XbaseTypeProvider> bindXbaseTypeProvider() {
 		return Xtend2TypeProvider.class;
 	}
 	
 	public Class<? extends org.eclipse.xtext.typing.IExpectedTypeProvider<JvmTypeReference>> bindIExpectedTypeProvider() {
 		return Xtend2ExpectedTypeProvider.class;
 	}

	public Class<? extends TypeArgumentContext.Provider> bindTypeArgumentContextProvider() {
		return XbaseTypeArgumentContextProvider.class;
	}
 	
 	@Override
 	public void configureIScopeProviderDelegate(Binder binder) {
 		binder.bind(IScopeProvider.class).annotatedWith(Names.named(AbstractDeclarativeScopeProvider.NAMED_DELEGATE)).to(XbaseImportedNamespaceScopeProvider.class);
 	}
 	
 	@Override
 	public Class<? extends IValueConverterService> bindIValueConverterService() {
 		return Xtend2ValueConverterService.class;
 	}
	
 }
