package games.strategy.engine.chat;


public class ChatMessage {
	private final String message;
	private final String from;
	private final boolean isMessage;

	public ChatMessage(final String message, final String from, final boolean isMeMessage) {
		this.message = message;
		this.from = from;
		isMessage = isMeMessage;
	}

	public String getFrom() {
		return from;
	}

	public boolean isMeMessage() {
		return isMessage;
	}

	public String getMessage() {
		return message;
	}
}