package org.triplea.maps.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapUploadModuleTest {

  //  @Mock private MapFileIngestion mapFileIngestion;
  //  @Mock private MapIngestionDao mapIngestionDao;

  @InjectMocks private MapUploadModule mapUploadModule;

  @Test
  public void verifyUpload() throws Exception {
    /*
        Path tempFolder = createTempFolder();
        createMapPropertiesWithMapName(tempFolder,"Map-Name");
        createThumbnail(tempFolder, "preview.png");
        createMapDescriptionFileWithContents(tempFolder, "map notes!");
        Path mapZip = createZip(tempFolder);
        final InputStream inputStream = new FileInputStream(mapZip.toFile());


    //    when(mapFileIngestion.acceptMapZip("Map-Name", any(Path.class));
    //    verify(mapFileIngestion).acceptPreviewImage("Map-Name", "preview.png", any(Path.class));
    //    mapIngestionDao


        final UploadResult uploadResult = mapUploadModule.apply(UploadRequestParams.builder()
            .inputStream(inputStream)
            .uploaderName("map-creator-name")
            .previewImage("preview.png")


            .build());


        assertThat(uploadResult.getMessageToUser().toLowerCase(), containsString("success"));
        assertThat(uploadResult.isSuccess(), is(true));

        verify(mapFileIngestion)
            .acceptMapZip("Map-Name", any(Path.class));
        verify(mapFileIngestion)
            .acceptPreviewImage("Map-Name", "map-thumbnail.png", any(Path.class));

        verify(mapIngestionDao)
            .storeMap(
                MapIngestionParams.builder()
                  .mapName("Map-Name")


                .build()

            );

    */
  }

}
