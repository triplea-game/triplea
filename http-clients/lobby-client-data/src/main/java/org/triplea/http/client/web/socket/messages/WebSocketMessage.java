package org.triplea.http.client.web.socket.messages;

import org.triplea.http.client.web.socket.MessageEnvelope;

/**
 * Interface for messages that are transmitted over websocket. Such messages will be encoded as JSON
 * when they are sent.
 *
 * <p>*Important* design note: websocket messages should only store Java primitive types (eg:
 * String, int, avoid enum types). This is to add greater flexibility for backward compatibility if
 * any kind of data type change occurs.
 *
 * <p>For example, if sending a message that contains an enumerated value, send the enumerated value
 * as a string, and in the getter of the message, convert the string value to the needed enum value.
 * In this example, if the enum changes name, we can change the getter code to handle the new type
 * name. Meanwhile, in this example, previous client versions would continue to work and both new
 * and old client versions would be compatible.
 */
public interface WebSocketMessage {
  MessageEnvelope toEnvelope();
}
