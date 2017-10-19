package games.strategy.debug;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.PrintStream;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public final class SynchedByteArrayOutputStreamTest {

  private static final Duration timeout = Duration.ofSeconds(5);

  @Mock
  private PrintStream mirror;

  @InjectMocks
  private SynchedByteArrayOutputStream os;

  @Test
  public void writeByte_ShouldTriggerReadFromDifferentThread() throws Exception {
    assertTimeoutPreemptively(timeout, () -> {
      final String expected = "X";
      final AtomicReference<String> actualRef = new AtomicReference<>();
      final CountDownLatch readerThreadReadyLatch = new CountDownLatch(1);

      final Thread readerThread = new Thread(() -> {
        readerThreadReadyLatch.countDown();
        actualRef.set(os.readFully());
      }, "Reader");
      readerThread.start();
      readerThreadReadyLatch.await();
      new Thread(() -> {
        final byte[] bytes = expected.getBytes();
        assert bytes.length == 1;
        os.write(bytes[0]);
      }, "Writer").start();
      readerThread.join();

      assertThat(actualRef.get(), is(expected));
    });
  }

  @Test
  public void writeBytes_ShouldTriggerReadFromDifferentThread() throws Exception {
    assertTimeoutPreemptively(timeout, () -> {
      final String expected = "the quick brown fox";
      final AtomicReference<String> actualRef = new AtomicReference<>();
      final CountDownLatch readerThreadReadyLatch = new CountDownLatch(1);

      final Thread readerThread = new Thread(() -> {
        readerThreadReadyLatch.countDown();
        actualRef.set(os.readFully());
      }, "Reader");
      readerThread.start();
      readerThreadReadyLatch.await();
      new Thread(() -> {
        final byte[] bytes = expected.getBytes();
        os.write(bytes, 0, bytes.length);
      }, "Writer").start();
      readerThread.join();

      assertThat(actualRef.get(), is(expected));
    });
  }
}
