package games.strategy.debug;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public final class SynchedByteArrayOutputStreamTest {
  @Rule
  public final Timeout globalTimeout = new Timeout(5, TimeUnit.SECONDS);

  @Mock
  private PrintStream mirror;

  @InjectMocks
  private SynchedByteArrayOutputStream os;

  @Test
  public void writeByte_ShouldTriggerReadFromDifferentThread() throws Exception {
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
  }

  @Test
  public void writeBytes_ShouldTriggerReadFromDifferentThread() throws Exception {
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
  }
}
