package games.strategy.engine.chat;

/**
 * A collection of chat messages sent by the server in response to an administrative event.
 *
 * <p>
 * Various types in the chat subsystem test these messages for equality. If one matches, a corresponding human-readable
 * message is displayed to the user appearing to be authored by the "chat administrator" rather than the user at the
 * node that sent the message. Therefore, these messages should be thought more of as opaque identifiers rather than
 * actual messages displayed to a user.
 * </p>
 * <p>
 * Each message contains a leading special character to make it more difficult for the user at the server/host to forge
 * an administrative chat message.
 * </p>
 */
public final class AdministrativeChatMessages {
  // FIXME: It appears the leading special character in each message was lost at some time in the past due to a dev
  // using a non-Unicode encoding, which resulted in their replacement with question marks. The question marks should
  // probably be changed back to a "difficult to type" Unicode character.

  public static final String YOU_HAVE_BEEN_MUTED_GAME = "?YOUR CHATTING IN THIS GAME HAS BEEN 'MUTED' BY THE HOST";
  public static final String YOU_HAVE_BEEN_MUTED_LOBBY =
      "?YOUR LOBBY CHATTING HAS BEEN TEMPORARILY 'MUTED' BY THE ADMINS, TRY AGAIN LATER";

  private AdministrativeChatMessages() {}
}
