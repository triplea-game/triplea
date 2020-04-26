package org.triplea.modules.chat.upload;

import com.google.common.base.Preconditions;
import es.moki.ratelimij.dropwizard.annotation.Rate;
import es.moki.ratelimij.dropwizard.annotation.RateLimited;
import es.moki.ratelimij.dropwizard.filter.KeyPart;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.data.UserRole;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.UserName;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.chat.upload.ChatMessageUpload;
import org.triplea.http.client.lobby.chat.upload.ChatUploadClient;
import org.triplea.modules.game.listing.GameListing;

/**
 * Provides an endpoint for game host servers to upload their chat messages. Those messages are then
 * stored in database and can later be retrieved by moderators.
 */
@Builder
public class ChatUploadController extends HttpController {
  @Nonnull private final Consumer<ChatMessageUpload> chatUploadModule;

  public static ChatUploadController build(final Jdbi jdbi, final GameListing gameListing) {
    return ChatUploadController.builder() //
        .chatUploadModule(ChatUploadModule.build(jdbi, gameListing))
        .build();
  }

  @POST
  @Path(ChatUploadClient.UPLOAD_CHAT_PATH)
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 10, duration = 1, timeUnit = TimeUnit.SECONDS)})
  @RolesAllowed(UserRole.HOST)
  public Response uploadChatMessage(
      @Context final HttpServletRequest request, final ChatMessageUpload chatMessageUpload) {
    Preconditions.checkArgument(chatMessageUpload != null);
    Preconditions.checkArgument(chatMessageUpload.getChatMessage() != null);
    Preconditions.checkArgument(chatMessageUpload.getFromPlayer() != null);
    Preconditions.checkArgument(chatMessageUpload.getGameId() != null);
    Preconditions.checkArgument(chatMessageUpload.getApiKey() != null);

    Preconditions.checkArgument(chatMessageUpload.getFromPlayer().length() <= UserName.MAX_LENGTH);
    Preconditions.checkArgument(chatMessageUpload.getApiKey().length() <= ApiKey.MAX_LENGTH);

    chatUploadModule.accept(chatMessageUpload);

    return Response.ok().build();
  }
}
