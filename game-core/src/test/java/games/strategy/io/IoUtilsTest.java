package games.strategy.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import games.strategy.io.IoUtils.InputStreamConsumer;
import games.strategy.io.IoUtils.InputStreamFunction;

public final class IoUtilsTest {
  private final byte[] bytes = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

  private void thenStreamContainsExpectedBytes(final InputStream is) throws Exception {
    final byte[] bytesRead = new byte[bytes.length];
    is.read(bytesRead, 0, bytesRead.length);
    assertThat(bytesRead, is(bytes));
    assertThat(is.read(), is(-1));
  }

  @Test
  public void consumeFromMemory_ShouldPassBytesToConsumer() throws Exception {
    final InputStreamConsumer consumer = mock(InputStreamConsumer.class);

    IoUtils.consumeFromMemory(bytes, consumer);

    final ArgumentCaptor<InputStream> inputStreamCaptor = ArgumentCaptor.forClass(InputStream.class);
    verify(consumer).accept(inputStreamCaptor.capture());
    thenStreamContainsExpectedBytes(inputStreamCaptor.getValue());
  }

  @Test
  public void readFromMemory_ShouldPassBytesToFunction() throws Exception {
    final InputStreamFunction<?> function = mock(InputStreamFunction.class);

    IoUtils.readFromMemory(bytes, function);

    final ArgumentCaptor<InputStream> inputStreamCaptor = ArgumentCaptor.forClass(InputStream.class);
    verify(function).apply(inputStreamCaptor.capture());
    thenStreamContainsExpectedBytes(inputStreamCaptor.getValue());
  }

  @Test
  public void readFromMemory_ShouldReturnFunctionResult() throws Exception {
    final Object result = new Object();

    assertThat(IoUtils.readFromMemory(bytes, is -> result), is(result));
  }

  @Test
  public void writeToMemory_ShouldReturnBytesWrittenByConsumer() throws Exception {
    assertThat(IoUtils.writeToMemory(os -> os.write(bytes)), is(bytes));
  }
}
