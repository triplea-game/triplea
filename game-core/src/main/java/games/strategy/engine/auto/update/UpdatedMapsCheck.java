package games.strategy.engine.auto.update;

import java.time.LocalDate;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;

import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.GameSetting;

final class UpdatedMapsCheck {
  private UpdatedMapsCheck() {}

  static boolean isMapUpdateCheckRequired() {
    return isMapUpdateCheckRequired(
        LocalDate.now(),
        ClientSetting.lastCheckForMapUpdates,
        ClientSetting::flush);
  }

  @VisibleForTesting
  static boolean isMapUpdateCheckRequired(
      final LocalDate now,
      final GameSetting<String> updateCheckDateSetting,
      final Runnable flushSetting) {
    // check at most once per month
    final String encodedUpdateCheckDate = updateCheckDateSetting.value();
    if (!encodedUpdateCheckDate.trim().isEmpty()) {
      final LocalDate updateCheckDate = parseUpdateCheckDate(encodedUpdateCheckDate);
      if (updateCheckDate.isAfter(now.minusMonths(1))) {
        return false;
      }
    }

    updateCheckDateSetting.save(formatUpdateCheckDate(now));
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
