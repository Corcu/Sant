package calypsox.engine.medusa.utils;

import calypsox.tk.core.SantanderUtil;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;

import javax.xml.datatype.XMLGregorianCalendar;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * XML Date util
 *
 * @author xIS15793
 *
 */
public class XmlDateUtil {
  private XmlDateUtil() {
    // nothing to do
  }

  /**
   * get date
   *
   * @param xmlGrgorianCalendar
   *            calendar
   * @return date
   */
  public static JDate getJDate(final XMLGregorianCalendar xmlGrgorianCalendar) {
    if (xmlGrgorianCalendar == null) {
      return null;
    }

    return new JDatetime(xmlGrgorianCalendar.toGregorianCalendar()
        .getTime()).getJDate(TimeZone.getDefault());
  }

  /**
   * get datetime
   *
   * @param xmlGrgorianCalendar
   *            calendar
   * @return date
   */
  public static JDatetime getJDatetime(
      final XMLGregorianCalendar xmlGrgorianCalendar) {
    if (xmlGrgorianCalendar == null) {
      return null;
    }

    return new JDatetime(xmlGrgorianCalendar.toGregorianCalendar()
        .getTime());
  }

  /**
   * Create date
   *
   * @param jDate
   *            date
   * @return calendar
   */
  public static String createXmlDate(final JDate jDate) {
    // return XMLGregorianCalendarImpl.createDate(jDate.getYear(),
    // jDate.getMonth(), jDate.getDayOfMonth(),
    // DatatypeConstants.FIELD_UNDEFINED);

    return SantanderUtil.getInstance().getDateString(jDate,
        new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()), false);
  }

  /**
   * create datetime
   *
   * @param jDatetime
   *            datetime
   * @return date xml
   */
  public static Object createXmlDatetime(final JDatetime jDatetime) {
    return createXmlDatetimeObj(jDatetime).toString().replace("T", " ");
  }

  /**
   * create xml datetime object
   *
   * @param jDatetime
   *            datetime to convert
   * @return date converted
   */
  public static String createXmlDatetimeObj(
      final JDatetime jDatetime) {
    final Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(jDatetime.getTime());
    int month = calendar.get(Calendar.MONTH) + 1;// months shifted
    if (month == 12) {
      month=0;
    }
    calendar.set(calendar.get(Calendar.YEAR), month, calendar.get(Calendar.DAY_OF_MONTH));
    //    final int year = calendar.get(Calendar.YEAR);
    //    final int day = calendar.get(Calendar.DAY_OF_MONTH);
    //    final int hour = calendar.get(Calendar.HOUR_OF_DAY);
    //    final int minute = calendar.get(Calendar.MINUTE);
    //    final int sec = calendar.get(Calendar.SECOND);
    final JDatetime jDatetimeShifted = new JDatetime(calendar);
    return SantanderUtil.getInstance().getDateString(jDatetimeShifted,
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()), false);
    // Webportal does not support "T" in a Date, replace it by space
    //    return XMLGregorianCalendarImpl.createDateTime(year, month, day, hour,
    //        minute, sec, DatatypeConstants.FIELD_UNDEFINED,
    //        DatatypeConstants.FIELD_UNDEFINED);
  }

}
