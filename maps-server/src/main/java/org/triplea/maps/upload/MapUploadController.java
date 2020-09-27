package org.triplea.maps.upload;

import com.google.common.base.Strings;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.util.collection.ByteBufferInputStream;

@Slf4j
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MapUploadController {


  @POST
  @Path("/maps/upload/{file}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response save(
      @FormDataParam("file") final InputStream uploadedInputStream,
      @PathParam("file") final String fileName) {

    log.info("received file: "+ fileName);

    String text = new BufferedReader(
        new InputStreamReader(uploadedInputStream, StandardCharsets.UTF_8))
	        .lines()
        .collect(Collectors.joining("\n"));

    log.info("read: " + text);
    return Response.ok().build();
  }

}
