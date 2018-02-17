package games.strategy.io;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A collection of useful methods related to I/O.
 */
public final class IoUtils {
  private IoUtils() {}

  /**
   * Invokes the specified consumer with an in-memory input stream that wraps the specified byte array.
   *
   * @param bytes The byte array from which to read.
   * @param consumer The consumer that will accept the input stream containing {@code bytes}.
   *
   * @throws IOException If {@code consumer} encounters an error while reading from the input stream.
   */
  public static void consumeFromMemory(final byte[] bytes, final InputStreamConsumer consumer) throws IOException {
    checkNotNull(bytes);
    checkNotNull(consumer);

    readFromMemory(bytes, is -> {
      consumer.accept(is);
      return null;
    });
  }

  /**
   * Applies the specified function to an in-memory input stream that wraps the specified byte array and returns the
   * function result.
   *
   * @param bytes The byte array from which to read.
   * @param function The function to apply to an input stream containing {@code bytes}.
   *
   * @return The function result.
   *
   * @throws IOException If {@code function} encounters an error while reading from the input stream.
   */
  public static <T> T readFromMemory(final byte[] bytes, final InputStreamFunction<T> function) throws IOException {
    checkNotNull(bytes);
    checkNotNull(function);

    // NB: ByteArrayInputStream does not need to be closed
    return function.apply(new ByteArrayInputStream(bytes));
  }

  /**
   * Invokes the specified consumer with an in-memory output stream and returns the bytes written.
   *
   * @param consumer The consumer whose output will be captured and returned.
   *
   * @return The bytes written by the consumer to the output stream.
   *
   * @throws IOException If {@code consumer} encounters an error while writing to the output stream.
   */
  public static byte[] writeToMemory(final OutputStreamConsumer consumer) throws IOException {
    checkNotNull(consumer);

    // NB: ByteArrayOutputStream does not need to be closed
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    consumer.accept(os);
    return os.toByteArray();
  }

  /**
   * An operation that accepts an {@link InputStream} and returns no result.
   */
  @FunctionalInterface
  public interface InputStreamConsumer {
    /**
     * Performs the operation using the specified input stream.
     *
     * @param is The input stream from which the consumer will read.
     *
     * @throws IOException If an I/O error occurs while reading from the input stream.
     */
    void accept(InputStream is) throws IOException;
  }

  /**
   * A function that accepts an {@link InputStream} and produces a result.
   *
   * @param <R> The type of the function result.
   */
  @FunctionalInterface
  public interface InputStreamFunction<R> {
    /**
     * Applies this function to the specified input stream.
     *
     * @param is The input stream from which the function will read.
     *
     * @return The function result.
     *
     * @throws IOException If an I/O error occurs while reading from the input stream.
     */
    R apply(InputStream is) throws IOException;
  }

  /**
   * An operation that accepts an {@link OutputStream} and returns no result.
   */
  @FunctionalInterface
  public interface OutputStreamConsumer {
    /**
     * Performs the operation using the specified output stream.
     *
     * @param os The output stream to which the consumer will write.
     *
     * @throws IOException If an I/O error occurs while writing to the output stream.
     */
    void accept(OutputStream os) throws IOException;
  }
}
