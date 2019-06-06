package org.triplea.server.moderator.toolbox.api.key.validation;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.lobby.server.db.ApiKeyDao;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import lombok.AllArgsConstructor;


/**
 * Performs rate limiting and moderator API key validation.
 */
@AllArgsConstructor
public class ApiKeyValidationService {
  public static final Response LOCK_OUT_RESPONSE = Response.status(403).entity("Request rejected").build();
  public static final Response API_KEY_NOT_FOUND_RESPONSE = Response.status(401).entity("Invalid API key").build();

  private final ApiKeyDao apiKeyDao;

  /**
   * Does a lookup of API key contained in request header and returns the user id of
   * the moderator that owns that key. If no user id is found then returns optional empty.
   *
   * @param request Server request object expected to contain an API key header.
   */
  public Optional<Integer> lookupModeratorIdByApiKey(final HttpServletRequest request) {
    final String apiKey = request.getHeader(ModeratorToolboxClient.MODERATOR_API_KEY_HEADER);
    if (apiKey == null || apiKey.isEmpty()) {
      return Optional.empty();
    }

    final String cryptedValue = Hashing.sha512().hashString(apiKey, Charsets.UTF_8).toString();
    return apiKeyDao.lookupModeratorIdByApiKey(cryptedValue);
  }
}
