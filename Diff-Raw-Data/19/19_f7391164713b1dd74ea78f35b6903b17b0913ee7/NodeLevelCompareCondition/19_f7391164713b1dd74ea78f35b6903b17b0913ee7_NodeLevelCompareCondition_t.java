 /*
  *  Freeplane - mind map editor
  *  Copyright (C) 2008 Dimitry Polivaev
  *
  *  This file author is Dimitry Polivaev
  *
  *  This program is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 2 of the License, or
  *  (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.freeplane.features.common.map;
 
 import org.freeplane.core.io.xml.TreeXmlReader;
 import org.freeplane.core.io.xml.TreeXmlWriter;
 import org.freeplane.core.util.TextUtils;
 import org.freeplane.features.common.filter.condition.CompareConditionAdapter;
 import org.freeplane.features.common.filter.condition.ASelectableCondition;
 import org.freeplane.features.common.map.NodeModel;
 import org.freeplane.n3.nanoxml.XMLElement;
 
 class NodeLevelCompareCondition extends CompareConditionAdapter {
 	static final String COMPARATION_RESULT = "COMPARATION_RESULT";
 	static final String NAME = "node_level_condition";
 	static final String SUCCEED = "SUCCEED";
 	static final String VALUE = "VALUE";
 
 	static ASelectableCondition load(final XMLElement element) {
 		return new NodeLevelCompareCondition(element.getAttribute(NodeLevelCompareCondition.VALUE, null), TreeXmlReader
 		    .xmlToBoolean(element.getAttribute(CompareConditionAdapter.MATCH_CASE, null)), Integer.parseInt(element
 		    .getAttribute(NodeLevelCompareCondition.COMPARATION_RESULT, null)), TreeXmlReader.xmlToBoolean(element
 		    .getAttribute(NodeLevelCompareCondition.SUCCEED, null)));
 	}
 
 	final private int comparationResult;
 	final private boolean succeed;
 
 	NodeLevelCompareCondition(final String value, final boolean ignoreCase, final int comparationResult,
 	                     final boolean succeed) {
		super(Long.valueOf(value));
 		this.comparationResult = comparationResult;
 		this.succeed = succeed;
 	}
 
 	public boolean checkNode(final NodeModel node) {
 		final long level = node.getNodeLevel(true);
 		return succeed == (compareTo(level) == comparationResult);
 	}
 
 	@SuppressWarnings("unused")
 	private boolean checkLevel(final long level) {
 		return succeed == (compareTo(level) == comparationResult);
 	}
 
 	@Override
 	protected String createDescription() {
 		final String nodeCondition = TextUtils.getText(NodeLevelConditionController.FILTER_LEVEL);
 		return super.createDescription(nodeCondition, comparationResult, succeed);
 	}
 
 	public void fillXML(final XMLElement child) {
 		super.fillXML(child);
 		child.setAttribute(NodeLevelCompareCondition.COMPARATION_RESULT, Integer.toString(comparationResult));
 		child.setAttribute(NodeLevelCompareCondition.SUCCEED, TreeXmlWriter.BooleanToXml(succeed));
 	}
 
 	@Override
     protected String getName() {
 	    return NAME;
     }
 }
