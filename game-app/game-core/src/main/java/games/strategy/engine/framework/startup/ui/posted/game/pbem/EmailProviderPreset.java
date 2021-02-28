package games.strategy.engine.framework.startup.ui.posted.game.pbem;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum that stores configuration values for predefined email providers. Used with PBEM when
 * selecting an email provider. If a user is not using a predefined email provider, they need to
 * supply the same configuration values manually.
 */
@AllArgsConstructor
@Getter
public enum EmailProviderPreset {
  GMAIL("Gmail", "smtp.gmail.com", 587, true),
  HOTMAIL("Hotmail", "smtp.live.com", 587, true),
  ;

  private final String name;
  private final String server;
  private final int port;
  private final boolean useTlsByDefault;

  public static Optional<EmailProviderPreset> lookupByName(final String name) {
    if (GMAIL.name.equals(name)) {
      return Optional.of(GMAIL);
    }
    if (HOTMAIL.name.equals(name)) {
      return Optional.of(HOTMAIL);
    }
    return Optional.empty();
  }
}
