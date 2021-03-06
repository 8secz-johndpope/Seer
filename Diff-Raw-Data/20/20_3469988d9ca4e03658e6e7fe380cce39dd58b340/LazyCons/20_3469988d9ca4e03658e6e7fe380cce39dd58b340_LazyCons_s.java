 /**
  *   Copyright (c) Rich Hickey. All rights reserved.
  *   The use and distribution terms for this software are covered by the
  *   Common Public License 1.0 (http://opensource.org/licenses/cpl.php)
  *   which can be found in the file CPL.TXT at the root of this distribution.
  *   By using this software in any fashion, you are agreeing to be bound by
  * 	 the terms of this license.
  *   You must not remove this notice, or any other, from this software.
  **/
 
 /* rich Aug 9, 2008 */
 
 package clojure.lang;
 
 final public class LazyCons extends ASeq{
 IFn f;
 Object _first;
 ISeq _rest;
 
 public LazyCons(IFn f){
 	this.f = f;
	this._first = this;
	this._rest = this;
 }
 
 LazyCons(IPersistentMap meta, Object first, ISeq rest){
 	super(meta);
	this._first = first;
	this._rest = rest;
 }
 
 final
 synchronized
 public Object first(){
	if(_first == this)
 		{
 		try
 			{
 			_first = f.invoke();
 			}
 		catch(Exception ex)
 			{
 			throw new RuntimeException(ex);
 			}
 		}
 	return _first;
 }
 
 final
 synchronized
 public ISeq rest(){
	if(_rest == this)
 		{
 		try
 			{
 			//force sequential evaluation
			if(_first == this)
 				first();
 			_rest = RT.seq(f.invoke(null));
 			}
 		catch(Exception ex)
 			{
 			throw new RuntimeException(ex);
 			}
 		f = null;
 		}
 	return _rest;
 }
 
 public LazyCons withMeta(IPersistentMap meta){
 	if(meta == meta())
 		return this;
 	//force before copying
 	rest();
 	return new LazyCons(meta, _first, _rest);
 }
 }
