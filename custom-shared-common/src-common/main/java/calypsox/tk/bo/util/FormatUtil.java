package calypsox.tk.bo.util;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;

public class FormatUtil
{
    private static final String DATE_REGEX = "^(0?[1-9]|[12][0-9]|3[01])[\\/\\-](0?[1-9]|1[012])[\\/\\-]\\d{4}$";
    private static final String DATE_TIME_REGEX = "^(0?[1-9]|[12][0-9]|3[01])[\\/\\-](0?[1-9]|1[012])[\\/\\-](\\d{4}) ([0-2][0-3]|[0-1][0-9]):([0-5][0-9]):([0-5][0-9])$";
    // private static final String DOUBLE_REGEX = "^([-+]?)(0|([1-9][0-9]*))(\\.[0-9]+)?$";
    // private static final String INTEGER_REGEX = "^([-+]?)(0|([1-9][0-9]*))$";
    private static final String DOUBLE_REGEX = "^([-+]?)(0?|([1-9][0-9]*))(\\.[0-9]+)?$";
    private static final String INTEGER_REGEX = "^([-+]?)(0?|([1-9][0-9]*))$";

    public static final String DATE_DEFAULT_FORMAT = "dd/MM/yyyy";
    public static final String DATE_TIME_DEFAULT_FORMAT = "dd/MM/yyyy HH:mm:ss";
    public static final String DATE_INTERNATIONAL_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_INTERNATIONAL_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS+00:00";
    
    public static final String DOUBLE_DEFAULT_FORMAT = "#.##";
    public static final String INTEGER_DEFAULT_FORMAT = "#";

    public static final Locale DEFAULT_LOCALE_TO_OBJECT = Locale.US;
    public static final Locale DEFAULT_LOCALE_TO_STRING = Locale.GERMAN;

    Object data;

    public FormatUtil()
    {
    }

    public FormatUtil(Object data)
    {
        this.data = data;
    }

    public static FormatUtil getInstance()
    {
        return new FormatUtil();
    }

    public static FormatUtil getInstance(Object data)
    {
        return new FormatUtil(data);
    }

    public Object getData()
    {
        return data;
    }

    public void setData(Object data)
    {
        this.data = data;
    }

    /*
     * Metodos de tratamiento de fecha Parametros formar: 
     * Cadena de formato de fecha "yyyy/MM/dd" 
     * locale: Objeto regional
     */

    public boolean isDate()
    {
        return (this.data instanceof Date);
    }

    public boolean isStringDateFormat()
    {
        boolean control = false;
        Pattern pattern = null;
        Matcher matcher = null;

        if ((this.data != null) && (this.data instanceof String))
        {
            pattern = Pattern.compile(DATE_REGEX);
            matcher = pattern.matcher(this.data.toString());

            control = matcher.matches();
        }

        return control;
    }

    public String parseDateToString()
    {
        return parseDateToString(DATE_DEFAULT_FORMAT, DEFAULT_LOCALE_TO_STRING);
    }

    public String parseDateToString(String format)
    {
        return parseDateToString(format, DEFAULT_LOCALE_TO_STRING);
    }

    public String parseDateToString(String format, Locale locale)
    {
        String value = null;
        SimpleDateFormat formatter = null;

        if (isDate())
        {
            formatter = new SimpleDateFormat(format, locale);
            value = formatter.format(this.data);
        }

        return value;
    }

    public Date parseStringToDate()
    {
        return parseStringToDate(DATE_DEFAULT_FORMAT, DEFAULT_LOCALE_TO_OBJECT);
    }

    public Date parseStringToDate(String format)
    {
        return parseStringToDate(format, DEFAULT_LOCALE_TO_OBJECT);
    }

    public Date parseStringToDate(String format, Locale locale)
    {
        Date value = null;
        SimpleDateFormat formatter = null;

        try
        {
            if (isStringDateFormat())
            {
                formatter = new SimpleDateFormat(format, locale);
                value = formatter.parse(this.data.toString());
            }
        }
        catch (ParseException e) {}

        return value;
    }

