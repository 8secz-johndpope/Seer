 package r.nodes;
 
 import java.util.*;
 
 import r.data.*;
 import r.data.RFunction.*;
 import r.data.internal.*;
 import r.nodes.tools.*;
 import r.nodes.truffle.RNode;
 
 public class Function extends ASTNode {
 
     final ArgumentList signature;
     final ASTNode body;
 
     private static ReadSetEntry[] emptyReadSet = new ReadSetEntry[0];
 
     RFunction rfunction; // FIXME: is it ok this is not final?
 
     Function(ArgumentList alist, ASTNode body) {
         this.signature = alist;
         this.body = updateParent(body);
     }
 
     public RFunction getRFunction() {
         return rfunction;
     }
 
     private void setRFunction(RFunction rfunction) {
         this.rfunction = rfunction;
     }
 
     public ArgumentList getSignature() {
         return signature;
     }
 
     public ASTNode getBody() {
         return body;
     }
 
     @Override
     public void accept(Visitor v) {
         v.visit(this);
     }
 
     @Override
     public void visit_all(Visitor v) {
         body.accept(v);
     }
 
     public static ASTNode create(ArgumentList alist, ASTNode body) {
         return new Function(alist, body);
     }
 
     @Override
     public String toString() {
         // FIXME: real R remembers the expression string for this
         StringBuilder str = new StringBuilder();
         str.append("function (");
         boolean first = true;
         for (ArgumentList.Entry a : signature) {
             if (first) {
                 first = false;
             } else {
                 str.append(", ");
             }
             str.append(a.getName().pretty());
             ASTNode exp = a.getValue();
             if (exp != null) {
                 str.append("=");
                 str.append(exp.toString());
             }
         }
        str.append(") {");
        PrettyPrinter.getStringPrettyPrinter().print(body);
        str.append(pp.)
        str.append(body.toString());
        str.append("}");
         return str.toString();
     }
 
     // note: the locallyWritten and locallyRead hash sets are modified input parameters
     public RFunction createImpl(RSymbol[] paramNames, RNode[] paramValues, RNode runnableBody, RFunction enclosing) {
         // note: we cannot read fnode.argNames() here as it is not yet initialized
 
         // find variables accessed
         Set<RSymbol> read = new HashSet<>();
         Set<RSymbol> written = new HashSet<>();
         findAccesses(read, written);
 
         RSymbol[] writeSet = buildWriteSet(paramNames, written);
         ReadSetEntry[] readSet = buildReadSet(enclosing, read);
 
         FunctionImpl impl = new FunctionImpl(this, paramNames, paramValues, runnableBody, enclosing, writeSet, readSet);
         setRFunction(impl);
         return impl;
     }
 
     void findAccesses(Set<RSymbol> rs, Set<RSymbol> ws) {
         new FindAccesses().find(rs, ws);
     }
 
     private static RSymbol[] buildWriteSet(RSymbol[] argNames, Set<RSymbol> origWSet) {
         RSymbol[] writeSet = new RSymbol[origWSet.size() + argNames.length];
         int i = 0;
         for (; i < argNames.length; i++) {
             writeSet[i] = argNames[i];
         }
         for (RSymbol s : origWSet) {
             writeSet[i++] = s;
         }
         return writeSet;
     }
 
     private static ReadSetEntry[] buildReadSet(RFunction parent, Set<RSymbol> origRSet) {
         // build read set
         if (parent == null || origRSet.isEmpty()) {
             return emptyReadSet;
         }
         ArrayList<ReadSetEntry> rsl = new ArrayList<>();
         for (RSymbol s : origRSet) {
             RFunction p = parent;
             int hops = 1;
             while (p != null) {
                 int pos = p.positionInWriteSet(s);
                 if (pos >= 0) {
                     rsl.add(new ReadSetEntry(s, hops, pos)); // FIXME: why not remember the RFunction reference instead of hops?
                     // FIXME: answer: The RFunction Reference will need and extra indirection to be tested (RFrame => RFunction)
                     break;
                 }
                 p = p.enclosing();
                 hops++;
             }
         }
         return rsl.toArray(new ReadSetEntry[0]); // FIXME: rewrite this to get rid of allocation/copying
     }
     class FindAccesses implements Visitor {
 
         Set<RSymbol> read;
         Set<RSymbol> written;
 
         public void find(Set<RSymbol> rs, Set<RSymbol> ws) {
             this.read = rs;
             this.written = ws;
 
             visit_all(this); // does a function body
             // FIXME: should visit_all visit the default expressions on its own?
             ArgumentList al = getSignature();
             for (ArgumentList.Entry e : al) {
                 ASTNode val = e.getValue();
                 if (val != null) {
                     val.visit_all(this);
                 }
                 // note: formal arguments are added to write set elsewhere
             }
         }
 
         @Override
         public void visit(If iff) {
             iff.visit_all(this);
         }
 
         @Override
         public void visit(Repeat repeat) {
             repeat.visit_all(this);
         }
 
         @Override
         public void visit(While w) {
             w.visit_all(this);
         }
 
         @Override
         public void visit(Sequence sequence) {
             sequence.visit_all(this);
         }
 
         @Override
         public void visit(Mult mult) {
             mult.visit_all(this);
         }
 
         @Override
         public void visit(Add add) {
             add.visit_all(this);
         }
 
         @Override
         public void visit(Not n) {
             n.visit_all(this);
         }
 
         @Override
         public void visit(Constant constant) {
         }
 
         @Override
         public void visit(SimpleAccessVariable readVariable) {
             read.add(readVariable.getSymbol());
         }
 
         @Override
         public void visit(FieldAccess fieldAccess) {
             fieldAccess.visit_all(this);
         }
 
         @Override
         public void visit(SimpleAssignVariable assign) {
             written.add(assign.getSymbol());
             assign.visit_all(this); // visit the rhs expression
         }
 
         @Override
         public void visit(Function function) {
         }
 
         @Override
         public void visit(FunctionCall functionCall) {
             read.add(functionCall.getName());
             functionCall.visit_all(this); // visit default value expressions if any
         }
     }
 }
