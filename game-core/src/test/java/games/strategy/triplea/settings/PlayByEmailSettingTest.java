package games.strategy.triplea.settings;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.github.openjson.JSONObject;

import games.strategy.security.CredentialManager;

public class PlayByEmailSettingTest {

  @Test
  void testParseValue() throws Exception {
    final PlayByEmailSetting setting = new PlayByEmailSetting("Test Name");
    final String host = "smtp.example.com";
    final int port = 25;
    final String username = "Test Username";
    final String password = "Test Password";
    try (CredentialManager manager = CredentialManager.newInstance()) {
      final String protectedUsername = manager.protect(username);
      final String protectedPassword = manager.protect(password);
      final String emailJSON = String
          .format("{\"host\": \"%s\", \"port\": %d, \"secure\": %b}", host, port, false)
          .replaceAll("\"", "\\\\\"");
      final PlayByEmailSetting.UserEmailConfiguration config = setting
          .parseValue(String.format(
              "{\"configuration\": \"%s\",\n"
              + "\"username\": \"%s\",\n"
              + "\"password\": \"%s\"\n}",
              emailJSON, protectedUsername, protectedPassword));
      assertEquals(username, config.getUsername());
      assertEquals(password, config.getPassword());
      final PlayByEmailSetting.EmailProviderSetting providerSetting = config.getEmailProviderSetting();
      assertEquals(host, providerSetting.getHost());
      assertEquals(port, providerSetting.getPort());
      assertFalse(providerSetting.isEncrypted());
    }
  }

  @Test
  void testSerializeValue() throws Exception {
    final PlayByEmailSetting setting = new PlayByEmailSetting("Test Name");
    final String host = "smtp.example.com";
    final int port = 25;
    final String username = "Test Username";
    final String password = "Test Password";
    final PlayByEmailSetting.UserEmailConfiguration config = new PlayByEmailSetting
        .UserEmailConfiguration(new PlayByEmailSetting
        .EmailProviderSetting(host, port, false), username, password);
    try (CredentialManager manager = CredentialManager.newInstance()) {
      final JSONObject result = new JSONObject(setting.formatValue(config));
      final String protectedUsername = result.optString("username");
      final String protectedPassword = result.optString("password");

      assertNotNull(protectedUsername);
      assertNotNull(protectedPassword);

      assertEquals(username, manager.unprotectToString(protectedUsername));
      assertEquals(password, manager.unprotectToString(protectedPassword));
      final String innerResultString = result.optString("configuration");
      assertNotNull(innerResultString);
      final JSONObject innerResult = new JSONObject(innerResultString);
      assertEquals(host, innerResult.optString("host"));
      assertEquals(port, innerResult.optInt("port"));
      assertTrue(innerResult.has("secure"));
      assertFalse(innerResult.getBoolean("secure"));
    }
  }

  @Test
  void testParseSerializeDefault() {
    final PlayByEmailSetting setting = new PlayByEmailSetting("Test Name");
    assertEquals("", setting.formatValue(null));
    assertNull(setting.parseValue(""));
  }

  @Test
  void testGetName() {
    final PlayByEmailSetting setting = new PlayByEmailSetting("Test Name");
    assertEquals("Test Name", setting.toString());
  }
}
