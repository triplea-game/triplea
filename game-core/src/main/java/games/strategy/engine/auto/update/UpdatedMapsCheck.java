package games.strategy.engine.auto.update;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;

import com.google.common.base.Strings;

import games.strategy.triplea.settings.ClientSetting;

class UpdatedMapsCheck {

  static boolean shouldRunMapUpdateCheck() {
    // check at most once per month
    final LocalDateTime locaDateTime = LocalDateTime.now();
    final int year = locaDateTime.get(ChronoField.YEAR);
    final int month = locaDateTime.get(ChronoField.MONTH_OF_YEAR);
    // format year:month
    final String lastCheckTime = ClientSetting.TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES.value();

    if (!Strings.nullToEmpty(lastCheckTime).trim().isEmpty()) {
      final String[] yearMonth = lastCheckTime.split(":");
      if (Integer.parseInt(yearMonth[0]) >= year && Integer.parseInt(yearMonth[1]) >= month) {
        return false;
      }
    }
    ClientSetting.TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES.save(year + ":" + month);
    ClientSetting.flush();
    return true;
  }
}
