 package com.xebia.smok.xml.domain;
 
 import java.util.ArrayList;
 import java.util.List;
 
 public class Smok {
 
 	private String className;
 	private String classPackageName;
	private List<String> methods;
 
 	public Smok() {
 		methods = new ArrayList<String>();
 	}
 
 	public String getClassName() {
 		return className;
 	}
 
 	public List<String> getMethods() {
 		return methods;
 	}
 
 	public void setClassName(String className) {
 		this.className = className;
 	}
 
 	public void setClassPackageName(String classPackageName) {
 		this.classPackageName = classPackageName;
 	}
 
 	public String getClassFQCN() {
 		return this.classPackageName + "." + this.className;
 	}
 
 	public String getClassPackageName() {
 		return this.classPackageName;
 	}
 
 	public void setMethods(List<String> methods) {
 		this.methods.addAll(methods);
 	}
 
 	public boolean onlyForClass() {
		return methods == null || methods.size() == 0;
 	}
 
 	@Override
 	public String toString() {
 		return "Smok [className=" + className + ", methods=" + methods + "]";
 	}
 
 }
