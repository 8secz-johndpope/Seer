 // Copyright 2004, 2005 The Apache Software Foundation
 //
 // Licensed under the Apache License, Version 2.0 (the "License");
 // you may not use this file except in compliance with the License.
 // You may obtain a copy of the License at
 //
 //     http://www.apache.org/licenses/LICENSE-2.0
 //
 // Unless required by applicable law or agreed to in writing, software
 // distributed under the License is distributed on an "AS IS" BASIS,
 // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 // See the License for the specific language governing permissions and
 // limitations under the License.
 
 package org.apache.tapestry.binding;
 
 import ognl.Node;
 import ognl.enhance.ExpressionAccessor;
 import org.apache.hivemind.Location;
 import org.apache.tapestry.BindingException;
 import org.apache.tapestry.IComponent;
 import org.apache.tapestry.coerce.ValueConverter;
 import org.apache.tapestry.services.ExpressionCache;
 import org.apache.tapestry.services.ExpressionEvaluator;
 
 /**
  * Implements a dynamic binding, based on evaluating an expression using an expression language.
  * Tapestry's default expression language is the <a href="http://www.ognl.org/">Object Graph
  * Navigation Language </a>.
  *
  * @see org.apache.tapestry.services.ExpressionEvaluator
  * @author Howard Lewis Ship
  * @since 2.2
  */
 
 public class ExpressionBinding extends AbstractBinding
 {
     /**
      * The root object against which the nested property name is evaluated.
      */
 
     private final IComponent _root;
 
     /**
      * If true, then the binding is invariant.
      */
 
     private boolean _invariant = false;
 
     /**
      * Parsed OGNL expression.
      */
 
     private Node _parsedExpression;
 
     /**
      * Compiled OGNL expression.
      */
 
     private ExpressionAccessor _accessor;
 
     /**
      * Flag set to true once the binding has initialized.
      */
 
     private boolean _initialized;
 
     /**
      * @since 4.0
      */
 
     private ExpressionEvaluator _evaluator;
 
     /** @since 4.0 */
 
     private ExpressionCache _cache;
 
     /**
      * Used to detect previous failed attempts at writing values when compiling expressions so
      * that as many expressions as possible can be fully compiled into their java byte form when
      * all objects in the expression are available.
      */
     private boolean _writeFailed;
 
     /**
      * Creates a {@link ExpressionBinding} from the root object and an OGNL expression.
      *
      * @param description
      *          Used by superclass constructor - {@link AbstractBinding#AbstractBinding(String, org.apache.tapestry.coerce.ValueConverter, org.apache.hivemind.Location)}.
      * @param location
      *          Used by superclass constructor - {@link AbstractBinding#AbstractBinding(String, org.apache.tapestry.coerce.ValueConverter, org.apache.hivemind.Location)}.
      * @param valueConverter
      *          Used by superclass constructor - {@link AbstractBinding#AbstractBinding(String, org.apache.tapestry.coerce.ValueConverter, org.apache.hivemind.Location)}.
      * @param root
      *          The object this binding should be resolved against.
      * @param expression
      *          The string expression.
      * @param evaluator
      *          Evaluator used to parse and run the expression.
      * @param cache
      *          Expression cache which does efficient caching of parsed expressions.
      */
     public ExpressionBinding(String description, Location location, ValueConverter valueConverter,
                              IComponent root, String expression, ExpressionEvaluator evaluator,
                              ExpressionCache cache)
     {
         super(expression, valueConverter, location);
 
         _root = root;
         _evaluator = evaluator;
         _cache = cache;
     }
 
     /**
      * Gets the value of the property path, with the assistance of the {@link ExpressionEvaluator}.
      *
      * @throws BindingException
      *             if an exception is thrown accessing the property.
      */
 
     public Object getObject()
     {
         initialize();
 
         return resolveExpression();
     }
 
     private Object resolveExpression()
     {
         try
         {
             if (_accessor == null && !_writeFailed)
             {
                 _parsedExpression = (Node)_cache.getCompiledExpression(_root, _description);
                 _accessor = _parsedExpression.getAccessor();
             }
 
             if (_accessor != null)
                 return _evaluator.read(_root, _accessor);
 
             return _evaluator.readCompiled(_root, _parsedExpression);
         }
         catch (Throwable t)
         {
             throw new BindingException(t.getMessage(), this, t);
         }
     }
 
     /**
      * Returns true if the binding is expected to always return the same value.
      */
 
     public boolean isInvariant()
     {
         initialize();
 
         return _invariant;
     }
 
     /**
      * Sets up the helper object, but also optimizes the property path and determines if the binding
      * is invarant.
      */
 
     private void initialize()
     {
         if (_initialized)
             return;
 
         _initialized = true;
 
         try
         {
             _parsedExpression = (Node)_cache.getCompiledExpression(_description);
             _invariant = _evaluator.isConstant(_description);
         }
         catch (Exception ex)
         {
             throw new BindingException(ex.getMessage(), this, ex);
         }
     }
 
     /**
      * Updates the property for the binding to the given value.
      *
      * @throws BindingException
      *             if the property can't be updated (typically due to an security problem, or a
      *             missing mutator method).
      */
 
     public void setObject(Object value)
     {
         initialize();
 
         if (_invariant)
             throw createReadOnlyBindingException(this);
 
         try
         {
             if (_accessor == null)
             {
                 _evaluator.writeCompiled(_root, _parsedExpression, value);
 
                 if (!_writeFailed)
                 {    
                     // re-parse expression as compilation may be possible now that it potentially has a value
                     try {
                         _parsedExpression = (Node)_cache.getCompiledExpression(_root, _description);
 
                         _accessor = _parsedExpression.getAccessor();
                     } catch (Throwable t) {
 
                         // ignore re-read failures as they aren't supposed to be happening now anyways
                         // and a more user friendly version will be available if someone actually calls
                         // getObject
 
                         // if writing fails then we're probably screwed...so don't do it again
                         if (value != null)
                             _writeFailed = true;
                     }
                 }
             } else
            {
                 _evaluator.write(_root, _accessor, value);
            }
         }
         catch (Throwable ex)
         {
             throw new BindingException(ex.getMessage(), this, ex);
         }
     }
 
     /**
      * Returns the a String representing the property path. This includes the
      * {@link IComponent#getExtendedId() extended id}of the root component and the property path
      * ... once the binding is used, these may change due to optimization of the property path.
      */
 
     public String toString()
     {
         StringBuffer buffer = new StringBuffer();
 
         buffer.append("ExpressionBinding[");
         buffer.append(_root.getExtendedId());
 
         if (_description != null)
         {
             buffer.append(' ');
             buffer.append(_description);
         }
 
         buffer.append(']');
 
         return buffer.toString();
     }
 
     /** @since 4.0 */
     public Object getComponent()
     {
         return _root;
     }
 }
