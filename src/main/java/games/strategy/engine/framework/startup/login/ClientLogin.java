package games.strategy.engine.framework.startup.login;

import java.awt.Component;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
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
  public static final String ENCRYPTED_PASSWORD_PROPERTY = "RSA Encrypted Password";
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
      final String publicKeyString = challengProperties.get(ClientLoginValidator.RANDOM_RSA_PUBLIC_KEY_PROPERTY);
      if (publicKeyString != null) {
        try {
          final PublicKey publicKey = KeyFactory.getInstance(ClientLoginValidator.RSA)
              .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyString)));
          final Cipher cipher = Cipher.getInstance(ClientLoginValidator.RSA_ECB_OAEPP);
          cipher.init(Cipher.ENCRYPT_MODE, publicKey);
          rVal.put(ENCRYPTED_PASSWORD_PROPERTY,
              Base64.getEncoder().encodeToString(cipher.doFinal(password.getBytes())));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException
            | InvalidKeyException | NoSuchPaddingException e) {
          throw new IllegalStateException(e);
        }
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
}
