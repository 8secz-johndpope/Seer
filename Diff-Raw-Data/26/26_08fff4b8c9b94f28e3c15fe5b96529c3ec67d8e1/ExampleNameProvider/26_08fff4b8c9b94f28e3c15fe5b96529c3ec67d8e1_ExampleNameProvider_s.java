 package de.bmw.carit.jnario.spec.naming;
 
 import static com.google.common.collect.Iterables.filter;
 import static de.bmw.carit.jnario.spec.util.Strings.convertToCamelCase;
 import static org.eclipse.xtext.EcoreUtil2.getContainerOfType;
 import static org.eclipse.xtext.util.Strings.toFirstLower;
 import static org.eclipse.xtext.util.Strings.toFirstUpper;
 
 import java.util.List;
 
 import org.eclipse.xtext.util.SimpleAttributeResolver;
 import org.eclipse.xtext.xtend2.xtend2.XtendMember;
 
 import de.bmw.carit.jnario.spec.spec.After;
 import de.bmw.carit.jnario.spec.spec.Before;
 import de.bmw.carit.jnario.spec.spec.Example;
 import de.bmw.carit.jnario.spec.spec.ExampleGroup;
 import de.bmw.carit.jnario.spec.spec.TestFunction;
 import de.bmw.carit.jnario.spec.spec.util.SpecSwitch;
 
 public class ExampleNameProvider {
 
 	private OperationNameProvider operationNameProvider = new OperationNameProvider();
 
 	public String describe(ExampleGroup exampleGroup) {
 		StringBuilder result = new StringBuilder();
 		if(exampleGroup.getTargetType() != null){
 			result.append(exampleGroup.getTargetType().getSimpleName());
 			result.append(" ");
 		}
 		if(exampleGroup.getTargetOperation() != null){
 			result.append(new OperationNameProvider().apply(exampleGroup.getTargetOperation()));
 			result.append(" ");
 		}
 		if(exampleGroup.getName() != null){
 			result.append(exampleGroup.getName());
 		}
		return result.toString().replace("(", "[").replace(")", "]").replace("#", "").trim();
 	}
 	
 	public String describe(Example example){
 		StringBuilder sb = new StringBuilder();
 		if(example.getException() != null){
 			sb.append("throws ");
 			sb.append(example.getException().getSimpleName());
 			sb.append(" ");
 		}
 		if(example.getName() != null){
 			sb.append(example.getName());
 		}
		return sb.toString().trim();
 	}
 
 	public String toJavaClassName(ExampleGroup exampleGroup) {
 		StringBuilder result = internalGetName(exampleGroup);
 		result.append("Spec");
 		return result.toString();
 	}
 
 	public String toMethodName(TestFunction function){
 		return new SpecSwitch<String>(){
 			public String caseAfter(After object) {
 				return toMethodName(object);
 			};
 			public String caseBefore(Before object) {
 				return toMethodName(object);
 			};
 			public String caseExample(Example object) {
 				return toMethodName(object);
 			};
 		}.doSwitch(function);
 	}
 	
 	public String toMethodName(Example example){
 		StringBuilder result = new StringBuilder();
 		appendMemberDescription(example, result);
 		return toFirstLower(convertToCamelCase(result).toString());
 	}
 	
 	public String toMethodName(Before before){
 		return toMethodName(before, "before");
 	}
 	
 	public String toMethodName(After before){
 		return toMethodName(before, "after");
 	}
 	
 	public String toMethodName(TestFunction target, String defaultName){
 		if(target.getName() != null){
 			return toFirstLower(convertToCamelCase(target.getName()));
 		}
 		int count = countPreviousWithDefaultName(target);
 		if(count > 1){
 			defaultName += count;
 		}
 		return defaultName;
 	}
 
 	protected int countPreviousWithDefaultName(TestFunction target) {
 		List<XtendMember> members = ((ExampleGroup)target.eContainer()).getMembers();
 		int index = members.indexOf(target);
 		int count = 1;
 		for (int i = 0; i < index; i++) {
 			XtendMember current = members.get(i);
 			if (target.getClass().isInstance(current)) {
 				if(((TestFunction)current).getName() == null){
 					count++;
 				}
 			}
 		}
 		return count;
 	}
 	
 	protected StringBuilder internalGetName(ExampleGroup exampleGroup) {
 		StringBuilder result = new StringBuilder();
 		ExampleGroup parent = getContainerOfType(exampleGroup.eContainer(), ExampleGroup.class);
 		if(parent != null){
 			result.append(internalGetName(parent));
 		}
 		if(exampleGroup.getTargetType() != null){
 			result.append(exampleGroup.getTargetType().getSimpleName());
 		}
 		if(exampleGroup.getTargetOperation() != null){
 			String operationName = operationNameProvider.apply(exampleGroup.getTargetOperation()).toString();
 			result.append(toFirstUpper(operationName));
 		}
 		appendMemberDescription(exampleGroup, result);
 		result = convertToCamelCase(result);
 		return result;
 	}
 	
 	private void appendMemberDescription(XtendMember member, StringBuilder result) {
 		String newName = SimpleAttributeResolver.NAME_RESOLVER.apply(member);
 		if(newName != null){
 			result.append(newName);
 		}
 	}
 
 }
