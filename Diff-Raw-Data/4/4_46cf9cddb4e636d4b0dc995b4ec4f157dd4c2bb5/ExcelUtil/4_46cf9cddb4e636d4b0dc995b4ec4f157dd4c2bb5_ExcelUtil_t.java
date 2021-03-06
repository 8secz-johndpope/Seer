 
 package org.eclipse.birt.report.engine.emitter.excel;
 
 import java.util.List;
 import java.text.NumberFormat;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 import java.util.Calendar;
 import java.sql.Time;
 import java.util.Date;
 import java.lang.Number;
 import java.lang.String;
 
 import org.eclipse.birt.report.engine.emitter.excel.GroupInfo.Position;
 import org.eclipse.birt.report.engine.ir.DimensionType;
 import org.eclipse.birt.core.format.DateFormatter;
 import org.eclipse.birt.core.format.StringFormatter;
 import org.eclipse.birt.core.format.NumberFormatter;
 import org.eclipse.birt.chart.util.CDateTime;
 
 import com.ibm.icu.text.DecimalFormat;
 import com.ibm.icu.text.SimpleDateFormat;
 public class ExcelUtil
 {
 
 	public static String ridQuote( String val )
 	{
 		if ( val.charAt( 0 ) == '"' && val.charAt( val.length( ) - 1 ) == '"' )
 		{
 			return val.substring( 1, val.length( ) - 1 );
 		}
 
 		return val;
 	}
 	
     public static String formatDate( Object data )
 	{
     	if(data == null) {
     		return null;
     	}
     	
 		SimpleDateFormat dateFormat = new SimpleDateFormat(
 				"yyyy-MM-dd'T'HH:mm:ss" );
 		Date date = null;
 		
 		if(data instanceof com.ibm.icu.util.Calendar) {
 			date = ((com.ibm.icu.util.Calendar) data).getTime( );
 		}
 		else if(data instanceof Date) {
 			date = (Date) data;
 		}
 		else {
 			return null;
 		}
 		
 		return  dateFormat.format( date );        
 	}
     
     public static String formatNumber( Object data)
     {
        DecimalFormat numberFormat = new DecimalFormat("0.00E00");
        return numberFormat.format( (Number)data);
     }
       public static String getType(Object val)
     {
     	if ( val instanceof Number )
     	{
     	   return Data.NUMBER;	
     	}
     	else if(val instanceof Date)
     	{
     	   return Data.DATE;	
     	}
     	else if (val instanceof Calendar)
     	{
     	   return Data.CALENDAR;	
     	}
     	else if(val instanceof CDateTime)
     	{
     	   return Data.CDATETIME;	
     	}
     	else 
     	{
     	   return Data.STRING;	
     	}
     }
       
     private static String replaceDateFormat( String pattern )
 	{
 		if ( pattern == null )
 		{
 			String rg = "";
 
 			return rg;
 		}
 
 		StringBuffer toAppendTo = new StringBuffer( );
 		boolean inQuote = false;
 		char prevCh = 0;
 		int count = 0;
 
 		for ( int i = 0; i < pattern.length( ); ++i )
 		{
 			char ch = pattern.charAt( i );
 
 			if ( ch != prevCh && count > 0 )
 			{
 				toAppendTo.append( subReplaceDateFormat( prevCh, count ) );
 				count = 0;
 			}
 
 			if ( ch == '/' )
 			{
 				toAppendTo.append( '\\' );
 				toAppendTo.append( ch );
 			}
 			else if ( ch == '\'' )
 			{
 				if ( ( i + 1 ) < pattern.length( )
 						&& pattern.charAt( i + 1 ) == '\'' )
 				{
 					toAppendTo.append( "\"" );
 					++i;
 				}
 				else
 				{
 					inQuote = !inQuote;
 				}
 			}
 			else if ( !inQuote )
 			{
 				prevCh = ch;
 				++count;
 			}
 			else
 			{
 
 				toAppendTo.append( ch );
 			}
 		}
 
 		if ( count > 0 )
 		{
 			toAppendTo.append( subReplaceDateFormat( prevCh, count ) );
 		}
 
 		return toAppendTo.toString( );
 	}
 
 	/**
 	 * only used in the method replaceDataFormat().
 	 */
 	private static String subReplaceDateFormat( char ch, int count )
 	{
 		String current = "";
 		int patternCharIndex = -1;
 		String datePatternChars = "GyMdkHmsSEDFwWahKz";
 		if ( ( patternCharIndex = datePatternChars.indexOf( ch ) ) == -1 )
 		{
 			for ( int i = 0; i < count; i++ )
 			{
 				current += "" + ch;
 			}
 
 			return current;
 		}
 
 		switch ( patternCharIndex )
 		{
 			case 0 : // 'G' - ERA
 				current = "";
 				break;
 			case 1 : // 'y' - YEAR
 				for ( int i = 0; i < count; i++ )
 				{
 					current += "" + ch;
 				}
 
 				break;
 			case 2 : // 'M' - MONTH
 				for ( int i = 0; i < count; i++ )
 				{
 					current += "" + ch;
 				}
 
 				break;
 			case 3 : // 'd' - Date
 				for ( int i = 0; i < count; i++ )
 				{
 					current += "" + ch;
 				}
 
 				break;
 			case 4 : // 'k' - HOUR_OF_DAY: 1-based. eg, 23:59 + 1 hour =>>
 						// 24:59
 				current = "h";
 				break;
 			case 5 : // case 5: // 'H'-HOUR_OF_DAY:0-based.eg, 23:59+1
 						// hour=>>00:59
 				for ( int i = 0; i < count; i++ )
 				{
 					current += "" + ch;
 				}
 
 				break;
 			case 6 : // case 6: // 'm' - MINUTE
 				for ( int i = 0; i < count; i++ )
 				{
 					current += "" + ch;
 				}
 
 				break;
 			case 7 : // case 7: // 's' - SECOND
 				for ( int i = 0; i < count; i++ )
 				{
 					current += "" + ch;
 				}
 
 				break;
 			case 8 : // case 8: // 'S' - MILLISECOND
 				for ( int i = 0; i < count; i++ )
 				{
 					current += "" + ch;
 				}
 
 				break;
 			case 9 : // 'E' - DAY_OF_WEEK
 				for ( int i = 0; i < count; i++ )
 				{
 					current += "a";
 				}
 
 				break;
 			case 14 : // 'a' - AM_PM
 				current = "AM/PM";
 				break;
 			case 15 : // 'h' - HOUR:1-based. eg, 11PM + 1 hour =>> 12 AM
 				for ( int i = 0; i < count; i++ )
 				{
 					current += "" + ch;
 				}
 
 				break;
 			case 17 : // 'z' - ZONE_OFFSET
 				current = "";
 				break;
 			default :
 				// case 10: // 'D' - DAY_OF_YEAR
 				// case 11: // 'F' - DAY_OF_WEEK_IN_MONTH
 				// case 12: // 'w' - WEEK_OF_YEAR
 				// case 13: // 'W' - WEEK_OF_MONTH
 				// case 16: // 'K' - HOUR: 0-based. eg, 11PM + 1 hour =>> 0 AM
 				current = "";
 				break;
 		}
 
 		return current;
 	}
        
     public static String getPattern(Object data, String val)
     {
     	if(val != null && data instanceof Date) {
     	   return replaceDateFormat(val);   
     	}
     	else if(val == null && data instanceof Time) {
     		return "Long Time";
     	}
     	else if(val == null && data instanceof java.sql.Date) 
     	{
    		// According to java SDK 1.4.2-16, sql.Date doesn't have
    		// a time component.
    		return "mmm d, yyyy";// hh:mm AM/PM";
     	}
     	else if(val == null && data instanceof java.util.Date) 
     	{
     		return "mmm d, yyyy hh:mm AM/PM";
     	}
     	else if(val != null && data instanceof Number)
     	{
     	   
     	   if(val.indexOf( "E" ) >= 0){
     	      return "Scientific";
     	   }
     	   return new NumberFormatter(val).getPattern( );	
     	}
     	else if(val != null && data instanceof String)
     	{
     		return new StringFormatter(val).getPattern( );
     	}
     	
     	return null;
     }
     
     public static String replaceAll(String str, String old, String news) {
         if(str == null) {
            return str;
         }
 
         int begin = 0;
         int idx = 0;
         int len = old.length();
         StringBuffer buf = new StringBuffer();
 
         while((idx = str.indexOf(old, begin)) >= 0) {
            buf.append(str.substring(begin, idx));
            buf.append(news);
            begin = idx + len;
         }
 
         return new String(buf.append(str.substring(begin)));
      }
 	// TODO
 	public static String getValue( String val )
 	{
 		if ( val == null )
 		{
 			return StyleConstant.NULL;
 		}
 		if ( val.charAt( 0 ) == '"' && val.charAt( val.length( ) - 1 ) == '"' )
 		{
 			return val.substring( 1, val.length( ) - 1 );
 		}
 
 		return val;
 	}
 
 	public static int convertToPt( String size )
 	{
 		try
 		{
 			int s = Integer.valueOf( size.substring( 0, size.length( ) - 2 ) )
 					.intValue( );
 			if ( size.endsWith( "in" ) )
 			{
 				return s * 72;
 			}
 			else if ( size.endsWith( "cm" ) )
 			{
 				return (int) ( s / 2.54 * 72 );
 			}
 			else if ( size.endsWith( "mm" ) )
 			{
 				return (int) ( s * 10 / 2.54 * 72 );
 			}
 			else if ( size.endsWith( "pc" ) )
 			{
 				return s;
 			}
 			else
 			{
 				return s;
 			}
 		}
 		catch ( Exception e )
 		{
 			e.printStackTrace( );
 			return 0;
 		}
 	}
     public static boolean isNumber( String val )
 	{
 		NumberFormat nf = NumberFormat.getInstance( );
 		try
 		{
 			Number num = nf.parse( val );
 			return true;
 		}
 		catch ( Exception e )
 		{
 			return false;
 		}
 	}
 	public static String getColumnOfExp( String exp )
 	{
 		return exp.substring( exp.indexOf( "dataSetRow[" ), exp
 				.lastIndexOf( "]" ) + 1 );
 	}
 	private static final int max_formula_length = 512;
 
 	public static String createFormula( String txt, String exp, List positions )
 	{
 		exp = getFormulaName( exp );
 		StringBuffer sb = new StringBuffer( exp + "(" );
 		for ( int i = 0; i < positions.size( ); i++ )
 		{
 			Position p = (Position) positions.get( i );
 			sb.append( "R" + p.row + "C" + p.column + "," );
 		}
 		sb.setCharAt( sb.length( ) - 1, ')' );
 
 		if ( sb.length( ) > max_formula_length || positions.size( ) == 0 )
 		{
 			return txt;
 		}
 		return sb.toString( );
 	}
 
 	private static String getFormulaName( String expression )
 	{
 		if ( expression.startsWith( "Total.sum" ) )
 		{
 			return "=SUM";
 		}
 		else if ( expression.startsWith( "Total.ave" ) )
 		{
 			return "=AVERAGE";
 		}
 		else if ( expression.startsWith( "Total.max" ) )
 		{
 			return "=MAX";
 		}
 		else if ( expression.startsWith( "Total.min" ) )
 		{
 			return "=MIN";
 		}
 		else if ( expression.startsWith( "Total.count" ) )
 		{
 			return "=COUNT";
 		}
 		throw new RuntimeException( "Cannot parse the expression" + expression );
 	}
 
 	private static final String reg1 = "Total." + "(count|ave|sum|max|min)"
 			+ "\\(", reg2 = "\\)", reg3 = "\\[", reg4 = "\\]";
 
 	public static boolean isValidExp( String exp, String[] columnNames )
 	{
 		StringBuffer sb = new StringBuffer( );
 		for ( int i = 0; i < columnNames.length; i++ )
 		{
 			sb.append( columnNames[i] + "|" );
 		}
 		String columnRegExp = "(" + sb.substring( 0, sb.length( ) - 1 ) + ")";
 		columnRegExp = columnRegExp.replaceAll( reg3, "Z" );
 		columnRegExp = columnRegExp.replaceAll( reg4, "Z" );
 
 		String aggregateRegExp = reg1 + columnRegExp + reg2;
 
 		exp = exp.replaceAll( reg3, "Z" );
 		exp = exp.replaceAll( reg4, "Z" );
 
 		Pattern p = Pattern.compile( aggregateRegExp );
 		Matcher m = p.matcher( exp );
 		boolean agg = m.matches( );
 
 		p = Pattern.compile( columnRegExp );
 		m = p.matcher( exp );
 		return agg || m.matches( );
 	}
 
 	public static String expression( String val, String target, String[] res,
 			boolean casesenstive )
 	{
 		boolean flag = casesenstive ? target.equals( val ) : target
 				.equalsIgnoreCase( val );
 		return flag ? res[1] : res[0];
 	}
 
 	public static int covertDimensionType( DimensionType value, int parent )
 	{
 		if ( DimensionType.UNITS_PERCENTAGE.equals( value.getUnits( ) ) )
 		{
 			return (int) (value.getMeasure( ) / 100 * parent);
 		}
 		else
 		{
 			return (int) (value.convertTo( DimensionType.UNITS_PT ));
 		}
 	}
 	
 
 	
 }
