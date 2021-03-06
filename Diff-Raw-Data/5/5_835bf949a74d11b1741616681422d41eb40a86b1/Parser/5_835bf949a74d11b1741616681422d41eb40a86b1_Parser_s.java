 /*
  * Copyright (c) 2013, European Bioinformatics Institute (EMBL-EBI)
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *
  * 1. Redistributions of source code must retain the above copyright notice, this
  *    list of conditions and the following disclaimer.
  * 2. Redistributions in binary form must reproduce the above copyright notice,
  *    this list of conditions and the following disclaimer in the documentation
  *    and/or other materials provided with the distribution.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  *
  * The views and conclusions contained in the software and documentation are those
  * of the authors and should not be interpreted as representing official policies,
  * either expressed or implied, of the FreeBSD Project.
  */
 
 package uk.ac.ebi.grins;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import static java.util.Map.Entry;
 import static uk.ac.ebi.grins.Atom.AromaticSubset;
 import static uk.ac.ebi.grins.Atom.OrganicSubset;
 
 
 /**
  * Parse a SMILES string and create a {@link ChemicalGraph}. A new parser should
  * be created for each invocation, for convenience {@link #parse(String)} is
  * provided.
  *
  * <blockquote><pre>
  * ChemicalGraph g = Parser.parse("CCO");
  * </pre></blockquote>
  *
  * @author John May
  */
 final class Parser {
 
     /** Keep track of branching. */
     private final IntStack stack = new IntStack(10);
 
     /** Molecule being loaded. */
     private final ChemicalGraph g;
 
     /** Keep track of ring information. */
     private RingBond[] rings = new RingBond[10];
 
     /** Local arrangement for ring openings. */
     private Map<Integer, LocalArrangement> arrangement
             = new HashMap<Integer, LocalArrangement>(5);
 
     private Map<Integer, Configuration> configurations
             = new HashMap<Integer, Configuration>(5);
 
     /** Current bond. */
     private Bond bond = Bond.IMPLICIT;
 
     /** Current configuration. */
     private Configuration configuration = Configuration.UNKNOWN;
 
     /**
      * Create a new parser for the specified buffer.
      *
      * @param buffer character buffer holding a SMILES string
      * @throws InvalidSmilesException thrown if the SMILES could not be parsed
      */
     Parser(CharBuffer buffer) throws InvalidSmilesException {
         g = new ChemicalGraph(1 + (2 * (buffer.length() / 3)));
         readSmiles(buffer);
         createTopologies();
     }
 
     /**
      * Create a new parser for the specified string.
      *
      * @param str SMILES string
      * @throws InvalidSmilesException thrown if the SMILES could not be parsed
      */
     Parser(String str) throws InvalidSmilesException {
         this(CharBuffer.fromString(str));
     }
 
     /**
      * Access the molecule created by the parser.
      *
      * @return the chemical graph for the parsed smiles string
      */
     ChemicalGraph molecule() {
         return g;
     }
 
     /**
      * Create the topologies (stereo configurations) for the chemical graph. The
      * topologies define spacial arrangement around atoms.
      */
     private void createTopologies() {
         // create topologies (stereo configurations)
         for (Entry<Integer, Configuration> e : configurations.entrySet())
             addTopology(e.getKey(),
                         Topology.toExplicit(g, e.getKey(), e.getValue()));
     }
 
     /**
      * Add a topology for vertex 'u' with configuration 'c'. If the atom 'u' was
      * involved in a ring closure the local arrangement is used instead of the
      * order in the graph. The configuration should be explicit '@TH1' or '@TH2'
      * instead of '@' or '@@'.
      *
      * @param u a vertex
      * @param c explicit configuration of that vertex
      * @see Topology#toExplicit(ChemicalGraph, int, Configuration)
      */
     private void addTopology(int u, Configuration c) {
 
         // stereo on ring closure - use local arrangement
         if (arrangement.containsKey(u)) {
             int[] vs = arrangement.get(u).toArray();
             List<Edge> es = new ArrayList<Edge>(vs.length);
             for (int v : vs)
                 es.add(g.edge(u, v));
             g.addTopology(Topology.create(u, vs, es, c));
         } else {
             int[] vs = new int[g.degree(u)];
             List<Edge> es = g.edges(u);
             for (int i = 0; i < vs.length; i++)
                 vs[i] = es.get(i).other(u);
             g.addTopology(Topology.create(u, vs, es, c));
         }
     }
 
     /**
      * Add an atom and bond with the atom on the stack (if available and non-dot
      * bond).
      *
      * @param a an atom to add
      */
     private void addAtom(Atom a) {
         int v = g.addAtom(a);
         if (!stack.empty()) {
             int u = stack.pop();
             if (bond != Bond.DOT)
                 g.addEdge(new Edge(u, v, bond));
             if (arrangement.containsKey(u))
                 arrangement.get(u).add(v);
             bond = Bond.IMPLICIT;
         }
         stack.push(v);
 
         // configurations used to create topologies after parsing
         if (configuration != Configuration.UNKNOWN) {
             configurations.put(v, configuration);
             configuration = Configuration.UNKNOWN;
         }
     }
 
     /**
      * Read a molecule from the character buffer.
      *
      * @param buffer a character buffer
      * @throws InvalidSmilesException invalid grammar
      */
     private void readSmiles(final CharBuffer buffer) throws
                                                      InvalidSmilesException {
         // primary dispatch
         while (buffer.hasRemaining()) {
             char c = buffer.get();
             switch (c) {
 
                 // organic subset
                 case 'B':
                     if (buffer.getIf('r'))
                         addAtom(OrganicSubset.Bromine);
                     else
                         addAtom(OrganicSubset.Boron);
                     break;
                 case 'C':
                     if (buffer.getIf('l'))
                         addAtom(OrganicSubset.Chlorine);
                     else
                         addAtom(OrganicSubset.Carbon);
                     break;
                 case 'N':
                     addAtom(OrganicSubset.Nitrogen);
                     break;
                 case 'O':
                     addAtom(OrganicSubset.Oxygen);
                     break;
                 case 'P':
                     addAtom(OrganicSubset.Phosphorus);
                     break;
                 case 'S':
                     addAtom(OrganicSubset.Sulfur);
                     break;
                 case 'F':
                     addAtom(OrganicSubset.Fluorine);
                     break;
                 case 'I':
                     addAtom(OrganicSubset.Iodine);
                     break;
 
                 // aromatic subset
                 case 'b':
                     addAtom(AromaticSubset.Boron);
                     break;
                 case 'c':
                     addAtom(AromaticSubset.Carbon);
                     break;
                 case 'n':
                     addAtom(AromaticSubset.Nitrogen);
                     break;
                 case 'o':
                     addAtom(AromaticSubset.Oxygen);
                     break;
                 case 'p':
                     addAtom(AromaticSubset.Phosphorus);
                     break;
                 case 's':
                     addAtom(AromaticSubset.Sulfur);
                     break;
 
                 // bracket atom
                 case '[':
                     addAtom(readBracketAtom(buffer));
                     break;
 
                 // ring bonds
                 case '0':
                 case '1':
                 case '2':
                 case '3':
                 case '4':
                 case '5':
                 case '6':
                 case '7':
                 case '8':
                 case '9':
                     ring(c - '0');
                     break;
                 case '%':
                     int num = buffer.getNumber();
                     if (num < 0)
                         throw new InvalidSmilesException("number (<digit>+) must follow '%'", buffer);
                     ring(num);
                     break;
 
                 // bond/dot
                 case '-':
                     bond = Bond.SINGLE;
                     break;
                 case '=':
                     bond = Bond.DOUBLE;
                     break;
                 case '#':
                     bond = Bond.TRIPLE;
                     break;
                 case '$':
                     bond = Bond.QUADRUPLE;
                     break;
                 case ':':
                     bond = Bond.AROMATIC;
                     break;
                 case '/':
                     bond = Bond.UP;
                     break;
                 case '\\':
                     bond = Bond.DOWN;
                     break;
                 case '.':
                     bond = Bond.DOT;
                     break;
 
                 // branching
                 case '(':
                     stack.push(stack.peek());
                     break;
                 case ')':
                     stack.pop();
                     break;
 
                 // termination
                 case '\t':
                 case ' ':
                 case '\n':
                 case '\r':
                     return;
 
                 default:
                     throw new InvalidSmilesException("unexpected character:", buffer);
             }
         }
     }
 
     /**
      * Read a bracket atom from the buffer. A bracket atom optionally defines
      * isotope, chirality, hydrogen count, formal charge and the atom class.
      *
      * <blockquote><pre>
      * bracket_atom ::= '[' isotope? symbol chiral? hcount? charge? class? ']'
      * </pre></blockquote>
      *
      * @param buffer a character buffer
      * @return a bracket atom
      * @throws InvalidSmilesException thrown if the bracket atom did not match
      *                                the grammar, invalid symbol, missing
      *                                closing bracket or invalid chiral
      *                                specification.
      */
     Atom readBracketAtom(final CharBuffer buffer) throws
                                                   InvalidSmilesException {
         final int isotope = buffer.getNumber();
         final boolean aromatic = buffer.next() >= 'a' && buffer.next() <= 'z';
         final Element element = Element.read(buffer);
 
         configuration = Configuration.read(buffer);
 
         int hCount = readHydrogens(buffer);
         int charge = readCharge(buffer);
         int atomClass = readClass(buffer);
 
         if (!buffer.getIf(']'))
             throw InvalidSmilesException.invalidBracketAtom(buffer);
 
        // currently we treat unspecified isotope the same as '0', this does not
        // follow the specification but is a point of contention and most parser
        // do this.
        return new Atom.BracketAtom(isotope < 0 ? 0 : isotope,
                                     element,
                                     hCount,
                                     charge,
                                     atomClass,
                                     aromatic);
     }
 
     /**
      * Read the hydrogen count and progress the provided buffer. The hydrogen
      * count is specified by a 'H' an 0 or more digits. A 'H' without digits is
      * intercepted as 'H1'. When there is no 'H' or 'H0' is specified then the
      * the hydrogen count is 0.
      *
      * @param buffer a character buffer
      * @return the hydrogen count, 0 if none
      */
     static int readHydrogens(final CharBuffer buffer) {
         if (buffer.getIf('H')) {
             // when no number is specified 'H' then there is 1 hydrogen
             int count = buffer.getNumber();
             return count < 0 ? 1 : count;
         }
         return 0;
     }
 
     /**
      * Read a charge value and progress the provide buffer. The charge value is
      * present in bracket atoms either directly after the symbol, the chiral
      * specification or the hydrogen count. The specification of charge by
      * concatenated signs (e.g. ++, --) and other bad form (e.g. '++-1') is
      * intercepted.
      *
      * @param buffer a character buffer
      * @return the formal charge value, 0 if none present
      * @see <a href="http://www.opensmiles.org/opensmiles.html#charge">Charge -
      *      OpenSMILES Specification</a>
      */
     static int readCharge(final CharBuffer buffer) {
         return readCharge(0, buffer);
     }
 
     /**
      * Internal method for parsing charge, to allow concatenated signs (--, ++)
      * the method recursively invokes increment or decrementing an accumulator.
      *
      * @param acc    accumulator
      * @param buffer a character buffer
      * @return the charge value
      */
     private static int readCharge(int acc, final CharBuffer buffer) {
         if (buffer.getIf('+'))
             return buffer.nextIsDigit() ? acc + buffer.getNumber()
                                         : readCharge(acc + 1, buffer);
         if (buffer.getIf('-'))
             return buffer.nextIsDigit() ? acc - buffer.getNumber()
                                         : readCharge(acc - 1, buffer);
         return acc;
     }
 
     /**
      * Read the atom class of a bracket atom and progress the buffer (if read).
      * The atom class is the last attribute of the bracket atom and is
      * identified by a ':' followed by one or more digits. The atom class may be
      * padded such that ':005' and ':5' are equivalent.
      *
      * @param buffer a character buffer
      * @return the atom class, or 0
      * @see <a href="http://www.opensmiles.org/opensmiles.html#atomclass">Atom
      *      Class - OpenSMILES Specification</a>
      */
     static int readClass(CharBuffer buffer) throws InvalidSmilesException {
         if (buffer.getIf(':')) {
             if (buffer.nextIsDigit())
                 return buffer.getNumber();
             throw new InvalidSmilesException("invalid atom class, <digit>+ must follow ':'", buffer);
         }
         return 0;
     }
 
     /**
      * Handle the ring open/closure of the specified ring number 'rnum'.
      *
      * @param rnum ring number
      * @throws InvalidSmilesException bond types did not match on ring closure
      */
     private void ring(int rnum) throws InvalidSmilesException {
         if (rings.length <= rnum || rings[rnum] == null) {
             openRing(rnum);
         } else {
             closeRing(rnum);
         }
     }
 
     /**
      * Open the ring bond with the specified 'rnum'.
      *
      * @param rnum ring number
      */
     private void openRing(int rnum) {
         if (rnum >= rings.length)
             rings = Arrays.copyOf(rings, rnum + 1);
         int u = stack.peek();
 
         // create a ring bond
         rings[rnum] = new RingBond(u, bond);
 
         // keep track of arrangement (important for stereo configurations)
         createArrangement(u).add(-rnum);
 
         bond = Bond.IMPLICIT;
     }
 
     /**
      * Create the current local arrangement for vertex 'u' - if the arrangment
      * already exists then that arrangement is used.
      *
      * @param u vertex to get the arrangement around
      * @return current local arrangement
      */
     private LocalArrangement createArrangement(int u) {
         LocalArrangement la = arrangement.get(u);
         if (la == null) {
             la = new LocalArrangement();
             for (Edge e : g.edges(stack.peek()))
                 la.add(e.other(u));
             arrangement.put(u, la);
         }
         return la;
     }
 
     /**
      * Close the ring bond with the specified 'rnum'.
      *
      * @param rnum ring number
      * @throws InvalidSmilesException bond types did not match
      */
     private void closeRing(int rnum) throws InvalidSmilesException {
         RingBond rbond = rings[rnum];
         rings[rnum] = null;
         g.addEdge(new Edge(rbond.u, stack.peek(),
                            decideBond(rbond.bond, bond.inverse())));
         bond = Bond.IMPLICIT;
         // adjust the arrangement replacing where this ring number was openned
         arrangement.get(rbond.u).replace(-rnum, stack.peek());
     }
 
     /**
      * Decide the bond to use for a ring bond. The bond symbol can be present on
      * either or both bonded atoms. This method takes those bonds, chooses the
      * correct one or reports an error if there is a conflict.
      *
      * Equivalent SMILES:
      * <blockquote><pre>
      *     C=1CCCCC=1
      *     C=1CCCCC1    (preferred)
      *     C1CCCCC=1
      * </pre></blockquote>
      *
      * @param a a bond
      * @param b other bond
      * @return the bond to use for this edge
      * @throws InvalidSmilesException ring bonds did not match
      */
     static Bond decideBond(final Bond a, final Bond b) throws
                                                        InvalidSmilesException {
         if (a == b)
             return a;
         else if (a == Bond.IMPLICIT)
             return b;
         else if (b == Bond.IMPLICIT)
             return a;
         throw new InvalidSmilesException("ring bond mismatch, " + a + " and " + b);
     }
 
     /**
      * Convenience method for parsing a SMILES string.
      *
      * @param str SMILES string
      * @return the chemical graph for the provided SMILES notation
      * @throws InvalidSmilesException thrown if the SMILES could not be
      *                                interpreted
      */
     static ChemicalGraph parse(String str) throws InvalidSmilesException {
         return new Parser(str).molecule();
     }
 
     /**
      * Hold information about ring open/closures. The ring bond can optionally
      * specify the bond type.
      */
     private static final class RingBond {
         int  u;
         Bond bond;
 
         private RingBond(int u, Bond bond) {
             this.u = u;
             this.bond = bond;
         }
     }
 
     /**
      * Hold information on the local arrangement around an atom. The arrangement
      * is normally identical to the order loaded unless the atom is involved in
      * a ring closure. This is particularly important for stereo specification
      * where the ring bonds should be in the order listed. This class stores the
      * local arrangement by setting a negated 'rnum' as a placeholder and then
      * replacing it once the connected atom has been read. Although this could
      * be stored directly on the graph (negated edge) it allows us to keep all
      * edges in sorted order.
      */
     private static final class LocalArrangement {
 
         int[] vs;
         int   n;
 
         /** New local arrangement. */
         private LocalArrangement() {
             this.vs = new int[4];
         }
 
         /**
          * Append a vertex to the arrangement.
          *
          * @param v vertex to append
          */
         void add(final int v) {
             if (n == vs.length)
                 vs = Arrays.copyOf(vs, n * 2);
             vs[n++] = v;
         }
 
         /**
          * Replace the vertex 'u' with 'v'. Allows us to use negated values as
          * placeholders.
          *
          * <blockquote><pre>
          * LocalArrangement la = new LocalArrangement();
          * la.add(1);
          * la.add(-2);
          * la.add(-1);
          * la.add(5);
          * la.replace(-1, 4);
          * la.replace(-2, 6);
          * la.toArray() = {1, 6, 4, 5}
          * </pre></blockquote>
          *
          * @param u negated vertex
          * @param v new vertex
          */
         void replace(final int u, final int v) {
             for (int i = 0; i < n; i++) {
                 if (vs[i] == u) {
                     vs[i] = v;
                     return;
                 }
             }
         }
 
         /**
          * Access the local arrange of vertices.
          *
          * @return array of vertices and there order around an atom.
          */
         int[] toArray() {
             return Arrays.copyOf(vs, n);
         }
     }
 }
