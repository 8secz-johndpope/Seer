 /*******************************************************************************
  * Copyright (c) 2005, 2009 Andrea Bittau, University College London, and others
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Andrea Bittau - initial API and implementation from the PsychoPath XPath 2.0
  *     Mukul Gandhi - bug 274805 - improvements to xs:integer data type 
  *******************************************************************************/
 
 package org.eclipse.wst.xml.xpath2.processor.internal.types;
 
 import java.math.BigInteger;
import java.text.DecimalFormat;
 
 import org.eclipse.wst.xml.xpath2.processor.DynamicError;
 import org.eclipse.wst.xml.xpath2.processor.ResultSequence;
 import org.eclipse.wst.xml.xpath2.processor.ResultSequenceFactory;
 import org.eclipse.wst.xml.xpath2.processor.internal.*;
 
 /**
  * A representation of the Decimal datatype
  */
 public class XSDecimal extends NumericType {
 
 	private double _value;
	private DecimalFormat format = new DecimalFormat("0.##################");
 
 	/**
 	 * Initiates a representation of 0.0
 	 */
 	public XSDecimal() {
 		this(0.0);
 	}
 
 	/**
 	 * Initiates a representation of the supplied number
 	 * 
 	 * @param x
 	 *            Number to be stored
 	 */
 	public XSDecimal(double x) {
 		_value = x;
 	}
 
 	/**
 	 * Retrieves the datatype's full pathname
 	 * 
 	 * @return "xs:decimal" which is the datatype's full pathname
 	 */
 	@Override
 	public String string_type() {
 		return "xs:decimal";
 	}
 
 	/**
 	 * Retrieves the datatype's name
 	 * 
 	 * @return "decimal" which is the datatype's name
 	 */
 	@Override
 	public String type_name() {
 		return "decimal";
 	}
 
 	/**
 	 * Retrieves a String representation of the Decimal value stored
 	 * 
 	 * @return String representation of the Decimal value stored
 	 */
 	@Override
 	public String string_value() {

		return format.format(_value);
 	}
 
 	/**
 	 * Check if this XSDecimal represents 0
 	 * 
 	 * @return True if this XSDecimal represents 0. False otherwise
 	 */
 	@Override
 	public boolean zero() {
 		return _value == 0.0;
 	}
 
 	/**
 	 * Creates a new result sequence consisting of the retrievable decimal
 	 * number in the supplied result sequence
 	 * 
 	 * @param arg
 	 *            The result sequence from which to extract the decimal number.
 	 * @throws DynamicError
 	 * @return A new result sequence consisting of the decimal number supplied.
 	 */
 	@Override
 	public ResultSequence constructor(ResultSequence arg) throws DynamicError {
 		ResultSequence rs = ResultSequenceFactory.create_new();
 
 		if (arg.empty())
 			return rs;
 
 		AnyType aat = arg.first();
 
 		try {
 			Double d = new Double(aat.string_value());
 			rs.add(new XSDecimal(d.doubleValue()));
 			return rs;
 		} catch (NumberFormatException e) {
 			throw DynamicError.cant_cast(null);
 		}
 
 	}
 
 	/**
 	 * Retrieves the actual value of the number stored
 	 * 
 	 * @return The actual value of the number stored
 	 */
 	public double double_value() {
 		return _value;
 	}
 
 	/**
 	 * Sets the number stored to that supplied
 	 * 
 	 * @param x
 	 *            Number to be stored
 	 */
 	public void set_double(double x) {
 		_value = x;
 	}
 
 	// comparisons
 	/**
 	 * Equality comparison between this number and the supplied representation.
 	 * Currently no numeric type promotion exists so the supplied representation
 	 * must be of type XSDecimal.
 	 * 
 	 * @param at
 	 *            Representation to be compared with (must currently be of type
 	 *            XSDecimal)
 	 * @return True if the 2 representation represent the same number. False
 	 *         otherwise
 	 */
 	public boolean eq(AnyType at) throws DynamicError {
 
 		if (!(at instanceof XSDecimal))
 			DynamicError.throw_type_error();
 
 		XSDecimal dt = (XSDecimal) at;
 
 		return double_value() == dt.double_value();
 	}
 
 	/**
 	 * Comparison between this number and the supplied representation. Currently
 	 * no numeric type promotion exists so the supplied representation must be
 	 * of type XSDecimal.
 	 * 
 	 * @param arg
 	 *            Representation to be compared with (must currently be of type
 	 *            XSDecimal)
 	 * @return True if the supplied type represents a number smaller than this
 	 *         one stored. False otherwise
 	 */
 	public boolean gt(AnyType arg) throws DynamicError {
 		XSDecimal val = (XSDecimal) get_single_type(arg, XSDecimal.class);
 		return double_value() > val.double_value();
 	}
 
 	/**
 	 * Comparison between this number and the supplied representation. Currently
 	 * no numeric type promotion exists so the supplied representation must be
 	 * of type XSDecimal.
 	 * 
 	 * @param arg
 	 *            Representation to be compared with (must currently be of type
 	 *            XSDecimal)
 	 * @return True if the supplied type represents a number greater than this
 	 *         one stored. False otherwise
 	 */
 	public boolean lt(AnyType arg) throws DynamicError {
 		XSDecimal val = (XSDecimal) get_single_type(arg, XSDecimal.class);
 		return double_value() < val.double_value();
 	}
 
 	// math
 	/**
 	 * Mathematical addition operator between this XSDecimal and the supplied
 	 * ResultSequence. Due to no numeric type promotion or conversion, the
 	 * ResultSequence must be of type XSDecimal.
 	 * 
 	 * @param arg
 	 *            The ResultSequence to perform an addition with
 	 * @return A XSDecimal consisting of the result of the mathematical
 	 *         addition.
 	 */
 	public ResultSequence plus(ResultSequence arg) throws DynamicError {
 		// get arg
 		AnyType at = get_single_arg(arg);
 		if (!(at instanceof XSDecimal))
 			DynamicError.throw_type_error();
 		XSDecimal dt = (XSDecimal) at;
 
 		// own it
 		return ResultSequenceFactory.create_new(new XSDecimal(double_value()
 				+ dt.double_value()));
 	}
 
 	/**
 	 * Mathematical subtraction operator between this XSDecimal and the supplied
 	 * ResultSequence. Due to no numeric type promotion or conversion, the
 	 * ResultSequence must be of type XSDecimal.
 	 * 
 	 * @param arg
 	 *            The ResultSequence to perform a subtraction with
 	 * @return A XSDecimal consisting of the result of the mathematical
 	 *         subtraction.
 	 */
 	public ResultSequence minus(ResultSequence arg) throws DynamicError {
 		AnyType at = get_single_arg(arg);
 		if (!(at instanceof XSDecimal))
 			DynamicError.throw_type_error();
 		XSDecimal dt = (XSDecimal) at;
 
 		return ResultSequenceFactory.create_new(new XSDecimal(double_value()
 				- dt.double_value()));
 	}
 
 	/**
 	 * Mathematical multiplication operator between this XSDecimal and the
 	 * supplied ResultSequence. Due to no numeric type promotion or conversion,
 	 * the ResultSequence must be of type XSDecimal.
 	 * 
 	 * @param arg
 	 *            The ResultSequence to perform a multiplication with
 	 * @return A XSDecimal consisting of the result of the mathematical
 	 *         multiplication.
 	 */
 	public ResultSequence times(ResultSequence arg) throws DynamicError {
 		XSDecimal val = (XSDecimal) get_single_type(arg, XSDecimal.class);
 		return ResultSequenceFactory.create_new(new XSDecimal(double_value()
 				* val.double_value()));
 	}
 
 	/**
 	 * Mathematical division operator between this XSDecimal and the supplied
 	 * ResultSequence. Due to no numeric type promotion or conversion, the
 	 * ResultSequence must be of type XSDecimal.
 	 * 
 	 * @param arg
 	 *            The ResultSequence to perform a division with
 	 * @return A XSDecimal consisting of the result of the mathematical
 	 *         division.
 	 */
 	public ResultSequence div(ResultSequence arg) throws DynamicError {
 		XSDecimal val = (XSDecimal) get_single_type(arg, XSDecimal.class);
 		if (val.zero())
 			throw DynamicError.div_zero(null);
 		return ResultSequenceFactory.create_new(new XSDecimal(double_value()
 				/ val.double_value()));
 	}
 
 	/**
 	 * Mathematical integer division operator between this XSDecimal and the
 	 * supplied ResultSequence. Due to no numeric type promotion or conversion,
 	 * the ResultSequence must be of type XSDecimal.
 	 * 
 	 * @param arg
 	 *            The ResultSequence to perform an integer division with
 	 * @return A XSInteger consisting of the result of the mathematical integer
 	 *         division.
 	 */
 	public ResultSequence idiv(ResultSequence arg) throws DynamicError {
 		XSDecimal val = (XSDecimal) get_single_type(arg, XSDecimal.class);
 
 		if (val.zero())
 			throw DynamicError.div_zero(null);
 		return ResultSequenceFactory.create_new(new 
 				           XSInteger(BigInteger.valueOf((int) (double_value() / val.double_value()))));
 	}
 
 	/**
 	 * Mathematical modulus operator between this XSDecimal and the supplied
 	 * ResultSequence. Due to no numeric type promotion or conversion, the
 	 * ResultSequence must be of type XSDecimal.
 	 * 
 	 * @param arg
 	 *            The ResultSequence to perform a modulus with
 	 * @return A XSDecimal consisting of the result of the mathematical modulus.
 	 */
 	public ResultSequence mod(ResultSequence arg) throws DynamicError {
 		XSDecimal val = (XSDecimal) get_single_type(arg, XSDecimal.class);
 		return ResultSequenceFactory.create_new(new XSDecimal(double_value()
 				% val.double_value()));
 	}
 
 	/**
 	 * Negation of the number stored
 	 * 
 	 * @return A XSDecimal representing the negation of this XSDecimal
 	 */
 	@Override
 	public ResultSequence unary_minus() {
 		return ResultSequenceFactory.create_new(new XSDecimal(-1
 				* double_value()));
 	}
 
 	// functions
 	/**
 	 * Absolutes the number stored
 	 * 
 	 * @return A XSDecimal representing the absolute value of the number stored
 	 */
 	@Override
 	public NumericType abs() {
 		return new XSDecimal(Math.abs(double_value()));
 	}
 
 	/**
 	 * Returns the smallest integer greater than the number stored
 	 * 
 	 * @return A XSDecimal representing the smallest integer greater than the
 	 *         number stored
 	 */
 	@Override
 	public NumericType ceiling() {
 		return new XSDecimal(Math.ceil(double_value()));
 	}
 
 	/**
 	 * Returns the largest integer smaller than the number stored
 	 * 
 	 * @return A XSDecimal representing the largest integer smaller than the
 	 *         number stored
 	 */
 	@Override
 	public NumericType floor() {
 		return new XSDecimal(Math.floor(double_value()));
 	}
 
 	/**
 	 * Returns the closest integer of the number stored.
 	 * 
 	 * @return A XSDecimal representing the closest long of the number stored.
 	 */
 	@Override
 	public NumericType round() {
 		return new XSDecimal(Math.round(double_value()));
 	}
 
 	/**
 	 * Returns the closest integer of the number stored.
 	 * 
 	 * @return A XSDecimal representing the closest long of the number stored.
 	 */
 	@Override
 	public NumericType round_half_to_even() {
 		return new XSDecimal(Math.rint(double_value()));
 	}
 }
