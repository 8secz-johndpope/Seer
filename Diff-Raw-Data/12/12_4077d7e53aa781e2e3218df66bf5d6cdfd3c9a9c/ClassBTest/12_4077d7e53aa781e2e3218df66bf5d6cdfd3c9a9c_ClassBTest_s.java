 /***
  * Copyright 2013 Teoti Graphix, LLC.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  * 
  * 
  * @author Michael Schmalle <mschmalle@teotigraphix.com>
  */
 
 package functional.tests;
 
 import org.apache.flex.compiler.tree.as.IFunctionNode;
 import org.apache.flex.compiler.tree.as.IGetterNode;
 import org.apache.flex.compiler.tree.as.ISetterNode;
 import org.junit.Assert;
 import org.junit.Test;
 
 import randori.compiler.internal.codegen.as.ASEmitter;
 
 public class ClassBTest extends FunctionalTestBase
 {
     @Test
     public void test_constructor()
     {
         IFunctionNode node = findFunction("ClassB", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB = function(param1, param2, param3) {"
                 + "\n\tthis.modela = null;\n\tthis.names = null;\n\tthis.thenContracts"
                 + " = null;\n\tthis.field3 = null;\n\tthis.field2 = 42;\n\tthis.field1"
                 + " = \"Hello\";\n\tthis.j = null;\n\tdemo.foo.ClassA.call(this, param1);"
                 + "\n\tthis.field2 = param2;\n}");
     }
 
     @Test
     public void test_method1()
     {
         IFunctionNode node = findFunction("method1", classNode);
         visitor.visitFunction(node);
         //assertOut("");
     }
 
     @Test
     public void test_while_binary_getter()
     {
         IFunctionNode node = findFunction("while_binary_getter", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.while_binary_getter = function() {\n\t"
                 + "while (this.thenContracts.length > 0) {\n\t}\n}");
     }
 
     @Test
     public void this_no_parent()
     {
         IFunctionNode node = findFunction("this_no_parent", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.this_no_parent = function() {\n\tvar foo = this;\n}");
     }
 
     @Test
     public void for_each_statement()
     {
         IFunctionNode node = findFunction("for_each_statement", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.for_each_statement = function() {"
                 + "\n\tvar $1;\n\tfor (var $0 in ($1 = this.thenContracts)) {\n\t}\n}");
     }
 
     @Test
     public void for_with_vector_length()
     {
         IFunctionNode node = findFunction("for_with_vector_length", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.for_with_vector_length = function() {"
                 + "\n\tvar viewPoints = [];\n\tfor (var i = 0; i < viewPoints.length; i++) {\n\t}\n}");
     }
 
     @Test
     public void as_cast()
     {
         IFunctionNode node = findFunction("as_cast", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.as_cast = function() {\n\tvar a;\n\tvar b = a;\n}");
     }
 
     @Test
     public void is_type_check()
     {
         IFunctionNode node = findFunction("is_type_check", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.is_type_check = function() {\n\t"
                 + "if (this.field1 instanceof Array) {\n\t}\n}");
     }
 
     @Test
     public void undefined_literal()
     {
         IFunctionNode node = findFunction("undefined_literal", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.undefined_literal = function() {"
                 + "\n\tif (this.field1 != undefined) {\n\t}\n}");
     }
 
     @Test
     public void static_var_qualified()
     {
         IFunctionNode node = findFunction("static_var_qualified", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.static_var_qualified = function() {"
                 + "\n\tdemo.foo.ClassB.FOO[this.get_foo()] = \"bar\";\n}");
     }
 
     @Test
     public void static_var_qualified_prepend_Type()
     {
         IFunctionNode node = findFunction("static_var_qualified_prepend_Type",
                 classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.static_var_qualified_prepend_Type = function() {"
                 + "\n\tdemo.foo.ClassB.FOO[this.get_foo()] = \"bar\";\n}");
     }
 
     @Test
     public void FileReader_get()
     {
         IGetterNode node = findGetter("FileReader_", classNode);
         visitor.visitGetter(node);
         assertOut("demo.foo.ClassB.get_FileReader = function() {\n\treturn null;\n}");
     }
 
     @Test
     public void FileReader_set()
     {
         ISetterNode node = findSetter("FileReader_", classNode);
         visitor.visitSetter(node);
         assertOut("demo.foo.ClassB.set_FileReader = function(value) {\n}");
     }
 
     @Test
     public void FileReader_use_get_set_rename()
     {
         IFunctionNode node = findFunction("FileReader_use_get_set_rename",
                 classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.FileReader_use_get_set_rename = function() {"
                 + "\n\tset_FileReader(new FileReader());\n\t"
                 + "var a = demo.foo.ClassB.get_FileReader();\n}");
     }
 
     @Test
     public void method_call_rename()
     {
         IFunctionNode node = findFunction("method_call_rename", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.method_call_rename = function() {\n\tthis.j.show();\n}");
     }
 
     @Test
     public void method_annotation_rename()
     {
         IFunctionNode node = findFunction("method_annotation_rename", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.fooBar = function() {\n}");
     }
 
     @Test
     public void default_parameters()
     {
         IFunctionNode node = findFunction("default_parameters", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.default_parameters = "
                 + "function(foo, bar, baz) {\n\tvar a = baz;\n}");
     }
 
     @Test
     public void window_static_method()
     {
         IFunctionNode node = findFunction("window_static_method", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.window_static_method = function() {\n\talert(\"foo\");\n}");
     }
 
     @Test
     public void window_static_accessor()
     {
         IFunctionNode node = findFunction("window_static_accessor", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.window_static_accessor = function() {\n\t"
                 + "if (console != null)\n\t\tconsole.log(\"foo\");\n}");
     }
 
     @Test
     public void member_accessor_get_and_set()
     {
         IFunctionNode node = findFunction("member_accessor_get_and_set",
                 classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.member_accessor_get_and_set = function() {"
                 + "\n\tvar c;\n\tc.get_foo().bar(42);\n}");
     }
 
     @Test
     public void window_reduction()
     {
         IFunctionNode node = findFunction("window_reduction", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.window_reduction = function() {\n\tvar nextLevel = window;\n}");
     }
 
     @Test
     public void this_get_accessor_explicit()
     {
         IFunctionNode node = findFunction("this_get_accessor_explicit",
                 classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.this_get_accessor_explicit = function() {"
                 + "\n\tvar c;\n\tc = this.get_accessor1();\n}");
     }
 
     @Test
     public void this_get_accessor_anytype_access()
     {
         IFunctionNode node = findFunction("this_get_accessor_anytype_access",
                 classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.this_get_accessor_anytype_access = function() {"
                 + "\n\tif (this.get_type().injectionPoints == null) {\n\t}\n}");
     }
 
     @Test
     public void new_anytype()
     {
         IFunctionNode node = findFunction("new_anytype", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.new_anytype = function() {\n\tvar "
                 + "instance = null;\n\tif (false)\n\t\tinstance = new (this.get_type())();\n}");
     }
 
     @Test
     public void JQueryStatic_J()
     {
         // JQueryStatic.J transformed to JQuery
         IFunctionNode node = findFunction("JQueryStatic_J", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.JQueryStatic_J = function() {"
                 + "\n\tthis.get_foo().injectPotentialNode(demo.foo.ClassB.FOO, "
                 + "jQuery(this.field1));\n}");
     }
 
     @Test
     public void anonymous_function_argument_delegate()
     {
         IFunctionNode node = findFunction(
                 "anonymous_function_argument_delegate", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.anonymous_function_argument_delegate = "
                 + "function() {\n\tsetTimeout(function() {\n\t\tthis.method1("
                 + "this.get_foo());\n\t}, 1);\n}");
     }
 
     @Test
     public void anonymous_function_field_delegate()
     {
         IFunctionNode node = findFunction("anonymous_function_field_delegate",
                 classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.anonymous_function_field_delegate = function() "
                 + "{\n\tdemo.foo.ClassB.onSelectHandler = function(event) {"
                 + "\n\t\tthis.set_foo(\"Hello\");\n\t};\n}");
     }
 
     @Test
     public void jscontext()
     {
         IFunctionNode node = findFunction("jscontext", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.jscontext = function() {"
                 + "\nfoo();\r\nvar i = \"was included\";\n}");
     }
 
     @Test
     public void conditional_lhs_getter()
     {
         IFunctionNode node = findFunction("conditional_lhs_getter", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.conditional_lhs_getter = function() {"
                 + "\n\tif ((this.get_data() == null) || (this.get_data().length == 0)) {"
                 + "\n\t\treturn;\n\t}\n}");
     }
 
     @Test
     public void functioncall_getter_invoke()
     {
         IFunctionNode node = findFunction("functioncall_getter_invoke",
                 classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.functioncall_getter_invoke = function() {"
                 + "\n\tvar a;\n\ta = this.get_renderFunction()(this.j, this.get_data()[this.j]);\n}");
     }
 
     @Test
     public void const_string()
     {
         IFunctionNode node = findFunction("const_string", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.const_string = function() {"
                 + "\n\tconst RANDORI_VENDOR_ITEM_EXPRESSION = \";\";\n\tvar "
                 + "anyVendorItems = new RegExp(RANDORI_VENDOR_ITEM_EXPRESSION, \"g\")"
                 + ";\n\tthis.get_foo()(0);\n}");
     }
 
     @Test
     public void transform_rest_to_arguments()
     {
         IFunctionNode node = findFunction("transform_rest_to_arguments",
                 classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.transform_rest_to_arguments = function(args) {"
                 + "\n\tvar listener;\n\twhile (this.thenContracts.length > 0) {\n\t\t"
                 + "listener = this.thenContracts.pop();\n\t\tlistener.apply(this, arguments);\n\t}\n}");
     }
 
     @Test
     public void new_HTMLBRElement()
     {
         IFunctionNode node = findFunction("new_HTMLBRElement", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.new_HTMLBRElement = function() {\n\t"
                 + "var breakIt = document.createElement('br');\n\tbreakIt.onchange = "
                 + "function() {\n\t\tconsole.log(\"We did it!\");\n\t};\n}");
     }
 
     //@Test
     public void proto_default_parameter_arg_replacement()
     {
         IFunctionNode node = findFunction(
                 "proto_default_parameter_arg_replacement", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.new_HTMLBRElement = function() {"
                 + "\n\tvar breakIt = document.createElement('br');\n\tbreakIt.onchange = "
                 + "$createAnonDelegate(this, function() {\n\t\tconsole.log(\"We did it!\");\n\t});\n}");
     }
 
     @Test
     public void getter_in_return()
     {
         IFunctionNode node = findFunction("getter_in_return", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.getter_in_return = function() {"
                 + "\n\treturn this.get_data();\n}");
     }
 
     @Test
     public void static_delegate()
     {
         IFunctionNode node = findFunction("static_delegate", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.static_delegate = function() {"
                 + "\n\tvar something;\n\tsomething($createStaticDelegate(this, handleMe));\n}");
     }
 
     @Test
     public void remove_type_cast()
     {
         IFunctionNode node = findFunction("remove_type_cast", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.remove_type_cast = function() {"
                 + "\n\tvar a = this.get_foo();\n}");
     }
 
     @Test
     public void simple_get_assign()
     {
         IFunctionNode node = findFunction("simple_get_assign", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.simple_get_assign = function() {"
                 + "\n\tvar a = this.get_foo();\n}");
     }
 
     @Test
     public void json_with_default_params()
     {
         IFunctionNode node = findFunction("json_with_default_params", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.json_with_default_params = function() {"
                 + "\n\tvar menuItems = [{name:\"Targets\", url:\"views\\/targets.html\","
                 + " isRedirect:true, param4:\"bah\", param5:42}, {name:\"Labs\", "
                 + "url:\"views\\/labs.html\", isRedirect:false, param4:\"bar\", param5:142}, "
                 + "{name:\"Intel\", url:\"views\\/intel.html\", isRedirect:true, "
                 + "param4:\"bah\", param5:42}];\n}");
     }
 
     @Test
     public void private_constructor_invoke()
     {
         IFunctionNode node = findFunction("private_constructor_invoke",
                 classNode);
         visitor.visitFunction(node);
         Assert.assertEquals(1, ((ASEmitter) emitter).getProblems().size());
     }
 
     @Test
     public void local_array_literal()
     {
         IFunctionNode node = findFunction("local_array_literal", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.local_array_literal = function() {"
                 + "\n\tvar o1 = {name:\"Mike\"};\n\tvar o2 = {name:\"Roland\"};\n\t"
                 + "var ar = [o1, o2];\n\tthis.names.set_data(ar);\n}");
     }
 
     @Test
     public void global_static()
     {
         IFunctionNode node = findFunction("global_static", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.global_static = function() {\n\tfillConsoleForIE();\n}");
     }
 
     @Test
     public void global_static_qualified()
     {
         IFunctionNode node = findFunction("global_static_qualified", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.global_static_qualified = function() {\n\tfillConsoleForIE();\n}");
     }
 
     @Test
     public void getter_in_while()
     {
         IFunctionNode node = findFunction("getter_in_while", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.getter_in_while = function() {"
                 + "\n\tvar viewStack;\n\twhile (viewStack.get_currentViewUrl() != null) {"
                 + "\n\t\tviewStack.popView();\n\t}\n}");
     }
 
     @Test
     public void constant_export_true()
     {
         IFunctionNode node = findFunction("constant_export_true", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.constant_export_true = function() {"
                 + "\n\tvar mapOptions;\n\tmapOptions.mapTypeId = "
                 + "demo.foo.support.MapTypeIdExportTrue.ROADMAP;\n}");
     }
 
     @Test
     public void constant_export_false()
     {
         IFunctionNode node = findFunction("constant_export_false", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.constant_export_false = function() {"
                 + "\n\tvar mapOptions;\n\tmapOptions.mapTypeId = \"roadmap\";\n}");
     }
 
     @Test
     public void non_resolving_identifier_masks_member()
     {
         IFunctionNode node = findFunction(
                 "non_resolving_identifier_masks_member", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.non_resolving_identifier_masks_member = "
                 + "function() {\n\tvar newMap;\n\tvar o = {position:42, field1:newMap};\n}");
     }
 
     @Test
     public void named_inner_function()
     {
         IFunctionNode node = findFunction("named_inner_function", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.named_inner_function = function() {"
                 + "\n\tfunction showMe(counter) {\n\t\tconsole.log(\"this is my counter\" "
                 + "+ counter);\n\t};\n\tfor (var i = 0; i < 5; i++) {\n\t\tshowMe(i);\n\t}\n}");
     }
 
     @Test
     public void delegate_field()
     {
         IFunctionNode node = findFunction("delegate_field", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.delegate_field = function() {"
                 + "\n\tthis.get_renderFunction()(demo.foo.ClassB.FOO, "
                 + "$createStaticDelegate(this, this.method1));\n}");
     }
 
     @Test
     public void delegate_composite_field()
     {
         IFunctionNode node = findFunction("delegate_composite_field", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.delegate_composite_field = function() {"
                 + "\n\tvar v;\n\tthis.get_renderFunction()(demo.foo.ClassB.FOO, "
                 + "$createStaticDelegate(v, v.get_renderFunction()));\n}");
     }
 
     @Test
     public void delegate_bug1()
     {
         IFunctionNode node = findFunction("delegate_bug1", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.delegate_bug1 = function() {"
                 + "\n\tvar promise;\n\tpromise.then(function(mediator) {\n\t\t"
                 + "console.log(\"got it\");\n\t});\n}");
     }
 
     @Test
     public void static_export_false_rename()
     {
         IFunctionNode node = findFunction("static_export_false_rename",
                 classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.static_export_false_rename = function() "
                 + "{\n\tmy.name.is.Mike.foo();\n}");
     }
 
     @Test
     public void static_qualified_export_false_rename()
     {
         IFunctionNode node = findFunction(
                 "static_qualified_export_false_rename", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.static_qualified_export_false_rename = function() "
                 + "{\n\tmy.name.is.Mike.foo();\n}");
     }
 
     @Test
     public void parseInt_radix()
     {
         IFunctionNode node = findFunction("parseInt_radix", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.parseInt_radix = function() "
                 + "{\n\tparseInt(\"42\", 0);\n\tparseInt(\"42\", 10);\n}");
     }
 
     @Test
     public void for_each_impl()
     {
         IFunctionNode node = findFunction("for_each_impl", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.for_each_impl = function() {\n\tvar x = "
                 + "[\"e\", \"f\"];\n\tvar $1;\n\tfor (var $0 in ($1 = x)) {\n\t\tvar s = "
                 + "$1[$0];\n\t\tconsole.log(s)\n\t}\n}");
     }
 
     @Test
     public void regexp_literal()
     {
         IFunctionNode node = findFunction("regexp_literal", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.regexp_literal = function() {"
                 + "\n\tvar fooPattern = /bar/ms;\n\tvar barPattern = /bar/;\n}");
     }
 
     @Test
     public void constant_private()
     {
         IFunctionNode node = findFunction("constant_private", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.constant_private = function() {"
                 + "\n\tvar margin;\n\tthis.field2 = 500 - margin.left - margin.right;"
                 + "\n\tthis.field2 = 300 - margin.top - margin.bottom;\n}");
     }
 
     @Test
     public void getter_bug()
     {
         IFunctionNode node = findFunction("getter_bug", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.getter_bug = function() {"
                 + "\n\tthis.modela.get_menuItem().isRedirect = false;\n}");
     }
 
     @Test
     public void compound_assignment()
     {
         IFunctionNode node = findFunction("compound_assignment", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.compound_assignment = function() {\n\t"
                 + "this.set_foo(this.get_foo() << 42));\n\t"
                 + "this.set_foo(this.get_foo() >> 42));\n\t"
                 + "this.set_foo(this.get_foo() >>> 42));\n\t"
                 + "this.set_foo(this.get_foo() * 42));\n\t"
                 + "this.set_foo(this.get_foo() / 42));\n\t"
                 + "this.set_foo(this.get_foo() % 42));\n\t"
                 + "this.set_foo(this.get_foo() & 42));\n\t"
                 + "this.set_foo(this.get_foo() ^ 42));\n\t"
                 + "this.set_foo(this.get_foo() | 42));\n\t"
                 + "this.set_foo(this.get_foo() + 42));\n\t"
                 + "this.set_foo(this.get_foo() - 42));\n\t"
                 + "this.set_foo(this.get_foo() && 42));\n}");
     }
 
     @Test
     public void explicit_this_rhs()
     {
         IFunctionNode node = findFunction("explicit_this_rhs", classNode);
         visitor.visitFunction(node);
         assertOut("demo.foo.ClassB.prototype.explicit_this_rhs = function() {"
                 + "\n\tvar field3 = this.field3;\n\tvar accessor1 = this.get_accessor1();"
                 + "\n\tvar field1 = this.field1;\n}");
     }
 
     @Test
     public void super_accessor_call()
     {
         IFunctionNode node = findFunction("goo", classNode);
         visitor.visitFunction(node);
        assertOut("demo.foo.ClassB.prototype.goo = function(value) {\n\tdemo.foo"
                 + ".ClassA.prototype.set_goo(this, value);\n\tvar o = demo.foo."
                 + "ClassA.prototype.get_goo(this);\n\tdemo.foo.ClassA.prototype."
                 + "set_goo(this, demo.foo.ClassA.prototype.get_goo(this));\n}");
     }
 
     @Test
     public void test_file()
     {
         visitor.visitFile(fileNode);
         //assertOut("");
     }
 
     @Override
     protected String getTypeUnderTest()
     {
         return "demo.foo.ClassB";
     }
 }
