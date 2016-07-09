package games.strategy.engine.chat;


public class ChatMessage {
	private final String message;
	private final String from;
	private final boolean isMyMessage;

	public ChatMessage(final String message, final String from, final boolean isMyMessage) {
		this.message = message;
		this.from = from;
		this.isMyMessage = isMyMessage;
	}

	public String getFrom() {
		return from;
	}

	public boolean isMyMessage() {
		return isMyMessage;
	}

	public String getMessage() {
		return message;
	}
}
