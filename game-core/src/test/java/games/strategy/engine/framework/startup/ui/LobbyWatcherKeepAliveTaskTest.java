package games.strategy.engine.framework.startup.ui;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LobbyWatcherKeepAliveTaskTest {
  private static final String ID_0 = "id0";
  private static final String ID_1 = "id1";
  private static final String ID_2 = "id2";

  @Mock private Consumer<String> gameIdSetter;
  @Mock private Consumer<String> connectionLostReporter;
  @Mock private Consumer<String> connectionReEstablishedReporter;
  @Mock private Predicate<String> keepAliveSender;
  @Mock private Supplier<String> gamePoster;

  private LobbyWatcherKeepAliveTask lobbyWatcherKeepAliveTask;

  @Mock private FeignException feignException;

  @BeforeEach
  void setup() {
    lobbyWatcherKeepAliveTask =
        LobbyWatcherKeepAliveTask.builder()
            .gameId(ID_0)
            .gameIdSetter(gameIdSetter)
            .connectionLostReporter(connectionLostReporter)
            .connectionReEstablishedReporter(connectionReEstablishedReporter)
            .keepAliveSender(keepAliveSender)
            .gamePoster(gamePoster)
            .build();
  }

  /** If we have a simple keep-alive, nothing special should happen. */
  @Test
  void happyCase() {
    when(keepAliveSender.test(ID_0)).thenReturn(true);

    lobbyWatcherKeepAliveTask.run();

    verify(gamePoster, never()).get();
    verify(gameIdSetter, never()).accept(any());
    verify(connectionLostReporter, never()).accept(any());
    verify(connectionReEstablishedReporter, never()).accept(any());
  }

  /**
   * Keep alive false means repost, so we should request id_1, if it keeps alive then we have
   * re-established connecton and should update game id.
   */
  @Test
  void negativeKeepAliveReEstablishesConnection() {
    when(keepAliveSender.test(ID_0)).thenReturn(false);
    when(gamePoster.get()).thenReturn(ID_1);
    when(keepAliveSender.test(ID_1)).thenReturn(true);

    lobbyWatcherKeepAliveTask.run();

    verify(gameIdSetter).accept(ID_1);
    verify(connectionReEstablishedReporter).accept(any());
    verify(connectionLostReporter, never()).accept(any());
  }

  /**
   * Id-0 fails keep alive, we repost, and get id-1. id-1 does not keep alive, we should not report
   * anything.
   */
  @Test
  void negativeKeepAliveDoesNotReportSuccess() {
    when(keepAliveSender.test(ID_0)).thenReturn(false);
    when(gamePoster.get()).thenReturn(ID_1);
    when(keepAliveSender.test(ID_1)).thenReturn(false);

    lobbyWatcherKeepAliveTask.run();

    verify(gameIdSetter, never()).accept(any());
    verify(connectionLostReporter, never()).accept(any());
    verify(connectionReEstablishedReporter, never()).accept(any());
  }

  /**
   * Verifies keep-alive is false, and we re-post and get a new ID. The new ID does not keep alive
   * either, and we have to re-post again to get a second new ID.<br>
   * Run #1: id_0 does not keep alive, we get id_1, it fails keep alive<br>
   * -> do not report anything<br>
   * Run #2: id_1 fails keep alive, repost for id_2, id_2 passes keep alive.<br>
   * -> update game id to id_2 and report re-established
   */
  @Test
  void negativeKeepAliveWillEventuallyReEstablishConnection() {
    when(keepAliveSender.test(ID_0)).thenReturn(false);
    when(keepAliveSender.test(ID_1)).thenReturn(false).thenReturn(false);
    when(keepAliveSender.test(ID_2)).thenReturn(true);
    when(gamePoster.get()).thenReturn(ID_1).thenReturn(ID_2);

    lobbyWatcherKeepAliveTask.run();
    lobbyWatcherKeepAliveTask.run();

    verify(gameIdSetter, never()).accept(ID_1);
    verify(connectionLostReporter, never()).accept(any());
    verify(gameIdSetter).accept(ID_2);
    verify(connectionReEstablishedReporter).accept(any());
  }

  /**
   * If we lose connection, then report connection lost.<br>
   * Run #1: id0 fails<br>
   * -> report connection lost<br>
   */
  @Test
  void connectionLostIsReported() {
    when(keepAliveSender.test(ID_0)).thenThrow(feignException);

    lobbyWatcherKeepAliveTask.run();

    verify(connectionLostReporter).accept(any());
    verify(connectionReEstablishedReporter, never()).accept(any());
  }

  /**
   * Make sure we only report the connection lost the first time. <br>
   * Run #1: id0 fails<br>
   * -> report connection lost<br>
   * Run #2: id0 fails again<br>
   * -> nothing reported
   */
  @Test
  void connectionLostIsOnlyReportedOnce() {
    when(keepAliveSender.test(ID_0)).thenThrow(feignException).thenThrow(feignException);

    lobbyWatcherKeepAliveTask.run();
    lobbyWatcherKeepAliveTask.run();

    verify(connectionLostReporter).accept(any());
    verify(connectionReEstablishedReporter, never()).accept(any());
  }

  /**
   * Lose connection, then get a new ID, it is alive, should report re-established. <br>
   * Run #1: Id_0 fails<br>
   * -> connection lost reported<br>
   * Run #2: Id 0 does not keep alive, repost for Id 1, id 1 keeps alive<br>
   * -> re-establish reported
   */
  @Test
  void connectionEstablishedIsReportedAfterSuccess() {
    when(keepAliveSender.test(ID_0)).thenThrow(feignException).thenReturn(false);
    when(gamePoster.get()).thenReturn(ID_1);
    when(keepAliveSender.test(ID_1)).thenReturn(true);

    lobbyWatcherKeepAliveTask.run();
    lobbyWatcherKeepAliveTask.run();

    verify(gameIdSetter).accept(ID_1);
    verify(connectionLostReporter).accept(any());
    verify(connectionReEstablishedReporter).accept(any());
  }

  /**
   * We'll lose connection (keep alive = false), get a new ID. We'll regain connection with a new
   * ID, and then the next keep alive will fail. We expect to be notified twice of connection lost.
   * <br>
   * Run #1: id0 fails<br>
   * -> connection lost reported<br>
   * Run #2: id0 does not keep alive, we re-post for new ID_1, ID_1 keeps alive<br>
   * -> connection re-established Run #3: id1 fails<br>
   * -> connection lost reported a second time.
   */
  @Test
  void connectionLostIsReportedAgainAfterSuccesfulReConnectionAndLostAgain() {
    when(keepAliveSender.test(ID_0)).thenThrow(feignException).thenReturn(false);
    when(gamePoster.get()).thenReturn(ID_1);
    when(keepAliveSender.test(ID_1)).thenReturn(true).thenThrow(feignException);

    lobbyWatcherKeepAliveTask.run();
    lobbyWatcherKeepAliveTask.run();
    lobbyWatcherKeepAliveTask.run();

    verify(connectionLostReporter, times(2)).accept(any());
    verify(connectionReEstablishedReporter).accept(any());
  }

  /**
   * Connection lost, then next keep alive we are alive (this is likely a client network error
   * maybe, or server could not process the keep-alive but did not evict the game). On next attempt,
   * keep alive is true, we should not re-post, but we should report the connection lost and the
   * connection re-established.<br>
   * Run #1: id0 fails<br>
   * Run #2: id0 keeps-alive<br>
   * -> no re-posting expected, but lost and reconnected messaging expected
   */
  // same game id not alive, then alive again
  @Test
  void notAliveAndThenAliveWithSameId() {
    when(keepAliveSender.test(ID_0)).thenThrow(feignException).thenReturn(true);

    lobbyWatcherKeepAliveTask.run();
    lobbyWatcherKeepAliveTask.run();

    verify(gameIdSetter, never()).accept(any());
    verify(gamePoster, never()).get();
    verify(connectionLostReporter).accept(any());
    verify(connectionReEstablishedReporter).accept(any());
  }

  /**
   * Connection lost, then we get a 'false' keep-alive and re-post and get a new ID. The new ID does
   * not 'keep-alive', we should not report re-established connecton.<br>
   * Run #1: id0 fails <br>
   * Run #2: id0 does not keep alive, we repost and get id1, id1 does not keep-alive<br>
   * -> should not report re-established
   */
  @Test
  void willSendLastPostedGameIdOnKeepAlive() {
    when(keepAliveSender.test(ID_0)).thenThrow(feignException).thenReturn(false);
    when(gamePoster.get()).thenReturn(ID_1);
    when(keepAliveSender.test(ID_1)).thenReturn(false);

    lobbyWatcherKeepAliveTask.run();
    lobbyWatcherKeepAliveTask.run();

    verify(gameIdSetter, never()).accept(any());
    verify(connectionLostReporter).accept(any());
    verify(connectionReEstablishedReporter, never()).accept(any());
  }
}
