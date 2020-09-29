package org.triplea.maps.upload;

import java.io.InputStream;
import java.util.function.Function;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/")
// @Produces(MediaType.APPLICATION_JSON)
// @AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MapUploadController {

  private final Function<UploadRequestParams, UploadResult> mapUploadModule = new MapUploadModule(null);

  @PUT
  @Path("/maps/upload")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response save(final InputStream uploadedInputStream) {
//    final UploadResult result = mapUploadModule.apply(uploadedInputStream);

//    return Response.status(result.isSuccess() ? 200 : 400).entity(result).build();
    return null;
  }
}
