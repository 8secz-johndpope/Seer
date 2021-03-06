 package org.jruby.debug;
 
 import org.jruby.Ruby;
 import org.jruby.RubyClass;
 import org.jruby.RubyFixnum;
 import org.jruby.RubyNumeric;
 import org.jruby.RubyObject;
 import org.jruby.anno.JRubyMethod;
 import org.jruby.runtime.Block;
 import org.jruby.runtime.builtin.IRubyObject;
 
 public class Breakpoint extends RubyObject {
     protected Breakpoint(Ruby runtime, RubyClass type) {
         super(runtime, type);
     }
 
     DebugBreakpoint debuggerBreakpoint() {
         return (DebugBreakpoint)dataGetStruct();
     }
 
     @JRubyMethod(name="id")
     public RubyFixnum id(Block block) {
         return getRuntime().newFixnum(debuggerBreakpoint().getId());
     }
 
     @JRubyMethod(name="source")
     public IRubyObject source(Block block) {
         return debuggerBreakpoint().getSource();
     }
 
     @JRubyMethod(name="source=", required=1)
     public IRubyObject source_set(IRubyObject source, Block block) {
         debuggerBreakpoint().setSource(source.convertToString());
         
         return source;
     }
 
     @JRubyMethod(name="pos")
     public IRubyObject pos(Block block) {
         DebugBreakpoint debugBreakpoint = debuggerBreakpoint();
         if (debugBreakpoint.getType() == DebugBreakpoint.Type.METHOD) {
             return getRuntime().newString(debuggerBreakpoint().getPos().getMethodName());
         } else {
             return getRuntime().newFixnum(debuggerBreakpoint().getPos().getLine());    
         }
     }
 
     @JRubyMethod(name="pos=", required=1)
     public IRubyObject pos_set(IRubyObject pos, Block block) {
         DebugBreakpoint debugBreakpoint = debuggerBreakpoint();
         if (debugBreakpoint.getType() == DebugBreakpoint.Type.METHOD) {
            return getRuntime().newString(debuggerBreakpoint().getPos().getMethodName());
         } else {
            return getRuntime().newFixnum(debuggerBreakpoint().getPos().getLine());    
         }
     }
 
     @JRubyMethod(name="expr")
     public IRubyObject expr(Block block) {
         return debuggerBreakpoint().getExpr();
     }
 
     @JRubyMethod(name="expr=", required=1)
     public IRubyObject expr_set(IRubyObject expr, Block block) {
         debuggerBreakpoint().setExpr(expr.convertToString());
         
         return expr;
     }
 
     @JRubyMethod(name="hit_count")
     public IRubyObject hit_count(Block block) {
         return getRuntime().newFixnum(debuggerBreakpoint().getHitCount());
     }
 
     @JRubyMethod(name="hit_value")
     public IRubyObject hit_value(Block block) {
         return getRuntime().newFixnum(debuggerBreakpoint().getHitValue());
     }
 
     @JRubyMethod(name="hit_value=", required=1)
     public IRubyObject hit_value_set(IRubyObject hit_value, Block block) {
         debuggerBreakpoint().setHitValue(RubyNumeric.fix2int(hit_value));
         
         return hit_value;
     }
 
     @JRubyMethod(name="hit_condition")
     public IRubyObject hit_condition(Block block) {
         DebugBreakpoint.HitCondition cond = debuggerBreakpoint().getHitCondition();
         if (cond == null) {
             return getRuntime().getNil();
         } else {
             switch (cond) {
             case GE:
                 return getRuntime().newSymbol("greater_or_equal");
             case EQ:
                 return getRuntime().newSymbol("equal");
             case MOD:
                 return getRuntime().newSymbol("modulo");
             case NONE:
             default:
                 return getRuntime().getNil();
             }
         }
     }
 
     @JRubyMethod(name="hit_condition=", required=1)
     public IRubyObject hit_condition_set(IRubyObject hit_condition, Block block) {
         DebugBreakpoint debugBreakpoint = debuggerBreakpoint();
         
         if (! hit_condition.isKindOf(getRuntime().getSymbol())) {
             throw getRuntime().newArgumentError("Invalid condition parameter");
         }
         
         String symbol = hit_condition.asSymbol();
         if (symbol.equals("greater_or_equal") || symbol.equals("ge")) {
             debugBreakpoint.setHitCondition(DebugBreakpoint.HitCondition.GE);
         } else if (symbol.equals("equal") || symbol.equals("eq")) {
             debugBreakpoint.setHitCondition(DebugBreakpoint.HitCondition.EQ);
         } else if (symbol.equals("modulo") || symbol.equals("mod")) {
             debugBreakpoint.setHitCondition(DebugBreakpoint.HitCondition.MOD);
         } else {
             throw getRuntime().newArgumentError("Invalid condition parameter");
         }
         
         return hit_condition;
     }
 }
