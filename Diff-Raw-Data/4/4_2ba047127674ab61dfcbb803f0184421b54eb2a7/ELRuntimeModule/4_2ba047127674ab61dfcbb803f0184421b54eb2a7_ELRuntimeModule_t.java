 /*
  * generated by Xtext
  */
 package ch.vorburger.el;
 
 import org.eclipse.xtext.common.types.xtext.AbstractTypeScopeProvider;
 import org.eclipse.xtext.conversion.IValueConverterService;
 import org.eclipse.xtext.generator.IGenerator;
 import org.eclipse.xtext.naming.IQualifiedNameProvider;
 import org.eclipse.xtext.scoping.IGlobalScopeProvider;
 import org.eclipse.xtext.scoping.IScopeProvider;
 import org.eclipse.xtext.xbase.featurecalls.IdentifiableSimpleNameProvider;
 import org.eclipse.xtext.xbase.interpreter.IExpressionInterpreter;
 import org.eclipse.xtext.xbase.jvmmodel.JvmGlobalScopeProvider;
 import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
 import org.eclipse.xtext.xbase.scoping.featurecalls.StaticImplicitMethodsFeatureForTypeProvider;
 import org.eclipse.xtext.xbase.typing.ITypeProvider;
 
 import ch.vorburger.el.engine.Expression;
 import ch.vorburger.el.engine.ExpressionImpl;
 import ch.vorburger.el.generator.ELGenerator;
 import ch.vorburger.el.interpreter.ELInterpreter;
 import ch.vorburger.el.jvmmodel.ELIdentifiableSimpleNameProvider;
 import ch.vorburger.el.naming.ELQualifiedNameProvider;
 import ch.vorburger.el.scoping.ELExtensionClassNameProvider;
 import ch.vorburger.el.scoping.ELScopeProvider;
 import ch.vorburger.el.scoping.ELTypeScopeProvider;
 import ch.vorburger.el.typing.ELJvmTypeProviderFactory;
 import ch.vorburger.el.typing.ELJvmTypesBuilder;
 import ch.vorburger.el.typing.ELTypeProvider;
 import ch.vorburger.el.typing.Ecore2JvmTypeMapper;
 import ch.vorburger.el.valueconverter.ELValueConverterService;
 
 import com.google.inject.Singleton;
 
 /**
  * Use this class to register components to be used at runtime / without the
  * Equinox extension registry.
  */
 @SuppressWarnings("restriction")
 public class ELRuntimeModule extends ch.vorburger.el.AbstractELRuntimeModule {
 
 	@Override
 	public Class<? extends IValueConverterService> bindIValueConverterService() {
 		return ELValueConverterService.class;
 	}
 
 	@Override
 	public Class<? extends IdentifiableSimpleNameProvider> bindIdentifiableSimpleNameProvider() {
 		return ELIdentifiableSimpleNameProvider.class;
 	}
 
 	@Override
 	public Class<? extends IQualifiedNameProvider> bindIQualifiedNameProvider() {
 		return ELQualifiedNameProvider.class;
 	}
 
 	public Class<? extends StaticImplicitMethodsFeatureForTypeProvider.ExtensionClassNameProvider> bindExtensionClassNameProvider() {
 		return ELExtensionClassNameProvider.class;
 	}
 	
 	@Override
 	public Class<? extends ITypeProvider> bindITypeProvider() {
 		return ELTypeProvider.class;
 	}
 
 	@Override
 	public Class<? extends IExpressionInterpreter> bindIExpressionInterpreter() {
 		return ELInterpreter.class;
 	}
 	
 	@Override
 	public Class<? extends IGenerator> bindIGenerator() {
 		return ELGenerator.class;
 	}
 	
 	public Class<? extends Expression> bindExpression() {
 		return ExpressionImpl.class;
 	}
 
 	@Override
 	@Singleton
 	public Class<? extends IScopeProvider> bindIScopeProvider() {
 		return ELScopeProvider.class;
 	}
 
 	@Override
 	public Class<? extends org.eclipse.xtext.common.types.access.IJvmTypeProvider.Factory> bindIJvmTypeProvider$Factory() {
 		return ELJvmTypeProviderFactory.class;
 	}
 	
 	public Class<? extends Ecore2JvmTypeMapper> bindEcore2JvmTypeMapper() {
 		return Ecore2JvmTypeMapper.class;
 	}
 	
 	public Class<? extends JvmTypesBuilder> bindJvmTypesBuilder() {
 		return ELJvmTypesBuilder.class;
 	}
 
 	@Override
 	public Class<? extends AbstractTypeScopeProvider> bindAbstractTypeScopeProvider() {
 		return ELTypeScopeProvider.class;
 	}
 	
 	@Override
 	public Class<? extends IGlobalScopeProvider> bindIGlobalScopeProvider() {
 		return JvmGlobalScopeProvider.class;
 	}
 }
