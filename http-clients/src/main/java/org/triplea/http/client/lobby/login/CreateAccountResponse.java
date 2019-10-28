package org.triplea.http.client.lobby.login;

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class CreateAccountResponse {
  public static final CreateAccountResponse SUCCESS_RESPONSE = new CreateAccountResponse("");
  String errorMessage;

  public boolean isSuccess() {
    return Strings.nullToEmpty(errorMessage).isEmpty();
  }
}
