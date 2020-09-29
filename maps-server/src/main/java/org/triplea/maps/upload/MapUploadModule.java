package org.triplea.maps.upload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.transaction.TransactionHandler;

@Slf4j
@AllArgsConstructor
public class MapUploadModule implements Function<UploadRequestParams, UploadResult> {

  private final Jdbi jdbi;

  @SneakyThrows
  @Override
  public UploadResult apply(final UploadRequestParams uploadRequestParams) {
    /*
    final Path tempFile = writeInputToTempFile(uploadRequestParams.getInputStream());
    String mapName = readMapNameFromProperties(tempFile);
    String mapDescription = readMapDescription(tempFile);
    int mapVersion = determineMapVersion(mapName, jdbi);
    Path mapFolder = createMapFolderOnLocalFileSystemIfNeeded();

    Path mapFile = mapFolder.resolve(mapName + "-" + mapVersion + ".zip");
    Files.move(tempFile, mapFile);

    final boolean success = storeMapInformationToDatabase(
        UploadedMapParameters.builder()
          .mapName(mapName)
          .mapVersion(mapVersion)
          .folder(mapFolder)
          .uploaderName(uploadRequestParams.getUploaderName())
          .mapDescription(mapDescription)
          .build());

    if(success) {
      return UploadResult.builder()
          .messageToUser("Successfully upload: " + mapName + " v" + mapVersion)
          .success(true)
          .build();
    } else {
      rollbackFileSystemChanges()

    }
        mapName, mapVersion, mapFolder, uploadRequestParams.getUploaderName(), )



    TransactionHandler transactionHandler = jdbi.getTransactionHandler();
    Handle jdbiHandle = jdbi.open();


    transactionHandler.

    // read map.properties so we know the name of the map
    // determine the map version number
    //   - check map table if the map is new, otherwise increment version number

    // move the zip file

    //
    log.info("read: " + text);


     */
    return null;
  }



  private Path writeInputToTempFile(final InputStream inputStream) throws IOException{
    final Path file = Files.createTempFile("map-", ".zip");
    inputStream.transferTo(new FileOutputStream(file.toFile()));
    return file;
  }

  private String readMapNameFromProperties(final Path file) {
    return "";
  }
}
