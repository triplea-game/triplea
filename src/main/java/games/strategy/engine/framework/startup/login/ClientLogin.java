package games.strategy.engine.framework.startup.login;

import java.awt.Component;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import games.strategy.engine.ClientContext;
import games.strategy.net.IConnectionLogin;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.MD5Crypt;

public class ClientLogin implements IConnectionLogin {
  public static final String ENGINE_VERSION_PROPERTY = "Engine.Version";
  public static final String JDK_VERSION_PROPERTY = "JDK.Version";
  public static final String PASSWORD_PROPERTY = "Password";
  private final Component parentComponent;

  public ClientLogin(final Component parent) {
    parentComponent = parent;
  }

  @Override
  public Map<String, String> getProperties(final Map<String, String> challengProperties) {
    final Map<String, String> rVal = new HashMap<>();
    if (challengProperties.get(ClientLoginValidator.PASSWORD_REQUIRED_PROPERTY).equals(Boolean.TRUE.toString())) {
      final JPasswordField passwordField = new JPasswordField();
      passwordField.setColumns(15);
      JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(parentComponent), passwordField,
          "Enter a password to join the game", JOptionPane.QUESTION_MESSAGE);
      final String password = new String(passwordField.getPassword());
      rVal.put(PASSWORD_PROPERTY, MD5Crypt.crypt(password, challengProperties.get(ClientLoginValidator.SALT_PROPERTY)));
      final String challengeString = challengProperties.get(ClientLoginValidator.CHALLENGE_STRING_PROPERTY);
      if (challengeString != null) {
        rVal.put(ClientLoginValidator.ENCRYPTED_STRING_PROPERTY,
            encryptString(challengeString, passwordField.getPassword()));
      }
    }
    rVal.put(ENGINE_VERSION_PROPERTY, ClientContext.engineVersion().toString());
    rVal.put(JDK_VERSION_PROPERTY, System.getProperty("java.runtime.version"));
    return rVal;
  }

  @Override
  public void notifyFailedLogin(final String message) {
    EventThreadJOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(parentComponent), message,
        new CountDownLatchHandler(true));
  }

  private String encryptString(final String input, char[] password) {
    try {
      final Cipher cipher = Cipher.getInstance(ClientLoginValidator.AES);
      final byte[] salt = new byte[8];
      final byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
      System.arraycopy(inputBytes, 0, salt, 0, 8);
      cipher.init(Cipher.ENCRYPT_MODE,
          new SecretKeySpec(SecretKeyFactory.getInstance(ClientLoginValidator.PBKDF2_WITH_HMAC_SHA512)
              .generateSecret(
                  new PBEKeySpec(password, salt, ClientLoginValidator.ITERATION_COUNT, ClientLoginValidator.KEY_LENGTH))
              .getEncoded(), ClientLoginValidator.AES));
      return Base64.getEncoder().encodeToString(cipher.doFinal(inputBytes));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
