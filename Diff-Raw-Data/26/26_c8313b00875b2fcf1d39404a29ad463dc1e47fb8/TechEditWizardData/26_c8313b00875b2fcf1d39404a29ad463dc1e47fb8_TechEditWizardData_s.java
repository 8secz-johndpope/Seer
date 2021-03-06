 /* -*- tab-width: 4 -*-
  *
  * Electric(tm) VLSI Design System
  *
  * File: TechEditWizardData.java
  * Create an Electric XML Technology from a simple numeric description of design rules
  * Written in Perl by Andrew Wewist, translated to Java by Steven Rubin.
  *
  * Copyright (c) 2008 Sun Microsystems and Static Free Software
  *
  * Electric(tm) is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 3 of the License, or
  * (at your option) any later version.
  *
  * Electric(tm) is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Electric(tm); see the file COPYING.  If not, write to
  * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  * Boston, Mass 02111-1307, USA.
  */
 package com.sun.electric.tool.user.tecEditWizard;
 
 import com.sun.electric.database.text.TextUtils;
 import com.sun.electric.database.geometry.DBMath;
 import com.sun.electric.tool.Job;
 import com.sun.electric.tool.io.FileType;
 import com.sun.electric.tool.user.dialogs.OpenFile;
 
 import java.awt.Color;
 import java.io.BufferedWriter;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.LineNumberReader;
 import java.io.PrintWriter;
 import java.net.URL;
 import java.net.URLConnection;
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * Class to handle the "Technology Creation Wizard" dialog.
  */
 public class TechEditWizardData
 {
 	/************************************** THE DATA **************************************/
 
 	private String tech_name;
 	private String tech_description;
 	private int num_metal_layers;
 	private int stepsize;
 
 	// DIFFUSION RULES
 	private WizardField diff_width = new WizardField();
 	private WizardField diff_poly_overhang = new WizardField();		// min. diff overhang from gate edge
 	private WizardField diff_contact_overhang = new WizardField();	// min. diff overhang contact
 	private WizardField diff_spacing = new WizardField();
 
 	// POLY RULES
 	private WizardField poly_width = new WizardField();
 	private WizardField poly_endcap = new WizardField();			// min. poly gate extension from edge of diffusion
 	private WizardField poly_spacing = new WizardField();
 	private WizardField poly_diff_spacing = new WizardField();		// min. spacing between poly and diffusion
 
 	// GATE RULES
 	private WizardField gate_length = new WizardField();			// min. transistor gate length
 	private WizardField gate_width = new WizardField();				// min. transistor gate width
 	private WizardField gate_spacing = new WizardField();			// min. gate to gate spacing on diffusion
 	private WizardField gate_contact_spacing = new WizardField();	// min. spacing from gate edge to contact inside diffusion
 
 	// CONTACT RULES
 	private WizardField contact_size = new WizardField();
 	private WizardField contact_spacing = new WizardField();
 	private WizardField contact_metal_overhang_inline_only = new WizardField();	// metal overhang when overhanging contact from two sides only
 	private WizardField contact_metal_overhang_all_sides = new WizardField();	// metal overhang when surrounding contact
 	private WizardField contact_poly_overhang = new WizardField();				// poly overhang contact
 	private WizardField polycon_diff_spacing = new WizardField();				// spacing between poly-metal contact edge and diffusion
 
 	// WELL AND IMPLANT RULES
 	private WizardField nplus_width = new WizardField();
 	private WizardField nplus_overhang_diff = new WizardField();
 	private WizardField nplus_spacing = new WizardField();
 
 	private WizardField pplus_width = new WizardField();
 	private WizardField pplus_overhang_diff = new WizardField();
 	private WizardField pplus_spacing = new WizardField();
 
 	private WizardField nwell_width = new WizardField();
 	private WizardField nwell_overhang_diff = new WizardField();
 	private WizardField nwell_spacing = new WizardField();
 
 	// METAL RULES
 	private WizardField [] metal_width;
 	private WizardField [] metal_spacing;
 
 	// VIA RULES
 	private WizardField [] via_size;
 	private WizardField [] via_spacing;
 	private WizardField [] via_array_spacing;
 	private WizardField [] via_overhang_inline;
 
 	// ANTENNA RULES
 	private double poly_antenna_ratio;
 	private double [] metal_antenna_ratio;
 
 	// GDS-II LAYERS
 	private int gds_diff_layer;
 	private int gds_poly_layer;
 	private int gds_nplus_layer;
 	private int gds_pplus_layer;
 	private int gds_nwell_layer;
 	private int gds_contact_layer;
 	private int [] gds_metal_layer;
 	private int [] gds_via_layer;
 	private int gds_marking_layer;		// Device marking layer
 
 	TechEditWizardData()
 	{
 		stepsize = 100;
 		num_metal_layers = 2;
 		metal_width = new WizardField[num_metal_layers];
 		metal_spacing = new WizardField[num_metal_layers];
 		via_size = new WizardField[num_metal_layers-1];
 		via_spacing = new WizardField[num_metal_layers-1];
 		via_array_spacing = new WizardField[num_metal_layers-1];
 		via_overhang_inline = new WizardField[num_metal_layers-1];
 		metal_antenna_ratio = new double[num_metal_layers];
 		gds_metal_layer = new int[num_metal_layers];
 		gds_via_layer = new int[num_metal_layers-1];
 		for(int i=0; i<num_metal_layers; i++)
 		{
 			metal_width[i] = new WizardField();
 			metal_spacing[i] = new WizardField();
 		}
 		for(int i=0; i<num_metal_layers-1; i++)
 		{
 			via_size[i] = new WizardField();
 			via_spacing[i] = new WizardField();
 			via_array_spacing[i] = new WizardField();
 			via_overhang_inline[i] = new WizardField();
 		}
 	}
 
 	/************************************** ACCESSOR METHODS **************************************/
 
 	public String getTechName() { return tech_name; }
 	public void setTechName(String s) { tech_name = s; }
 
 	public String getTechDescription() { return tech_description; }
 	public void setTechDescription(String s) { tech_description = s; }
 
 	public int getStepSize() { return stepsize; }
 	public void setStepSize(int n) { stepsize = n; }
 
 	public int getNumMetalLayers() { return num_metal_layers; }
 	public void setNumMetalLayers(int n)
 	{
 		int smallest = Math.min(n, num_metal_layers);
 
 		WizardField [] new_metal_width = new WizardField[n];
 		for(int i=0; i<smallest; i++) new_metal_width[i] = metal_width[i];
 		for(int i=smallest; i<n; i++) new_metal_width[i] = new WizardField();
 		metal_width = new_metal_width;
 
 		WizardField [] new_metal_spacing = new WizardField[n];
 		for(int i=0; i<smallest; i++) new_metal_spacing[i] = metal_spacing[i];
 		for(int i=smallest; i<n; i++) new_metal_spacing[i] = new WizardField();
 		metal_spacing = new_metal_spacing;
 
 		WizardField [] new_via_size = new WizardField[n-1];
 		for(int i=0; i<smallest-1; i++) new_via_size[i] = via_size[i];
 		for(int i=smallest-1; i<n-1; i++) new_via_size[i] = new WizardField();
 		via_size = new_via_size;
 
 		WizardField [] new_via_spacing = new WizardField[n-1];
 		for(int i=0; i<smallest-1; i++) new_via_spacing[i] = via_spacing[i];
 		for(int i=smallest-1; i<n-1; i++) new_via_spacing[i] = new WizardField();
 		via_spacing = new_via_spacing;
 
 		WizardField [] new_via_array_spacing = new WizardField[n-1];
 		for(int i=0; i<smallest-1; i++) new_via_array_spacing[i] = via_array_spacing[i];
 		for(int i=smallest-1; i<n-1; i++) new_via_array_spacing[i] = new WizardField();
 		via_array_spacing = new_via_array_spacing;
 
 		WizardField [] new_via_overhang_inline = new WizardField[n-1];
 		for(int i=0; i<smallest-1; i++) new_via_overhang_inline[i] = via_overhang_inline[i];
 		for(int i=smallest-1; i<n-1; i++) new_via_overhang_inline[i] = new WizardField();
 		via_overhang_inline = new_via_overhang_inline;
 
 		double [] new_metal_antenna_ratio = new double[n];
 		for(int i=0; i<smallest; i++) new_metal_antenna_ratio[i] = metal_antenna_ratio[i];
 		metal_antenna_ratio = new_metal_antenna_ratio;
 
 		int [] new_gds_metal_layer = new int[n];
 		for(int i=0; i<smallest; i++) new_gds_metal_layer[i] = gds_metal_layer[i];
 		gds_metal_layer = new_gds_metal_layer;
 
 		int [] new_gds_via_layer = new int[n-1];
 		for(int i=0; i<smallest-1; i++) new_gds_via_layer[i] = gds_via_layer[i];
 		gds_via_layer = new_gds_via_layer;
 
 		num_metal_layers = n;
 	}
 
 	// DIFFUSION RULES
 	public WizardField getDiffWidth() { return diff_width; }
 	public void setDiffWidth(WizardField v) { diff_width = v; }
 	public WizardField getDiffPolyOverhang() { return diff_poly_overhang; }
 	public void setDiffPolyOverhang(WizardField v) { diff_poly_overhang = v; }
 	public WizardField getDiffContactOverhang() { return diff_contact_overhang; }
 	public void setDiffContactOverhang(WizardField v) { diff_contact_overhang = v; }
 	public WizardField getDiffSpacing() { return diff_spacing; }
 	public void setDiffSpacing(WizardField v) { diff_spacing = v; }
 
 	// POLY RULES
 	public WizardField getPolyWidth() { return poly_width; }
 	public void setPolyWidth(WizardField v) { poly_width = v; }
 	public WizardField getPolyEndcap() { return poly_endcap; }
 	public void setPolyEndcap(WizardField v) { poly_endcap = v; }
 	public WizardField getPolySpacing() { return poly_spacing; }
 	public void setPolySpacing(WizardField v) { poly_spacing = v; }
 	public WizardField getPolyDiffSpacing() { return poly_diff_spacing; }
 	public void setPolyDiffSpacing(WizardField v) { poly_diff_spacing = v; }
 
 	// GATE RULES
 	public WizardField getGateLength() { return gate_length; }
 	public void setGateLength(WizardField v) { gate_length = v; }
 	public WizardField getGateWidth() { return gate_width; }
 	public void setGateWidth(WizardField v) { gate_width = v; }
 	public WizardField getGateSpacing() { return gate_spacing; }
 	public void setGateSpacing(WizardField v) { gate_spacing = v; }
 	public WizardField getGateContactSpacing() { return gate_contact_spacing; }
 	public void setGateContactSpacing(WizardField v) { gate_contact_spacing = v; }
 
 	// CONTACT RULES
 	public WizardField getContactSize() { return contact_size; }
 	public void setContactSize(WizardField v) { contact_size = v; }
 	public WizardField getContactSpacing() { return contact_spacing; }
 	public void setContactSpacing(WizardField v) { contact_spacing = v; }
 	public WizardField getContactMetalOverhangInlineOnly() { return contact_metal_overhang_inline_only; }
 	public void setContactMetalOverhangInlineOnly(WizardField v) { contact_metal_overhang_inline_only = v; }
 	public WizardField getContactMetalOverhangAllSides() { return contact_metal_overhang_all_sides; }
 	public void setContactMetalOverhangAllSides(WizardField v) { contact_metal_overhang_all_sides = v; }
 	public WizardField getContactPolyOverhang() { return contact_poly_overhang; }
 	public void setContactPolyOverhang(WizardField v) { contact_poly_overhang = v; }
 	public WizardField getPolyconDiffSpacing() { return polycon_diff_spacing; }
 	public void setPolyconDiffSpacing(WizardField v) { polycon_diff_spacing = v; }
 
 	// WELL AND IMPLANT RULES
 	public WizardField getNPlusWidth() { return nplus_width; }
 	public void setNPlusWidth(WizardField v) { nplus_width = v; }
 	public WizardField getNPlusOverhangDiff() { return nplus_overhang_diff; }
 	public void setNPlusOverhangDiff(WizardField v) { nplus_overhang_diff = v; }
 	public WizardField getNPlusSpacing() { return nplus_spacing; }
 	public void setNPlusSpacing(WizardField v) { nplus_spacing = v; }
 
 	public WizardField getPPlusWidth() { return pplus_width; }
 	public void setPPlusWidth(WizardField v) { pplus_width = v; }
 	public WizardField getPPlusOverhangDiff() { return pplus_overhang_diff; }
 	public void setPPlusOverhangDiff(WizardField v) { pplus_overhang_diff = v; }
 	public WizardField getPPlusSpacing() { return pplus_spacing; }
 	public void setPPlusSpacing(WizardField v) { pplus_spacing = v; }
 
 	public WizardField getNWellWidth() { return nwell_width; }
 	public void setNWellWidth(WizardField v) { nwell_width = v; }
 	public WizardField getNWellOverhangDiff() { return nwell_overhang_diff; }
 	public void setNWellOverhangDiff(WizardField v) { nwell_overhang_diff = v; }
 	public WizardField getNWellSpacing() { return nwell_spacing; }
 	public void setNWellSpacing(WizardField v) { nwell_spacing = v; }
 
 	// METAL RULES
 	public WizardField [] getMetalWidth() { return metal_width; }
 	public void setMetalWidth(int met, WizardField value) { metal_width[met] = value; }
 	public WizardField [] getMetalSpacing() { return metal_spacing; }
 	public void setMetalSpacing(int met, WizardField value) { metal_spacing[met] = value; }
 
 	// VIA RULES
 	public WizardField [] getViaSize() { return via_size; }
 	public void setViaSize(int via, WizardField value) { via_size[via] = value; }
 	public WizardField [] getViaSpacing() { return via_spacing; }
 	public void setViaSpacing(int via, WizardField value) { via_spacing[via] = value; }
 	public WizardField [] getViaArraySpacing() { return via_array_spacing; }
 	public void setViaArraySpacing(int via, WizardField value) { via_array_spacing[via] = value; }
 	public WizardField [] getViaOverhangInline() { return via_overhang_inline; }
 	public void setViaOverhangInline(int via, WizardField value) { via_overhang_inline[via] = value; }
 
 	// ANTENNA RULES
 	public double getPolyAntennaRatio() { return poly_antenna_ratio; }
 	public void setPolyAntennaRatio(double v) { poly_antenna_ratio = v; }
 	public double [] getMetalAntennaRatio() { return metal_antenna_ratio; }
 	public void setMetalAntennaRatio(int met, double value) { metal_antenna_ratio[met] = value; }
 
 	// GDS-II LAYERS
 	public int getGDSDiff() { return gds_diff_layer; }
 	public void setGDSDiff(int l) { gds_diff_layer = l; }
 	public int getGDSPoly() { return gds_poly_layer; }
 	public void setGDSPoly(int l) { gds_poly_layer = l; }
 	public int getGDSNPlus() { return gds_nplus_layer; }
 	public void setGDSNPlus(int l) { gds_nplus_layer = l; }
 	public int getGDSPPlus() { return gds_pplus_layer; }
 	public void setGDSPPlus(int l) { gds_pplus_layer = l; }
 	public int getGDSNWell() { return gds_nwell_layer; }
 	public void setGDSNWell(int l) { gds_nwell_layer = l; }
 	public int getGDSContact() { return gds_contact_layer; }
 	public void setGDSContact(int l) { gds_contact_layer = l; }
 	public int [] getGDSMetal() { return gds_metal_layer; }
 	public void setGDSMetal(int met, int l) { gds_metal_layer[met] = l; }
 	public int [] getGDSVia() { return gds_via_layer; }
 	public void setGDSVia(int via, int l) { gds_via_layer[via] = l; }
 	public int getGDSMarking() { return gds_marking_layer; }
 	public void setGDSMarking(int l) { gds_marking_layer = l; }
 
 	public String errorInData()
 	{
 		// check the General data
 		if (tech_name == null || tech_name.length() == 0) return "General panel: No technology name";
 		if (stepsize == 0) return "General panel: Invalid unit size";
 
 		// check the Active data
 		if (diff_width.v == 0) return "Active panel: Invalid width";
 
 		// check the Poly data
 		if (poly_width.v == 0) return "Poly panel: Invalid width";
 
 		// check the Gate data
 		if (gate_width.v == 0) return "Gate panel: Invalid width";
 		if (gate_length.v == 0) return "Gate panel: Invalid length";
 
 		// check the Contact data
 		if (contact_size.v == 0) return "Contact panel: Invalid size";
 
 		// check the Well/Implant data
 		if (nplus_width.v == 0) return "Well/Implant panel: Invalid NPlus width";
 		if (pplus_width.v == 0) return "Well/Implant panel: Invalid PPlus width";
 		if (nwell_width.v == 0) return "Well/Implant panel: Invalid NWell width";
 
 		// check the Metal data
 		for(int i=0; i<num_metal_layers; i++)
 			if (metal_width[i].v == 0) return "Metal panel: Invalid Metal-" + (i+1) + " width";
 
 		// check the Via data
 		for(int i=0; i<num_metal_layers-1; i++)
 			if (via_size[i].v == 0) return "Via panel: Invalid Via-" + (i+1) + " size";
 		return null;
 	}
 
 	/************************************** IMPORT RAW NUMBERS FROM DISK **************************************/
 
 	/**
 	 * Method to import data from a file to this object.
 	 * @return true on success; false on failure.
 	 */
 	public boolean importData()
 	{
 		String fileName = OpenFile.chooseInputFile(FileType.ANY, "Technology Wizard File");
 		if (fileName == null) return false;
 		URL url = TextUtils.makeURLToFile(fileName);
 		try
 		{
 			URLConnection urlCon = url.openConnection();
 			InputStreamReader is = new InputStreamReader(urlCon.getInputStream());
 			LineNumberReader lineReader = new LineNumberReader(is);
 			for(;;)
 			{
 				String buf = lineReader.readLine();
 				if (buf == null) break;
 				buf = buf.trim();
 				if (buf.length() == 0 || buf.startsWith("#")) continue;
 
 				// parse the assignment
 				if (buf.startsWith("$") || buf.startsWith("@"))
 				{
 					int spacePos = buf.indexOf(' ');
 					int equalsPos = buf.indexOf('=');
 					if (equalsPos < 0)
 					{
 						Job.getUserInterface().showErrorMessage("Missing '=' on line " + lineReader.getLineNumber(),
 							"Syntax Error In Technology File");
 						break;
 					}
 					if (spacePos < 0) spacePos = equalsPos; else
 						spacePos = Math.min(spacePos, equalsPos);
 					String varName = buf.substring(1, spacePos);
 
 					int semiPos = buf.indexOf(';');
 					if (semiPos < 0)
 					{
 						Job.getUserInterface().showErrorMessage("Missing ';' on line " + lineReader.getLineNumber(),
 							"Syntax Error In Technology File");
 						break;
 					}
 					equalsPos++;
 					while (equalsPos < semiPos && buf.charAt(equalsPos) == ' ') equalsPos++;
 					String varValue = buf.substring(equalsPos, semiPos);
 
 					// now figure out what to assign
 					if (varName.equalsIgnoreCase("tech_libname")) { } else
 					if (varName.equalsIgnoreCase("tech_name")) setTechName(stripQuotes(varValue)); else
 					if (varName.equalsIgnoreCase("tech_description")) setTechDescription(stripQuotes(varValue)); else
 					if (varName.equalsIgnoreCase("num_metal_layers")) setNumMetalLayers(TextUtils.atoi(varValue)); else
 					if (varName.equalsIgnoreCase("stepsize")) setStepSize(TextUtils.atoi(varValue)); else
 
 					if (varName.equalsIgnoreCase("diff_width")) diff_width.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("diff_width_rule")) diff_width.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("diff_poly_overhang")) diff_poly_overhang.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("diff_poly_overhang_rule")) diff_poly_overhang.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("diff_contact_overhang")) diff_contact_overhang.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("diff_contact_overhang_rule")) diff_contact_overhang.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("diff_spacing")) diff_spacing.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("diff_spacing_rule")) diff_spacing.rule = stripQuotes(varValue); else
 
 					if (varName.equalsIgnoreCase("poly_width")) poly_width.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("poly_width_rule")) poly_width.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("poly_endcap")) poly_endcap.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("poly_endcap_rule")) poly_endcap.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("poly_spacing")) poly_spacing.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("poly_spacing_rule")) poly_spacing.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("poly_diff_spacing")) poly_diff_spacing.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("poly_diff_spacing_rule")) poly_diff_spacing.rule = stripQuotes(varValue); else
 
 					if (varName.equalsIgnoreCase("gate_length")) gate_length.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("gate_length_rule")) gate_length.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("gate_width")) gate_width.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("gate_width_rule")) gate_width.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("gate_spacing")) gate_spacing.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("gate_spacing_rule")) gate_spacing.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("gate_contact_spacing")) gate_contact_spacing.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("gate_contact_spacing_rule")) gate_contact_spacing.rule = stripQuotes(varValue); else
 
 					if (varName.equalsIgnoreCase("contact_size")) contact_size.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("contact_size_rule")) contact_size.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("contact_spacing")) contact_spacing.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("contact_spacing_rule")) contact_spacing.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("contact_metal_overhang_inline_only")) contact_metal_overhang_inline_only.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("contact_metal_overhang_inline_only_rule")) contact_metal_overhang_inline_only.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("contact_metal_overhang_all_sides")) contact_metal_overhang_all_sides.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("contact_metal_overhang_all_sides_rule")) contact_metal_overhang_all_sides.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("contact_poly_overhang")) contact_poly_overhang.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("contact_poly_overhang_rule")) contact_poly_overhang.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("polycon_diff_spacing")) polycon_diff_spacing.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("polycon_diff_spacing_rule")) polycon_diff_spacing.rule = stripQuotes(varValue); else
 
 					if (varName.equalsIgnoreCase("nplus_width")) nplus_width.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("nplus_width_rule")) nplus_width.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("nplus_overhang_diff")) nplus_overhang_diff.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("nplus_overhang_diff_rule")) nplus_overhang_diff.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("nplus_spacing")) nplus_spacing.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("nplus_spacing_rule")) nplus_spacing.rule = stripQuotes(varValue); else
 
 					if (varName.equalsIgnoreCase("pplus_width")) pplus_width.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("pplus_width_rule")) pplus_width.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("pplus_overhang_diff")) pplus_overhang_diff.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("pplus_overhang_diff_rule")) pplus_overhang_diff.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("pplus_spacing")) pplus_spacing.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("pplus_spacing_rule")) pplus_spacing.rule = stripQuotes(varValue); else
 
 					if (varName.equalsIgnoreCase("nwell_width")) nwell_width.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("nwell_width_rule")) nwell_width.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("nwell_overhang_diff")) nwell_overhang_diff.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("nwell_overhang_diff_rule")) nwell_overhang_diff.rule = stripQuotes(varValue); else
 					if (varName.equalsIgnoreCase("nwell_spacing")) nwell_spacing.v = TextUtils.atof(varValue); else
 					if (varName.equalsIgnoreCase("nwell_spacing_rule")) nwell_spacing.rule = stripQuotes(varValue); else
 
 					if (varName.equalsIgnoreCase("metal_width")) fillWizardArray(varValue, metal_width, num_metal_layers, false); else
 					if (varName.equalsIgnoreCase("metal_width_rule")) fillWizardArray(varValue, metal_width, num_metal_layers, true); else
 					if (varName.equalsIgnoreCase("metal_spacing")) fillWizardArray(varValue, metal_spacing, num_metal_layers, false); else
 					if (varName.equalsIgnoreCase("metal_spacing_rule")) fillWizardArray(varValue, metal_spacing, num_metal_layers, true); else
 					if (varName.equalsIgnoreCase("via_size")) fillWizardArray(varValue, via_size, num_metal_layers-1, false); else
 					if (varName.equalsIgnoreCase("via_size_rule")) fillWizardArray(varValue, via_size, num_metal_layers-1, true); else
 					if (varName.equalsIgnoreCase("via_spacing")) fillWizardArray(varValue, via_spacing, num_metal_layers-1, false); else
 					if (varName.equalsIgnoreCase("via_spacing_rule")) fillWizardArray(varValue, via_spacing, num_metal_layers-1, true); else
 					if (varName.equalsIgnoreCase("via_array_spacing")) fillWizardArray(varValue, via_array_spacing, num_metal_layers-1, false); else
 					if (varName.equalsIgnoreCase("via_array_spacing_rule")) fillWizardArray(varValue, via_array_spacing, num_metal_layers-1, true); else
 					if (varName.equalsIgnoreCase("via_overhang_inline")) fillWizardArray(varValue, via_overhang_inline, num_metal_layers-1, false); else
 					if (varName.equalsIgnoreCase("via_overhang_inline_rule")) fillWizardArray(varValue, via_overhang_inline, num_metal_layers-1, true); else
 
 					if (varName.equalsIgnoreCase("poly_antenna_ratio")) setPolyAntennaRatio(TextUtils.atof(varValue)); else
 					if (varName.equalsIgnoreCase("metal_antenna_ratio")) metal_antenna_ratio = makeDoubleArray(varValue); else
 
 					if (varName.equalsIgnoreCase("gds_diff_layer")) setGDSDiff(TextUtils.atoi(varValue)); else
 					if (varName.equalsIgnoreCase("gds_poly_layer")) setGDSPoly(TextUtils.atoi(varValue)); else
 					if (varName.equalsIgnoreCase("gds_nplus_layer")) setGDSNPlus(TextUtils.atoi(varValue)); else
 					if (varName.equalsIgnoreCase("gds_pplus_layer")) setGDSPPlus(TextUtils.atoi(varValue)); else
 					if (varName.equalsIgnoreCase("gds_nwell_layer")) setGDSNWell(TextUtils.atoi(varValue)); else
 					if (varName.equalsIgnoreCase("gds_contact_layer")) setGDSContact(TextUtils.atoi(varValue)); else
 					if (varName.equalsIgnoreCase("gds_metal_layer")) gds_metal_layer = makeIntArray(varValue); else
 					if (varName.equalsIgnoreCase("gds_via_layer")) gds_via_layer = makeIntArray(varValue); else
 					if (varName.equalsIgnoreCase("gds_marking_layer")) setGDSMarking(TextUtils.atoi(varValue)); else
 					{
 						Job.getUserInterface().showErrorMessage("Unknown keyword '" + varName + "' on line " + lineReader.getLineNumber(),
 							"Syntax Error In Technology File");
 						break;
 					}
 				}
 			}
 			lineReader.close();
 		} catch (IOException e)
 		{
 			System.out.println("Error reading " + fileName);
 			return false;
 		}
 		return true;
 	}
 
 	private String stripQuotes(String str)
 	{
 		if (str.startsWith("\"") && str.endsWith("\""))
 			return str.substring(1, str.length()-1);
 		return str;
 	}
 
 	private int [] makeIntArray(String str)
 	{
 		WizardField [] foundArray = new WizardField[num_metal_layers];
 		for(int i=0; i<num_metal_layers; i++) foundArray[i] = new WizardField();
 		fillWizardArray(str, foundArray, num_metal_layers, false);
 		int [] retArray = new int[foundArray.length];
 		for(int i=0; i<foundArray.length; i++)
 			retArray[i] = (int)foundArray[i].v;
 		return retArray;
 	}
 
 	private double [] makeDoubleArray(String str)
 	{
 		WizardField [] foundArray = new WizardField[num_metal_layers];
 		for(int i=0; i<num_metal_layers; i++) foundArray[i] = new WizardField();
 		fillWizardArray(str, foundArray, num_metal_layers, false);
 		double [] retArray = new double[foundArray.length];
 		for(int i=0; i<foundArray.length; i++)
 			retArray[i] = foundArray[i].v;
 		return retArray;
 	}
 
 	private void fillWizardArray(String str, WizardField [] fieldArray, int expectedLength, boolean getRule)
 	{
 		if (!str.startsWith("("))
 		{
 			Job.getUserInterface().showErrorMessage("Array does not start with '(' on " + str,
 				"Syntax Error In Technology File");
 			return;
 		}
 
 		int pos = 1;
 		int index = 0;
 		for(;;)
 		{
 			while (pos < str.length() && str.charAt(pos) == ' ') pos++;
 			if (getRule)
 			{
 				if (str.charAt(pos) != '"')
 				{
 					Job.getUserInterface().showErrorMessage("Rule element does not start with quote on " + str,
 						"Syntax Error In Technology File");
 					return;
 				}
 				pos++;
 				int end = pos;
 				while (end < str.length() && str.charAt(end) != '"') end++;
 				if (str.charAt(end) != '"')
 				{
 					Job.getUserInterface().showErrorMessage("Rule element does not end with quote on " + str,
 						"Syntax Error In Technology File");
 					return;
 				}
 				fieldArray[index++].rule = str.substring(pos, end);
 				pos = end+1;
 			} else
 			{
 				double v = TextUtils.atof(str.substring(pos));
 				fieldArray[index++].v = v;
 			}
 			while (pos < str.length() && str.charAt(pos) != ',' && str.charAt(pos) != ')') pos++;
 			if (str.charAt(pos) != ',') break;
 			pos++;
 		}
 	}
 
 	/************************************** EXPORT RAW NUMBERS TO DISK **************************************/
 
 	public void exportData()
 	{
 		String fileName = OpenFile.chooseOutputFile(FileType.TEXT, "Technology Wizard File", "Technology.txt");
 		if (fileName == null) return;
 		try
 		{
 			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
 			dumpNumbers(printWriter);
 			printWriter.close();
 		} catch (IOException e)
 		{
 			System.out.println("Error writing XML file");
 			return;
 		}
 	}
 
 	private void dumpNumbers(PrintWriter pw)
 	{
 		pw.println("#### Technology wizard data file");
 		pw.println("####");
 		pw.println("#### All dimensions in nanometers.");
 		pw.println();
 		pw.println("$tech_name = \"" + tech_name + "\";");
 		pw.println("$tech_description = \"" + tech_description + "\";");
 		pw.println("$num_metal_layers = " + num_metal_layers + ";");
 		pw.println();
 		pw.println("## stepsize is minimum granularity that will be used as movement grid");
 		pw.println("## set to manufacturing grid or lowest common denominator with design rules");
 		pw.println("$stepsize = " + stepsize + ";");
 		pw.println();
 		pw.println("######  DIFFUSION RULES  #####");
 		pw.println("$diff_width = " + TextUtils.formatDouble(diff_width.v) + ";");
 		pw.println("$diff_width_rule = \"" + diff_width.rule + "\";");
 		pw.println("$diff_poly_overhang = " + TextUtils.formatDouble(diff_poly_overhang.v) + ";        # min. diff overhang from gate edge");
 		pw.println("$diff_poly_overhang_rule = \"" + diff_poly_overhang.rule + "\";        # min. diff overhang from gate edge");
 		pw.println("$diff_contact_overhang = " + TextUtils.formatDouble(diff_contact_overhang.v) + ";     # min. diff overhang contact");
 		pw.println("$diff_contact_overhang_rule = \"" + diff_contact_overhang.rule + "\";     # min. diff overhang contact");
 		pw.println("$diff_spacing = " + TextUtils.formatDouble(diff_spacing.v) + ";");
 		pw.println("$diff_spacing_rule = \"" + diff_spacing.rule + "\";");
 		pw.println();
 		pw.println("######  POLY RULES  #####");
 		pw.println("$poly_width = " + TextUtils.formatDouble(poly_width.v) + ";");
 		pw.println("$poly_width_rule = \"" + poly_width.rule + "\";");
 		pw.println("$poly_endcap = " + TextUtils.formatDouble(poly_endcap.v) + ";               # min. poly gate extension from edge of diffusion");
 		pw.println("$poly_endcap_rule = \"" + poly_endcap.rule + "\";               # min. poly gate extension from edge of diffusion");
 		pw.println("$poly_spacing = " + TextUtils.formatDouble(poly_spacing.v) + ";");
 		pw.println("$poly_spacing_rule = \"" + poly_spacing.rule + "\";");
 		pw.println("$poly_diff_spacing = " + TextUtils.formatDouble(poly_diff_spacing.v) + ";         # min. spacing between poly and diffusion");
 		pw.println("$poly_diff_spacing_rule = \"" + poly_diff_spacing.rule + "\";         # min. spacing between poly and diffusion");
 		pw.println();
 		pw.println("######  GATE RULES  #####");
 		pw.println("$gate_length = " + TextUtils.formatDouble(gate_length.v) + ";               # min. transistor gate length");
 		pw.println("$gate_length_rule = \"" + gate_length.rule + "\";               # min. transistor gate length");
 		pw.println("$gate_width = " + TextUtils.formatDouble(gate_width.v) + ";                # min. transistor gate width");
 		pw.println("$gate_width_rule = \"" + gate_width.rule + "\";                # min. transistor gate width");
 		pw.println("$gate_spacing = " + TextUtils.formatDouble(gate_spacing.v) + ";             # min. gate to gate spacing on diffusion");
 		pw.println("$gate_spacing_rule = \"" + gate_spacing.rule + "\";             # min. gate to gate spacing on diffusion");
 		pw.println("$gate_contact_spacing = " + TextUtils.formatDouble(gate_contact_spacing.v) + ";      # min. spacing from gate edge to contact inside diffusion");
 		pw.println("$gate_contact_spacing_rule = \"" + gate_contact_spacing.rule + "\";      # min. spacing from gate edge to contact inside diffusion");
 		pw.println();
 		pw.println("######  CONTACT RULES  #####");
 		pw.println("$contact_size = " + TextUtils.formatDouble(contact_size.v) + ";");
 		pw.println("$contact_size_rule = \"" + contact_size.rule + "\";");
 		pw.println("$contact_spacing = " + TextUtils.formatDouble(contact_spacing.v) + ";");
 		pw.println("$contact_spacing_rule = \"" + contact_spacing.rule + "\";");
 		pw.println("$contact_metal_overhang_inline_only = " + TextUtils.formatDouble(contact_metal_overhang_inline_only.v) + ";      # metal overhang when overhanging contact from two sides only");
 		pw.println("$contact_metal_overhang_inline_only_rule = \"" + contact_metal_overhang_inline_only.rule + "\";      # metal overhang when overhanging contact from two sides only");
 		pw.println("$contact_metal_overhang_all_sides = " + TextUtils.formatDouble(contact_metal_overhang_all_sides.v) + ";         # metal overhang when surrounding contact");
 		pw.println("$contact_metal_overhang_all_sides_rule = \"" + contact_metal_overhang_all_sides.rule + "\";         # metal overhang when surrounding contact");
 		pw.println("$contact_poly_overhang = " + TextUtils.formatDouble(contact_poly_overhang.v) + ";                    # poly overhang contact");
 		pw.println("$contact_poly_overhang_rule = \"" + contact_poly_overhang.rule + "\";                    # poly overhang contact");
 		pw.println("$polycon_diff_spacing = " + TextUtils.formatDouble(polycon_diff_spacing.v) + ";                    # spacing between poly-metal contact edge and diffusion");
 		pw.println("$polycon_diff_spacing_rule = \"" + polycon_diff_spacing.rule + "\";                    # spacing between poly-metal contact edge and diffusion");
 		pw.println();
 		pw.println("######  WELL AND IMPLANT RULES  #####");
 		pw.println("$nplus_width = " + TextUtils.formatDouble(nplus_width.v) + ";");
 		pw.println("$nplus_width_rule = \"" + nplus_width.rule + "\";");
 		pw.println("$nplus_overhang_diff = " + TextUtils.formatDouble(nplus_overhang_diff.v) + ";");
 		pw.println("$nplus_overhang_diff_rule = \"" + nplus_overhang_diff.rule + "\";");
 		pw.println("$nplus_spacing = " + TextUtils.formatDouble(nplus_spacing.v) + ";");
 		pw.println("$nplus_spacing_rule = \"" + nplus_spacing.rule + "\";");
 		pw.println();
 		pw.println("$pplus_width = " + TextUtils.formatDouble(pplus_width.v) + ";");
 		pw.println("$pplus_width_rule = \"" + pplus_width.rule + "\";");
 		pw.println("$pplus_overhang_diff = " + TextUtils.formatDouble(pplus_overhang_diff.v) + ";");
 		pw.println("$pplus_overhang_diff_rule = \"" + pplus_overhang_diff.rule + "\";");
 		pw.println("$pplus_spacing = " + TextUtils.formatDouble(pplus_spacing.v) + ";");
 		pw.println("$pplus_spacing_rule = \"" + pplus_spacing.rule + "\";");
 		pw.println();
 		pw.println("$nwell_width = " + TextUtils.formatDouble(nwell_width.v) + ";");
 		pw.println("$nwell_width_rule = \"" + nwell_width.rule + "\";");
 		pw.println("$nwell_overhang_diff = " + TextUtils.formatDouble(nwell_overhang_diff.v) + ";");
 		pw.println("$nwell_overhang_diff_rule = \"" + nwell_overhang_diff.rule + "\";");
 		pw.println("$nwell_spacing = " + TextUtils.formatDouble(nwell_spacing.v) + ";");
 		pw.println("$nwell_spacing_rule = \"" + nwell_spacing.rule + "\";");
 		pw.println();
 		pw.println("######  METAL RULES  #####");
 		pw.print("@metal_width = (");
 		for(int i=0; i<num_metal_layers; i++)
 		{
 			if (i > 0) pw.print(", ");
 			pw.print(TextUtils.formatDouble(metal_width[i].v));
 		}
 		pw.println(");");
 		pw.print("@metal_width_rule = (");
 		for(int i=0; i<num_metal_layers; i++)
 		{
 			if (i > 0) pw.print(", ");
 			pw.print("\"" + metal_width[i].rule + "\"");
 		}
 		pw.println(");");
 		pw.print("@metal_spacing = (");
 		for(int i=0; i<num_metal_layers; i++)
 		{
 			if (i > 0) pw.print(", ");
 			pw.print(TextUtils.formatDouble(metal_spacing[i].v));
 		}
 		pw.println(");");
 		pw.print("@metal_spacing_rule = (");
 		for(int i=0; i<num_metal_layers; i++)
 		{
 			if (i > 0) pw.print(", ");
 			pw.print("\"" + metal_spacing[i].rule + "\"");
 		}
 		pw.println(");");
 		pw.println();
 		pw.println("######  VIA RULES  #####");
 		pw.print("@via_size = (");
 		for(int i=0; i<num_metal_layers-1; i++)
 		{
 			if (i > 0) pw.print(", ");
 			pw.print(TextUtils.formatDouble(via_size[i].v));
 		}
 		pw.println(");");
 		pw.print("@via_size_rule = (");
 		for(int i=0; i<num_metal_layers-1; i++)
 		{
 			if (i > 0) pw.print(", ");
 			pw.print("\"" + via_size[i].rule + "\"");
 		}
 		pw.println(");");
 		pw.print("@via_spacing = (");
 		for(int i=0; i<num_metal_layers-1; i++)
 		{
 			if (i > 0) pw.print(", ");
 			pw.print(TextUtils.formatDouble(via_spacing[i].v));
 		}
 		pw.println(");");
 		pw.print("@via_spacing_rule = (");
 		for(int i=0; i<num_metal_layers-1; i++)
 		{
 			if (i > 0) pw.print(", ");
 			pw.print("\"" + via_spacing[i].rule + "\"");
 		}
 		pw.println(");");
 		pw.println();
 		pw.println("## \"sep2d\" spacing, close proximity via array spacing");
 		pw.print("@via_array_spacing = (");
 		for(int i=0; i<num_metal_layers-1; i++)
 		{
 			if (i > 0) pw.print(", ");
 			pw.print(TextUtils.formatDouble(via_array_spacing[i].v));
 		}
 		pw.println(");");
 		pw.print("@via_array_spacing_rule = (");
 		for(int i=0; i<num_metal_layers-1; i++)
 		{
 			if (i > 0) pw.print(", ");
 			pw.print("\"" + via_array_spacing[i].rule + "\"");
 		}
 		pw.println(");");
 		pw.print("@via_overhang_inline = (");
 		for(int i=0; i<num_metal_layers-1; i++)
 		{
 			if (i > 0) pw.print(", ");
 			pw.print(TextUtils.formatDouble(via_overhang_inline[i].v));
 		}
 		pw.println(");");
 		pw.print("@via_overhang_inline_rule = (");
 		for(int i=0; i<num_metal_layers-1; i++)
 		{
 			if (i > 0) pw.print(", ");
 			pw.print("\"" + via_overhang_inline[i].rule + "\"");
 		}
 		pw.println(");");
 		pw.println();
 		pw.println("######  ANTENNA RULES  #####");
 		pw.println("$poly_antenna_ratio = " + TextUtils.formatDouble(poly_antenna_ratio) + ";");
 		pw.print("@metal_antenna_ratio = (");
 		for(int i=0; i<num_metal_layers; i++)
 		{
 			if (i > 0) pw.print(", ");
 			pw.print(TextUtils.formatDouble(metal_antenna_ratio[i]));
 		}
 		pw.println(");");
 		pw.println();
 		pw.println("######  GDS-II LAYERS  #####");
 		pw.println("$gds_diff_layer = " + gds_diff_layer + ";");
 		pw.println("$gds_poly_layer = " + gds_poly_layer + ";");
 		pw.println("$gds_nplus_layer = " + gds_nplus_layer + ";");
 		pw.println("$gds_pplus_layer = " + gds_pplus_layer + ";");
 		pw.println("$gds_nwell_layer = " + gds_nwell_layer + ";");
 		pw.println("$gds_contact_layer = " + gds_contact_layer + ";");
 		pw.print("@gds_metal_layer = (");
 		for(int i=0; i<num_metal_layers; i++)
 		{
 			if (i > 0) pw.print(", ");
 			pw.print(gds_metal_layer[i]);
 		}
 		pw.println(");");
 		pw.print("@gds_via_layer = (");
 		for(int i=0; i<num_metal_layers-1; i++)
 		{
 			if (i > 0) pw.print(", ");
 			pw.print(gds_via_layer[i]);
 		}
 		pw.println(");");
 		pw.println();
 		pw.println("## Device marking layer");
 		pw.println("$gds_marking_layer = " + gds_marking_layer + ";");
 		pw.println();
 		pw.println("# End of techfile");
 	}
 
 	/************************************** WRITE XML FILE **************************************/
 
 	public void writeXML()
 	{
 		String errorMessage = errorInData();
 		if (errorMessage != null)
 		{
 			Job.getUserInterface().showErrorMessage("ERROR: " + errorMessage,
 				"Missing Technology Data");
 			return;
 		}
 		String fileName = OpenFile.chooseOutputFile(FileType.XML, "Technology XML File", "Technology.xml");
 		if (fileName == null) return;
 		try
 		{
 			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
 			dumpTechnology(printWriter);
 			printWriter.close();
 		} catch (IOException e)
 		{
 			System.out.println("Error writing XML file");
 			return;
 		}
 	}
 
 	private void dumpTechnology(PrintWriter pw)
 	{
 		// LAYER COLOURS
 		Color [] metal_colour = new Color[]
 		{
 			new Color(0,150,255),   // cyan/blue
 			new Color(148,0,211),   // purple
 			new Color(255,215,0),   // yellow
 			new Color(132,112,255), // mauve
 			new Color(255,160,122), // salmon
 			new Color(34,139,34),   // dull green
 			new Color(178,34,34),   // dull red
 			new Color(34,34,178),   // dull blue
 			new Color(153,153,153), // light gray
 			new Color(102,102,102)  // dark gray
 		};
 		Color poly_colour = new Color(255,155,192);   // pink
 		Color diff_colour = new Color(107,226,96);    // light green
 		Color via_colour = new Color(205,205,205);    // lighter gray
 		Color contact_colour = new Color(40,40,40);   // darker gray
 		Color nplus_colour = new Color(224,238,224);
 		Color pplus_colour = new Color(224,224,120);
 		Color nwell_colour = new Color(140,140,140);
 
 		// write the header
 		String foundry_name = "NONE";
 		pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
 		pw.println();
 		pw.println("<!--");
 		pw.println(" *");
 		pw.println(" *  Electric technology file for process \"" + tech_name + "\"");
 		pw.println(" *");
 		pw.println(" *  Automatically generated by Electric's technology wizard");
 		pw.println(" *");
 		pw.println("-->");
 		pw.println();
 		pw.println("<technology name=\"" + tech_name + "\"");
 		pw.println("    xmlns=\"http://electric.sun.com/Technology\"");
 		pw.println("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
 		pw.println("    xsi:schemaLocation=\"http://electric.sun.com/Technology ../../technology/Technology.xsd\">");
 		pw.println();
 		pw.println("    <shortName>" + tech_name + "</shortName>");
 		pw.println("    <description>" + tech_description + "</description>");
 		pw.println("    <numMetals min=\"" + num_metal_layers + "\" max=\"" + num_metal_layers + "\" default=\"" + num_metal_layers + "\"/>");
 		pw.println("    <scale value=\"" + floaty(stepsize) + "\" relevant=\"true\"/>");
 		pw.println("    <defaultFoundry value=\"" + foundry_name + "\"/>");
 		pw.println("    <minResistance value=\"1.0\"/>");
 		pw.println("    <minCapacitance value=\"0.1\"/>");
 		pw.println();
 
 		// write the transparent layer colors
 		int li = 1;
 		pw.println("    <!-- Transparent layers -->");
 		pw.println("    <transparentLayer transparent=\"" + (li++) + "\">");
 		pw.println("        <r>" + poly_colour.getRed() + "</r>");
 		pw.println("        <g>" + poly_colour.getGreen() + "</g>");
 		pw.println("        <b>" + poly_colour.getBlue() + "</b>");
 		pw.println("    </transparentLayer>");
 		pw.println("    <transparentLayer transparent=\"" + (li++) + "\">");
 		pw.println("        <r>" + diff_colour.getRed() + "</r>");
 		pw.println("        <g>" + diff_colour.getGreen() + "</g>");
 		pw.println("        <b>" + diff_colour.getBlue() + "</b>");
 		pw.println("    </transparentLayer>");
 		pw.println("    <transparentLayer transparent=\"" + (li++) + "\">");
 		pw.println("        <r>" + metal_colour[0].getRed() + "</r>");
 		pw.println("        <g>" + metal_colour[0].getGreen() + "</g>");
 		pw.println("        <b>" + metal_colour[0].getBlue() + "</b>");
 		pw.println("    </transparentLayer>");
 		pw.println("    <transparentLayer transparent=\"" + (li++) + "\">");
 		pw.println("        <r>" + metal_colour[1].getRed() + "</r>");
 		pw.println("        <g>" + metal_colour[1].getGreen() + "</g>");
 		pw.println("        <b>" + metal_colour[1].getBlue() + "</b>");
 		pw.println("    </transparentLayer>");
 		pw.println("    <transparentLayer transparent=\"" + (li++) + "\">");
 		pw.println("        <r>" + metal_colour[2].getRed() + "</r>");
 		pw.println("        <g>" + metal_colour[2].getGreen() + "</g>");
 		pw.println("        <b>" + metal_colour[2].getBlue() + "</b>");
 		pw.println("    </transparentLayer>");
 
 		// write the layers
 		pw.println();
 		pw.println("<!--  LAYERS  -->");
 		List<String> layers = new ArrayList<String>();
 		for(int i=1; i<=num_metal_layers; i++)
 			layers.add("Metal-"+i);
 		for(int i=1; i<=num_metal_layers-1; i++)
 			layers.add("Via-"+i);
 		layers.add("Poly");
 		layers.add("PolyGate");
 		layers.add("PolyCon");
 		layers.add("DiffCon");
 		layers.add("N-Diff");
 		layers.add("P-Diff");
 		layers.add("NPlus");
 		layers.add("PPlus");
 		layers.add("N-Well");
 		layers.add("DeviceMark");
 		for(int i=0; i<layers.size(); i++)
 		{
 			String l = layers.get(i);
 			int tcol = 0;
 			String fun = "";
 			String extrafun = "";
 			int r = 255;
 			int g = 0;
 			int b = 0;
 			double la = -1;
 			String pat = null;
 
 			if (l.startsWith("Metal"))
 			{
 				int metLay = TextUtils.atoi(l.substring(6));
 				int metLayDig = (metLay-1) % 10;
 				switch (metLayDig)
 				{
 					case 0: tcol = 3;   break;
 					case 1: tcol = 4;   break;
 					case 2: tcol = 5;   break;
 					case 3:
 						pat="        <pattern>XXXXXXXXXXXXXXXX</pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>XXXXXXXXXXXXXXXX</pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>XXXXXXXXXXXXXXXX</pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>XXXXXXXXXXXXXXXX</pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>XXXXXXXXXXXXXXXX</pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>XXXXXXXXXXXXXXXX</pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>XXXXXXXXXXXXXXXX</pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>XXXXXXXXXXXXXXXX</pattern>\n" +
 							"        <pattern>                </pattern>";
 						break;
 					case 4:
 						pat="        <pattern>X   X   X   X   </pattern>\n" +
 							"        <pattern>   X   X   X   X</pattern>\n" +
 							"        <pattern>  X   X   X   X </pattern>\n" +
 							"        <pattern> X   X   X   X  </pattern>\n" +
 							"        <pattern>X   X   X   X   </pattern>\n" +
 							"        <pattern>   X   X   X   X</pattern>\n" +
 							"        <pattern>  X   X   X   X </pattern>\n" +
 							"        <pattern> X   X   X   X  </pattern>\n" +
 							"        <pattern>X   X   X   X   </pattern>\n" +
 							"        <pattern>   X   X   X   X</pattern>\n" +
 							"        <pattern>  X   X   X   X </pattern>\n" +
 							"        <pattern> X   X   X   X  </pattern>\n" +
 							"        <pattern>X   X   X   X   </pattern>\n" +
 							"        <pattern>   X   X   X   X</pattern>\n" +
 							"        <pattern>  X   X   X   X </pattern>\n" +
 							"        <pattern> X   X   X   X  </pattern>";
 						break;
 					case 5:
 						pat="        <pattern>   X   X   X   X</pattern>\n" +
 							"        <pattern>XXXXXXXXXXXXXXXX</pattern>\n" +
 							"        <pattern>   X   X   X   X</pattern>\n" +
 							"        <pattern> X X X X X X X X</pattern>\n" +
 							"        <pattern>   X   X   X   X</pattern>\n" +
 							"        <pattern>XXXXXXXXXXXXXXXX</pattern>\n" +
 							"        <pattern>   X   X   X   X</pattern>\n" +
 							"        <pattern> X X X X X X X X</pattern>\n" +
 							"        <pattern>   X   X   X   X</pattern>\n" +
 							"        <pattern>XXXXXXXXXXXXXXXX</pattern>\n" +
 							"        <pattern>   X   X   X   X</pattern>\n" +
 							"        <pattern> X X X X X X X X</pattern>\n" +
 							"        <pattern>   X   X   X   X</pattern>\n" +
 							"        <pattern>XXXXXXXXXXXXXXXX</pattern>\n" +
 							"        <pattern>   X   X   X   X</pattern>\n" +
 							"        <pattern> X X X X X X X X</pattern>";
 						break;
 					case 6:
 						pat="        <pattern>X   X   X   X   </pattern>\n" +
 							"        <pattern> X   X   X   X  </pattern>\n" +
 							"        <pattern>  X   X   X   X </pattern>\n" +
 							"        <pattern>   X   X   X   X</pattern>\n" +
 							"        <pattern>X   X   X   X   </pattern>\n" +
 							"        <pattern> X   X   X   X  </pattern>\n" +
 							"        <pattern>  X   X   X   X </pattern>\n" +
 							"        <pattern>   X   X   X   X</pattern>\n" +
 							"        <pattern>X   X   X   X   </pattern>\n" +
 							"        <pattern> X   X   X   X  </pattern>\n" +
 							"        <pattern>  X   X   X   X </pattern>\n" +
 							"        <pattern>   X   X   X   X</pattern>\n" +
 							"        <pattern>X   X   X   X   </pattern>\n" +
 							"        <pattern> X   X   X   X  </pattern>\n" +
 							"        <pattern>  X   X   X   X </pattern>\n" +
 							"        <pattern>   X   X   X   X</pattern>";
 						break;
 					case 7:
 						pat="        <pattern>  X   X   X   X </pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>X   X   X   X   </pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>  X   X   X   X </pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>X   X   X   X   </pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>  X   X   X   X </pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>X   X   X   X   </pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>  X   X   X   X </pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>X   X   X   X   </pattern>\n" +
 							"        <pattern>                </pattern>";
 						break;
 					case 8:
 						pat="        <pattern>                </pattern>\n" +
 							"        <pattern>  X   X   X   X </pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>X   X   X   X   </pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>  X   X   X   X </pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>X   X   X   X   </pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>  X   X   X   X </pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>X   X   X   X   </pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>  X   X   X   X </pattern>\n" +
 							"        <pattern>                </pattern>\n" +
 							"        <pattern>X   X   X   X   </pattern>";
 					case 9:
 						pat="        <pattern>X X X X X X X X </pattern>\n" +
 							"        <pattern>X X X X X X X X </pattern>\n" +
 							"        <pattern>X X X X X X X X </pattern>\n" +
 							"        <pattern>X X X X X X X X </pattern>\n" +
 							"        <pattern>X X X X X X X X </pattern>\n" +
 							"        <pattern>X X X X X X X X </pattern>\n" +
 							"        <pattern>X X X X X X X X </pattern>\n" +
 							"        <pattern>X X X X X X X X </pattern>\n" +
 							"        <pattern>X X X X X X X X </pattern>\n" +
 							"        <pattern>X X X X X X X X </pattern>\n" +
 							"        <pattern>X X X X X X X X </pattern>\n" +
 							"        <pattern>X X X X X X X X </pattern>\n" +
 							"        <pattern>X X X X X X X X </pattern>\n" +
 							"        <pattern>X X X X X X X X </pattern>\n" +
 							"        <pattern>X X X X X X X X </pattern>\n" +
 							"        <pattern>X X X X X X X X </pattern>";
 						break;
 				}
 				fun = "METAL" + metLay;
 				int metLayHigh = (metLay-1) / 10;
 				r = metal_colour[metLayDig].getRed() * (10-metLayHigh) / 10;
 				g = metal_colour[metLayDig].getGreen() * (10-metLayHigh) / 10;
 				b = metal_colour[metLayDig].getBlue() * (10-metLayHigh) / 10;
 				la = metal_width[metLay-1].v / stepsize;
 			}
 
 			if (l.startsWith("Via"))
 			{
 				int viaLay = TextUtils.atoi(l.substring(4));
 				fun = "CONTACT" + viaLay;
 				extrafun = "connects-metal";
 				r = via_colour.getRed();
 				g = via_colour.getGreen();
 				b = via_colour.getBlue();
 				la = via_size[viaLay-1].v / stepsize;
 			}
 
 			if (l.equals("DeviceMark"))
 			{
 				fun = "CONTROL";
 				la = nplus_width.v / stepsize;
 				pat="        <pattern>                </pattern>\n" +
 					"        <pattern>                </pattern>\n" +
 					"        <pattern>                </pattern>\n" +
 					"        <pattern>                </pattern>\n" +
 					"        <pattern>                </pattern>\n" +
 					"        <pattern>                </pattern>\n" +
 					"        <pattern>                </pattern>\n" +
 					"        <pattern>                </pattern>\n" +
 					"        <pattern>                </pattern>\n" +
 					"        <pattern>                </pattern>\n" +
 					"        <pattern>                </pattern>\n" +
 					"        <pattern>                </pattern>\n" +
 					"        <pattern>                </pattern>\n" +
 					"        <pattern>                </pattern>\n" +
 					"        <pattern>                </pattern>\n" +
 					"        <pattern>                </pattern>";
 			}
 
 			if (l.equals("Poly"))
 			{
 				fun = "POLY1";
 				tcol = 1;
 				la = poly_width.v / stepsize;
 			}
 
 			if (l.equals("PolyGate"))
 			{
 				fun = "GATE";
 				tcol = 1;
 			}
 
 			if (l.equals("P-Diff"))
 			{
 				fun = "DIFFP";
 				tcol = 2;
 				la = diff_width.v / stepsize;
 			}
 
 			if (l.equals("N-Diff"))
 			{
 				fun = "DIFFN";
 				tcol = 2;
 				la = diff_width.v / stepsize;
 			}
 
 			if (l.equals("NPlus"))
 			{
 				fun = "IMPLANTN";
 				r = nplus_colour.getRed();
 				g = nplus_colour.getGreen();
 				b = nplus_colour.getBlue();
 				la = nplus_width.v / stepsize;
 				pat="        <pattern>   X       X    </pattern>\n" +
 					"        <pattern>  X       X     </pattern>\n" +
 					"        <pattern> X       X      </pattern>\n" +
 					"        <pattern>X       X       </pattern>\n" +
 					"        <pattern>       X       X</pattern>\n" +
 					"        <pattern>      X       X </pattern>\n" +
 					"        <pattern>     X       X  </pattern>\n" +
 					"        <pattern>    X       X   </pattern>\n" +
 					"        <pattern>   X       X    </pattern>\n" +
 					"        <pattern>  X       X     </pattern>\n" +
 					"        <pattern> X       X      </pattern>\n" +
 					"        <pattern>X       X       </pattern>\n" +
 					"        <pattern>       X       X</pattern>\n" +
 					"        <pattern>      X       X </pattern>\n" +
 					"        <pattern>     X       X  </pattern>\n" +
 					"        <pattern>    X       X   </pattern>";
 			}
 
 
 			if (l.equals("PPlus"))
 			{
 				fun = "IMPLANTP";
 				r = pplus_colour.getRed();
 				g = pplus_colour.getGreen();
 				b = pplus_colour.getBlue();
 				la = pplus_width.v / stepsize;
 				pat="        <pattern>   X       X    </pattern>\n" +
 					"        <pattern>  X       X     </pattern>\n" +
 					"        <pattern> X       X      </pattern>\n" +
 					"        <pattern>X       X       </pattern>\n" +
 					"        <pattern>       X       X</pattern>\n" +
 					"        <pattern>      X       X </pattern>\n" +
 					"        <pattern>     X       X  </pattern>\n" +
 					"        <pattern>    X       X   </pattern>\n" +
 					"        <pattern>   X       X    </pattern>\n" +
 					"        <pattern>  X       X     </pattern>\n" +
 					"        <pattern> X       X      </pattern>\n" +
 					"        <pattern>X       X       </pattern>\n" +
 					"        <pattern>       X       X</pattern>\n" +
 					"        <pattern>      X       X </pattern>\n" +
 					"        <pattern>     X       X  </pattern>\n" +
 					"        <pattern>    X       X   </pattern>";
 			}
 
 			if (l.equals("N-Well"))
 			{
 				fun = "WELLN";
 				r = nwell_colour.getRed();
 				g = nwell_colour.getGreen();
 				b = nwell_colour.getBlue();
 				la = nwell_width.v / stepsize;
 				pat="        <pattern>       X       X</pattern>\n" +
 					"        <pattern>X       X       </pattern>\n" +
 					"        <pattern> X       X      </pattern>\n" +
 					"        <pattern>  X       X     </pattern>\n" +
 					"        <pattern>   X       X    </pattern>\n" +
 					"        <pattern>    X       X   </pattern>\n" +
 					"        <pattern>     X       X  </pattern>\n" +
 					"        <pattern>      X       X </pattern>\n" +
 					"        <pattern>       X       X</pattern>\n" +
 					"        <pattern>X       X       </pattern>\n" +
 					"        <pattern> X       X      </pattern>\n" +
 					"        <pattern>  X       X     </pattern>\n" +
 					"        <pattern>   X       X    </pattern>\n" +
 					"        <pattern>    X       X   </pattern>\n" +
 					"        <pattern>     X       X  </pattern>\n" +
 					"        <pattern>      X       X </pattern>";
 			}
 
 			if (l.equals("PolyCon"))
 			{
 				fun = "CONTACT1";
 				extrafun = "connects-poly";
 				r = contact_colour.getRed();
 				g = contact_colour.getGreen();
 				b = contact_colour.getBlue();
 				la = contact_size.v / stepsize;
 			}
 
 			if (l.equals("DiffCon"))
 			{
 				fun = "CONTACT1";
 				extrafun = "connects-diff";
 				r = contact_colour.getRed();
 				g = contact_colour.getGreen();
 				b = contact_colour.getBlue();
 				la = contact_size.v / stepsize;
 			}
 
 			pw.println();
 			pw.println("    <layer name=\"" + l + "\" " + (fun.length() > 0 ? ("fun=\"" + fun + "\"") : "") +
 				(extrafun.length() > 0 ? (" extraFun=\"" + extrafun + "\"") : "") + ">");
 			if (tcol == 0)
 			{
 				pw.println("        <opaqueColor r=\"" + r + "\" g=\"" + g + "\" b=\"" + b + "\"/>");
 			} else
 			{
 				pw.println("        <transparentColor transparent=\"" + tcol + "\"/>");
 			}
 			pw.println("        <patternedOnDisplay>" + (pat == null ? "false" : "true") + "</patternedOnDisplay>");
 			pw.println("        <patternedOnPrinter>" + (pat == null ? "false" : "true") + "</patternedOnPrinter>");
 			if (pat == null)
 			{
 				for(int j=0; j<16; j++)
 					pw.println("        <pattern>                </pattern>");
 			} else
 			{
 				pw.println(pat);
 			}
 			pw.println("        <outlined>NOPAT</outlined>");
			pw.println("        <opacity>0</opacity>");
 			pw.println("        <foreground>true</foreground>");
 			pw.println("        <display3D thick=\"1\" height=\"1\" mode=\"NONE\" factor=\"1\"/>");
 			char cifLetter = (char)('A' + i);
 			pw.println("        <cifLayer cif=\"C" + cifLetter + cifLetter + "\"/>");
 			pw.println("        <skillLayer skill=\"" + l + "\"/>");
 			pw.println("        <parasitics resistance=\"1\" capacitance=\"0.0\" edgeCapacitance=\"0.0\"/>");
 			if (fun.startsWith("METAL") || fun.startsWith("POLY") || fun.startsWith("DIFF"))
 			{
 				pw.println("        <pureLayerNode name=\"" + l + "-Node\" port=\"Port_" + l + "\"> ");
 				pw.println("            <lambda>" + floaty(la) + "</lambda>");
 				pw.println("            <portArc>" + l + "</portArc>");
 				pw.println("        </pureLayerNode>");
 			}
 			pw.println("    </layer>");
 		}
 
 		// write the arcs
 		List<String> arcs = new ArrayList<String>();
 		for(int i=1; i<=num_metal_layers; i++)
 			arcs.add("Metal-"+i);
 		arcs.add("Poly");
 		arcs.add("N-Diff");
 		arcs.add("P-Diff");
 		pw.println();
 		pw.println("<!--  ARCS  -->");
 		for(String l : arcs)
 		{
 			String fun = "";
 			int ant = -1;
 			double la = 0;
 			List<String> h = new ArrayList<String>();
 			if (l.startsWith("Metal"))
 			{
 				int metalLay = TextUtils.atoi(l.substring(6));
 				fun = "METAL" + metalLay;
 				la = metal_width[metalLay-1].v / stepsize;
 				ant = (int)Math.round(metal_antenna_ratio[metalLay-1]) | 200;
 				h.add(l + "=" + la);
 			}
 
 			if (l.equals("N-Diff"))
 			{
 				fun = "DIFFN";
 				h.add("N-Diff=" + (diff_width.v/stepsize));
 				h.add("NPlus=" + ((nplus_overhang_diff.v*2+diff_width.v)/stepsize));
 			}
 
 			if (l.equals("P-Diff"))
 			{
 				fun = "DIFFP";
 				h.add("P-Diff=" + (diff_width.v / stepsize));
 				h.add("PPlus=" + ((pplus_overhang_diff.v*2 + diff_width.v) / stepsize));
 				h.add("N-Well=" + ((nwell_overhang_diff.v*2 + diff_width.v) / stepsize));
 			}
 
 			if (l.equals("Poly"))
 			{
 				fun = "POLY1";
 				la = poly_width.v / stepsize;
 				ant = (int)Math.round(poly_antenna_ratio) | 200;
 				h.add(l + "=" + la);
 			}
 
 			double max = 0;
 			for(String hEach : h)
 			{
 				int equalsPos = hEach.indexOf('=');
 				double lim = TextUtils.atof(hEach.substring(equalsPos+1));
 				if (lim > max) max = lim;
 			}
 
 			if (ant >= 0) ant = Math.round(ant);
 			pw.println();
 			pw.println("    <arcProto name=\"" + l + "\" fun=\"" + fun + "\">");
 			pw.println("        <wipable/>");
 			pw.println("        <extended>true</extended>");
 			pw.println("        <fixedAngle>true</fixedAngle>");
 			pw.println("        <angleIncrement>90</angleIncrement>");
             if (ant >= 0)
                 pw.println("        <antennaRatio>" + floaty(ant) + "</antennaRatio>");
 
 			for(String each : h)
 			{
 				int equalsPos = each.indexOf('=');
 				String nom = each.substring(0, equalsPos);
 				double lim = TextUtils.atof(each.substring(equalsPos+1));
 
 				pw.println("        <arcLayer layer=\"" + nom + "\" style=\"FILLED\">");
 				pw.println("            <lambda>" + floaty(lim/2) + "</lambda>");
 				pw.println("        </arcLayer>");
 			}
 			pw.println("    </arcProto>");
 		}
 
 		// write the pins
 		pw.println();
 		pw.println("<!--  PINS  -->");
 		for(int i=1; i<=num_metal_layers; i++)
 		{
 			double hla = metal_width[i-1].v / (stepsize*2);
 			pw.println();
 			pw.println("    <primitiveNode name=\"Metal-" + i + "-Pin\" fun=\"PIN\">");
 			pw.println("        <shrinkArcs/>");
 			pw.println("        <nodeLayer layer=\"Metal-" + i + "\" style=\"CROSSED\">");
 			pw.println("            <box>");
 			pw.println("                <lambdaBox klx=\"-" + floaty(hla) + "\" khx=\"" + floaty(hla) +
 				"\" kly=\"-" + floaty(hla) + "\" khy=\"" + floaty(hla) +"\"/>");
 			pw.println("            </box>");
 			pw.println("        </nodeLayer>");
 			pw.println("        <primitivePort name=\"M" + i + "\">");
 			pw.println("            <portAngle primary=\"0\" range=\"180\"/>");
 			pw.println("            <portTopology>0</portTopology>");
 			pw.println("            <box>");
 			pw.println("                <lambdaBox klx=\"0.0\" khx=\"0.0\" kly=\"0.0\" khy=\"0.0\"/>");
 			pw.println("            </box>");
 			pw.println("            <portArc>Metal-" + i + "</portArc>");
 			pw.println("        </primitivePort>");
 			pw.println("    </primitiveNode>");
 		}
 		double hla = poly_width.v / (stepsize*2);
 		pw.println();
 		pw.println("    <primitiveNode name=\"Poly-Pin\" fun=\"PIN\">");
 		pw.println("        <shrinkArcs/>");
 		pw.println("        <nodeLayer layer=\"Poly\" style=\"CROSSED\">");
 		pw.println("            <box>");
 		pw.println("                <lambdaBox klx=\"-" + floaty(hla) + "\" khx=\"" + floaty(hla) +
 			"\" kly=\"-" + floaty(hla) + "\" khy=\"" + floaty(hla) + "\"/>");
 		pw.println("            </box>");
 		pw.println("        </nodeLayer>");
 		pw.println("        <primitivePort name=\"Poly\">");
 		pw.println("            <portAngle primary=\"0\" range=\"180\"/>");
 		pw.println("            <portTopology>0</portTopology>");
 		pw.println("            <box>");
 		pw.println("                <lambdaBox klx=\"0.0\" khx=\"0.0\" kly=\"0.0\" khy=\"0.0\"/>");
 		pw.println("            </box>");
 		pw.println("            <portArc>Poly</portArc>");
 		pw.println("        </primitivePort>");
 		pw.println("    </primitiveNode>");
 
 		pw.println();
 		pw.println("<!--  P-Diff AND N-Diff PINS  -->");
 		for(int i=0; i<=1; i++)
 		{
 			String t, d1, d2, d3;
 			if (i == 1)
 			{
 				t = "P";
 				d1 = floaty(diff_width.v/(stepsize*2));
 				d2 = floaty((diff_width.v+pplus_overhang_diff.v*2)/(stepsize*2));
 				d3 = floaty((diff_width.v+nwell_overhang_diff.v*2)/(stepsize*2));
 			} else
 			{
 				t = "N";
 				d1 = floaty(diff_width.v/(stepsize*2));
 				d2 = floaty((diff_width.v+nplus_overhang_diff.v*2)/(stepsize*2));
 				d3 = d2;
 			}
 
 			String x = floaty(TextUtils.atof(d3) - TextUtils.atof(d1));
 			pw.println();
 			pw.println("    <primitiveNode name=\"" + t + "-Diff-Pin\" fun=\"PIN\">");
 			pw.println("        <shrinkArcs/>");
 //			pw.println("        <diskOffset untilVersion=\"1\" x=\"" + d3 + "\" y=\"" + d3 + "\"/>");
 //			pw.println("        <diskOffset untilVersion=\"2\" x=\"" + d1 + "\" y=\"" + d1 + "\"/>");
 			pw.println("        <sizeOffset lx=\"" + x + "\" hx=\"" + x + "\" ly=\"" + x + "\" hy=\"" + x + "\"/>");
 			if (t.equals("P"))
 			{
 				pw.println("        <nodeLayer layer=\"N-Well\" style=\"CROSSED\">");
 				pw.println("            <box>");
 				pw.println("                <lambdaBox klx=\"-" + d3 + "\" khx=\"" + d3 + "\" kly=\"-" + d3 + "\" khy=\"" + d3 + "\"/>");
 				pw.println("            </box>");
 				pw.println("        </nodeLayer>");
 			}
 			pw.println("        <nodeLayer layer=\"" + t + "Plus\" style=\"CROSSED\">");
 			pw.println("            <box>");
 			pw.println("                <lambdaBox klx=\"-" + d2 + "\" khx=\"" + d2 + "\" kly=\"-" + d2 + "\" khy=\"" + d2 + "\"/>");
 			pw.println("            </box>");
 			pw.println("        </nodeLayer>");
 			pw.println("        <nodeLayer layer=\"" + t + "-Diff\" style=\"CROSSED\">");
 			pw.println("            <box>");
 			pw.println("                <lambdaBox klx=\"-" + d1 + "\" khx=\"" + d1 + "\" kly=\"-" + d1 + "\" khy=\"" + d1 + "\"/>");
 			pw.println("            </box>");
 			pw.println("        </nodeLayer>");
 			pw.println("        <primitivePort name=\"" + t + "-Diff\">");
 			pw.println("            <portAngle primary=\"0\" range=\"180\"/>");
 			pw.println("            <portTopology>0</portTopology>");
 			pw.println("            <box>");
 			pw.println("                <lambdaBox klx=\"0.0\" khx=\"0.0\" kly=\"0.0\" khy=\"0.0\"/>");
 			pw.println("            </box>");
 			pw.println("            <portArc>" + t + "-Diff</portArc>");
 			pw.println("        </primitivePort>");
 			pw.println("    </primitiveNode>");
 		}
 
 		// write the contacts
 		pw.println();
 		pw.println("<!--  METAL TO METAL VIAS / CONTACTS  -->");
 		for(int alt=0; alt<=1; alt++)
 		{
 			for(int vl=0; vl<num_metal_layers; vl++)
 			{
 				String src, il;
 				if (vl == 0) { src = "Poly"; il = "PolyCon"; } else { src = "Metal-" + vl; il = "Via-" + vl; }
 				String dest = "Metal-" + (vl+1);
 				String upperx, uppery, lowerx, lowery, cs, cs2, c;
 				if (vl == 0)
 				{
 					// poly
 					if (alt != 0)
 					{
 						upperx = floaty((contact_metal_overhang_inline_only.v*2+contact_size.v)/(stepsize*2));
 						uppery = floaty(contact_size.v/(stepsize*2));
 					} else
 					{
 						upperx = floaty((contact_metal_overhang_all_sides.v*2+contact_size.v)/(stepsize*2));
 						uppery = upperx;
 					}
 					lowerx = floaty((contact_poly_overhang.v*2+contact_size.v)/(stepsize*2));
 					lowery = lowerx;
 
 					cs = floaty(contact_spacing.v/stepsize);
 					cs2 = cs;
 					c = floaty(contact_size.v/stepsize);
 				} else
 				{
 					if (alt != 0)
 					{
 						upperx = floaty(via_size[vl-1].v/(stepsize*2));
 						uppery = floaty((via_overhang_inline[vl-1].v*2+via_size[vl-1].v)/(stepsize*2));
 						lowerx = uppery;
 						lowery = upperx;
 					} else
 					{
 						upperx = floaty((via_overhang_inline[vl-1].v*2+via_size[vl-1].v)/(stepsize*2));
 						uppery = floaty(via_size[vl-1].v/(stepsize*2));
 						lowerx = upperx;
 						lowery = uppery;
 					}
 
 					c = floaty(via_size[vl-1].v/stepsize);
 					cs = floaty(via_spacing[vl-1].v/stepsize);
 					cs2 = floaty(via_array_spacing[vl-1].v/stepsize);
 				}
 
 				double maxx = TextUtils.atof(upperx);
 				if (TextUtils.atof(lowerx) > maxx) maxx = TextUtils.atof(lowerx);
 				double maxy = TextUtils.atof(uppery);
 				if (TextUtils.atof(lowery) > maxy) maxy = TextUtils.atof(lowery);
 				double minx = TextUtils.atof(upperx);
 				if (TextUtils.atof(lowerx) < minx) minx = TextUtils.atof(lowerx);
 				double miny = TextUtils.atof(uppery);
 				if (TextUtils.atof(lowery) < miny) miny = TextUtils.atof(lowery);
 				String ox = floaty(maxx-minx);
 				String oy = floaty(maxy-miny);
 
 				pw.println();
 				pw.println("    <primitiveNode name=\"" + src + "-" + dest + "-Con" + (alt != 0 ? "-X" : "") + "\" fun=\"CONTACT\">");
 //				pw.println("        <diskOffset untilVersion=\"2\" x=\"" + maxx + "\" y=\"" + maxy + "\"/>");
 //				pw.println("        <sizeOffset lx=\"" + ox + "\" hx=\"" + ox + "\" ly=\"" + oy + "\" hy=\"" + oy + "\"/>");
 				pw.println("        <nodeLayer layer=\"" + src + "\" style=\"FILLED\">");
 				pw.println("            <box>");
 				pw.println("                <lambdaBox klx=\"-" + lowerx + "\" khx=\"" + lowerx + "\" kly=\"-" + lowery + "\" khy=\"" + lowery + "\"/>");
 				pw.println("            </box>");
 				pw.println("        </nodeLayer>");
 				pw.println("        <nodeLayer layer=\"" + dest + "\" style=\"FILLED\">");
 				pw.println("            <box>");
 				pw.println("                <lambdaBox klx=\"-" + upperx + "\" khx=\"" + upperx + "\" kly=\"-" + uppery + "\" khy=\"" + uppery + "\"/>");
 				pw.println("            </box>");
 				pw.println("        </nodeLayer>");
 				pw.println("        <nodeLayer layer=\"" + il + "\" style=\"FILLED\">");
 				pw.println("            <multicutbox sizex=\"" + c + "\" sizey=\"" + c + "\" sep1d=\"" + cs + "\" sep2d=\"" + cs2 + "\">");
 				pw.println("                <lambdaBox klx=\"0.0\" khx=\"0.0\" kly=\"0.0\" khy=\"0.0\"/>");
 				pw.println("            </multicutbox>");
 				pw.println("        </nodeLayer>");
 				pw.println("        <primitivePort name=\"" + src + "-" + dest + "\">");
 				pw.println("            <portAngle primary=\"0\" range=\"180\"/>");
 				pw.println("            <portTopology>0</portTopology>");
 				pw.println("            <box>");
 				pw.println("                <lambdaBox klx=\"-" + minx + "\" khx=\"" + minx + "\" kly=\"-" + miny + "\" khy=\"" + miny + "\"/>");
 				pw.println("            </box>");
 				pw.println("            <portArc>" + src + "</portArc>");
 				pw.println("            <portArc>" + dest + "</portArc>");
 				pw.println("        </primitivePort>");
 				pw.println("        <minSizeRule width=\"" + floaty(2*maxx) + "\" height=\"" + floaty(2*maxy) + "\" rule=\"" + src + "-" + dest + " rules\"/>");
 				pw.println("    </primitiveNode>");
 			}
 		}
 
 		pw.println();
 		pw.println("<!--  N-Diff-Metal-1 and P-Diff-Metal-1  -->");
 		for(int alt=0; alt<=1; alt++)
 		{
 			for(int i=0; i<2; i++)
 			{
 				String t = "", sx = "", mx = "", my = "";
 				if (i == 0)
 				{
 					t = "N";
 					sx = floaty((nplus_overhang_diff.v*2+diff_contact_overhang.v*2+contact_size.v)/(stepsize*2));
 				} else
 				{
 					t = "P";
 					sx = floaty((pplus_overhang_diff.v*2+diff_contact_overhang.v*2+contact_size.v)/(stepsize*2));
 				}
 
 				if (alt != 0)
 				{
 					mx = floaty((contact_metal_overhang_inline_only.v*2+contact_size.v)/(stepsize*2));
 					my = floaty(contact_size.v/(stepsize*2));
 				} else
 				{
 					mx = floaty((contact_metal_overhang_all_sides.v*2+contact_size.v)/(stepsize*2));
 					my = mx;
 				}
 
 				String dx = floaty((diff_contact_overhang.v*2+contact_size.v)/(stepsize*2));
 				String wx = floaty((nwell_overhang_diff.v*2+diff_contact_overhang.v*2+contact_size.v)/(stepsize*2));
 
 				String maxx = mx;
 				if (TextUtils.atof(dx) > TextUtils.atof(maxx)) maxx = dx;
 				if (i==1 && TextUtils.atof(wx) > TextUtils.atof(maxx)) maxx = wx;
 				String maxy = my;
 				if (TextUtils.atof(dx) > TextUtils.atof(maxy)) maxy = dx;
 				if (i==1 && TextUtils.atof(wx) > TextUtils.atof(maxy)) maxy = wx;
 
 				String minx = mx;
 				if (TextUtils.atof(dx) < TextUtils.atof(minx)) minx = dx;
 				if (i==1 && TextUtils.atof(wx) < TextUtils.atof(minx)) minx = wx;
 				String miny = my;
 				if (TextUtils.atof(dx) < TextUtils.atof(miny)) miny = dx;
 				if (i==1 && TextUtils.atof(wx) < TextUtils.atof(miny)) miny = wx;
 
 				String sox = floaty(TextUtils.atof(maxx)-TextUtils.atof(dx));
 				String soy = floaty(TextUtils.atof(maxy)-TextUtils.atof(dx));
 
 				pw.println();
 				pw.println("    <primitiveNode name=\"" + t + "-Diff-Metal-1" + (alt != 0 ? "-X" : "") + "\" fun=\"CONTACT\">");
 //				pw.println("        <diskOffset untilVersion=\"1\" x=\"" + maxx + "\" y=\"" + maxy + "\"/>");
 //				pw.println("        <diskOffset untilVersion=\"2\" x=\"" + minx + "\" y=\"" + miny + "\"/>");
 				pw.println("        <sizeOffset lx=\"" + sox + "\" hx=\"" + sox + "\" ly=\"" + soy + "\" hy=\"" + soy + "\"/>");
 				pw.println("        <nodeLayer layer=\"Metal-1\" style=\"FILLED\">");
 				pw.println("            <box>");
 				pw.println("                <lambdaBox klx=\"-" + mx + "\" khx=\"" + mx + "\" kly=\"-" + my + "\" khy=\"" + my + "\"/>");
 				pw.println("            </box>");
 				pw.println("        </nodeLayer>");
 				pw.println("        <nodeLayer layer=\"" + t + "-Diff\" style=\"FILLED\">");
 				pw.println("            <box>");
 				pw.println("                <lambdaBox klx=\"-" + dx + "\" khx=\"" + dx + "\" kly=\"-" + dx + "\" khy=\"" + dx + "\"/>");
 				pw.println("            </box>");
 				pw.println("        </nodeLayer>");
 				if (i != 0)
 				{
 					pw.println("        <nodeLayer layer=\"N-Well\" style=\"FILLED\">");
 					pw.println("            <box>");
 					pw.println("                <lambdaBox klx=\"-" + wx + "\" khx=\"" + wx + "\" kly=\"-" + wx + "\" khy=\"" + wx + "\"/>");
 					pw.println("            </box>");
 					pw.println("        </nodeLayer>");
 				}
 				pw.println("        <nodeLayer layer=\"" + t + "Plus\" style=\"FILLED\">");
 				pw.println("            <box>");
 				pw.println("                <lambdaBox klx=\"-" + sx + "\" khx=\"" + sx + "\" kly=\"-" + sx + "\" khy=\"" + sx + "\"/>");
 				pw.println("            </box>");
 				pw.println("        </nodeLayer>");
 				pw.println("        <nodeLayer layer=\"DiffCon\" style=\"FILLED\">");
 				pw.println("            <multicutbox sizex=\"" + floaty(contact_size.v/stepsize) + "\" sizey=\"" +
 					floaty(contact_size.v/stepsize) + "\" sep1d=\"" + (floaty(contact_spacing.v/stepsize)) +
 					"\" sep2d=\"" + floaty(contact_spacing.v/stepsize) + "\">");
 				pw.println("                <lambdaBox klx=\"0.0\" khx=\"0.0\" kly=\"0.0\" khy=\"0.0\"/>");
 				pw.println("            </multicutbox>");
 				pw.println("        </nodeLayer>");
 				pw.println("        <primitivePort name=\"" + t + "-Diff-Metal-1" + "\">");
 				pw.println("            <portAngle primary=\"0\" range=\"180\"/>");
 				pw.println("            <portTopology>0</portTopology>");
 				pw.println("            <box>");
 				pw.println("                <lambdaBox klx=\"-" + dx + "\" khx=\"" + dx + "\" kly=\"-" + dx + "\" khy=\"" + dx + "\"/>");
 				pw.println("            </box>");
 				pw.println("            <portArc>" + t + "-Diff</portArc>");
 				pw.println("            <portArc>Metal-1</portArc>");
 				pw.println("        </primitivePort>");
 				pw.println("        <minSizeRule width=\"" + floaty(2*TextUtils.atof(maxx)) + "\" height=\"" +
 					floaty(2*TextUtils.atof(maxy)) + "\" rule=\"" + t + "-Diff, " + t + "+, M1" +
 					(i==1 ? ", N-Well" : "") + " and Contact rules\"/>");
 				pw.println("    </primitiveNode>");
 			}
 		}
 
 		pw.println();
 		pw.println("<!--  VDD-Tie-Metal-1 and VSS-Tie-Metal-1  -->");
 		for(int alt=0; alt<=1; alt++)
 		{
 			for(int i=0; i<2; i++)
 			{
 				String t, fun, dt, sx, mx, my;
 				if (i == 0)
 				{
 					t = "VDD";
 					fun = "WELL";
 					dt = "N";
 					sx = floaty((nplus_overhang_diff.v*2+diff_contact_overhang.v*2+contact_size.v)/(stepsize*2));
 				} else
 				{
 					t = "VSS";
 					fun = "SUBSTRATE";
 					dt = "P";
 					sx = floaty((pplus_overhang_diff.v*2+diff_contact_overhang.v*2+contact_size.v)/(stepsize*2));
 				}
 
 				if (alt != 0)
 				{
 					mx = floaty((contact_metal_overhang_inline_only.v*2+contact_size.v)/(stepsize*2));
 					my = floaty(contact_size.v/(stepsize*2));
 				} else
 				{
 					mx = floaty((contact_metal_overhang_all_sides.v*2+contact_size.v)/(stepsize*2));
 					my = mx;
 				}
 
 				String dx = floaty((diff_contact_overhang.v*2+contact_size.v)/(stepsize*2));
 				String wx = floaty((nwell_overhang_diff.v*2+diff_contact_overhang.v*2+contact_size.v)/(stepsize*2));
 
 				String maxx = mx;
 				if (TextUtils.atof(dx) > TextUtils.atof(maxx)) maxx = dx;
 				if (i==0 && TextUtils.atof(wx)>TextUtils.atof(maxx)) maxx = wx;
 				String maxy = my;
 				if (TextUtils.atof(dx) > TextUtils.atof(maxy)) maxy = dx;
 				if (i==0 && TextUtils.atof(wx)>TextUtils.atof(maxy)) maxy = wx;
 
 				String minx = mx;
 				if (TextUtils.atof(dx) < TextUtils.atof(minx)) minx = dx;
 				if (i==0 && TextUtils.atof(wx)<TextUtils.atof(minx)) minx = wx;
 				String miny = my;
 				if (TextUtils.atof(dx) < TextUtils.atof(miny)) miny = dx;
 				if (i==0 && TextUtils.atof(wx)<TextUtils.atof(miny)) miny = wx;
 
 				String sox = floaty(TextUtils.atof(maxx)-TextUtils.atof(dx));
 				String soy = floaty(TextUtils.atof(maxy)-TextUtils.atof(dx));
 
 				pw.println();
 				pw.println("    <primitiveNode name=\"" + t + "-Tie-Metal-1" + (alt != 0 ? "-X" : "") + "\" fun=\"" + fun + "\">");
 //				pw.println("        <diskOffset untilVersion=\"1\" x=\"" + maxx + "\" y=\"" + maxy + "\"/>");
 //				pw.println("        <diskOffset untilVersion=\"2\" x=\"" + minx + "\" y=\"" + miny + "\"/>");
 				pw.println("        <sizeOffset lx=\"" + sox + "\" hx=\"" + sox + "\" ly=\"" + soy + "\" hy=\"" + soy + "\"/>");
 				pw.println("        <nodeLayer layer=\"Metal-1\" style=\"FILLED\">");
 				pw.println("            <box>");
 				pw.println("                <lambdaBox klx=\"-" + mx + "\" khx=\"" + mx + "\" kly=\"-" + my + "\" khy=\"" + my + "\"/>");
 				pw.println("            </box>");
 				pw.println("        </nodeLayer>");
 				pw.println("        <nodeLayer layer=\"" + dt + "-Diff\" style=\"FILLED\">");
 				pw.println("            <box>");
 				pw.println("                <lambdaBox klx=\"-" + dx + "\" khx=\"" + dx + "\" kly=\"-" + dx + "\" khy=\"" + dx + "\"/>");
 				pw.println("            </box>");
 				pw.println("        </nodeLayer>");
 				if (i != 1)
 				{
 					pw.println("        <nodeLayer layer=\"N-Well\" style=\"FILLED\">");
 					pw.println("            <box>");
 					pw.println("                <lambdaBox klx=\"-" + wx + "\" khx=\"" + wx + "\" kly=\"-" + wx + "\" khy=\"" + wx + "\"/>");
 					pw.println("            </box>");
 					pw.println("        </nodeLayer>");
 				}
 				pw.println("        <nodeLayer layer=\"" + dt + "Plus\" style=\"FILLED\">");
 				pw.println("            <box>");
 				pw.println("                <lambdaBox klx=\"-" + sx + "\" khx=\"" + sx + "\" kly=\"-" + sx + "\" khy=\"" + sx + "\"/>");
 				pw.println("            </box>");
 				pw.println("        </nodeLayer>");
 				pw.println("        <nodeLayer layer=\"DiffCon\" style=\"FILLED\">");
 				pw.println("            <multicutbox sizex=\"" + floaty(contact_size.v/stepsize) + "\" sizey=\"" +
 					floaty(contact_size.v/stepsize) + "\" sep1d=\"" + floaty(contact_spacing.v/stepsize) +
 					"\" sep2d=\"" + floaty(contact_spacing.v/stepsize) + "\">");
 				pw.println("                <lambdaBox klx=\"0.0\" khx=\"0.0\" kly=\"0.0\" khy=\"0.0\"/>");
 				pw.println("            </multicutbox>");
 				pw.println("        </nodeLayer>");
 				pw.println("        <primitivePort name=\"" + t + "-Tie-M1" + "\">");
 				pw.println("            <portAngle primary=\"0\" range=\"180\"/>");
 				pw.println("            <portTopology>0</portTopology>");
 				pw.println("            <box>");
 				pw.println("                <lambdaBox klx=\"-" + dx + "\" khx=\"" + dx + "\" kly=\"-" + dx + "\" khy=\"" + dx + "\"/>");
 				pw.println("            </box>");
 				pw.println("            <portArc>Metal-1</portArc>");
 				pw.println("        </primitivePort>");
 				pw.println("        <minSizeRule width=\"" + floaty(2*TextUtils.atof(maxx)) + "\" height=\"" +
 					floaty(2*TextUtils.atof(maxy)) + "\" rule=\"" + dt + "-Diff, " + dt + "+, M1" +
 					(i==0 ? ", N-Well" : "") + " and Contact rules\"/>");
 				pw.println("    </primitiveNode>");
 			}
 		}
 
 		// write the transistors
 		for(int i=0; i<2; i++)
 		{
 			String wellx = "", welly = "", t, impx, impy;
 			if (i==0)
 			{
 				t = "P";
 				wellx = floaty((gate_width.v+nwell_overhang_diff.v*2)/(stepsize*2));
 				welly = floaty((gate_length.v+diff_poly_overhang.v*2+nwell_overhang_diff.v*2)/(stepsize*2));
 				impx = floaty((gate_width.v+pplus_overhang_diff.v*2)/(stepsize*2));
 				impy = floaty((gate_length.v+diff_poly_overhang.v*2+pplus_overhang_diff.v*2)/(stepsize*2));
 			} else
 			{
 				t = "N";
 				impx = floaty((gate_width.v+nplus_overhang_diff.v*2)/(stepsize*2));
 				impy = floaty((gate_length.v+diff_poly_overhang.v*2+nplus_overhang_diff.v*2)/(stepsize*2));
 			}
 			String diffx = floaty(gate_width.v/(stepsize*2));
 			String diffy = floaty((gate_length.v+diff_poly_overhang.v*2)/(stepsize*2));
 			String porty = floaty((gate_length.v+diff_poly_overhang.v*2-diff_width.v)/(stepsize*2));
 			String polyx = floaty((gate_width.v+poly_endcap.v*2)/(stepsize*2));
 			String polyy = floaty(gate_length.v/(stepsize*2));
 			String polyx2 = floaty((poly_endcap.v*2)/(stepsize*2));
 			String sx = floaty(TextUtils.atof(polyx)-TextUtils.atof(diffx));
 			String sy = floaty(TextUtils.atof(diffy)-TextUtils.atof(polyy));
 			pw.println();
 			pw.println("<!-- " + t + "-Transistor -->");
 			pw.println();
 			pw.println("    <primitiveNode name=\"" + t + "-Transistor\" fun=\"TRA" + t + "MOS\">");
 //			pw.println("        <diskOffset untilVersion=\"2\" x=\"" + polyx + "\" y=\"" + diffy + "\"/>");
 //			pw.println("        <sizeOffset lx=\"" + sx + "\" hx=\"" + sx + "\" ly=\"" + sy + "\" hy=\"" + sy + "\"/>");
 
 			pw.println("        <nodeLayer layer=\"Poly\" style=\"FILLED\">");
 			pw.println("        <box>");
 			pw.println("            <lambdaBox klx=\"-" + polyx + "\" khx=\"" + polyx + "\" kly=\"-" + polyy + "\" khy=\"" + polyy + "\"/>");
 			pw.println("        </box>");
 			pw.println("        </nodeLayer>");
 
 			pw.println("        <nodeLayer layer=\"PolyGate\" style=\"FILLED\">");
 			pw.println("        <box>");
 			pw.println("            <lambdaBox klx=\"-" + diffx + "\" khx=\"" + diffx + "\" kly=\"-" + polyy + "\" khy=\"" + polyy + "\"/>");
 			pw.println("        </box>");
 			pw.println("        </nodeLayer>");
 
 			pw.println("        <nodeLayer layer=\"" + t + "-Diff\" style=\"FILLED\">");
 			pw.println("        <box>");
 			pw.println("            <lambdaBox klx=\"-" + diffx + "\" khx=\"" + diffx + "\" kly=\"-" + diffy + "\" khy=\"" + diffy + "\"/>");
 			pw.println("        </box>");
 			pw.println("        </nodeLayer>");
 
 			pw.println("        <nodeLayer layer=\"" + t + "Plus\" style=\"FILLED\">");
 			pw.println("        <box>");
 			pw.println("            <lambdaBox klx=\"-" + impx + "\" khx=\"" + impx + "\" kly=\"-" + impy + "\" khy=\"" + impy + "\"/>");
 			pw.println("        </box>");
 			pw.println("        </nodeLayer>");
 
 			pw.println("        <nodeLayer layer=\"DeviceMark\" style=\"FILLED\">");
 			pw.println("        <box>");
 			pw.println("            <lambdaBox klx=\"-" + impx + "\" khx=\"" + impx + "\" kly=\"-" + impy + "\" khy=\"" + impy + "\"/>");
 			pw.println("        </box>");
 			pw.println("        </nodeLayer>");
 
 			if (i==0)
 			{
 				pw.println("        <nodeLayer layer=\"N-Well\" style=\"FILLED\">");
 				pw.println("        <box>");
 				pw.println("            <lambdaBox klx=\"-" + wellx + "\" khx=\"" + wellx + "\" kly=\"-" + welly + "\" khy=\"" + welly + "\"/>");
 				pw.println("        </box>");
 				pw.println("        </nodeLayer>");
 			}
 
 			pw.println("        <primitivePort name=\"Gate-Left\">");
 			pw.println("            <portAngle primary=\"180\" range=\"90\"/>");
 			pw.println("            <portTopology>0</portTopology>");
 			pw.println("            <box>");
 			pw.println("                <lambdaBox klx=\"-" + polyx + "\" khx=\"-" + polyx2 + "\" kly=\"-" + polyy + "\" khy=\"" + polyy + "\"/>");
 			pw.println("            </box>");
 			pw.println("            <portArc>Poly</portArc>");
 			pw.println("        </primitivePort>");
 
 			pw.println("        <primitivePort name=\"Diff-Top\">");
 			pw.println("            <portAngle primary=\"90\" range=\"90\"/>");
 			pw.println("            <portTopology>1</portTopology>");
 			pw.println("            <box>");
 			pw.println("                <lambdaBox klx=\"-" + diffx + "\" khx=\"" + diffx + "\" kly=\"" + porty + "\" khy=\"" + porty + "\"/>");
 			pw.println("            </box>");
 			pw.println("            <portArc>" + t + "-Diff</portArc>");
 			pw.println("        </primitivePort>");
 
 			pw.println("        <primitivePort name=\"Gate-Right\">");
 			pw.println("            <portAngle primary=\"0\" range=\"90\"/>");
 			pw.println("            <portTopology>0</portTopology>");
 			pw.println("            <box>");
 			pw.println("                <lambdaBox klx=\"" + polyx2 + "\" khx=\"" + polyx + "\" kly=\"-" + polyy + "\" khy=\"" + polyy + "\"/>");
 			pw.println("            </box>");
 			pw.println("            <portArc>Poly</portArc>");
 			pw.println("        </primitivePort>");
 
 			pw.println("        <primitivePort name=\"Diff-Bottom\">");
 			pw.println("            <portAngle primary=\"270\" range=\"90\"/>");
 			pw.println("            <portTopology>2</portTopology>");
 			pw.println("            <box>");
 			pw.println("                <lambdaBox klx=\"-" + diffx + "\" khx=\"" + diffx + "\" kly=\"-" + porty + "\" khy=\"-" + porty + "\"/>");
 			pw.println("            </box>");
 			pw.println("            <portArc>" + t + "-Diff</portArc>");
 			pw.println("        </primitivePort>");
 			pw.println("    </primitiveNode>");
 		}
 
 		// write trailing boilerplate
 		pw.println();
 		pw.println("<!--  SKELETON HEADERS  -->");
 		pw.println();
 		pw.println("    <spiceHeader level=\"1\">");
 		pw.println("        <spiceLine line=\"* Spice header (level 1)\"/>");
 		pw.println("    </spiceHeader>");
 		pw.println();
 		pw.println("    <spiceHeader level=\"2\">");
 		pw.println("        <spiceLine line=\"* Spice header (level 2)\"/>");
 		pw.println("    </spiceHeader>");
 
 		// write the component menu layout
 		int ts = 5;
 		pw.println();
 		pw.println("<!--  PALETTE  -->");
 		pw.println();
 		pw.println("    <menuPalette numColumns=\"3\">");
 		for(int i=1; i<=num_metal_layers; i++)
 		{
 			int h = i-1;
 			pw.println();
 			pw.println("        <menuBox>");
 			pw.println("            <menuArc>Metal-" + i + "</menuArc>");
 			pw.println("        </menuBox>");
 			pw.println("        <menuBox>");
 			pw.println("            <menuNode>Metal-" + i + "-Pin</menuNode>");
 			pw.println("        </menuBox>");
 			if (i != 1)
 			{
 				pw.println("        <menuBox>");
                 String name = "Metal-" + h + "-Metal-" + i + "-Con";
                 pw.println("            <menuNodeInst protoName=\"" + name + "\" function=\"CONTACT\">");
 				pw.println("                <menuNodeText text=\"" + h + "   " + i + "\" size=\"" + ts + "\"/>");
 				pw.println("            </menuNodeInst>");
 				pw.println("            <menuNodeInst protoName=\"" + name + "-X\" function=\"CONTACT\"/>");
 				pw.println("        </menuBox>");
 			} else
 			{
 				pw.println("        <menuBox>");
 				pw.println("        </menuBox>");
 			}
 		}
 		pw.println();
 		pw.println("        <menuBox>");
 		pw.println("            <menuArc>Poly</menuArc>");
 		pw.println("        </menuBox>");
 		pw.println("        <menuBox>");
 		pw.println("            <menuNode>Poly-Pin</menuNode>");
 		pw.println("        </menuBox>");
 		pw.println("        <menuBox>");
 		pw.println("            <menuNodeInst protoName=\"Poly-Metal-1-Con\" function=\"CONTACT\"/>");
 //		pw.println("            </menuNodeInst>");
 		pw.println("            <menuNodeInst protoName=\"Poly-Metal-1-Con-X\" function=\"CONTACT\"/>");
 		pw.println("        </menuBox>");
 		pw.println();
 		pw.println("        <menuBox>");
 		pw.println("            <menuArc>P-Diff</menuArc>");
 		pw.println("        </menuBox>");
 		pw.println("        <menuBox>");
 		pw.println("            <menuNode>P-Diff-Pin</menuNode>");
 		pw.println("        </menuBox>");
 		pw.println("        <menuBox>");
 		pw.println("            <menuNodeInst protoName=\"P-Transistor\" function=\"TRAPMOS\">");
 		pw.println("                <menuNodeText text=\"P\" size=\"" + ts + "\"/>");
 		pw.println("            </menuNodeInst>");
 		pw.println("        </menuBox>");
 		pw.println();
 		pw.println("        <menuBox>");
 		pw.println("            <menuArc>N-Diff</menuArc>");
 		pw.println("        </menuBox>");
 		pw.println("        <menuBox>");
 		pw.println("            <menuNode>N-Diff-Pin</menuNode>");
 		pw.println("        </menuBox>");
 		pw.println("        <menuBox>");
 		pw.println("            <menuNodeInst protoName=\"N-Transistor\" function=\"TRANMOS\">");
 		pw.println("                <menuNodeText text=\"N\" size=\"" + ts + "\"/>");
 		pw.println("            </menuNodeInst>");
 		pw.println("        </menuBox>");
 		pw.println();
 		pw.println("        <menuBox>");
 		pw.println("            <menuNodeInst protoName=\"VSS-Tie-Metal-1\" function=\"SUBSTRATE\">");
 		pw.println("                <menuNodeText text=\"VSS-Tie\" size=\"" + ts + "\"/>");
 		pw.println("            </menuNodeInst>");
 		pw.println("            <menuNodeInst protoName=\"VSS-Tie-Metal-1-X\" function=\"SUBSTRATE\"/>");
 		pw.println("        </menuBox>");
 		pw.println();
 		pw.println("        <menuBox>");
 		pw.println("            <menuNodeInst protoName=\"N-Diff-Metal-1\" function=\"CONTACT\">");
 		pw.println("                <menuNodeText text=\"N-Con\" size=\"" + ts + "\"/>");
 		pw.println("            </menuNodeInst>");
 		pw.println("            <menuNodeInst protoName=\"N-Diff-Metal-1-X\" function=\"CONTACT\"/>");
 		pw.println("        </menuBox>");
 		pw.println("        <menuBox>");
 		pw.println("        </menuBox>");
 		pw.println();
 		pw.println("        <menuBox>");
 		pw.println("            <menuNodeInst protoName=\"VDD-Tie-Metal-1\" function=\"WELL\">");
 		pw.println("                <menuNodeText text=\"VDD-Tie\" size=\"" + ts + "\"/>");
 		pw.println("            </menuNodeInst>");
 		pw.println("            <menuNodeInst protoName=\"VDD-Tie-Metal-1-X\" function=\"WELL\"/>");
 		pw.println("        </menuBox>");
 		pw.println("        <menuBox>");
 		pw.println("            <menuNodeInst protoName=\"P-Diff-Metal-1\" function=\"CONTACT\">");
 		pw.println("                <menuNodeText text=\"P-Con\" size=\"" + ts + "\"/>");
 		pw.println("            </menuNodeInst>");
 		pw.println("            <menuNodeInst protoName=\"P-Diff-Metal-1-X\" function=\"CONTACT\"/>");
 		pw.println("        </menuBox>");
 		pw.println("        <menuBox>");
 		pw.println("        </menuBox>");
 		pw.println();
 		pw.println("        <menuBox>");
 		pw.println("            <menuText>Pure</menuText>");
 		pw.println("        </menuBox>");
 		pw.println("        <menuBox>");
 		pw.println("            <menuText>Misc.</menuText>");
 		pw.println("        </menuBox>");
 		pw.println("        <menuBox>");
 		pw.println("            <menuText>Cell</menuText>");
 		pw.println("        </menuBox>");
 		pw.println("    </menuPalette>");
 		pw.println();
 		pw.println("    <Foundry name=\"" + foundry_name + "\">");
 
         // write GDS layers
 		pw.println();
 		for(int i=1; i<=num_metal_layers; i++)
 		{
 			pw.println("        <layerGds layer=\"Metal-" + i + "\" gds=\"" + gds_metal_layer[i-1] + "\"/>");
 			if (i != num_metal_layers)
 			{
 				pw.println("        <layerGds layer=\"Via-" + i + "\" gds=\"" + gds_via_layer[i-1] + "\"/>");
 			}
 		}
 		pw.println("        <layerGds layer=\"Poly\" gds=\"" + gds_poly_layer + "\"/>");
 		pw.println("        <layerGds layer=\"PolyGate\" gds=\"" + gds_poly_layer + "\"/>");
 		pw.println("        <layerGds layer=\"DiffCon\" gds=\"" + gds_contact_layer + "\"/>");
 		pw.println("        <layerGds layer=\"PolyCon\" gds=\"" + gds_contact_layer + "\"/>");
 		pw.println("        <layerGds layer=\"N-Diff\" gds=\"" + gds_diff_layer + "\"/>");
 		pw.println("        <layerGds layer=\"P-Diff\" gds=\"" + gds_diff_layer + "\"/>");
 		pw.println("        <layerGds layer=\"NPlus\" gds=\"" + gds_nplus_layer + "\"/>");
 		pw.println("        <layerGds layer=\"PPlus\" gds=\"" + gds_pplus_layer + "\"/>");
 		pw.println("        <layerGds layer=\"N-Well\" gds=\"" + gds_nwell_layer + "\"/>");
 		pw.println("        <layerGds layer=\"DeviceMark\" gds=\"" + gds_marking_layer + "\"/>");
 
 		// write GDS layers
 		pw.println();
 		for(int i=1; i<=num_metal_layers; i++)
 		{
 			pw.println("        <layerGds layer=\"Metal-" + i + "\" gds=\"" + gds_metal_layer[i-1] + "\"/>");
 			if (i != num_metal_layers)
 			{
 				pw.println("        <layerGds layer=\"Via-" + i + "\" gds=\"" + gds_via_layer[i-1] + "\"/>");
 			}
 		}
 		pw.println("        <layerGds layer=\"Poly\" gds=\"" + gds_poly_layer + "\"/>");
 		pw.println("        <layerGds layer=\"PolyGate\" gds=\"" + gds_poly_layer + "\"/>");
 		pw.println("        <layerGds layer=\"DiffCon\" gds=\"" + gds_contact_layer + "\"/>");
 		pw.println("        <layerGds layer=\"PolyCon\" gds=\"" + gds_contact_layer + "\"/>");
 		pw.println("        <layerGds layer=\"N-Diff\" gds=\"" + gds_diff_layer + "\"/>");
 		pw.println("        <layerGds layer=\"P-Diff\" gds=\"" + gds_diff_layer + "\"/>");
 		pw.println("        <layerGds layer=\"NPlus\" gds=\"" + gds_nplus_layer + "\"/>");
 		pw.println("        <layerGds layer=\"PPlus\" gds=\"" + gds_pplus_layer + "\"/>");
 		pw.println("        <layerGds layer=\"N-Well\" gds=\"" + gds_nwell_layer + "\"/>");
 		pw.println("        <layerGds layer=\"DeviceMark\" gds=\"" + gds_marking_layer + "\"/>");
 		pw.println();
 
 		// Write basic design rules not implicit in primitives
 
 		// WIDTHS
 		for(int i=0; i<num_metal_layers; i++)
 		{
 			pw.println("        <LayerRule ruleName=\"" + metal_width[i].rule + "\" layerName=\"Metal-" + (i+1) + "\" type=\"MINWID\" when=\"ALL\" value=\"" + floaty(metal_width[i].v/stepsize) + "\"/>");
 		}
 
 		pw.println("        <LayerRule ruleName=\"" + diff_width.rule + "\" layerName=\"N-Diff\" type=\"MINWID\" when=\"ALL\" value=\"" + floaty(diff_width.v/stepsize) + "\"/>");
 		pw.println("        <LayerRule ruleName=\"" + diff_width.rule + "\" layerName=\"P-Diff\" type=\"MINWID\" when=\"ALL\" value=\"" + floaty(diff_width.v/stepsize) + "\"/>");
 		pw.println("        <LayerRule ruleName=\"" + nwell_width.rule + "\" layerName=\"N-Well\" type=\"MINWID\" when=\"ALL\" value=\"" + floaty(nwell_width.v/stepsize) + "\"/>");
 		pw.println("        <LayerRule ruleName=\"" + nplus_width.rule + "\" layerName=\"NPlus\" type=\"MINWID\" when=\"ALL\" value=\"" + floaty(nplus_width.v/stepsize) + "\"/>");
 		pw.println("        <LayerRule ruleName=\"" + pplus_width.rule + "\" layerName=\"PPlus\" type=\"MINWID\" when=\"ALL\" value=\"" + floaty(pplus_width.v/stepsize) + "\"/>");
 		pw.println("        <LayerRule ruleName=\"" + poly_width.rule + "\" layerName=\"Poly\" type=\"MINWID\" when=\"ALL\" value=\"" + floaty(poly_width.v/stepsize) + "\"/>");
 		pw.println("        <LayerRule ruleName=\"" + poly_width.rule + "\" layerName=\"PolyGate\" type=\"MINWID\" when=\"ALL\" value=\"" + floaty(poly_width.v/stepsize) + "\"/>");
 
 		// SPACINGS
 		pw.println("        <LayersRule ruleName=\"" + diff_spacing.rule + "\" layerNames=\"{N-Diff,N-Diff}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(diff_spacing.v/stepsize) + "\"/>");
 		pw.println("        <LayersRule ruleName=\"" + diff_spacing.rule + "\" layerNames=\"{N-Diff,P-Diff}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(diff_spacing.v/stepsize) + "\"/>");
 		pw.println("        <LayersRule ruleName=\"" + diff_spacing.rule + "\" layerNames=\"{P-Diff,P-Diff}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(diff_spacing.v/stepsize) + "\"/>");
 		pw.println("        <LayersRule ruleName=\"" + poly_diff_spacing.rule + "\" layerNames=\"{Poly,N-Diff}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(poly_diff_spacing.v/stepsize) + "\"/>");
 		pw.println("        <LayersRule ruleName=\"" + poly_diff_spacing.rule + "\" layerNames=\"{Poly,P-Diff}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(poly_diff_spacing.v/stepsize) + "\"/>");
 		pw.println("        <LayersRule ruleName=\"" + poly_spacing.rule + "\" layerNames=\"{Poly,Poly}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(poly_spacing.v/stepsize) + "\"/>");
 
 		pw.println("        <LayersRule ruleName=\"" + gate_spacing.rule + "\" layerNames=\"{PolyGate,PolyGate}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(gate_spacing.v/stepsize) + "\"/>");
 
 		pw.println("        <LayersRule ruleName=\"" + nwell_spacing.rule + "\" layerNames=\"{N-Well,N-Well}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(nwell_spacing.v/stepsize) + "\"/>");
 		pw.println("        <LayersRule ruleName=\"" + nplus_spacing.rule + "\" layerNames=\"{NPlus,NPlus}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(nplus_spacing.v/stepsize) + "\"/>");
 		pw.println("        <LayersRule ruleName=\"" + pplus_spacing.rule + "\" layerNames=\"{PPlus,PPlus}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(pplus_spacing.v/stepsize) + "\"/>");
 
 		pw.println("        <LayersRule ruleName=\"" + contact_spacing.rule + "\" layerNames=\"{PolyCon,PolyCon}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(contact_spacing.v/stepsize) + "\"/>");
 		pw.println("        <LayersRule ruleName=\"" + contact_spacing.rule + "\" layerNames=\"{DiffCon,DiffCon}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(contact_spacing.v/stepsize) + "\"/>");
 
 		pw.println("        <LayersRule ruleName=\"" + polycon_diff_spacing.rule + "\" layerNames=\"{PolyCon,N-Diff}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(polycon_diff_spacing.v/stepsize) + "\"/>");
 		pw.println("        <LayersRule ruleName=\"" + polycon_diff_spacing.rule + "\" layerNames=\"{PolyCon,P-Diff}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(polycon_diff_spacing.v/stepsize) + "\"/>");
 
 		pw.println("        <LayersRule ruleName=\"" + gate_contact_spacing.rule + "\" layerNames=\"{DiffCon,Poly}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(gate_contact_spacing.v/stepsize) + "\"/>");
 		pw.println("        <LayersRule ruleName=\"" + gate_contact_spacing.rule + "\" layerNames=\"{DiffCon,PolyGate}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(gate_contact_spacing.v/stepsize) + "\"/>");
 
 		for(int i=1; i<=num_metal_layers; i++)
 		{
 			pw.println("        <LayersRule ruleName=\"" + metal_spacing[i-1].rule + "\" layerNames=\"{Metal-" + i + ",Metal-" + i + "}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(metal_spacing[i-1].v/stepsize) + "\"/>");
 			if (i != num_metal_layers)
 			{
 				pw.println("        <LayersRule ruleName=\"" + via_spacing[i-1].rule + "\" layerNames=\"{Via-" + i + ",Via-" + i + "}\" type=\"UCONSPA\" when=\"ALL\" value=\"" + floaty(via_spacing[i-1].v/stepsize) + "\"/>");
 			}
 		}
 		pw.println("    </Foundry>");
 		pw.println();
 		pw.println("</technology>");
 	}
 
 	private String floaty(double v)
 	{
         if (v < 0)
             System.out.println("Negative distance of " + v + " in the tech editor wizard");
 //        long rounded = Math.round(v);
 //		if (Math.abs(rounded-v) < 0.001) return rounded + ".0";
         double roundedV = DBMath.round(v);
         return roundedV + "";
 	}
 }