    public Date toDate()
    {
        Date value = null;

        if (this.data instanceof Date)
        {
            value = (Date) this.data;
        }
        else if (this.data instanceof JDate)
        {
            value = ((JDate) this.data).getDate();
        }   
        else if (this.data instanceof String)
        {
            value = parseStringToDate();
        }
        
        return value;
    }

    /*
     * Metodos de tratamiento de fecha y hora Parametros formar: 
     * Cadena de formato de fecha "yyyy/MM/dd" "dd-M-yyyy hh:mm:ss" "dd MMMM yyyy zzzz" "E, dd MMM yyyy HH:mm:ss z" 
     * locale: Objeto regional
     */

    public boolean isDateTime()
    {
        return (this.data instanceof Date);
    }

    public boolean isStringDateTimeFormat()
    {
        boolean control = false;
        Pattern pattern = null;
        Matcher matcher = null;

        if ((this.data != null) && (this.data instanceof String))
        {
            pattern = Pattern.compile(DATE_TIME_REGEX);
            matcher = pattern.matcher(this.data.toString());

            control = matcher.matches();
        }

        return control;
    }

    public String parseDateTimeToString()
    {
        return parseDateTimeToString(DATE_TIME_DEFAULT_FORMAT, DEFAULT_LOCALE_TO_STRING);
    }

    public String parseDateTimeToString(String format)
    {
        return parseDateTimeToString(format, DEFAULT_LOCALE_TO_STRING);
    }

    public String parseDateTimeToString(String format, Locale locale)
    {
        String value = null;
        SimpleDateFormat formatter = null;

        if (isDate())
        {
            formatter = new SimpleDateFormat(format, locale);
            value = formatter.format(this.data);
        }

        return value;
    }

    public Date parseStringToDateTime()
    {
        return parseStringToDateTime(DATE_TIME_DEFAULT_FORMAT, DEFAULT_LOCALE_TO_OBJECT);
    }

    public Date parseStringToDateTime(String format)
    {
        return parseStringToDateTime(format, DEFAULT_LOCALE_TO_OBJECT);
    }

    public Date parseStringToDateTime(String format, Locale locale)
    {
        Date value = null;
        SimpleDateFormat formatter = null;

        try
        {
            if (isStringDateTimeFormat())
            {
                formatter = new SimpleDateFormat(format, locale);
                value = formatter.parse(this.data.toString());
            }
        }
        catch (ParseException e) {}

        return value;
    }

    public Date toDateTime()
    {
        Date value = null;

        if (this.data instanceof Date)
            value = (Date) this.data;
        else if (this.data instanceof String)
            value = parseStringToDateTime();

        return value;
    }

    /*
     * Metodos de tratamiento de fecha y hora Parametros formar: 
     * Cadena de formato de fecha "yyyy/MM/dd" "dd-M-yyyy hh:mm:ss" "dd MMMM yyyy zzzz" "E, dd MMM yyyy HH:mm:ss z" 
     * locale: Objeto regional
     */

    public boolean isJDate()
    {
        return (this.data instanceof JDate);
    }

    public boolean isStringJDateFormat()
    {
        boolean control = false;
        Pattern pattern = null;
        Matcher matcher = null;

        if ((this.data != null) && (this.data instanceof String))
        {
            pattern = Pattern.compile(DATE_REGEX);
            matcher = pattern.matcher(this.data.toString());

            control = matcher.matches();
        }

        return control;
    }

    public String parseJDateToString()
    {
        return parseJDateToString(DATE_DEFAULT_FORMAT, DEFAULT_LOCALE_TO_STRING);
    }

    public String parseJDateToString(String format)
    {
        return parseJDateToString(format, DEFAULT_LOCALE_TO_STRING);
    }

    public String parseJDateToString(String format, Locale locale)
    {
        String value = null;
        SimpleDateFormat formatter = null;

        if (isDate())
        {
            formatter = new SimpleDateFormat(format, locale);
            value = formatter.format(this.data);
        }

        return value;
    }

    public JDate parseStringToJDate()
    {
        return parseStringToJDate(DATE_DEFAULT_FORMAT, DEFAULT_LOCALE_TO_OBJECT);
    }

