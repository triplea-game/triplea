package games.strategy.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

public class TimeManager {
  /**
   * 
   * Replacement for Date.toGMTString();
   * 
   * @param date The Date being returned as String
   * @return formatted GMT Date String
   */
  public static String getGMTString(Date date){
    DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z");
    dateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
    return dateFormat.format(date);
  }
}
