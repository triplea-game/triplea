package games.strategy.engine.lobby.moderator.toolbox;

import java.time.Duration;
import javax.swing.JFrame;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.triplea.swing.Toast;

/**
 * Utility class to show 'toast' pop-ups that are used to confirm success actions have been executed
 * on the server.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessagePopup {
  public static void showMessage(final JFrame frame, final String message) {
    Toast.builder()
        .parent(frame)
        .message(message)
        .sleepTime(Duration.ofMillis(350))
        .build()
        .showToast();
  }
}
