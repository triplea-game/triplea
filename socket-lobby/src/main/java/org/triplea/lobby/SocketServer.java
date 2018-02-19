package org.triplea.lobby;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.glassfish.tyrus.server.Server;

public class SocketServer {

  public static void main(final String[] args) {
    runServer();
  }

  public static void runServer() {
    final Server server = new Server("localhost", 8025, "/websockets", new HashMap<>(), SocketLobby.class);

    try {
      server.start();
      final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      System.out.print("Please press a key to stop the server.");
      reader.readLine();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    } finally {
      server.stop();
    }
  }
}
