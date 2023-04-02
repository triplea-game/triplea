package org.triplea.http.client.error.report;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.triplea.http.client.HttpClientConstants;
import org.triplea.java.StringUtils;

/** Represents data that would be uploaded to a server. */
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ErrorReportRequest {
  /**
   * GAME_VERSION_MAJOR_MINOR regex matches first digits, and optional;u a dot followed by more
   * digits. Examples:<br>
   * 1.1 -> 1.1<br>
   * 2.3.1 -> 2.3<br>
   * 10 -> 10<br>
   * 2.3+1 -> 3.2<br>
   * Of note, we have a variety of matches to account for old version formats and to handle possible
   * future variety of versions.
   */
  private static final Pattern GAME_VERSION_MAJOR_MINOR = Pattern.compile("^[0-9]+(\\.[0-9]+)?");

  @Nonnull private String title;
  @Nonnull private String body;
  @Nonnull @Getter private String gameVersion;

  public String getTitle() {
    return StringUtils.truncate(title, HttpClientConstants.TITLE_MAX_LENGTH);
  }

  public String getBody() {
    return StringUtils.truncate(body, HttpClientConstants.REPORT_BODY_MAX_LENGTH);
  }

  /**
   * Returns the 'major.minor' part of the game version, eg: "2.6+123" -> "2.6". If the gameVersion
   * value is in an unexpected format, we return the gameVersion value as-is.
   */
  public String getSimpleGameVersion() {
    Matcher m = GAME_VERSION_MAJOR_MINOR.matcher(gameVersion);
    // return the matched part if found, otherwise just return 'gameVersion' as-is
    return m.find() ? m.group() : gameVersion;
  }
}
