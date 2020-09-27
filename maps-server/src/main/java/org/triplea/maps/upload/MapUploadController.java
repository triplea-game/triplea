package org.triplea.maps.upload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
// @AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MapUploadController {

  @PUT
  @Path("/maps/upload/{file}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response save(
      final InputStream uploadedInputStream, @PathParam("file") final String fileName)
      throws IOException {

    log.info("received file: " + fileName);

    uploadedInputStream.transferTo(new FileOutputStream(new File(fileName)));

    String text =
        new BufferedReader(new InputStreamReader(uploadedInputStream, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));

    log.info("read: " + text);
    return Response.ok().build();
  }
}