    public JDate parseStringToJDate(String format)
    {
        return parseStringToJDate(format, DEFAULT_LOCALE_TO_OBJECT);
    }

    public JDate parseStringToJDate(String format, Locale locale)
    {
        JDate value = null;
        Date date = null;
        SimpleDateFormat formatter = null;

        try
        {
            if (isStringDateFormat())
            {
                formatter = new SimpleDateFormat(format, locale);
                date = formatter.parse(this.data.toString());
                value = (date != null) ? JDate.valueOf(date) : null;
            }
        }
        catch (ParseException e) {}

        return value;
    }

    public JDate toJDate()
    {
        JDate value = null;

        if (this.data instanceof JDate)
            value = (JDate) this.data;
        else if (this.data instanceof Date)
            value = (this.data != null) ? JDate.valueOf((Date) this.data) : null;
        else if (this.data instanceof String)
            value = parseStringToJDate();

        return value;
    }
    
    /*
     * Metodos de tratamiento de fecha y hora Parametros formar: 
     * Cadena de formato de fecha "yyyy/MM/dd" "dd-M-yyyy hh:mm:ss" "dd MMMM yyyy zzzz" "E, dd MMM yyyy HH:mm:ss z" 
     * locale: Objeto regional
     */
    
    public boolean isJDatetime()
    {
        return (this.data instanceof JDatetime);
    }
    
    public boolean isStringJDatetimeFormat()
    {
        boolean control = false;
        Pattern pattern = null;
        Matcher matcher = null;
    
        if ((this.data != null) && (this.data instanceof String))
        {
            pattern = Pattern.compile(DATE_TIME_REGEX);
            matcher = pattern.matcher(this.data.toString());
    
            control = matcher.matches();
        }
    
        return control;
    }
    
    public String parseJDatetimeToString()
    {
        return parseJDatetimeToString(DATE_TIME_DEFAULT_FORMAT, DEFAULT_LOCALE_TO_STRING);
    }
    
    public String parseJDatetimeToString(String format)
    {
        return parseJDatetimeToString(format, DEFAULT_LOCALE_TO_STRING);
    }
    
    public String parseJDatetimeToString(String format, Locale locale)
    {
        String value = null;
        SimpleDateFormat formatter = null;
    
        if (isDate())
        {
            formatter = new SimpleDateFormat(format, locale);
            value = formatter.format(this.data);
        }
    
        return value;
    }
    
    public JDatetime parseStringToJDatetime()
    {
        return parseStringToJDatetime(DATE_TIME_DEFAULT_FORMAT, DEFAULT_LOCALE_TO_OBJECT);
    }
    
    public JDatetime parseStringToJDatetime(String format)
    {
        return parseStringToJDatetime(format, DEFAULT_LOCALE_TO_OBJECT);
    }
    
    public JDatetime parseStringToJDatetime(String format, Locale locale)
    {
        JDatetime value = null;
        Date date = null;
        SimpleDateFormat formatter = null;
    
        try
        {
            if (isStringDateTimeFormat())
            {
                formatter = new SimpleDateFormat(format, locale);
                date = formatter.parse(this.data.toString());
                value = (date != null) ? new JDatetime(date) : null;
            }
        }
        catch (ParseException e) {}
    
        return value;
    }
    
    public JDatetime toJDatetime()
    {
        JDatetime value = null;
    
        if (this.data instanceof JDatetime)
            value = (JDatetime) this.data;
        else if (this.data instanceof Date)
            value = (this.data != null) ? new JDatetime((Date) this.data) : null;
        else if (this.data instanceof String)
            value = parseStringToJDatetime();
    
        return value;
    }
    
    /*
     * Metodos de tratamiento de decimales Parametros formar: 
     * Cadena de formato de decimal 
     * locale: Objeto regional
     */

    public boolean isDouble()
    {
        return (this.data instanceof Double);
    }

    public boolean isStringDoubleFormat()
    {
        boolean control = false;
        Pattern pattern = null;
        Matcher matcher = null;

        if ((this.data != null) && (this.data instanceof String))
        {
            pattern = Pattern.compile(DOUBLE_REGEX);
            matcher = pattern.matcher(this.data.toString());

            control = matcher.matches();
        }

        return control;
    }

