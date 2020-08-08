package org.triplea.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.function.ThrowingConsumer;
import org.triplea.java.function.ThrowingFunction;

@ExtendWith(MockitoExtension.class)
final class IoUtilsTest {
  private final byte[] bytes = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
  @Mock private ThrowingConsumer<InputStream, IOException> consumer;
  @Mock private ThrowingFunction<InputStream, ?, IOException> function;

  private void thenStreamContainsExpectedBytes(final InputStream is) throws Exception {
    final byte[] bytesRead = new byte[bytes.length];
    is.read(bytesRead, 0, bytesRead.length);
    assertThat(bytesRead, is(bytes));
    assertThat(is.read(), is(-1));
  }

  @Test
  void consumeFromMemoryShouldPassBytesToConsumer() throws Exception {
    IoUtils.consumeFromMemory(bytes, consumer);

    final ArgumentCaptor<InputStream> inputStreamCaptor =
        ArgumentCaptor.forClass(InputStream.class);
    verify(consumer).accept(inputStreamCaptor.capture());
    thenStreamContainsExpectedBytes(inputStreamCaptor.getValue());
  }

  @Test
  void readFromMemoryShouldPassBytesToFunction() throws Exception {
    IoUtils.readFromMemory(bytes, function);

    final ArgumentCaptor<InputStream> inputStreamCaptor =
        ArgumentCaptor.forClass(InputStream.class);
    verify(function).apply(inputStreamCaptor.capture());
    thenStreamContainsExpectedBytes(inputStreamCaptor.getValue());
  }

  @Test
  void readFromMemoryShouldReturnFunctionResult() throws Exception {
    final Object result = new Object();

    assertThat(IoUtils.readFromMemory(bytes, is -> result), is(result));
  }

  @Test
  void writeToMemoryShouldReturnBytesWrittenByConsumer() throws Exception {
    assertThat(IoUtils.writeToMemory(os -> os.write(bytes)), is(bytes));
  }
}
