 /*
  *        JacORB - a free Java ORB
  *
  *   Copyright (C) 1997-98  Gerald Brose.
  *
  *   This library is free software; you can redistribute it and/or
  *   modify it under the terms of the GNU Library General Public
  *   License as published by the Free Software Foundation; either
  *   version 2 of the License, or (at your option) any later version.
  *
  *   This library is distributed in the hope that it will be useful,
  *   but WITHOUT ANY WARRANTY; without even the implied warranty of
  *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  *   Library General Public License for more details.
  *
  *   You should have received a copy of the GNU Library General Public
  *   License along with this library; if not, write to the Free
  *   Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
  */
 
 package org.jacorb.idl;
 
 /**
  * @author Gerald Brose
  */
 
 import java.util.Vector;
 import java.util.Enumeration;
 import java.io.*;
 
 public class ObjectTypeSpec 
 	extends TypeSpec
 	{
 
 	public ObjectTypeSpec(int num) 
 	{
 		super(num);
 	}
 
 	public Object clone()
 	{ 
 		return this;
 	}
 
 	public String typeName()
 	{
 		return "org.omg.CORBA.Object";
 	}
 
 	public String signature()
 	{
 		return "L" + typeName() + ";";
 	}
 
 	public TypeSpec typeSpec()
 	{
 		return this;
 	}
 
    public void setPackage( String s)
    {
        s = parser.pack_replace(s);
 	}
 
 	public boolean basic()
 	{
 		return true;
 	} 
 
 	public void set_constr(TypeDeclaration td)
 	{
 	}
 
 	public void parse() 
		 
 	{
 	}
 
 	public String toString()
 	{
 		return "org.omg.CORBA.Object";
 	}
 
 	/**
 	 * @returns a string for an expression of type TypeCode 
 	 * 			that describes this type
 	 */
 
	public String getTypeCodeExpression()
 	{
		return "org.omg.CORBA.ORB.init().create_interface_tc(\"IDL:omg.org/CORBA/Object:1.0\",\"org.omg.CORBA.Object\")";
 	}
 
 	public void print(PrintWriter ps)
 	{
 	}
 
 	public String holderName()
 	{
 		return typeName() + "Holder";
 	}
 
 	public String printReadExpression(String streamname)
 	{
 		return streamname + ".read_Object()";
 	}
 
 	public String printWriteStatement(String var_name, String streamname)
 	{
 		return streamname + ".write_Object("+ var_name + ");";
 	}
 
 }




