    public String parseDoubleToString()
    {
        return parseDoubleToString(DOUBLE_DEFAULT_FORMAT, DEFAULT_LOCALE_TO_STRING);
    }

    public String parseDoubleToString(String format)
    {
        return parseDoubleToString(format, DEFAULT_LOCALE_TO_STRING);
    }

    public String parseDoubleToString(String format, Locale locale)
    {
        String value = null;
        NumberFormat formatter = null;

        if (isDouble())
        {
            formatter = NumberFormat.getNumberInstance(locale);
            value = formatter.format(this.data);
        }

        return value;
    }

    public Double parseStringToDouble()
    {
        return parseStringToDouble(DOUBLE_DEFAULT_FORMAT, DEFAULT_LOCALE_TO_OBJECT);
    }

    public Double parseStringToDouble(String format)
    {
        return parseStringToDouble(format, DEFAULT_LOCALE_TO_OBJECT);
    }

    public Double parseStringToDouble(String format, Locale locale)
    {
        Double value = null;
        Number number = null;
        NumberFormat formatter = null;

        try
        {
            if (isStringDoubleFormat())
            {
                formatter = NumberFormat.getNumberInstance(locale);
                number = formatter.parse(this.data.toString());

                if (number != null)
                    value = number.doubleValue();
            }
        }
        catch (ParseException e) {}

        return value;
    }

    public Double toDouble()
    {
        Double value = null;

        if (this.data instanceof Double)
            value = ((Double) this.data).doubleValue();
        else if (this.data instanceof Integer)
            value = ((Integer) this.data).doubleValue();
        else if (this.data instanceof Long)
            value = ((Long) this.data).doubleValue();
        else if (this.data instanceof String)
            value = parseStringToDouble();

        return value;
    }

    /*
     * Metodos de tratamiento de enteros Parametros formar: 
     * Cadena de formato de entero 
     * locale: Objeto regional
     */

    public boolean isInteger()
    {
        return (this.data instanceof Integer);
    }

    public boolean isStringIntegerFormat()
    {
        boolean control = false;
        Pattern pattern = null;
        Matcher matcher = null;

        if ((this.data != null) && (this.data instanceof String))
        {
            pattern = Pattern.compile(INTEGER_REGEX);
            matcher = pattern.matcher(this.data.toString());

            control = matcher.matches();
        }

        return control;
    }

    public String parseIntegerToString()
    {
        return parseIntegerToString(INTEGER_DEFAULT_FORMAT, DEFAULT_LOCALE_TO_STRING);
    }

    public String parseIntegerToString(String format)
    {
        return parseIntegerToString(format, DEFAULT_LOCALE_TO_STRING);
    }

    public String parseIntegerToString(String format, Locale locale)
    {
        String value = null;
        NumberFormat formatter = null;

        if (isInteger())
        {
            formatter = NumberFormat.getIntegerInstance(locale);
            value = formatter.format(this.data);
        }

        return value;
    }

    public Integer parseStringToInteger()
    {
        return parseStringToInteger(INTEGER_DEFAULT_FORMAT, DEFAULT_LOCALE_TO_OBJECT);
    }

    public Integer parseStringToInteger(String format)
    {
        return parseStringToInteger(format, DEFAULT_LOCALE_TO_OBJECT);
    }

    public Integer parseStringToInteger(String format, Locale locale)
    {
        Integer value = null;
        Number number = null;
        NumberFormat formatter = null;

        try
        {
            if (isStringDoubleFormat())
            {
                formatter = NumberFormat.getIntegerInstance(locale);
                number = formatter.parse(this.data.toString());

                if (number != null)
                    value = number.intValue();
            }
        }
        catch (ParseException e) {}

        return value;
    }

    public Integer toInteger()
    {
        Integer value = null;

        if (this.data instanceof Double)
            value = ((Double) this.data).intValue();
        else if (this.data instanceof Integer)
            value = ((Integer) this.data).intValue();
        else if (this.data instanceof Long)
            value = ((Long) this.data).intValue();
        else if (this.data instanceof String)
            value = parseStringToInteger();

        return value;
    }

