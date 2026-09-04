package games.strategy.engine.message.wire;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Base64;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;

/**
 * Carries a {@link Serializable} payload across the wire in its existing Java-serialized form,
 * base64-encoded so it rides untouched inside the JSON envelope. Payloads that overlap on-disk save
 * state travel opaque this way, so the wire needs no second codec for them and the receiver
 * reconstructs the identical object graph.
 */
@EqualsAndHashCode
public final class OpaqueBlob implements Serializable {
  private static final long serialVersionUID = 1L;

  @Nonnull private final String data;

  private OpaqueBlob(final String data) {
    this.data = data;
  }

  public static OpaqueBlob of(final Serializable value) {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(value);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to serialize opaque payload", e);
    }
    return new OpaqueBlob(Base64.getEncoder().encodeToString(bytes.toByteArray()));
  }

  public Serializable read() {
    return read(Serializable.class);
  }

  public <T extends Serializable> T read(final Class<T> type) {
    final byte[] bytes = Base64.getDecoder().decode(data);
    try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      return type.cast(in.readObject());
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to deserialize opaque payload", e);
    } catch (final ClassNotFoundException e) {
      throw new IllegalStateException("Unknown class in opaque payload", e);
    }
  }
}
