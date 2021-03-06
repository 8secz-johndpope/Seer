 /**
  *    Copyright (c) 2008. Adobe Systems Incorporated.
  *    All rights reserved.
  *
  *    Redistribution and use in source and binary forms, with or without
  *    modification, are permitted provided that the following conditions
  *    are met:
  *
  *      * Redistributions of source code must retain the above copyright
  *        notice, this list of conditions and the following disclaimer.
  *      * Redistributions in binary form must reproduce the above copyright
  *        notice, this list of conditions and the following disclaimer in
  *        the documentation and/or other materials provided with the
  *        distribution.
  *      * Neither the name of Adobe Systems Incorporated nor the names of
  *        its contributors may be used to endorse or promote products derived
  *        from this software without specific prior written permission.
  *
  *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  *    "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  *    LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package com.adobe.ac.pmd.nodes.utils;
 
 import com.adobe.ac.pmd.parser.IParserNode;
 import com.adobe.ac.pmd.parser.NodeKind;
 
 final public class ClassUtils
 {
    public static IParserNode getClassExtension( final IParserNode classNode )
    {
       IParserNode content = null;
 
       if ( classNode != null
             && classNode.getChildren() != null )
       {
          for ( final IParserNode child : classNode.getChildren() )
          {
            if ( NodeKind.EXTENDS.equals( child.getId() ) )
             {
                content = child;
                break;
             }
          }
       }
       return content;
    }
 
    public static String getClassNameFromClassNode( final IParserNode classNode )
    {
       return classNode.getChild( 0 ).getStringValue();
    }
 
    public static IParserNode getTypeFromFieldDeclaration( final IParserNode fieldNode )
    {
       IParserNode typeNode = null;
 
       for ( final IParserNode node : fieldNode.getChildren() )
       {
          if ( node.is( NodeKind.NAME_TYPE_INIT )
                && node.numChildren() > 1 )
          {
             typeNode = node.getChild( 1 );
             break;
          }
       }
       return typeNode;
    }
 
    private ClassUtils()
    {
    }
 }
