 /*
  * RangeError.java - No description
  * Created on 27. Feb 2002
  * 
  * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
  * Jan Arne Petersen <japetersen@web.de>
  * Stefan Matthias Aust <sma@3plus4.de>
  * Alan Moore <alan_moore@gmx.net>
  * Benoit Cerrina <b.cerrina@wanadoo.fr>
  * 
  * JRuby - http://jruby.sourceforge.net
  * 
  * This file is part of JRuby
  * 
  * JRuby is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  * 
  * JRuby is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  * 
  * You should have received a copy of the GNU General Public License
  * along with JRuby; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  * 
  */
 
 package org.jruby.exceptions;
 
 import org.jruby.*;
 
 /**
  *
  * @author  jpetersen
  */
 public class RangeError extends RaiseException {
 
     /**
      * Constructs an <code>RubyTypeException</code> with the specified detail message.
      * @param msg the detail message.
      */
     public RangeError(Ruby ruby, String msg) {
        super(ruby, ruby.getExceptions().getNameError(), msg);
     }
 }
