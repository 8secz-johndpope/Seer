 /*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
 // DWB5: session.err is not redirected when creating pipeline
 // DWB6: add 'set -x' trace feature if echo is set
 // DWB7: removing variable via 'execute("name=") throws OutOfBoundsException
 package org.apache.felix.gogo.runtime.shell;
 
 import org.osgi.service.command.CommandSession;
 import org.osgi.service.command.Function;
 
 import java.util.*;
 
 public class Closure extends Reflective implements Function
 {
     private static final long serialVersionUID = 1L;
     final CharSequence source;
     final Closure parent;
     CommandSessionImpl session;
     List<Object> parms;
 
     Closure(CommandSessionImpl session, Closure parent, CharSequence source)
     {
         this.session = session;
         this.parent = parent;
         this.source = source;
     }
 
     public Object execute(CommandSession x, List<Object> values) throws Exception
     {
         parms = values;
         Parser parser = new Parser(source);
         ArrayList<Pipe> pipes = new ArrayList<Pipe>();
         List<List<List<CharSequence>>> program = parser.program();
 
         for (List<List<CharSequence>> statements : program)
         {
             Pipe current = new Pipe(this, statements);
 
             if (pipes.isEmpty())
             {
                 current.setIn(session.in);
                 current.setOut(session.out);
                 current.setErr(session.err);    // XXX: derek.baum@paremus.com
             }
             else
             {
                 Pipe previous = pipes.get(pipes.size() - 1);
                 previous.connect(current);
             }
             pipes.add(current);
         }
         if (pipes.size() == 0)
         {
             return null;
         }
 
         if (pipes.size() == 1)
         {
             pipes.get(0).run();
         }
         else
         {
             for (Pipe pipe : pipes)
             {
                 pipe.start();
             }
             for (Pipe pipe : pipes)
             {
                 pipe.join();
             }
         }
 
         Pipe last = pipes.get(pipes.size() - 1);
         if (last.exception != null)
         {
             throw last.exception;
         }
 
         if (last.result instanceof Object[])
         {
             return Arrays.asList((Object[]) last.result);
         }
         return last.result;
     }
 
     Object executeStatement(List<CharSequence> statement) throws Exception
     {
         Object result;
         List<Object> values = new ArrayList<Object>();
         CharSequence statement0 = statement.remove(0);
 
         // derek: FEATURE: add set -x facility if echo is set
         if (Boolean.TRUE.equals(session.get("echo"))) {
             StringBuilder buf = new StringBuilder("+ ");
             buf.append(statement0);
             for (CharSequence token : statement)
             {
                 buf.append(' ');
                 buf.append(token);
             }
             System.err.println(buf);
         }
 
         Object cmd = eval(statement0);
         for (CharSequence token : statement)
         {
             values.add(eval(token));
         }
 
         result = execute(cmd, values);
         return result;
     }
 
     private Object execute(Object cmd, List<Object> values) throws Exception
     {
         if (cmd == null)
         {
             if (values.isEmpty())
             {
                 return null;
             }
             else
             {
                 throw new IllegalArgumentException("Command name evaluates to null");
             }
         }
 
         // Now there are the following cases
         // <string> '=' statement // complex assignment
         // <string> statement // cmd call
         // <object> // value of <object>
         // <object> statement // method call
 
         if (cmd instanceof CharSequence)
         {
             String scmd = cmd.toString();
 
             if (values.size() > 0 && "=".equals(values.get(0)))
             {
                 //if (values.size() == 0)
                 if (values.size() == 1)            // derek: BUGFIX
                 {
                     return session.variables.remove(scmd);
                 }
                 else
                 {
                     Object value = execute(values.get(1), values.subList(2, values.size()));
                     return assignment(scmd, value);
                 }
             }
             else
             {
                 String scopedFunction = scmd;
                 Object x = get(scmd);
                 if (!(x instanceof Function))
                 {
                     if (scmd.indexOf(':') < 0)
                     {
                         scopedFunction = "*:" + scmd;
                     }
                     x = get(scopedFunction);
                     if (x == null || !(x instanceof Function))
                     {
                         if (values.isEmpty())
                         {
                             return scmd;
                         }
                         throw new IllegalArgumentException("Command not found:  " + scopedFunction);
                     }
                 }
                 return ((Function) x).execute(session, values);
             }
         }
         else
         {
             if (values.isEmpty())
             {
                 return cmd;
             }
             else
             {
                 return method(session, cmd, values.remove(0).toString(), values);
             }
         }
     }
 
     private Object assignment(Object name, Object value)
     {
         session.variables.put(name, value);
         return value;
     }
 
     private Object eval(CharSequence seq) throws Exception
     {
         Object res = null;
         StringBuilder sb = null;
         Parser p = new Parser(seq);
         int start = p.current;
         while (!p.eof())
         {
             char c = p.peek();
             if (!p.escaped)
             {
                 if (c == '$' || c == '<' || c == '\'' || c == '"' || c == '[' || c == '{')
                 {
                     if (start != p.current || res != null)
                     {
                         if (sb == null)
                         {
                             sb = new StringBuilder();
                            if (res != null)
                            {
                                sb.append(res);
                                res = null;
                            }
                         }
                         if (start != p.current)
                         {
                             sb.append(new Parser(p.text.subSequence(start, p.current)).unescape());
                             start = p.current;
                             continue;
                         }
                     }
                     switch (c)
                     {
                         case '\'':
                             p.next();
                             p.quote(c);
                             res = new Parser(p.text.subSequence(start + 1, p.current - 1)).unescape();
                             start = p.current;
                             continue;
                         case '\"':
                             p.next();
                             p.quote(c);
                             res = eval(p.text.subSequence(start + 1, p.current - 1));
                             start = p.current;
                             continue;
                         case '[':
                             p.next();
                             res = array(seq.subSequence(start + 1, p.find(']', '[') - 1));
                             start = p.current;
                             continue;
                         case '<':
                             p.next();
                             Closure cl = new Closure(session, this, p.text.subSequence(start + 1, p.find('>', '<') - 1));
                             res = cl.execute(session, parms);
                             start = p.current;
                             continue;
                         case '{':
                             p.next();
                             res = new Closure(session, this, p.text.subSequence(start + 1, p.find('}', '{') - 1));
                             start = p.current;
                             continue;
                         case '$':
                             p.next();
                             res = var(p.findVar());
                             start = p.current;
                             continue;
                     }
                 }
             }
             p.next();
         }
         if (start != p.current)
         {
             if (sb == null)
             {
                 sb = new StringBuilder();
                if (res != null)
                {
                    sb.append(res);
                    res = null;
                }
             }
             sb.append(new Parser(p.text.subSequence(start, p.current)).unescape());
         }
         if (sb != null)
         {
             if (res != null)
             {
                 sb.append(res);
             }
             res = sb;
         }
         if (res instanceof CharSequence)
         {
             String r = res.toString();
             if ("null".equals(r))
             {
                 return null;
             }
             else if ("false".equals(r))
             {
                 return false;
             }
             else if ("true".equals(r))
             {
                 return true;
             }
             return r;
         }
 
         return res;
     }
 
     private Object array(CharSequence array) throws Exception
     {
         List<Object> list = new ArrayList<Object>();
         Map<Object, Object> map = new LinkedHashMap<Object, Object>();
         Parser p = new Parser(array);
 
         while (!p.eof())
         {
             CharSequence token = p.value();
 
             p.ws();
             if (p.peek() == '=')
             {
                 p.next();
                 p.ws();
                 if (!p.eof())
                 {
                     CharSequence value = p.messy();
                     map.put(eval(token), eval(value));
                 }
             }
             else
             {
                 list.add(eval(token));
             }
 
             if (p.peek() == ',')
             {
                 p.next();
             }
             p.ws();
         }
         p.ws();
         if (!p.eof())
         {
             throw new IllegalArgumentException("Invalid array syntax: " + array);
         }
 
         if (map.size() != 0 && list.size() != 0)
         {
             throw new IllegalArgumentException("You can not mix maps and arrays: " + array);
         }
 
         if (map.size() > 0)
         {
             return map;
         }
         else
         {
             return list;
         }
     }
 
     private Object var(CharSequence var) throws Exception
     {
         Object v = eval(var);
         if (v instanceof Closure) {
             v = ((Closure) v).execute(session, null);
         }
         String name = v.toString();
         return get(name);
     }
 
     /**
      * @param name
      * @return
      */
     private Object get(String name)
     {
         if (parms != null)
         {
             if ("it".equals(name))
             {
                 return parms.get(0);
             }
             if ("args".equals(name))
             {
                 return parms;
             }
 
             if (name.length() == 1 && Character.isDigit(name.charAt(0)))
             {
                 return parms.get(name.charAt(0) - '0');
             }
         }
         return session.get(name);
     }
 }
