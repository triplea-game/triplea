package games.strategy.engine.chat;


public class ChatMessage {
	private final String m_message;
	private final String m_from;
	private final boolean m_isMeMessage;

	public ChatMessage(final String message, final String from, final boolean isMeMessage) {
		m_message = message;
		m_from = from;
		m_isMeMessage = isMeMessage;
	}

	public String getFrom() {
		return m_from;
	}

	public boolean isMeMessage() {
		return m_isMeMessage;
	}

	public String getMessage() {
		return m_message;
	}
}