    /*
     * Metodos de tratamiento de enteros Parametros formar: 
     * Cadena de formato de entero 
     * locale: Objeto regional
     */

    public boolean isLong()
    {
        return (this.data instanceof Long);
    }

    public boolean isStringLongFormat()
    {
        boolean control = false;
        Pattern pattern = null;
        Matcher matcher = null;

        if ((this.data != null) && (this.data instanceof String))
        {
            pattern = Pattern.compile(INTEGER_REGEX);
            matcher = pattern.matcher(this.data.toString());

            control = matcher.matches();
        }

        return control;
    }

    public String parseLongToString()
    {
        return parseLongToString(INTEGER_DEFAULT_FORMAT, DEFAULT_LOCALE_TO_STRING);
    }

    public String parseLongToString(String format)
    {
        return parseLongToString(format, DEFAULT_LOCALE_TO_STRING);
    }

    public String parseLongToString(String format, Locale locale)
    {
        String value = null;
        NumberFormat formatter = null;

        if (isLong())
        {
            formatter = NumberFormat.getIntegerInstance(locale);
            value = formatter.format(this.data);
        }

        return value;
    }

    public Long parseStringToLong()
    {
        return parseStringToLong(INTEGER_DEFAULT_FORMAT, DEFAULT_LOCALE_TO_OBJECT);
    }

    public Long parseStringToLong(String format)
    {
        return parseStringToLong(format, DEFAULT_LOCALE_TO_OBJECT);
    }

    public Long parseStringToLong(String format, Locale locale)
    {
        Long value = null;
        Number number = null;
        NumberFormat formatter = null;

        try
        {
            if (isStringDoubleFormat())
            {
                formatter = NumberFormat.getIntegerInstance(locale);
                number = formatter.parse(this.data.toString());

                if (number != null)
                    value = number.longValue();
            }
        }
        catch (ParseException e) {}

        return value;
    }

    public Long toLong()
    {
        Long value = null;

        if (this.data instanceof Double)
            value = ((Double) this.data).longValue();
        else if (this.data instanceof Integer)
            value = ((Integer) this.data).longValue();
        else if (this.data instanceof Long)
            value = ((Long) this.data).longValue();
        else if (this.data instanceof String)
            value = parseStringToLong();

        return value;
    }

    /*
     * Metodos de tratamiento de cadenas Parametros
     */

    public boolean isString()
    {
        boolean control = false;

        if (!isDate() && !isDouble() && isInteger())
            control = true;

        return control;
    }

    @Override
    public String toString()
    {
        String value = null;

        if (this.data != null)
            value = this.data.toString();

        return value;
    }
    
    public String getUKDayNumberSuffix(final int day) {
      if (day >= 11 && day <= 13) {
        return "th";
      }
      switch (day % 10) {
      case 1:
        return "st";
      case 2:
        return "nd";
      case 3:
        return "rd";
      default:
        return "th";
      }
    }
    
    public String toCamelCase(final String str, final boolean capitalizeFirstLetter, final String delimiter) {

      if (str == null) {
        return null;
      }
      
      if (str.isEmpty()) {
        return str;
      }
      
      if (str.length() == 1) {
        return (capitalizeFirstLetter) ? str.toUpperCase() : str.toLowerCase();
      }

      // Split by delimiter
      final String[] split = str.split(delimiter);
      
      if(split.length == 1 && split[0].trim().length() == 1) {
        return (capitalizeFirstLetter) ? split[0].toUpperCase() : split[0].toLowerCase();
      }
      
      final StringBuilder sb = new StringBuilder();

      for (final String element : split) {
        final String firstLetter = element.substring(0, 1);
        final String rest = element.substring(1);
        sb.append(firstLetter.toUpperCase()).append(rest.toLowerCase());
      }

      String camelCase = sb.toString();
      if (!capitalizeFirstLetter) {
        final String firstLetter = camelCase.substring(0, 1);
        final String rest = camelCase.substring(1);
        camelCase = firstLetter.toLowerCase().concat(rest);
      }

      return camelCase;

    }
}
