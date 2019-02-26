package games.strategy.engine.chat;

class ChatMessage {
  private final String message;
  private final String from;
  private final boolean isMyMessage;

  ChatMessage(final String message, final String from, final boolean isMyMessage) {
    this.message = message;
    this.from = from;
    this.isMyMessage = isMyMessage;
  }

  String getFrom() {
    return from;
  }

  boolean isMyMessage() {
    return isMyMessage;
  }

  String getMessage() {
    return message;
  }
}
