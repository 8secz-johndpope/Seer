 /*
  * Copyright (c) 2008-2010, Stephen Colebourne & Michael Nascimento Santos
  *
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *
  *  * Redistributions of source code must retain the above copyright notice,
  *    this list of conditions and the following disclaimer.
  *
  *  * Redistributions in binary form must reproduce the above copyright notice,
  *    this list of conditions and the following disclaimer in the documentation
  *    and/or other materials provided with the distribution.
  *
  *  * Neither the name of JSR-310 nor the names of its contributors
  *    may be used to endorse or promote products derived from this software
  *    without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
  * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package javax.time.period;
 
 import java.io.Serializable;
 import java.util.TreeMap;
 
 import javax.time.CalendricalException;
 import javax.time.Duration;
 import javax.time.MathUtils;
 import javax.time.calendar.ISOChronology;
 import javax.time.calendar.PeriodUnit;
 import javax.time.calendar.format.CalendricalParseException;
 
 /**
  * An immutable period consisting of the ISO-8601 year, month, day, hour,
  * minute, second and nanosecond units, such as '3 Months, 4 Days and 7 Hours'.
  * <p>
  * A period is a human-scale description of an amount of time.
  * This class represents the 7 standard definitions from {@link ISOChronology}.
  * The period units used are 'Years', 'Months', 'Days', 'Hours', 'Minutes',
  * 'Seconds' and 'Nanoseconds'.
  * <p>
  * The {@code ISOChronology} defines a relationship between some of the units:
  * <ul>
  * <li>12 months in a year</li>
  * <li>24 hours in a day (ignoring time-zones)</li>
  * <li>60 minutes in an hour</li>
  * <li>60 seconds in a minute</li>
  * <li>1,000,000,000 nanoseconds in a second</li>
  * </ul>
  * The 24 hours in a day connection is not always true, due to time-zone changes.
  * As such, methods on this class make it clear when the that connection is being used.
  * <p>
  * Period is immutable and thread-safe.
  *
  * @author Stephen Colebourne
  */
 public final class Period
         implements PeriodProvider, Serializable {
 
     /**
      * A constant for a period of zero.
      */
     public static final Period ZERO = new Period(0, 0, 0, 0, 0, 0, 0);
     /**
      * The serialization version.
      */
     private static final long serialVersionUID = 1L;
     /**
      * The ISO period units, trusted to not be altered.
      */
     private static final PeriodUnit[] UNITS = new PeriodUnit[] {
         ISOChronology.periodYears(), ISOChronology.periodMonths(), ISOChronology.periodDays(),
         ISOChronology.periodHours(), ISOChronology.periodMinutes(), ISOChronology.periodSeconds(), ISOChronology.periodNanos(),
     };
 
     /**
      * The number of years.
      */
     private final int years;
     /**
      * The number of months.
      */
     private final int months;
     /**
      * The number of days.
      */
     private final int days;
     /**
      * The number of hours.
      */
     private final int hours;
     /**
      * The number of minutes.
      */
     private final int minutes;
     /**
      * The number of seconds.
      */
     private final int seconds;
     /**
      * The number of nanoseconds.
      */
     private final long nanos;
     /**
      * The cached PeriodFields.
      */
     private transient volatile PeriodFields periodFields;
     /**
      * The cached toString value.
      */
     private transient volatile String string;
 
     //-----------------------------------------------------------------------
     /**
      * Obtains a {@code Period} from a provider of periods.
      * <p>
      * A {@code Period} supports 7 units, ISO years, months, days, hours,
      * minutes, seconds and nanoseconds. Any period that contains amounts in
      * these units, or in units that can be converted to these units will be
      * accepted. If the provider contains any other unit, an exception is thrown.
      *
      * @param periodProvider  a provider of period information, not null
      * @return the {@code Period} instance, never null
      * @throws CalendricalException if the provided period cannot be converted to the supported units
      * @throws ArithmeticException if any provided amount, except nanos, exceeds an {@code int}
      */
     public static Period from(PeriodProvider periodProvider) {
         PeriodFields.checkNotNull(periodProvider, "PeriodProvider must not be null");
         if (periodProvider instanceof Period) {
             return (Period) periodProvider;
         }
         PeriodFields periodFields = PeriodFields.from(periodProvider);
         periodFields = periodFields.toEquivalentPeriod(UNITS);
         int years = periodFields.getAmountInt(ISOChronology.periodYears());
         int months = periodFields.getAmountInt(ISOChronology.periodMonths());
         int days = periodFields.getAmountInt(ISOChronology.periodDays());
         int hours = periodFields.getAmountInt(ISOChronology.periodHours());
         int minutes = periodFields.getAmountInt(ISOChronology.periodMinutes());
         int seconds = periodFields.getAmountInt(ISOChronology.periodSeconds());
         long nanos = periodFields.getAmount(ISOChronology.periodNanos());
         return of(years, months, days, hours, minutes, seconds, nanos);
     }
 
     /**
      * Obtains a {@code Period} from amounts from years to seconds.
      *
      * @param years  the amount of years, may be negative
      * @param months  the amount of months, may be negative
      * @param days  the amount of days, may be negative
      * @param hours  the amount of hours, may be negative
      * @param minutes  the amount of minutes, may be negative
      * @param seconds  the amount of seconds, may be negative
      * @return the {@code Period} instance, never null
      */
     public static Period of(int years, int months, int days, int hours, int minutes, int seconds) {
         if ((years | months | days | hours | minutes | seconds) == 0) {
             return ZERO;
         }
         return new Period(years, months, days, hours, minutes, seconds, 0);
     }
 
     /**
      * Obtains a {@code Period} from amounts from years to nanoseconds.
      *
      * @param years  the amount of years, may be negative
      * @param months  the amount of months, may be negative
      * @param days  the amount of days, may be negative
      * @param hours  the amount of hours, may be negative
      * @param minutes  the amount of minutes, may be negative
      * @param seconds  the amount of seconds, may be negative
      * @param nanos  the amount of nanos, may be negative
      * @return the {@code Period} instance, never null
      */
     public static Period of(int years, int months, int days, int hours, int minutes, int seconds, long nanos) {
         if ((years | months | days | hours | minutes | seconds | nanos) == 0) {
             return ZERO;
         }
         return new Period(years, months, days, hours, minutes, seconds, nanos);
     }
 
     /**
      * Obtains a {@code Period} from years, months and days.
      *
      * @param years  the amount of years, may be negative
      * @param months  the amount of months, may be negative
      * @return the {@code Period} instance, never null
      */
     public static Period yearsMonths(int years, int months) {
         if ((years | months) == 0) {
             return ZERO;
         }
         return new Period(years, months, 0, 0, 0, 0, 0);
     }
 
     /**
      * Obtains a {@code Period} from years, months and days.
      *
      * @param years  the amount of years, may be negative
      * @param months  the amount of months, may be negative
      * @param days  the amount of days, may be negative
      * @return the {@code Period} instance, never null
      */
     public static Period yearsMonthsDays(int years, int months, int days) {
         if ((years | months | days) == 0) {
             return ZERO;
         }
         return new Period(years, months, days, 0, 0, 0, 0);
     }
 
     /**
      * Obtains a {@code Period} from hours, minutes and seconds.
      *
      * @param hours  the amount of hours, may be negative
      * @param minutes  the amount of minutes, may be negative
      * @param seconds  the amount of seconds, may be negative
      * @return the {@code Period} instance, never null
      */
     public static Period hoursMinutesSeconds(int hours, int minutes, int seconds) {
         if ((hours | minutes | seconds) == 0) {
             return ZERO;
         }
         return new Period(0, 0, 0, hours, minutes, seconds, 0);
     }
 
     //-----------------------------------------------------------------------
     /**
      * Obtains a {@code Period} from a number of years.
      *
      * @param years  the amount of years, may be negative
      * @return the {@code Period} instance, never null
      */
     public static Period years(int years) {
         if (years == 0) {
             return ZERO;
         }
         return new Period(years, 0, 0, 0, 0, 0, 0);
     }
 
     /**
      * Obtains a {@code Period} from a number of months.
      *
      * @param months  the amount of months, may be negative
      * @return the {@code Period} instance, never null
      */
     public static Period months(int months) {
         if (months == 0) {
             return ZERO;
         }
         return new Period(0, months, 0, 0, 0, 0, 0);
     }
 
     /**
      * Obtains a {@code Period} from a number of days.
      *
      * @param days  the amount of days, may be negative
      * @return the {@code Period} instance, never null
      */
     public static Period days(int days) {
         if (days == 0) {
             return ZERO;
         }
         return new Period(0, 0, days, 0, 0, 0, 0);
     }
 
     /**
      * Obtains a {@code Period} from a number of hours.
      *
      * @param hours  the amount of hours, may be negative
      * @return the {@code Period} instance, never null
      */
     public static Period hours(int hours) {
         if (hours == 0) {
             return ZERO;
         }
         return new Period(0, 0, 0, hours, 0, 0, 0);
     }
 
     /**
      * Obtains a {@code Period} from a number of minutes.
      *
      * @param minutes  the amount of minutes, may be negative
      * @return the {@code Period} instance, never null
      */
     public static Period minutes(int minutes) {
         if (minutes == 0) {
             return ZERO;
         }
         return new Period(0, 0, 0, 0, minutes, 0, 0);
     }
 
     /**
      * Obtains a {@code Period} from a number of seconds.
      *
      * @param seconds  the amount of seconds, may be negative
      * @return the {@code Period} instance, never null
      */
     public static Period seconds(int seconds) {
         if (seconds == 0) {
             return ZERO;
         }
         return new Period(0, 0, 0, 0, 0, seconds, 0);
     }
 
     /**
      * Obtains a {@code Period} from a number of nanoseconds.
      *
      * @param nanos  the amount of nanos, may be negative
      * @return the {@code Period} instance, never null
      */
     public static Period nanos(long nanos) {
         if (nanos == 0) {
             return ZERO;
         }
         return new Period(0, 0, 0, 0, 0, 0, nanos);
     }
 
     //-----------------------------------------------------------------------
     /**
      * Obtains a {@code Period} from a string formatted as {@code PnYnMnDTnHnMn.nS}.
      * <p>
      * This will parse the string produced by {@code toString()} which is
      * a subset of the ISO8601 period format {@code PnYnMnDTnHnMn.nS}.
      * <p>
      * The string consists of a series of numbers with a suffix identifying their meaning.
      * The values, and suffixes, must be in the sequence year, month, day, hour, minute, second.
      * Any of the number/suffix pairs may be omitted providing at least one is present.
      * If the period is zero, the value is normally represented as {@code PT0S}.
      * The numbers must consist of ASCII digits.
      * Any of the numbers may be negative. Negative zero is not accepted.
      * The number of nanoseconds is expressed as an optional fraction of the seconds.
      * There must be at least one digit before any decimal point.
      * There must be between 1 and 9 inclusive digits after any decimal point.
      * The letters will all be accepted in upper or lower case.
      * The decimal point may be either a dot or a comma.
      *
      * @param text  the text to parse, not null
      * @return the created Period, never null
      * @throws CalendricalParseException if the text cannot be parsed to a Period
      */
     public static Period parse(final String text) {
         PeriodFields.checkNotNull(text, "Text to parse must not be null");
         return PeriodParser.getInstance().parse(text);
     }
 
     //-----------------------------------------------------------------------
     /**
      * Constructor.
      *
      * @param years  the amount
      * @param months  the amount
      * @param days  the amount
      * @param hours  the amount
      * @param minutes  the amount
      * @param seconds  the amount
      * @param nanos  the amount
      */
     private Period(int years, int months, int days, int hours, int minutes, int seconds, long nanos) {
         this.years = years;
         this.months = months;
         this.days = days;
         this.hours = hours;
         this.minutes = minutes;
         this.seconds = seconds;
         this.nanos = nanos;
     }
 
     /**
      * Resolves singletons.
      *
      * @return the resolved instance
      */
     private Object readResolve() {
         if ((years | months | days | hours | minutes | seconds | nanos) == 0) {
             return ZERO;
         }
         return this;
     }
 
     //-----------------------------------------------------------------------
     /**
      * Checks if the period is zero-length.
      *
      * @return true if this period is zero-length
      */
     public boolean isZero() {
         return (this == ZERO);
     }
 
     /**
      * Checks if the period is fully positive, including zero.
      * <p>
      * This checks whether all the amounts in the period are positive,
      * defined as greater than or equal to zero.
      *
      * @return true if this period is fully positive
      */
     public boolean isPositive() {
         return ((years | months | days | hours | minutes | seconds | nanos) >= 0);
     }
 
     //-----------------------------------------------------------------------
     /**
      * Gets the amount of years of the overall period, if any.
      *
      * @return the amount of years of the overall period
      */
     public int getYears() {
         return years;
     }
 
     /**
      * Gets the amount of months of the overall period, if any.
      *
      * @return the amount of months of the overall period
      */
     public int getMonths() {
         return months;
     }
 
     /**
      * Gets the amount of days of the overall period, if any.
      *
      * @return the amount of days of the overall period
      */
     public int getDays() {
         return days;
     }
 
     /**
      * Gets the amount of hours of the overall period, if any.
      *
      * @return the amount of hours of the overall period
      */
     public int getHours() {
         return hours;
     }
 
     /**
      * Gets the amount of minutes of the overall period, if any.
      *
      * @return the amount of minutes of the overall period
      */
     public int getMinutes() {
         return minutes;
     }
 
     /**
      * Gets the amount of seconds of the overall period, if any.
      *
      * @return the amount of seconds of the overall period
      */
     public int getSeconds() {
         return seconds;
     }
 
     /**
      * Gets the amount of nanoseconds of the overall period, if any.
      *
      * @return the amount of nanoseconds of the overall period
      */
     public long getNanos() {
         return nanos;
     }
 
     /**
      * Gets the amount of nanoseconds of the overall period safely converted
      * to an {@code int}.
      *
      * @return the amount of nanoseconds of the overall period
      * @throws ArithmeticException if the number of nanoseconds exceeds the capacity of an {@code int}
      */
     public int getNanosInt() {
         return MathUtils.safeToInt(nanos);
     }
 
     //-----------------------------------------------------------------------
     /**
      * Returns a copy of this period with the specified amount of years.
      * <p>
      * This method will only affect the the years field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param years  the years to represent
      * @return a new updated period instance, never null
      */
     public Period withYears(int years) {
         if (years == this.years) {
             return this;
         }
         return of(years, months, days, hours, minutes, seconds, nanos);
     }
 
     /**
      * Returns a copy of this period with the specified amount of months.
      * <p>
      * This method will only affect the the months field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param months  the months to represent
      * @return a new updated period instance, never null
      */
     public Period withMonths(int months) {
         if (months == this.months) {
             return this;
         }
         return of(years, months, days, hours, minutes, seconds, nanos);
     }
 
     /**
      * Returns a copy of this period with the specified amount of days.
      * <p>
      * This method will only affect the the days field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param days  the days to represent
      * @return a new updated period instance, never null
      */
     public Period withDays(int days) {
         if (days == this.days) {
             return this;
         }
         return of(years, months, days, hours, minutes, seconds, nanos);
     }
 
     /**
      * Returns a copy of this period with the specified amount of hours.
      * <p>
      * This method will only affect the the hours field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param hours  the hours to represent
      * @return a new updated period instance, never null
      */
     public Period withHours(int hours) {
         if (hours == this.hours) {
             return this;
         }
         return of(years, months, days, hours, minutes, seconds, nanos);
     }
 
     /**
      * Returns a copy of this period with the specified amount of minutes.
      * <p>
      * This method will only affect the the minutes field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param minutes  the minutes to represent
      * @return a new updated period instance, never null
      */
     public Period withMinutes(int minutes) {
         if (minutes == this.minutes) {
             return this;
         }
         return of(years, months, days, hours, minutes, seconds, nanos);
     }
 
     /**
      * Returns a copy of this period with the specified amount of seconds.
      * <p>
      * This method will only affect the the seconds field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param seconds  the seconds to represent
      * @return a new updated period instance, never null
      */
     public Period withSeconds(int seconds) {
         if (seconds == this.seconds) {
             return this;
         }
         return of(years, months, days, hours, minutes, seconds, nanos);
     }
 
     /**
      * Returns a copy of this period with the specified amount of nanoseconds.
      * <p>
      * This method will only affect the the nanoseconds field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param nanos  the nanoseconds to represent
      * @return a new updated period instance, never null
      */
     public Period withNanos(long nanos) {
         if (nanos == this.nanos) {
             return this;
         }
         return of(years, months, days, hours, minutes, seconds, nanos);
     }
 
     //-----------------------------------------------------------------------
     /**
      * Returns a copy of this period with the specified period added.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param periodProvider  the period to add, not null
      * @return a new updated period instance, never null
      * @throws NullPointerException if the period to add is null
      * @throws ArithmeticException if the capacity of any field is exceeded
      */
     public Period plus(PeriodProvider periodProvider) {
         Period other = from(periodProvider);
         return of(
                 MathUtils.safeAdd(years, other.years),
                 MathUtils.safeAdd(months, other.months),
                 MathUtils.safeAdd(days, other.days),
                 MathUtils.safeAdd(hours, other.hours),
                 MathUtils.safeAdd(minutes, other.minutes),
                 MathUtils.safeAdd(seconds, other.seconds),
                 MathUtils.safeAdd(nanos, other.nanos));
     }
 
     //-----------------------------------------------------------------------
     /**
      * Returns a copy of this period with the specified number of years added.
      * <p>
      * This method will only affect the the years field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param years  the years to add, positive or negative
      * @return a new updated period instance, never null
      * @throws ArithmeticException if the capacity of an {@code int} is exceeded
      */
     public Period plusYears(int years) {
         return withYears(MathUtils.safeAdd(this.years, years));
     }
 
     /**
      * Returns a copy of this period with the specified number of months added.
      * <p>
      * This method will only affect the the months field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param months  the months to add, positive or negative
      * @return a new updated period instance, never null
      * @throws ArithmeticException if the capacity of an {@code int} is exceeded
      */
     public Period plusMonths(int months) {
         return withMonths(MathUtils.safeAdd(this.months, months));
     }
 
     /**
      * Returns a copy of this period with the specified number of days added.
      * <p>
      * This method will only affect the the days field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param days  the days to add, positive or negative
      * @return a new updated period instance, never null
      * @throws ArithmeticException if the capacity of an {@code int} is exceeded
      */
     public Period plusDays(int days) {
         return withDays(MathUtils.safeAdd(this.days, days));
     }
 
     /**
      * Returns a copy of this period with the specified number of hours added.
      * <p>
      * This method will only affect the the hours field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param hours  the hours to add, positive or negative
      * @return a new updated period instance, never null
      * @throws ArithmeticException if the capacity of an {@code int} is exceeded
      */
     public Period plusHours(int hours) {
         return withHours(MathUtils.safeAdd(this.hours, hours));
     }
 
     /**
      * Returns a copy of this period with the specified number of minutes added.
      * <p>
      * This method will only affect the the minutes field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param minutes  the minutes to add, positive or negative
      * @return a new updated period instance, never null
      * @throws ArithmeticException if the capacity of an {@code int} is exceeded
      */
     public Period plusMinutes(int minutes) {
         return withMinutes(MathUtils.safeAdd(this.minutes, minutes));
     }
 
     /**
      * Returns a copy of this period with the specified number of seconds added.
      * <p>
      * This method will only affect the the seconds field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param seconds  the seconds to add, positive or negative
      * @return a new updated period instance, never null
      * @throws ArithmeticException if the capacity of an {@code int} is exceeded
      */
     public Period plusSeconds(int seconds) {
         return withSeconds(MathUtils.safeAdd(this.seconds, seconds));
     }
 
     /**
      * Returns a copy of this period with the specified number of nanoseconds added.
      * <p>
      * This method will only affect the the nanoseconds field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param nanos  the nanoseconds to add, positive or negative
      * @return a new updated period instance, never null
      * @throws ArithmeticException if the capacity of a {@code long} is exceeded
      */
     public Period plusNanos(long nanos) {
         return withNanos(MathUtils.safeAdd(this.nanos, nanos));
     }
 
     //-----------------------------------------------------------------------
     /**
      * Returns a copy of this period with the specified period subtracted.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param periodProvider  the period to subtract, not null
      * @return a new updated period instance, never null
      * @throws NullPointerException if the period to subtract is null
      * @throws ArithmeticException if the capacity of any field is exceeded
      */
     public Period minus(PeriodProvider periodProvider) {
         Period other = from(periodProvider);
         return of(
                 MathUtils.safeSubtract(years, other.years),
                 MathUtils.safeSubtract(months, other.months),
                 MathUtils.safeSubtract(days, other.days),
                 MathUtils.safeSubtract(hours, other.hours),
                 MathUtils.safeSubtract(minutes, other.minutes),
                 MathUtils.safeSubtract(seconds, other.seconds),
                 MathUtils.safeSubtract(nanos, other.nanos));
     }
 
     //-----------------------------------------------------------------------
     /**
      * Returns a copy of this period with the specified number of years subtracted.
      * <p>
      * This method will only affect the the years field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param years  the years to subtract, positive or negative
      * @return a new updated period instance, never null
      * @throws ArithmeticException if the capacity of an {@code int} is exceeded
      */
     public Period minusYears(int years) {
         return withYears(MathUtils.safeSubtract(this.years, years));
     }
 
     /**
      * Returns a copy of this period with the specified number of months subtracted.
      * <p>
      * This method will only affect the the months field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param months  the months to subtract, positive or negative
      * @return a new updated period instance, never null
      * @throws ArithmeticException if the capacity of an {@code int} is exceeded
      */
     public Period minusMonths(int months) {
         return withMonths(MathUtils.safeSubtract(this.months, months));
     }
 
     /**
      * Returns a copy of this period with the specified number of days subtracted.
      * <p>
      * This method will only affect the the days field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param days  the days to subtract, positive or negative
      * @return a new updated period instance, never null
      * @throws ArithmeticException if the capacity of an {@code int} is exceeded
      */
     public Period minusDays(int days) {
         return withDays(MathUtils.safeSubtract(this.days, days));
     }
 
     /**
      * Returns a copy of this period with the specified number of hours subtracted.
      * <p>
      * This method will only affect the the hours field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param hours  the hours to subtract, positive or negative
      * @return a new updated period instance, never null
      * @throws ArithmeticException if the capacity of an {@code int} is exceeded
      */
     public Period minusHours(int hours) {
         return withHours(MathUtils.safeSubtract(this.hours, hours));
     }
 
     /**
      * Returns a copy of this period with the specified number of minutes subtracted.
      * <p>
      * This method will only affect the the minutes field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param minutes  the minutes to subtract, positive or negative
      * @return a new updated period instance, never null
      * @throws ArithmeticException if the capacity of an {@code int} is exceeded
      */
     public Period minusMinutes(int minutes) {
         return withMinutes(MathUtils.safeSubtract(this.minutes, minutes));
     }
 
     /**
      * Returns a copy of this period with the specified number of seconds subtracted.
      * <p>
      * This method will only affect the the seconds field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param seconds  the seconds to subtract, positive or negative
      * @return a new updated period instance, never null
      * @throws ArithmeticException if the capacity of an {@code int} is exceeded
      */
     public Period minusSeconds(int seconds) {
         return withSeconds(MathUtils.safeSubtract(this.seconds, seconds));
     }
 
     /**
      * Returns a copy of this period with the specified number of nanoseconds subtracted.
      * <p>
      * This method will only affect the the nanoseconds field.
      * All other fields are left untouched.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @param nanos  the nanoseconds to subtract, positive or negative
      * @return a new updated period instance, never null
      * @throws ArithmeticException if the capacity of a {@code long} is exceeded
      */
     public Period minusNanos(long nanos) {
         return withNanos(MathUtils.safeSubtract(this.nanos, nanos));
     }
 
     //-----------------------------------------------------------------------
     /**
      * Returns a new instance with each element in this period multiplied
      * by the specified scalar.
      *
      * @param scalar  the scalar to multiply by, not null
      * @return the new updated period instance, never null
      * @throws ArithmeticException if the capacity of any field is exceeded
      */
     public Period multipliedBy(int scalar) {
         if (this == ZERO || scalar == 1) {
             return this;
         }
         return of(
                 MathUtils.safeMultiply(years, scalar),
                 MathUtils.safeMultiply(months, scalar),
                 MathUtils.safeMultiply(days, scalar),
                 MathUtils.safeMultiply(hours, scalar),
                 MathUtils.safeMultiply(minutes, scalar),
                 MathUtils.safeMultiply(seconds, scalar),
                 MathUtils.safeMultiply(nanos, scalar));
     }
 
     /**
      * Returns a new instance with each element in this period divided
      * by the specified value.
      * <p>
      * The implementation simply divides each separate field by the divisor
      * using integer division.
      *
      * @param divisor  the value to divide by, not null
      * @return the new updated period instance, never null
      * @throws ArithmeticException if dividing by zero
      */
     public Period dividedBy(int divisor) {
         if (divisor == 0) {
             throw new ArithmeticException("Cannot divide by zero");
         }
         if (this == ZERO || divisor == 1) {
             return this;
         }
         return of(
                 years / divisor, months / divisor, days / divisor,
                 hours / divisor, minutes / divisor, seconds / divisor, nanos / divisor);
     }
 
     /**
      * Returns a new instance with each amount in this period negated.
      *
      * @return the new updated period instance, never null
      * @throws ArithmeticException if any field has the minimum value
      */
     public Period negated() {
         return multipliedBy(-1);
     }
 
     //-----------------------------------------------------------------------
     /**
      * Returns a copy of this period with all amounts normalized to the
      * standard ranges for date-time fields.
      * <p>
      * Two normalizations occur, one for years and months, and one for
      * hours, minutes, seconds and nanoseconds.
      * Days are not normalized, as a day may vary in length at daylight savings cutover.
      * For example, a period of P1Y15M1DT28H61M will be normalized to P2Y3M1DT29H1M.
      * <p>
      * Note that this method normalizes using assumptions:
      * <ul>
      * <li>12 months in a year</li>
      * <li>60 minutes in an hour</li>
      * <li>60 seconds in a minute</li>
      * <li>1,000,000,000 nanoseconds in a second</li>
      * </ul>
      * This method is only appropriate to call if these assumptions are met.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @return a new updated period instance, never null
      * @throws ArithmeticException if the capacity of any field is exceeded
      */
     public Period normalized() {
         if (this == ZERO) {
             return ZERO;
         }
         int years = this.years;
         int months = this.months;
         if (months >= 12) {
             years = MathUtils.safeAdd(years, months / 12);
             months = months % 12;
         }
         long total = (hours * 60L * 60L) + (minutes * 60L) + seconds;  // will not overflow
         total = MathUtils.safeMultiply(total, 1000000000);
         total = MathUtils.safeAdd(total, nanos);
         long nanos = total % 1000000000L;
         total /= 1000000000L;
         int seconds = (int) (total % 60);
         total /= 60;
         int minutes = (int) (total % 60);
         total /= 60;
         int hours = MathUtils.safeToInt(total);
         return of(years, months, days, hours, minutes, seconds, nanos);
     }
 
     /**
      * Returns a copy of this period with all amounts normalized to the
      * standard ranges for date-time fields including the assumption that
      * days are 24 hours long.
      * <p>
      * Two normalizations occur, one for years and months, and one for
      * days, hours, minutes, seconds and nanoseconds.
      * For example, a period of P1Y15M1DT28H will be normalized to P2Y3M2DT4H.
      * <p>
      * Note that this method normalizes using assumptions:
      * <ul>
      * <li>12 months in a year</li>
      * <li>24 hours in a day</li>
      * <li>60 minutes in an hour</li>
      * <li>60 seconds in a minute</li>
      * <li>1,000,000,000 nanoseconds in a second</li>
      * </ul>
      * This method is only appropriate to call if these assumptions are met.
      * <p>
      * This instance is immutable and unaffected by this method call.
      *
      * @return a new updated period instance, never null
      * @throws ArithmeticException if the capacity of any field is exceeded
      */
     public Period normalizedWith24HourDays() {
         if (this == ZERO) {
             return ZERO;
         }
         int years = this.years;
         int months = this.months;
         if (months >= 12) {
             years = MathUtils.safeAdd(years, months / 12);
             months = months % 12;
         }
         long total = (days * 24L * 60L * 60L) +
                         (hours * 60L * 60L) +
                         (minutes * 60L) + seconds;  // will not overflow
         total = MathUtils.safeMultiply(total, 1000000000);
         total = MathUtils.safeAdd(total, nanos);
         long nanos = total % 1000000000L;
         total /= 1000000000L;
         int seconds = (int) (total % 60);
         total /= 60;
         int minutes = (int) (total % 60);
         total /= 60;
         int hours = (int) (total % 24);
         total /= 24;
         int days = MathUtils.safeToInt(total);
         return of(years, months, days, hours, minutes, seconds, nanos);
     }
 
     //-----------------------------------------------------------------------
     /**
      * Gets the total number of years represented by this period using standard
      * assumptions for the meaning of month.
      * <p>
      * This method ignores days, hours, minutes, seconds and nanos.
      * It calculates using the assumption:
      * <ul>
      * <li>12 months in a year</li>
      * </ul>
      * This method is only appropriate to call if these assumptions are met.
      *
      * @return the total number of years
      * @throws ArithmeticException if the capacity of a {@code long} is exceeded
      */
     public long totalYears() {
         return MathUtils.safeAdd((long) years, (long) (months / 12));
     }
 
     /**
      * Gets the total number of months represented by this period using standard
      * assumptions for the meaning of month.
      * <p>
      * This method ignores days, hours, minutes, seconds and nanos.
      * It calculates using the assumption:
      * <ul>
      * <li>12 months in a year</li>
      * </ul>
      * This method is only appropriate to call if these assumptions are met.
      *
      * @return the total number of years
      * @throws ArithmeticException if the capacity of a {@code long} is exceeded
      */
     public long totalMonths() {
         return MathUtils.safeAdd(MathUtils.safeMultiply((long) years, 12), months);
     }
 
     //-----------------------------------------------------------------------
     /**
      * Gets the total number of days represented by this period using standard
      * assumptions for the meaning of day, hour, minute and second.
      * <p>
      * This method ignores years and months.
      * It calculates using assumptions:
      * <ul>
      * <li>24 hours in a day</li>
      * <li>60 minutes in an hour</li>
      * <li>60 seconds in a minute</li>
      * <li>1,000,000,000 nanoseconds in a second</li>
      * </ul>
      * This method is only appropriate to call if these assumptions are met.
      *
      * @return the total number of days
      */
     public long totalDaysWith24HourDays() {
         if (this == ZERO) {
             return 0;
         }
         return days + (hours + (minutes + (seconds + (nanos / 1000000000L)) / 60L) / 60L) / 24L;  // will not overflow
     }
 
     //-----------------------------------------------------------------------
     /**
      * Gets the total number of hours represented by this period using standard
      * assumptions for the meaning of hour, minute and second.
      * <p>
      * This method ignores years, months and days.
      * It calculates using assumptions:
      * <ul>
      * <li>60 minutes in an hour</li>
      * <li>60 seconds in a minute</li>
      * <li>1,000,000,000 nanoseconds in a second</li>
      * </ul>
      * This method is only appropriate to call if these assumptions are met.
      *
      * @return the total number of hours
      */
     public long totalHours() {
         if (this == ZERO) {
             return 0;
         }
         return hours + (minutes + (seconds + (nanos / 1000000000L)) / 60L) / 60L;  // will not overflow
     }
 
     /**
      * Gets the total number of hours represented by this period using standard
      * assumptions for the meaning of day, hour, minute and second.
      * <p>
      * This method ignores years and months.
      * It calculates using assumptions:
      * <ul>
      * <li>24 hours in a day</li>
      * <li>60 minutes in an hour</li>
      * <li>60 seconds in a minute</li>
      * <li>1,000,000,000 nanoseconds in a second</li>
      * </ul>
      * This method is only appropriate to call if these assumptions are met.
      *
      * @return the total number of hours
      */
     public long totalHoursWith24HourDays() {
         if (this == ZERO) {
             return 0;
         }
         return days * 24L + hours + (minutes + (seconds + (nanos / 1000000000L)) / 60L) / 60L;  // will not overflow
     }
 
     //-----------------------------------------------------------------------
     /**
      * Gets the total number of minutes represented by this period using standard
      * assumptions for the meaning of hour, minute and second.
      * <p>
      * This method ignores years, months and days.
      * It calculates using assumptions:
      * <ul>
      * <li>60 minutes in an hour</li>
      * <li>60 seconds in a minute</li>
      * <li>1,000,000,000 nanoseconds in a second</li>
      * </ul>
      * This method is only appropriate to call if these assumptions are met.
      *
      * @return the total number of minutes
      */
     public long totalMinutes() {
         if (this == ZERO) {
             return 0;
         }
         return hours * 60L + minutes + (seconds + (nanos / 1000000000L)) / 60L;  // will not overflow
     }
 
     /**
      * Gets the total number of minutes represented by this period using standard
      * assumptions for the meaning of day, hour, minute and second.
      * <p>
      * This method ignores years and months.
      * It calculates using assumptions:
      * <ul>
      * <li>24 hours in a day</li>
      * <li>60 minutes in an hour</li>
      * <li>60 seconds in a minute</li>
      * <li>1,000,000,000 nanoseconds in a second</li>
      * </ul>
      * This method is only appropriate to call if these assumptions are met.
      *
      * @return the total number of minutes
      */
     public long totalMinutesWith24HourDays() {
         if (this == ZERO) {
             return 0;
         }
         return (days * 24L + hours) * 60L + minutes + (seconds + (nanos / 1000000000L)) / 60L;  // will not overflow
     }
 
     //-----------------------------------------------------------------------
     /**
      * Gets the total number of seconds represented by this period using standard
      * assumptions for the meaning of hour, minute and second.
      * <p>
      * This method ignores years, months and days.
      * It calculates using assumptions:
      * <ul>
      * <li>60 minutes in an hour</li>
      * <li>60 seconds in a minute</li>
      * <li>1,000,000,000 nanoseconds in a second</li>
      * </ul>
      * This method is only appropriate to call if these assumptions are met.
      *
      * @return the total number of seconds
      */
     public long totalSeconds() {
         if (this == ZERO) {
             return 0;
         }
         return (hours * 60L + minutes) * 60L + seconds + nanos / 1000000000L;  // will not overflow
     }
 
     /**
      * Gets the total number of seconds represented by this period using standard
      * assumptions for the meaning of day, hour, minute and second.
      * <p>
      * This method ignores years and months.
      * It calculates using assumptions:
      * <ul>
      * <li>24 hours in a day</li>
      * <li>60 minutes in an hour</li>
      * <li>60 seconds in a minute</li>
      * <li>1,000,000,000 nanoseconds in a second</li>
      * </ul>
      * This method is only appropriate to call if these assumptions are met.
      *
      * @return the total number of seconds
      */
     public long totalSecondsWith24HourDays() {
         if (this == ZERO) {
             return 0;
         }
         return ((days * 24L + hours) * 60L + minutes) * 60L + seconds + nanos / 1000000000L;  // will not overflow
     }
 
     //-----------------------------------------------------------------------
     /**
      * Gets the total number of nanoseconds represented by this period using standard
      * assumptions for the meaning of hour, minute and second.
      * <p>
      * This method ignores years, months and days.
      * It calculates using assumptions:
      * <ul>
      * <li>60 minutes in an hour</li>
      * <li>60 seconds in a minute</li>
      * <li>1,000,000,000 nanoseconds in a second</li>
      * </ul>
      * This method is only appropriate to call if these assumptions are met.
      *
      * @return the total number of nanoseconds
      * @throws ArithmeticException if the capacity of a {@code long} is exceeded
      */
     public long totalNanos() {
         if (this == ZERO) {
             return 0;
         }
         long secs = ((hours * 60L + minutes) * 60L + seconds);  // will not overflow
         long otherNanos = MathUtils.safeMultiply(secs, 1000000000L);
         return MathUtils.safeAdd(otherNanos, nanos);
     }
 
     /**
      * Gets the total number of nanoseconds represented by this period using standard
      * assumptions for the meaning of day, hour, minute and second.
      * <p>
      * This method ignores years and months.
      * It calculates using assumptions:
      * <ul>
      * <li>24 hours in a day</li>
      * <li>60 minutes in an hour</li>
      * <li>60 seconds in a minute</li>
      * <li>1,000,000,000 nanoseconds in a second</li>
      * </ul>
      * This method is only appropriate to call if these assumptions are met.
      *
      * @return the total number of nanoseconds
      * @throws ArithmeticException if the capacity of a {@code long} is exceeded
      */
     public long totalNanosWith24HourDays() {
         if (this == ZERO) {
             return 0;
         }
         long secs = (((days * 24L + hours) * 60L + minutes) * 60L + seconds);  // will not overflow
         long otherNanos = MathUtils.safeMultiply(secs, 1000000000L);
         return MathUtils.safeAdd(otherNanos, nanos);
     }
 
     //-----------------------------------------------------------------------
     /**
      * Converts this period to a {@code PeriodFields}.
      * <p>
      * The returned {@code PeriodFields} will only contain the non-zero amounts.
      *
      * @return the equivalent period, never null
      */
     public PeriodFields toPeriodFields() {
         PeriodFields fields = periodFields;
         if (fields == null) {
             TreeMap<PeriodUnit, PeriodField> map = new TreeMap<PeriodUnit, PeriodField>();
             if (years != 0) {
                 map.put(ISOChronology.periodYears(), PeriodField.of(years, ISOChronology.periodYears()));
             }
             if (months != 0) {
                 map.put(ISOChronology.periodMonths(), PeriodField.of(months, ISOChronology.periodMonths()));
             }
             if (days != 0) {
                 map.put(ISOChronology.periodDays(), PeriodField.of(days, ISOChronology.periodDays()));
             }
             if (hours != 0) {
                 map.put(ISOChronology.periodHours(), PeriodField.of(hours, ISOChronology.periodHours()));
             }
             if (minutes != 0) {
                 map.put(ISOChronology.periodMinutes(), PeriodField.of(minutes, ISOChronology.periodMinutes()));
             }
             if (seconds != 0) {
                 map.put(ISOChronology.periodSeconds(), PeriodField.of(seconds, ISOChronology.periodSeconds()));
             }
             if (nanos != 0) {
                 map.put(ISOChronology.periodNanos(), PeriodField.of(nanos, ISOChronology.periodNanos()));
             }
             periodFields = fields = PeriodFields.create(map);
         }
         return fields;
     }
 
     /**
      * Converts this object to a {@code Duration} using the hours, minutes,
      * seconds and nanoseconds fields.
      * If years, months or days are present an exception is thrown.
      * <p>
      * The duration is calculated using assumptions:
      * <ul>
      * <li>60 minutes in an hour</li>
      * <li>60 seconds in a minute</li>
      * <li>1,000,000,000 nanoseconds in a second</li>
      * </ul>
      * This method is only appropriate to call if these assumptions are met.
      *
      * @return {@code this}, never null
      * @throws CalendricalException if the period cannot be converted as it contains years/months/days
      */
     public Duration toDuration() {
         if ((years | months | days) > 0) {
             throw new CalendricalException("Unable to convert period to duration as years/months/days are present: " + this);
         }
         long secs = (hours * 60L + minutes) * 60L + seconds;  // will not overflow
         return Duration.seconds(secs, nanos);
     }
 
     /**
      * Converts this object to a {@code Duration} using the days, hours, minutes,
      * seconds and nanoseconds fields.
      * If years or months are present an exception is thrown.
      * <p>
      * The duration is calculated using assumptions:
      * <ul>
      * <li>60 hours in a day</li>
      * <li>60 minutes in an hour</li>
      * <li>60 seconds in a minute</li>
      * <li>1,000,000,000 nanoseconds in a second</li>
      * </ul>
      * This method is only appropriate to call if these assumptions are met.
      *
      * @return {@code this}, never null
      * @throws CalendricalException if the period cannot be converted as it contains years/months/days
      */
     public Duration toDurationWith24HourDays() {
         if ((years | months) > 0) {
             throw new CalendricalException("Unable to convert period to duration as years/months are present: " + this);
         }
         long secs = ((days * 24L + hours) * 60L + minutes) * 60L + seconds;  // will not overflow
         return Duration.seconds(secs, nanos);
     }
 
     //-----------------------------------------------------------------------
     /**
      * Is this period equal to the specified period.
      *
      * @param obj  the other period to compare to, null returns false
      * @return true if this instance is equal to the specified period
      */
     @Override
     public boolean equals(Object obj) {
         if (this == obj) {
             return true;
         }
         if (obj instanceof Period) {
             Period other = (Period) obj;
             return years == other.years && months == other.months && days == other.days &
                     hours == other.hours && minutes == other.minutes &&
                     seconds == other.seconds && nanos == other.nanos;
         }
         return false;
     }
 
     /**
      * Returns the hash code for this period.
      *
      * @return a suitable hash code
      */
     @Override
     public int hashCode() {
         // SPEC: Require unique hash code for all periods where fields within these inclusive bounds:
         // years 0-31, months 0-11, days 0-31, hours 0-23, minutes 0-59, seconds 0-59
         // IMPL: Ordered such that overflow from one field doesn't immediately affect the next field
         // years 5 bits, months 4 bits, days 6 bits, hours 5 bits, minutes 6 bits, seconds 6 bits
         return ((years << 27) | (years >>> 5)) ^
                 ((hours << 22) | (hours >>> 10)) ^
                 ((months << 18) | (months >>> 14)) ^
                 ((minutes << 12) | (minutes >>> 20)) ^
                 ((days << 6) | (days >>> 26)) ^ seconds ^ (((int) nanos) + 37);
     }
 
     //-----------------------------------------------------------------------
     /**
      * Returns a string representation of the amount of time.
      *
      * @return the amount of time in ISO8601 string format
      */
     @Override
     public String toString() {
         // TODO: toString doesn't match state nanos/secs
         String str = string;
         if (str == null) {
             if (this == ZERO) {
                 str = "PT0S";
             } else {
                 StringBuilder buf = new StringBuilder();
                 buf.append('P');
                 if (years != 0) {
                     buf.append(years).append('Y');
                 }
                 if (months != 0) {
                     buf.append(months).append('M');
                 }
                 if (days != 0) {
                     buf.append(days).append('D');
                 }
                 if ((hours | minutes | seconds) != 0 || nanos != 0) {
                     buf.append('T');
                     if (hours != 0) {
                         buf.append(hours).append('H');
                     }
                     if (minutes != 0) {
                         buf.append(minutes).append('M');
                     }
                     if (seconds != 0 || nanos != 0) {
                         if (nanos == 0) {
                             buf.append(seconds).append('S');
                         } else {
                             long s = seconds + (nanos / 1000000000);
                             long n = nanos % 1000000000;
                             if (s < 0 && n > 0) {
                                 n -= 1000000000;
                                 s++;
                             } else if (s > 0 && n < 0) {
                                 n += 1000000000;
                                 s--;
                             }
                             if (n < 0) {
                                 n = -n;
                                 if (s == 0) {
                                     buf.append('-');
                                 }
                             }
                            buf.append(s).append('.').append(n);
                            while (buf.charAt(buf.length() - 1) == '0') {
                                buf.setLength(buf.length() - 1);
                             }
                             buf.append('S');
                         }
                     }
                 }
                 str = buf.toString();
             }
             string = str;
         }
         return str;
     }
 
 }
