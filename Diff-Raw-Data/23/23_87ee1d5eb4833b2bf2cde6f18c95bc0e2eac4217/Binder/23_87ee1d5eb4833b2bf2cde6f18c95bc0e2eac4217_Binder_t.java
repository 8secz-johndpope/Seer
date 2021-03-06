 package de.jbee.inject.bind;
 
 import static de.jbee.inject.Instance.defaultInstanceOf;
 import static de.jbee.inject.Instance.instance;
 import static de.jbee.inject.Type.instanceType;
 import static de.jbee.inject.Type.raw;
 import static de.jbee.inject.util.SuppliedBy.constant;
 import static de.jbee.inject.util.SuppliedBy.provider;
 
 import java.lang.reflect.Constructor;
 import java.lang.reflect.Modifier;
 
 import de.jbee.inject.InjectionStrategy;
 import de.jbee.inject.Instance;
 import de.jbee.inject.Name;
 import de.jbee.inject.Packages;
 import de.jbee.inject.Resource;
 import de.jbee.inject.Scope;
 import de.jbee.inject.Source;
 import de.jbee.inject.Supplier;
 import de.jbee.inject.Target;
 import de.jbee.inject.Type;
 import de.jbee.inject.util.Factory;
 import de.jbee.inject.util.Provider;
 import de.jbee.inject.util.Scoped;
 import de.jbee.inject.util.SuppliedBy;
 import de.jbee.inject.util.TypeReflector;
 
 public class Binder
 		implements BasicBinder {
 
 	public static final InjectionStrategy DEFAULE_INJECTION_STRATEGY = new BuildinInjectionStrategy();
 
 	private static class BuildinInjectionStrategy
 			implements InjectionStrategy {
 
 		BuildinInjectionStrategy() {
 			// make visible
 		}
 
 		@Override
 		public <T> Constructor<T> constructorFor( Class<T> type ) {
 			try {
 				// FIXME thats should be up to the supplier so it can be done once a once place but for the implicit ones we want to return null on a security exception
 				return TypeReflector.accessibleConstructor( type );
 			} catch ( RuntimeException e ) {
 				return null;
 			}
 		}
 
 		@Override
 		public <T> Instance<?>[] parametersFor( Constructor<T> constructor ) {
 			return Instance.anyOf( Type.parameterTypes( constructor ) );
 		}
 
 	}
 
 	private static class AutobindBindings
 			implements Bindings {
 
 		private final Bindings delegate;
 
 		AutobindBindings( Bindings delegate ) {
 			super();
 			this.delegate = delegate;
 		}
 
 		@Override
 		public <T> void add( Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
 				Source source ) {
 			delegate.add( resource, supplier, scope, source );
 			Type<T> type = resource.getType();
 			for ( Type<? super T> supertype : type.getSupertypes() ) {
 				// Object is of cause a superclass of everything but not indented when doing auto-binds
 				if ( supertype.getRawType() != Object.class ) {
 					delegate.add( resource.typed( supertype ), supplier, scope, source );
 				}
 			}
 		}
 	}
 
 	public static Bindings autobinding( Bindings delegate ) {
 		return new AutobindBindings( delegate );
 	}
 
 	public static RootBinder create( Bindings bindings, Source source ) {
 		return create( bindings, DEFAULE_INJECTION_STRATEGY, source );
 	}
 
 	public static RootBinder create( Bindings bindings, InjectionStrategy strategy, Source source ) {
 		return new RootBinder( bindings, strategy, source );
 	}
 
 	private final Bindings bindings;
 	private final InjectionStrategy strategy;
 	private final Source source;
 	private final Scope scope;
 	private final Target target;
 
 	Binder( Bindings bindings, InjectionStrategy strategy, Source source, Scope scope, Target target ) {
 		super();
 		this.bindings = bindings;
 		this.strategy = strategy;
 		this.source = source;
 		this.scope = scope;
 		this.target = target;
 	}
 
 	public <T> TypedBinder<T> starbind( Class<T> type ) {
 		return bind( Instance.anyOf( Type.raw( type ) ) );
 	}
 
 	@Override
 	public <T> TypedBinder<T> bind( Instance<T> instance ) {
 		return new TypedBinder<T>( this, instance );
 	}
 
 	public <T> TypedBinder<T> bind( Name name, Class<T> type ) {
 		// also do a implicit bind to constructor ?!
 		return bind( name, Type.raw( type ) );
 	}
 
 	public <T> TypedBinder<T> bind( Name name, Type<T> type ) {
 		return bind( instance( name, type ) );
 	}
 
 	public <T> TypedBinder<T> bind( Type<T> type ) {
 		return bind( defaultInstanceOf( type ) );
 	}
 
 	public <T> TypedBinder<T> bind( Class<T> type ) {
 		implicitBindToConstructor( defaultInstanceOf( raw( type ) ) );
 		return bind( Type.raw( type ) );
 	}
 
 	public <E> TypedElementBinder<E> bind( Class<E[]> type ) {
 		return new TypedElementBinder<E>( this, defaultInstanceOf( raw( type ) ) );
 	}
 
 	public <T> TypedBinder<T> autobind( Type<T> type ) {
 		return into( autobinding( bindings ) ).bind( type );
 	}
 
 	public <T> TypedBinder<T> autobind( Class<T> type ) {
 		return autobind( Type.raw( type ) );
 	}
 
 	public <T> TypedBinder<T> multibind( Instance<T> instance ) {
 		return with( source.multi() ).bind( instance );
 	}
 
 	public <T> TypedBinder<T> multibind( Type<T> type ) {
 		return multibind( defaultInstanceOf( type ) );
 	}
 
 	public <T> TypedBinder<T> multibind( Class<T> type ) {
 		return multibind( Type.raw( type ) );
 	}
 
 	public <T> TypedBinder<T> multibind( Name name, Class<T> type ) {
 		return multibind( instance( name, Type.raw( type ) ) );
 	}
 
 	public <T> TypedBinder<T> multibind( Name name, Type<T> type ) {
 		return multibind( instance( name, type ) );
 	}
 
 	final Target target() {
 		return target;
 	}
 
 	final Source source() {
 		return source;
 	}
 
 	final Scope scope() {
 		return scope;
 	}
 
 	//OPEN maybe Binder implements bindings and delegates instead of exposing it here
 	final Bindings bindings() {
 		return bindings;
 	}
 
 	final InjectionStrategy strategy() {
 		return strategy;
 	}
 
 	protected final <T> void bind( Resource<T> resource, Supplier<? extends T> supplier ) {
 		bindings.add( resource, supplier, scope, source );
 	}
 
 	protected final void implicitBindToConstructor( Class<?>... impls ) {
 		for ( Class<?> impl : impls ) {
 			implicitBindToConstructor( impl );
 		}
 	}
 
 	protected final <I> void implicitBindToConstructor( Class<I> impl ) {
 		implicitBindToConstructor( defaultInstanceOf( raw( impl ) ) );
 	}
 
 	protected final <I> void implicitBindToConstructor( Instance<I> instance ) {
 		Class<I> impl = instance.getType().getRawType();
 		if ( notConstructable( impl ) ) {
 			return;
 		}
 		Constructor<I> constructor = strategy().constructorFor( impl );
 		if ( constructor != null ) {
 			implicit().with( Target.ANY ).bind( instance ).to( constructor );
 		}
 	}
 
 	protected final boolean notConstructable( Class<?> type ) {
 		return type.isInterface() || type.isEnum() || type.isPrimitive() || type.isArray()
 				|| Modifier.isAbstract( type.getModifiers() ) || type == String.class
 				|| Number.class.isAssignableFrom( type );
 	}
 
 	protected final Binder implicit() {
 		return with( source.implicit() );
 	}
 
 	protected final Binder multi() {
 		return with( source.multi() );
 	}
 
 	protected Binder with( Source source ) {
 		return new Binder( bindings, strategy, source, scope, target );
 	}
 
 	protected Binder with( Target target ) {
 		return new Binder( bindings, strategy, source, scope, target );
 	}
 
 	protected Binder into( Bindings bindings ) {
 		return new Binder( bindings, strategy, source, scope, target );
 	}
 
 	/**
 	 * This kind of bindings actually re-map the []-type so that the automatic behavior of returning
 	 * all known instances of the element type will no longer be used whenever the bind made
 	 * applies.
 	 * 
 	 * @author Jan Bernitt (jan.bernitt@gmx.de)
 	 * 
 	 */
 	public static class TypedElementBinder<E>
 			extends TypedBinder<E[]> {
 
 		TypedElementBinder( Binder binder, Instance<E[]> instance ) {
 			super( binder.multi(), instance );
 		}
 
 		public void to( Supplier<? extends E>[] elements ) {
 			to( SuppliedBy.elements( getType().getRawType(), elements ) );
 		}
 
 		@SuppressWarnings ( "unchecked" )
 		public void to( Supplier<? extends E> supplier1, Supplier<? extends E> supplier2 ) {
 			to( (Supplier<? extends E>[]) new Supplier<?>[] { supplier1, supplier2 } );
 		}
 
 		@SuppressWarnings ( "unchecked" )
 		public void to( Supplier<? extends E> supplier1, Supplier<? extends E> supplier2,
 				Supplier<? extends E> supplier3 ) {
 			to( (Supplier<? extends E>[]) new Supplier<?>[] { supplier1, supplier2, supplier3 } );
 		}
 
 		public void toElements( E constant1, E constant2 ) {
 			to( constant( constant1 ), constant( constant2 ) );
 		}
 
 		public void toElements( Class<? extends E> impl1, Class<? extends E> impl2 ) {
 			to( SuppliedBy.type( impl1 ), SuppliedBy.type( impl2 ) );
 			implicitBindToConstructor( impl1, impl2 );
 		}
 
 		public void toElements( Class<? extends E> impl1, Class<? extends E> impl2,
 				Class<? extends E> impl3 ) {
 			to( SuppliedBy.type( impl1 ), SuppliedBy.type( impl2 ), SuppliedBy.type( impl3 ) );
 			implicitBindToConstructor( impl1, impl2, impl3 );
 		}
 
 		// and so on.... will avoid generic array warning here 
 	}
 
 	public static class RootBinder
 			extends ScopedBinder
 			implements RootBasicBinder {
 
 		RootBinder( Bindings bindings, InjectionStrategy strategy, Source source ) {
 			super( bindings, strategy, source, Scoped.INJECTION );
 		}
 
 		@Override
 		public ScopedBinder per( Scope scope ) {
 			return new ScopedBinder( bindings(), strategy(), source(), scope );
 		}
 
 		@Override
 		protected RootBinder with( Source source ) {
 			return new RootBinder( bindings(), strategy(), source );
 		}
 
 		@Override
 		protected RootBinder into( Bindings bindings ) {
 			return new RootBinder( bindings, strategy(), source() );
 		}
 
 		protected RootBinder asDefault() {
 			return with( source().asDefault() );
 		}
 	}
 
 	public static class ScopedBinder
 			extends TargetedBinder
 			implements ScopedBasicBinder {
 
 		ScopedBinder( Bindings bindings, InjectionStrategy strategy, Source source, Scope scope ) {
 			super( bindings, strategy, source, scope );
 		}
 
 		public TargetedBinder injectingInto( Class<?> target ) {
 			return injectingInto( raw( target ) );
 		}
 
 		public TargetedBinder injectingInto( Type<?> target ) {
 			return injectingInto( defaultInstanceOf( target ) );
 		}
 
 		public TargetedBinder injectingInto( Name name, Class<?> type ) {
 			return injectingInto( name, raw( type ) );
 		}
 
 		public TargetedBinder injectingInto( Name name, Type<?> type ) {
 			return injectingInto( Instance.instance( name, type ) );
 		}
 
 		@Override
 		public TargetedBinder injectingInto( Instance<?> target ) {
 			return new TargetedBinder( bindings(), strategy(), source(), scope(), target );
 		}
 
 	}
 
 	public static class TargetedBinder
 			extends Binder
 			implements TargetedBasicBinder {
 
 		TargetedBinder( Bindings bindings, InjectionStrategy strategy, Source source, Scope scope ) {
 			super( bindings, strategy, source, scope, Target.ANY );
 		}
 
 		TargetedBinder( Bindings bindings, InjectionStrategy strategy, Source source, Scope scope,
 				Instance<?> target ) {
 			super( bindings, strategy, source, scope, Target.targeting( target ) );
 		}
 
 		//TODO improve this since from a dependency point of view it is good to localize all binds somehow
 		// instead of narrow explicit we could expose explicit and make binds as narrow as possible by default (classic interface to impl binds in same package)
 
 		@Override
 		public Binder within( Packages packages ) {
 			return with( target().within( packages ) );
 		}
 
 		public Binder inPackageOf( Class<?> type ) {
 			return with( target().inPackageOf( type ) );
 		}
 
 		public Binder inSubPackagesOf( Class<?> type ) {
 			return with( target().inSubPackagesOf( type ) );
 		}
 
 		public Binder inPackageAndSubPackagesOf( Class<?> type ) {
 			return with( target().inPackageAndSubPackagesOf( type ) );
 		}
 	}
 
 	public static class TypedBinder<T>
 			implements BasicBinder.TypedBasicBinder<T> {
 
 		/**
 		 * The binder instance who's {@link RichBasicBinder#install(Instance)} method had been
 		 * called to get to this {@link TypedBasicBinder}.
 		 */
 		private final Binder binder;
 		private final Resource<T> resource;
 
 		TypedBinder( Binder binder, Instance<T> instance ) {
 			this( binder, new Resource<T>( instance, binder.target() ) );
 		}
 
 		TypedBinder( Binder binder, Resource<T> resource ) {
 			super();
 			this.binder = binder;
 			this.resource = resource;
 		}
 
 		protected final Type<T> getType() {
 			return resource.getType();
 		}
 
 		@Override
 		public void to( Supplier<? extends T> supplier ) {
 			binder.bind( resource, supplier );
 		}
 
 		public void to( Factory<? extends T> factory ) {
 			to( SuppliedBy.factory( factory ) );
 		}
 
 		public void to( Constructor<? extends T> constructor ) {
 			to( SuppliedBy.costructor( constructor ) );
 		}
 
		public void to( Constructor<? extends T> constructor, Object... hints ) {
			to( SuppliedBy.costructor( constructor, hints ) );
		}

 		public void toConstructor( Class<? extends T> type, Object... hints ) {
			to( SuppliedBy.costructor( binder.strategy().constructorFor( type ), hints ) );
 		}
 
 		public void toConstructor( Object... hints ) {
 			toConstructor( resource.getType().getRawType(), hints );
 		}
 
 		public void to( T constant ) {
 			toConstant( constant );
 		}
 
 		/**
 		 * This is to do multi-binds in the same module. The {@link Binder#multibind(Class)} methods
 		 * are use when a module just does one bind but that is meant to co-exist with others from
 		 * other modules.
 		 */
 		public void to( T constant1, T constant2 ) {
 			multi().toConstant( constant1 ).toConstant( constant2 );
 		}
 
 		public <I extends Supplier<? extends T>> void toSupplier( Class<I> impl ) {
 			to( SuppliedBy.reference( impl ) );
 			implicitBindToConstructor( impl );
 		}
 
 		public void to( Provider<? extends T> provider ) {
 			to( provider( provider ) );
 			binder.implicit().bind( defaultInstanceOf( instanceType( Provider.class, provider ) ) ).to(
 					SuppliedBy.constant( provider ) );
 		}
 
 		public void toConstructor() {
 			to( binder.strategy().constructorFor( resource.getType().getRawType() ) );
 		}
 
 		public <I extends T> void to( Class<I> impl ) {
 			if ( resource.getType().getRawType() != impl || !resource.getName().isDefault() ) {
 				to( SuppliedBy.type( Type.raw( impl ) ) );
 			}
 			implicitBindToConstructor( impl );
 		}
 
 		public <I extends T> void to( Name name, Type<I> type ) {
 			to( instance( name, type ) );
 		}
 
 		public <I extends T> void to( Name name, Class<I> type ) {
 			to( instance( name, raw( type ) ) );
 		}
 
 		public <I extends T> void to( Instance<I> instance ) {
 			//FIXME don#t do the below when this is exactly the same instance as the resource - would describe a loop
 			to( SuppliedBy.instance( instance ) );
 			implicitBindToConstructor( instance );
 		}
 
 		void implicitBindToConstructor( Class<?>... impls ) {
 			binder.implicitBindToConstructor( impls );
 		}
 
 		<I> void implicitBindToConstructor( Class<I> impl ) {
 			binder.implicitBindToConstructor( impl );
 		}
 
 		<I> void implicitBindToConstructor( Instance<I> instance ) {
 			binder.implicitBindToConstructor( instance );
 		}
 
 		protected final TypedBinder<T> multi() {
 			return new TypedBinder<T>( binder.multi(), resource );
 		}
 
 		private TypedBinder<T> toConstant( T constant ) {
 			to( SuppliedBy.constant( constant ) );
 			return this;
 		}
 
 	}
 }
