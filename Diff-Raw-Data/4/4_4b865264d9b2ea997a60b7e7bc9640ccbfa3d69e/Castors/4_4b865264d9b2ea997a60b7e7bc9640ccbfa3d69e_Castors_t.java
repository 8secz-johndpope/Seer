 package org.nutz.castor;
 
 import java.lang.reflect.Method;
 import java.lang.reflect.Modifier;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import org.nutz.castor.castor.Array2Array;
 import org.nutz.lang.Mirror;
 import org.nutz.lang.TypeExtractor;
 import org.nutz.lang.util.Resources;
 
 /**
  * 一个创建 Castor 的工厂类。它的使用方式是：
  * 
  * <pre>
  * Castors.me().cast(obj, fromType, toType);
  * </pre>
  * 
  * 
  * @author zozoh(zozohtnt@gmail.com)
  * @author Wendal(wendal1985@gmail.com)
  */
 public class Castors {
 
	private static Castors one = new Castors();
 	
 	/**
 	 * @return 单例
 	 */
 	public static Castors me() {
 		return one;
 	}
 
 	/**
 	 * 如何抽取对象的类型级别
 	 */
 	private TypeExtractor extractor = null;
 	/**
 	 * Castor 的搜索路径
 	 */
 	private List<Class<?>> paths = null;
 	/**
 	 * Castor 的配置
 	 */
 	private Object setting;
 
 	/**
 	 * 设置转换的配置
 	 * <p>
 	 * 配置对象所有的共有方法都会被遍历。只要这个方法有一个且只有一个参数，并且该参数 是一个 org.nutz.castor.Castor
 	 * 接口的实现类。那么就会被认为是对该种 Castor 的一个 配置。
 	 * <p>
 	 * 当初始化这个 Castor 之前，会用这个方法来设置一下你的 Castor （如果你的 Castor 需要配置的话）
 	 * 
 	 * @param obj
 	 *            配置对象。可以是任意的 Java 对象。
 	 */
 	public synchronized Castors setSetting(Object obj) {
 		if (obj != null) {
 			setting = obj;
 			this.reload();
 		}
 		return this;
 	}
 
 	/**
 	 * 你的的 Castor 可能存在在不同包下，你可以把每个包下的随便一个 Castor 作为例子放到一个列表里。 这样， Nutz.Castor
 	 * 就能找到同一包下其他的 Castor 了。
 	 * <p>
 	 * 你的 Castor 存放在 CLASSPAH 下或者 Jar 包里都是没有问题的
 	 * 
 	 * @param paths
 	 *            Castor 例子列表
 	 */
 	public synchronized Castors setPaths(List<Class<?>> paths) {
 		this.paths = paths;
 		reload();
 		return this;
 	}
 
 	/**
 	 * 将 Castor 的寻找路径恢复成默认值。
 	 */
 	public synchronized Castors resetPaths() {
 		List<Class<?>> list = new ArrayList<Class<?>>();
 		list.add(Array2Array.class);
 		setPaths(list);
 		return this;
 	}
 
 	/**
 	 * 增加 Castor 的寻找路径。
 	 * 
 	 * @param paths
 	 *            示例 Castor
 	 */
 	public synchronized Castors addPaths(Class<?>... paths) {
 		if (null != paths) {
 			for (Class<?> path : paths)
 				this.paths.add(path);
 		}
 		setSetting(setting);
 		return this;
 	}
 
 	/**
 	 * 设置自定义的对象类型提取器逻辑
 	 * 
 	 * @param te
 	 *            类型提取器
 	 */
 	public synchronized Castors setTypeExtractor(TypeExtractor te) {
 		extractor = te;
 		return this;
 	}
 
 	private Castors() {
 		setting = new DefaultCastorSetting();
		reload();
 	}
 
 	private void reload() {
 		HashMap<Class<?>, Method> settingMap = new HashMap<Class<?>, Method>();
 		if (null != setting) {
 			for (Method m1 : setting.getClass().getMethods()) {
 				Class<?>[] pts = m1.getParameterTypes();
 				if (pts.length == 1 && Castor.class.isAssignableFrom(pts[0])) {
 					settingMap.put(pts[0], m1);
 				}
 			}
 		}
 		// build castors
 		this.map = new HashMap<String, Map<String, Castor<?, ?>>>();
 		if (null == paths) {
 			paths = new ArrayList<Class<?>>();
 			paths.add(Array2Array.class);
 		}
 		for (Iterator<Class<?>> it = paths.iterator(); it.hasNext();) {
 			Class<?> baseClass = it.next();
 			if (baseClass == null)
 				continue;
 			List<Class<?>> list = Resources.scanClass(baseClass);
 			if (null == list)
 				continue;
 			for (Class<?> klass : list) {
 				try {
 					if (Modifier.isAbstract(klass.getModifiers()))
 						continue;
 					Castor<?, ?> castor = (Castor<?, ?>) klass.newInstance();
 					Map<String, Castor<?, ?>> map2 = this.map.get(castor.getFromClass().getName());
 					if (null == map2) {
 						map2 = new HashMap<String, Castor<?, ?>>();
 						this.map.put(castor.getFromClass().getName(), map2);
 					}
 					if (!map2.containsKey(castor.getToClass().getName())) {
 						Method m = settingMap.get(castor.getClass());
 						if (null == m) {
 							for (Iterator<Class<?>> mit = settingMap.keySet().iterator(); mit
 									.hasNext();) {
 								Class<?> cc = mit.next();
 								if (cc.isAssignableFrom(klass)) {
 									m = settingMap.get(cc);
 									break;
 								}
 							}
 						}
 						if (null != m)
 							m.invoke(setting, castor);
 						map2.put(castor.getToClass().getName(), castor);
 					}
 				} catch (Throwable e) {
 					System.err.println(String.format("Fail to create castor [%s] because: %s",
 							klass, e.getMessage()));
 				}
 			}
 		}
 	}
 
 	/**
 	 * First index is "from" (source) The second index is "to" (target)
 	 */
 	private Map<String, Map<String, Castor<?, ?>>> map;
 
 	/**
 	 * 转换一个 POJO 从一个指定的类型到另外的类型
 	 * 
 	 * @param src
 	 *            源对象
 	 * @param fromType
 	 *            源对象类型
 	 * @param toType
 	 *            目标类型
 	 * @param args
 	 *            转换时参数。有些 Castor 可能需要这个参数，比如 Array2Map
 	 * @return 目标对象
 	 * @throws FailToCastObjectException
 	 *             如果没有找到转换器，或者转换失败
 	 */
 	@SuppressWarnings("unchecked")
 	public <F, T> T cast(Object src, Class<F> fromType, Class<T> toType, String... args)
 			throws FailToCastObjectException {
 		if (null == src) {
 			if (toType.isPrimitive())
 				return cast(0, int.class, toType);
 			return null;
 		}
 		if (fromType == toType || toType == null || fromType == null)
 			return (T) src;
 		if (fromType.getName().equals(toType.getName()))
 			return (T) src;
 		if (toType.isAssignableFrom(fromType))
 			return (T) src;
 		Mirror<?> from = Mirror.me(fromType, extractor);
 		if (from.canCastToDirectly(toType)) // Use language built-in cases
 			return (T) src;
 		Mirror<T> to = Mirror.me(toType, extractor);
 
 		Castor c = null;
 		Class<?>[] fets = from.extractTypes();
 		Class<?>[] tets = to.extractTypes();
 		for (Class<?> ft : fets) {
 			Map<String, Castor<?, ?>> m2 = map.get(ft.getName());
 			if (null != m2)
 				for (Class<?> tt : tets) {
 					c = m2.get(tt.getName());
 					if (null != c)
 						break;
 				}
 			if (null != c)
 				break;
 		}
 		if (null == c)
 			throw new FailToCastObjectException(String.format(
 					"Can not find castor for '%s'=>'%s' because:\n%s", fromType.getName(), toType
 							.getName(), "Fail to find matched castor"));
 		try {
 			return (T) c.cast(src, toType, args);
 		} catch (FailToCastObjectException e) {
 			throw e;
 		} catch (Exception e) {
 			throw new FailToCastObjectException(String.format(
 					"Fail to cast from <%s> to <%s> for {%s} because:\n%s:%s", fromType.getName(),
 					toType.getName(), src, e.getClass().getSimpleName(), e.getMessage()));
 		}
 	}
 
 	/**
 	 * 转换一个 POJO 到另外的类型
 	 * 
 	 * @param src
 	 *            源对象
 	 * @param toType
 	 *            目标类型
 	 * @return 目标对象
 	 * @throws FailToCastObjectException
 	 *             如果没有找到转换器，或者转换失败
 	 */
 	public <T> T castTo(Object src, Class<T> toType) throws FailToCastObjectException {
 		return cast(src, null == src ? null : src.getClass(), toType);
 	}
 
 	/**
 	 * 将一个 POJO 转换成字符串
 	 * 
 	 * @param src
 	 *            源对象
 	 * @return 字符串
 	 */
 	public String castToString(Object src) {
 		try {
 			return castTo(src, String.class);
 		} catch (FailToCastObjectException e) {
 			return String.valueOf(src);
 		}
 	}
 }
