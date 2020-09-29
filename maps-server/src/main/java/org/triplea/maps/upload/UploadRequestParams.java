package org.triplea.maps.upload;

import java.io.InputStream;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UploadRequestParams {
  @Nonnull private final String uploaderName;
  @Nonnull private final InputStream inputStream;
}
