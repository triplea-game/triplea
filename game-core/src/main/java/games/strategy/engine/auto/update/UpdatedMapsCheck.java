package games.strategy.engine.auto.update;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.GameSetting;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

final class UpdatedMapsCheck {
  private UpdatedMapsCheck() {}

  static boolean isMapUpdateCheckRequired() {
    return isMapUpdateCheckRequired(
        LocalDate.now(ZoneId.systemDefault()),
        ClientSetting.lastCheckForMapUpdates,
        ClientSetting::flush);
  }

  @VisibleForTesting
  static boolean isMapUpdateCheckRequired(
      final LocalDate now,
      final GameSetting<String> updateCheckDateSetting,
      final Runnable flushSetting) {
    // check at most once per month
    final boolean updateCheckRequired =
        updateCheckDateSetting
            .getValue()
            .map(
                encodedUpdateCheckDate ->
                    !parseUpdateCheckDate(encodedUpdateCheckDate).isAfter(now.minusMonths(1)))
            .orElse(true);
    if (!updateCheckRequired) {
      return false;
    }

    updateCheckDateSetting.setValue(formatUpdateCheckDate(now));
    flushSetting.run();
    return true;
  }

  @VisibleForTesting
  static LocalDate parseUpdateCheckDate(final String encodedUpdateCheckDate) {
    final List<String> tokens = Splitter.on(':').splitToList(encodedUpdateCheckDate);
    return LocalDate.of(Integer.parseInt(tokens.get(0)), Integer.parseInt(tokens.get(1)), 1);
  }

  @VisibleForTesting
  static String formatUpdateCheckDate(final LocalDate updateCheckDate) {
    return updateCheckDate.getYear() + ":" + updateCheckDate.getMonthValue();
  }
}
