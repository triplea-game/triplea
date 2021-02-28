package org.triplea.modules.forgot.password;

import com.google.common.annotations.VisibleForTesting;
import java.util.UUID;

/** Generates a plaintext password to be stored as a temporary password. */
class PasswordGenerator {
  @VisibleForTesting static final int PASSWORD_LENGTH = 18;

  String generatePassword() {
    return UUID.randomUUID().toString().substring(0, PASSWORD_LENGTH);
  }
}
