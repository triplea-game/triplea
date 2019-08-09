package org.triplea.http.client.moderator.toolbox.register.key;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data class to represent the JSON sent from client to server for "registering" a single use API
 * key.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterApiKeyParam {
  private String newPassword;
  private String singleUseKey;
}
