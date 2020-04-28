package org.triplea.test.common;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import com.google.common.base.Preconditions;
import java.time.Instant;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;

@UtilityClass
public class IsInstant {

  /**
   * Matches if a given instant has the equivalent date as defined by the parameters (parameters are
   * assumed to be UTC).
   *
   * <p>Example usage: <code>
   *   assertThat(Instant.now(), isInstant(2020, 12, 24, 23, 59, 59));
   * </code> <br>
   * The above is equivalent to:<code>
   *   assertThat(Instant.now(), is(Instant.parse("2020-12-24T23:59:59Z")));
   * </code>
   *
   * @param year The year to match (should be YYYY format, eg: 2020)
   * @param month The month to match (1-12)
   * @param day The day to match (1-31)
   * @param hour The hour of day to match (1-23)
   * @param minute The minute of the hour to match (0-59)
   * @param second The second of the minute to match (0-59)
   */
  public static Matcher<Instant> isInstant(
      final int year,
      final int month,
      final int day,
      final int hour,
      final int minute,
      final int second) {

    Preconditions.checkArgument(month > 0 && month < 13);
    Preconditions.checkArgument(day > 0 && day < 32);
    Preconditions.checkArgument(hour >= 0 && hour < 24);
    Preconditions.checkArgument(minute >= 0 && minute < 60);
    Preconditions.checkArgument(second >= 0 && second < 60);

    return is(
        equalTo(
            Instant.parse(
                String.format(
                    "%s-%s-%sT%s:%s:%sZ",
                    StringUtils.leftPad(String.valueOf(year), 4, '0'),
                    StringUtils.leftPad(String.valueOf(month), 2, '0'),
                    StringUtils.leftPad(String.valueOf(day), 2, '0'),
                    StringUtils.leftPad(String.valueOf(hour), 2, '0'),
                    StringUtils.leftPad(String.valueOf(minute), 2, '0'),
                    StringUtils.leftPad(String.valueOf(second), 2, '0')))));
  }
}
