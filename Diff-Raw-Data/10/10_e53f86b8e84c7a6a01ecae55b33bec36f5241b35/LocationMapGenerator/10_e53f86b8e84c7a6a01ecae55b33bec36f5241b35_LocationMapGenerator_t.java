 package org.emftext.sdk.codegen.generators;
 
 import static org.emftext.sdk.codegen.generators.IClassNameConstants.ARRAY_LIST;
 import static org.emftext.sdk.codegen.generators.IClassNameConstants.BASIC_E_MAP;
 import static org.emftext.sdk.codegen.generators.IClassNameConstants.COLLECTIONS;
 import static org.emftext.sdk.codegen.generators.IClassNameConstants.COMPARATOR;
 import static org.emftext.sdk.codegen.generators.IClassNameConstants.E_MAP;
 import static org.emftext.sdk.codegen.generators.IClassNameConstants.E_OBJECT;
 import static org.emftext.sdk.codegen.generators.IClassNameConstants.LIST;
 
 import java.io.PrintWriter;
 
 import org.emftext.sdk.codegen.EArtifact;
 import org.emftext.sdk.codegen.GenerationContext;
 import org.emftext.sdk.codegen.IGenerator;
 
 public class LocationMapGenerator extends BaseGenerator {
 
 	public LocationMapGenerator() {
 		super();
 	}
 
 	private LocationMapGenerator(GenerationContext context) {
 		super(context, EArtifact.LOCATION_MAP);
 	}
 
 	public boolean generate(PrintWriter out) {
 		org.emftext.sdk.codegen.composites.StringComposite sc = new org.emftext.sdk.codegen.composites.JavaComposite();
 		sc.add("package " + getResourcePackageName() + ";");
 		sc.addLineBreak();
 		sc.add("// A basic implementation of the ILocationMap interface. Instances");
 		sc.add("// store information about element locations using four maps.");
 		sc.add("// <p>");
 		sc.add("// The set-methods can be called multiple times by the parser that may visit");
 		sc.add("// multiple children from which it copies the localization information for the parent");
 		sc.add("// (i.e., the element for which set-method is called)");
 		sc.add("// It implements the following behavior:");
 		sc.add("// <p>");
 		sc.add("// Line:   The lowest of all sources is used for target<br>");
 		sc.add("// Column: The lowest of all sources is used for target<br>");
 		sc.add("// Start:  The lowest of all sources is used for target<br>");
 		sc.add("// End:    The highest of all sources is used for target<br>");
 		sc.add("//");
 		sc.add("public class " + getResourceClassName() + " implements " + getClassNameHelper().getI_LOCATION_MAP() + " {");
 		sc.addLineBreak();
 		sc.add("// A basic interface that can be implemented to select");
 		sc.add("// EObjects based of their location in a text resource.");
 		sc.add("public interface ISelector {");
 		sc.add("boolean accept(int startOffset, int endOffset);");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("protected " + E_MAP + "<" + E_OBJECT + ", Integer> columnMap = new " + BASIC_E_MAP + "<" + E_OBJECT + ", Integer>();");
 		sc.add("protected " + E_MAP + "<" + E_OBJECT + ", Integer> lineMap = new " + BASIC_E_MAP + "<" + E_OBJECT + ", Integer>();");
 		sc.add("protected " + E_MAP + "<" + E_OBJECT + ", Integer> charStartMap = new " + BASIC_E_MAP + "<" + E_OBJECT + ", Integer>();");
 		sc.add("protected " + E_MAP + "<" + E_OBJECT + ", Integer> charEndMap = new " + BASIC_E_MAP + "<" + E_OBJECT + ", Integer>();");
 		sc.addLineBreak();
 		
 		sc.add("public void setLine(" + E_OBJECT + " element, int line) {");
 		sc.add("setMapValueToMin(lineMap, element, line);");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("public int getLine(" + E_OBJECT + " element) {");
 		sc.add("return getMapValue(lineMap, element);");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("public void setColumn(" + E_OBJECT + " element, int column) {");
 		sc.add("setMapValueToMin(columnMap, element, column);");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("public int getColumn(" + E_OBJECT + " element) {");
 		sc.add("return getMapValue(columnMap, element);");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("public void setCharStart(" + E_OBJECT + " element, int charStart) {");
 		sc.add("setMapValueToMin(charStartMap, element, charStart);");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("public int getCharStart(" + E_OBJECT + " element) {");
 		sc.add("return getMapValue(charStartMap, element);");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("public void setCharEnd(" + E_OBJECT + " element, int charEnd) {");
 		sc.add("setMapValueToMax(charEndMap, element, charEnd);");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("public int getCharEnd(" + E_OBJECT + " element) {");
 		sc.add("return getMapValue(charEndMap, element);");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("private int getMapValue(" + E_MAP + "<" + E_OBJECT + ", Integer> map, " + E_OBJECT + " element) {");
 		sc.add("if (!map.containsKey(element)) return -1;");
 		sc.add("return map.get(element);");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("private void setMapValueToMin(" + E_MAP + "<" + E_OBJECT + ", Integer> map, " + E_OBJECT + " element, int value) {");
		sc.add("// we need to synchronize the write access, because other threads may iterate");
 		sc.add("// over the map concurrently");
 		sc.add("synchronized (this) {");
 		sc.add("if (element == null || value < 0) return;");
 		sc.add("if (map.containsKey(element) && map.get(element) < value) return;");
 		sc.add("map.put(element, value);");
 		sc.add("}");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("private void setMapValueToMax(" + E_MAP + "<" + E_OBJECT + ", Integer> map, " + E_OBJECT + " element, int value) {");
		sc.add("// we need to synchronize the write access, because other threads may iterate");
 		sc.add("// over the map concurrently");
 		sc.add("synchronized (this) {");
 		sc.add("if (element == null || value < 0) return;");
 		sc.add("if (map.containsKey(element) && map.get(element) > value) return;");
 		sc.add("map.put(element, value);");
 		sc.add("}");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("public " + LIST + "<" + E_OBJECT + "> getElementsAt(final int documentOffset) {");
 		sc.add(LIST + "<" + E_OBJECT + "> result = getElements(new ISelector() {");
 		sc.add("public boolean accept(int start, int end) {");
 		sc.add("return start <= documentOffset && end >= documentOffset;");
 		sc.add("}");
 		sc.add("});");
 		sc.add("return result;");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("public " + LIST + "<" + E_OBJECT + "> getElementsBetween(final int startOffset, final int endOffset) {");
 		sc.add("" + LIST + "<" + E_OBJECT + "> result = getElements(new ISelector() {");
 		sc.add("public boolean accept(int start, int end) {");
 		sc.add("return start >= startOffset && end <= endOffset;");
 		sc.add("}");
 		sc.add("});");
 		sc.add("return result;");
 		sc.add("}");
 		sc.addLineBreak();
 		sc.add("private " + LIST + "<" + E_OBJECT + "> getElements(ISelector s) {");
 		sc.add("// there might be more than one element at the given offset");
 		sc.add("// thus, we collect all of them and sort them afterwards");
 		sc.add(LIST + "<" + E_OBJECT + "> result = new " + ARRAY_LIST + "<" + E_OBJECT + ">();");
 		sc.addLineBreak();
		sc.add("// we need to synchronize the iteration over the map, because");
		sc.add("// other threads may write to the map concurrently");
 		sc.add("synchronized (this) {");
 		sc.add("for (" + E_OBJECT + " next : charStartMap.keySet()) {");
 		sc.add("int start = charStartMap.get(next);");
 		sc.add("int end = charEndMap.get(next);");
 		sc.add("if (s.accept(start, end)) {");
 		sc.add("result.add(next);");
 		sc.add("}");
 		sc.add("}");
 		sc.add("}");
 		sc.add(COLLECTIONS + ".sort(result, new " + COMPARATOR + "<" + E_OBJECT + ">() {");
 		sc.add("public int compare(" + E_OBJECT + " objectA, " + E_OBJECT + " objectB) {");
 		sc.add("int lengthA = getCharEnd(objectA) - getCharStart(objectA);");
 		sc.add("int lengthB = getCharEnd(objectB) - getCharStart(objectB);");
 		sc.add("return lengthA - lengthB;");
 		sc.add("}");
 		sc.add("});");
 		sc.add("return result;");
 		sc.add("}");
 		sc.add("}");
 		out.print(sc.toString());
 		return true;
 	}
 
 	public IGenerator newInstance(GenerationContext context) {
 		return new LocationMapGenerator(context);
 	}
 }
