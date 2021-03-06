 /**
  * 
  */
 package org.minnal.instrument.resource;
 
 import java.io.StringWriter;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 
 import javassist.ClassPool;
 import javassist.CtClass;
 import javassist.CtMethod;
 import javassist.CtNewMethod;
 
 import org.javalite.common.Inflector;
 import org.minnal.core.Request;
 import org.minnal.core.Response;
 import org.minnal.core.server.exception.NotFoundException;
 import org.minnal.instrument.entity.EntityNode;
 import org.minnal.instrument.entity.EntityNode.EntityNodePath;
 
 /**
  * @author ganeshs
  *
  */
 public abstract class MethodCreator {
 
 	private CtClass ctClass;
 	
 	private EntityNodePath path;
 	
 	protected static final String FIND_ENTITY_TEMPLATE = ":model_class :field_name = :model_class.first(new Object[]{\":entity_key\", " +
 			"request.getHeader(\":param_name\")}); if (:field_name == null) {throw new " + NotFoundException.class.getName() + 
 			"(\":field_name with :entity_key \" + request.getHeader(\":param_name\") + \" not found\");}";
 	
 	protected static final String FIND_COLLECTION_ITEM_TEMPLATE = ":model_class :field_name = :parent.collection(\":resource_name\").first(new Object[]{\":collection_entity_key\", " +
 			"request.getHeader(\":param_name\")}); if (:field_name == null) {throw new " + NotFoundException.class.getName() + 
 			"(\":field_name with :entity_key \" + request.getHeader(\":param_name\") + \" not found\");}";
 	
 	public MethodCreator(CtClass ctClass, EntityNodePath path) {
 		this.ctClass = ctClass;
 		this.path = path;
 	}
 	
 	/**
 	 * @return the path
 	 */
 	public EntityNodePath getPath() {
 		return path;
 	}
 
 	public void create() throws Exception {
 		createMethod(createBody());
 	}
 	
 	protected abstract String createBody();
 	
 	protected boolean returnVoid() {
 		return false;
 	}
 	
 	protected void createMethod(String body) throws Exception {
 		String methodName = getMethodName();
 		if (methodExists(methodName)) {
 			return;
 		}
 		if (ctClass.isFrozen()) {
 			ctClass.defrost();
 		}
 		StringWriter writer = new StringWriter();
 		writer.append("public ").append(returnVoid() ? "void " : "Object ").append(methodName).append("(").append(Request.class.getName()).append(" request, ");
 		writer.append(Response.class.getName()).append(" response) {").append(body).append("}");
 		CtMethod method = CtNewMethod.make(writer.toString(), ctClass);
 		ctClass.addMethod(method);
 	}
 	
 	public abstract String getMethodName();
 	
 	protected boolean methodExists(String methodName) {
 		try {
			ctClass.getDeclaredMethod(methodName, new CtClass[]{ClassPool.getDefault().get(Request.class.getName()), 
					ClassPool.getDefault().get(Request.class.getName())});
			return true;
 		} catch (javassist.NotFoundException e) {
 			return false;
 		}
 	}
 	
 	protected void resolveTemplate(StringWriter writer, String template, EntityNode node, String paramName, String parent) {
 		Map<String, String> placeholders = new HashMap<String, String>();
 		placeholders.put("model_class", node.getEntityMetaData().getEntityClass().getName());
 		placeholders.put("field_name", node.getName());
 		String resourceName = Inflector.camelize(node.getResourceName(), false);
 		placeholders.put("resource_name", resourceName);
 		placeholders.put("entity_key", node.getEntityMetaData().getEntityKey());
 		placeholders.put("collection_entity_key", resourceName + "." + node.getEntityMetaData().getEntityKey());
 		placeholders.put("param_name", paramName);
 		placeholders.put("parent", parent);
 		List<String> searchParams = getPath().getSearchParams();
 		StringWriter searchParamString = new StringWriter();
 		for (int i = 0; i < searchParams.size(); i++) {
 			if (i == 0) {
 				searchParamString.append("\"").append(searchParams.get(i)).append("\"");
 			} else {
 				searchParamString.append(", \"").append(searchParams.get(i)).append("\"");
 			}
 		}
 		if (! searchParams.isEmpty()) {
 			placeholders.put("search_params", "java.util.Arrays.asList(new String[]{" + searchParamString.toString() + "})");
 		} else {
 			placeholders.put("search_params", "new java.util.ArrayList()");
 		}
 		
 		for (Entry<String, String> entry : placeholders.entrySet()) {
 			template = template.replaceAll(":" + entry.getKey(), entry.getValue());
 		}
 		writer.append(template);
 	}
 }
