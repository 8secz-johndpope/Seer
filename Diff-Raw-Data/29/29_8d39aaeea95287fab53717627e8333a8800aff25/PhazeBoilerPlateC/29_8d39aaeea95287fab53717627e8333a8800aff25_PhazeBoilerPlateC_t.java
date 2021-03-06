 /*----------------------------------------------------------------------------*
  * PhazeBoilerPlate
  *----------------------------------------------------------------------------*/
 
 package Phaze;
 
 public class PhazeBoilerPlateC implements PhazeBoilerPlate {
 
 	static final String comment_start_ =
 		"/*--------------------------------------" +
 		"--------------------------------------*\n";
 
 	static final String comment_end_ =
 		" *--------------------------------------" +
 		"--------------------------------------*/\n";
 
 	static final String header_ = comment_start_ +
 		" * Copyright (c) 2012 Los Alamos National Security, LLC\n" +
 		" * All rights reserved.\n" +
 		" *\n" +
 		" * DO NOT EDIT!!! THIS FILE WAS GENERATED BY PHAZE\n" +
		" * Input File: %s\n" + comment_end_;
 
 	public String startComment() { return comment_start_; }

 	public String endComment() { return comment_end_; }

 	public String continueComment() { return " *"; }

	public String genericHeader(String inputFile) {
		return String.format(header_, inputFile);
	} // genericHeader

 	public String staticInterface() { return PhazeInterfaceC.staticInterface; }
 
 } // class PhazeBoilerPlate
