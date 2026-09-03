package games.strategy.engine.random;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.vault.VaultId;
import java.io.Serial;
import java.io.Serializable;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * A service that generates random numbers. All generated numbers are stored in a cryptographic
 * vault to prevent tampering.
 */
public interface IRemoteRandom extends IRemote {
  /**
   * Generate a random number, and lock it in the vault.
   *
   * @param serverVaultId - the vaultID where the server has stored his numbers
   * @return the vault id for which we have locked the data
   */
  @RemoteActionCode(0)
  int[] generate(int max, int count, String annotation, VaultId serverVaultId);

  /** unlock the random number last generated. */
  @RemoteActionCode(1)
  void verifyNumbers();

  /**
   * Typed request to generate and lock random numbers. {@code serverVaultId} is an opaque {@link
   * Serializable} that rides the Java wire; it is omitted from Gson wire fixtures because it
   * carries an {@code INode} Gson cannot instantiate.
   */
  @AllArgsConstructor
  class GenerateRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 3390014800912830011L;

    public static final MessageType<GenerateRequest> TYPE = MessageType.of(GenerateRequest.class);

    @Getter private final int max;
    @Getter private final int count;
    @Getter private final String annotation;
    @Getter @Nullable private final VaultId serverVaultId;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed response carrying the generated numbers. */
  @AllArgsConstructor
  class GenerateResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 8811003345620190021L;

    public static final MessageType<GenerateResponse> TYPE = MessageType.of(GenerateResponse.class);

    @Getter private final int[] numbers;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request to unlock and verify the last generated numbers. */
  class VerifyNumbersRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5540118820033910021L;

    public static final MessageType<VerifyNumbersRequest> TYPE =
        MessageType.of(VerifyNumbersRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed acknowledgement that the numbers were verified. */
  class VerifyNumbersResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6612000934871220021L;

    public static final MessageType<VerifyNumbersResponse> TYPE =
        MessageType.of(VerifyNumbersResponse.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }
}
