package org.triplea.http.client.lobby.chat.upload;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface ChatUploadFeignClient {

  @RequestLine("POST " + ChatUploadClient.UPLOAD_CHAT_PATH)
  String uploadChat(@HeaderMap Map<String, Object> headers, ChatMessageUpload chatMessageUpload);
}
