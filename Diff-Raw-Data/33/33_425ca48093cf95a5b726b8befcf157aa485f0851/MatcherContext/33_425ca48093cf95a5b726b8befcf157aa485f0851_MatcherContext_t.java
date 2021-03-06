 /*
  * Copyright (C) 2009 Mathias Doenitz
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.parboiled;
 
 import com.google.common.collect.ImmutableList;
 import org.jetbrains.annotations.NotNull;
 import org.parboiled.common.Reference;
 import org.parboiled.common.StringUtils;
 import org.parboiled.errors.BasicParseError;
 import org.parboiled.errors.ParseError;
 import org.parboiled.errors.ParserRuntimeException;
 import org.parboiled.matchers.ActionMatcher;
 import org.parboiled.matchers.Matcher;
 import org.parboiled.matchers.ProxyMatcher;
 import org.parboiled.matchers.TestMatcher;
 import org.parboiled.support.*;
 
import java.util.ArrayList;
 import java.util.List;
 
 import static org.parboiled.errors.ErrorUtils.printParseError;
 import static org.parboiled.support.ParseTreeUtils.findNode;
 import static org.parboiled.support.ParseTreeUtils.findNodeByPath;
 
 /**
  * <p>The Context implementation orchestrating most of the matching process.</p>
  * <p>The parsing process works as following:</br>
  * After the rule tree (which is in fact a directed and potentially even cyclic graph of {@link Matcher} instances)
  * has been created a root MatcherContext is instantiated for the root rule (Matcher).
  * A subsequent call to {@link #runMatcher()} starts the parsing process.</p>
  * <p>The MatcherContext delegates to a given {@link MatchHandler} to call {@link Matcher#match(MatcherContext)},
  * passing itself to the Matcher which executes its logic, potentially calling sub matchers.
  * For each sub matcher the matcher creates/initializes a sub context with {@link #getSubContext(org.parboiled.matchers.Matcher)}
  * and then calls {@link #runMatcher()} on it.</p>
  * <p>This basically creates a stack of MatcherContexts, each corresponding to their rule matchers. The MatcherContext
  * instances serve as companion objects to the matchers, providing them with support for building the
  * parse tree nodes, keeping track of input locations and error recovery.</p>
  * <p>At each point during the parsing process the matchers and action expressions have access to the current
  * MatcherContext and all "open" parent MatcherContexts through the {@link #getParent()} chain.</p>
  * <p>For performance reasons sub context instances are reused instead of being recreated. If a MatcherContext instance
  * returns null on a {@link #getMatcher()} call it has been retired (is invalid) and is waiting to be reinitialized
  * with a new Matcher by its parent</p>
  *
  * @param <V> the node value type
  */
 public class MatcherContext<V> implements Context<V> {
 
     private final InputBuffer inputBuffer;
     private final List<ParseError> parseErrors;
     private final MatchHandler<V> matchHandler;
     private final Reference<Node<V>> lastNodeRef;
     private final MatcherContext<V> parent;
     private final int level;
 
     private MatcherContext<V> subContext;
     private InputLocation startLocation;
     private InputLocation currentLocation;
     private Matcher<V> matcher;
     private Node<V> node;
     private List<Node<V>> subNodes = ImmutableList.of();
     private V nodeValue;
     private boolean hasError;
     private int intTag;
     private boolean belowLeafLevel;
 
     public MatcherContext(@NotNull InputBuffer inputBuffer, @NotNull InputLocation startLocation,
                           @NotNull List<ParseError> parseErrors, @NotNull MatchHandler<V> matchHandler,
                           @NotNull Matcher<V> matcher) {
         this(inputBuffer, parseErrors, matchHandler, new Reference<Node<V>>(), null, 0);
         this.startLocation = startLocation;
         this.currentLocation = startLocation;
         this.matcher = ProxyMatcher.unwrap(matcher);
     }
 
     private MatcherContext(@NotNull InputBuffer inputBuffer, @NotNull List<ParseError> parseErrors,
                            @NotNull MatchHandler<V> matchHandler, @NotNull Reference<Node<V>> lastNodeRef,
                            MatcherContext<V> parent, int level) {
         this.inputBuffer = inputBuffer;
         this.parseErrors = parseErrors;
         this.matchHandler = matchHandler;
         this.lastNodeRef = lastNodeRef;
         this.parent = parent;
         this.level = level;
     }
 
     @Override
     public String toString() {
         return getPath().toString();
     }
 
     //////////////////////////////// CONTEXT INTERFACE ////////////////////////////////////
 
     public MatcherContext<V> getParent() {
         return parent;
     }
 
     public MatcherContext<V> getSubContext() {
         // if the subContext has a null matcher it has been retired and is invalid
         return subContext != null && subContext.matcher != null ? subContext : null;
     }
 
     @NotNull
     public InputBuffer getInputBuffer() {
         return inputBuffer;
     }
 
     public InputLocation getStartLocation() {
         return startLocation;
     }
 
     public Matcher<V> getMatcher() {
         return matcher;
     }
 
     @NotNull
     public List<ParseError> getParseErrors() {
         return parseErrors;
     }
 
     public InputLocation getCurrentLocation() {
         return currentLocation;
     }
 
     public String getNodeText(Node<V> node) {
         return ParseTreeUtils.getNodeText(node, inputBuffer);
     }
 
     @NotNull
     public MatcherPath<V> getPath() {
         return new MatcherPath<V>(this);
     }
 
     public int getLevel() {
         return level;
     }
 
     public V getNodeValue() {
         return nodeValue;
     }
 
     public void setNodeValue(V value) {
         this.nodeValue = value;
     }
 
     public V getTreeValue() {
         V treeValue = nodeValue;
         int i = subNodes.size();
         while (treeValue == null && i-- > 0) {
             treeValue = subNodes.get(i).getValue();
         }
         return treeValue;
     }
 
     public Node<V> getNodeByPath(String path) {
         return findNodeByPath(subNodes, path);
     }
 
     public Node<V> getNodeByLabel(String labelPrefix) {
        return findNode(subNodes, new LabelPrefixPredicate<V>(labelPrefix));
     }
 
     public Node<V> getLastNode() {
         return lastNodeRef.getTarget();
     }
 
     @NotNull
     public List<Node<V>> getSubNodes() {
         return subNodes;
     }
 
     public char lookAhead(int delta) {
         return currentLocation.lookAhead(inputBuffer, delta);
     }
 
     public boolean inPredicate() {
         return matcher instanceof TestMatcher || parent != null && parent.inPredicate();
     }
 
     public boolean isBelowLeafLevel() {
         return belowLeafLevel;
     }
 
     public boolean hasError() {
         return hasError;
     }
 
     //////////////////////////////// PUBLIC ////////////////////////////////////
 
     public void setStartLocation(InputLocation startLocation) {
         this.startLocation = startLocation;
     }
 
     public void setCurrentLocation(InputLocation currentLocation) {
         this.currentLocation = currentLocation;
     }
 
     public void advanceInputLocation() {
         setCurrentLocation(currentLocation.advance(inputBuffer));
     }
 
     public Node<V> getNode() {
         return node;
     }
 
     public int getIntTag() {
         return intTag;
     }
 
     public void setIntTag(int intTag) {
         this.intTag = intTag;
     }
 
     public void markError() {
         hasError = true;
         if (parent != null) parent.markError();
     }
 
     public void clearBelowLeafLevelMarker() {
         belowLeafLevel = false;
         if (parent != null) parent.clearBelowLeafLevelMarker();
     }
 
     public void createNode() {
         if (belowLeafLevel || matcher instanceof TestMatcher) {
             return;
         }
         node = new NodeImpl<V>(matcher, subNodes, startLocation, currentLocation, getTreeValue(), hasError);
         if (parent != null) parent.addChildNode(node);
         lastNodeRef.setTarget(node);
     }
 
    @SuppressWarnings({"fallthrough"})
     public void addChildNode(@NotNull Node<V> node) {
         switch (subNodes.size()) {
             case 0:
                 subNodes = ImmutableList.of(node);
                 break;
             case 1:
                 subNodes = ImmutableList.of(subNodes.get(0), node);
                 break;
             case 2:
                 subNodes = ImmutableList.of(subNodes.get(0), subNodes.get(1), node);
                 break;
             case 3:
                 subNodes = ImmutableList.of(subNodes.get(0), subNodes.get(1), subNodes.get(2), node);
                 break;
             case 4:
                Node<V> node0 = subNodes.get(0);
                Node<V> node1 = subNodes.get(1);
                Node<V> node2 = subNodes.get(2);
                Node<V> node3 = subNodes.get(3);
                subNodes = new ArrayList<Node<V>>();
                subNodes.add(node0);
                subNodes.add(node1);
                subNodes.add(node2);
                subNodes.add(node3);
                // fall-through
             default:
                subNodes.add(node);
                 break;
         }
     }
 
     public MatcherContext<V> getSubContext(Matcher<V> matcher) {
         if (subContext == null) {
            // introduce a new level
             subContext = new MatcherContext<V>(inputBuffer, parseErrors, matchHandler, lastNodeRef, this, level + 1);
         }
 
        // normally just reuse the existing subContext instance
         subContext.matcher = ProxyMatcher.unwrap(matcher);
         subContext.startLocation = currentLocation;
         subContext.currentLocation = currentLocation;
         subContext.node = null;
         subContext.subNodes = ImmutableList.of();
         subContext.nodeValue = null;
         subContext.belowLeafLevel = belowLeafLevel || this.matcher.isLeaf();
         subContext.hasError = false;
         return subContext;
     }
 
     public boolean runMatcher() {
         try {
             if (matchHandler.match(this)) {
                 if (parent != null) parent.setCurrentLocation(currentLocation);
                 matcher = null; // "retire" this context
                 return true;
             }
             matcher = null; // "retire" this context until is "activated" again by a getSubContext(...) on the parent
             return false;
         } catch (ParserRuntimeException e) {
             throw e; // don't wrap, just bubble up
         } catch (Throwable e) {
             throw new ParserRuntimeException(e,
                     printParseError(new BasicParseError(currentLocation,
                             StringUtils.escape(String.format("Error while parsing %s '%s' at input position",
                                     matcher instanceof ActionMatcher ? "action" : "rule", getPath()))), inputBuffer));
         }
     }
 }
