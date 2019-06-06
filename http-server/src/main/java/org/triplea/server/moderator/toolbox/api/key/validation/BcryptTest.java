package org.triplea.server.moderator.toolbox.api.key.validation;


import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import org.triplea.http.client.moderator.toolbox.ModeratorEvent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BcryptTest {

  private static final String SHA_512 = "SHA-512";
  private static final String PSEUDO_SALT = "TripleA";

  public static void main(String[] args) {
      ModeratorEvent event = new Gson().fromJson(
          "{\"date\":1559792158.634177000,\"moderatorName\":\"moderator\",\"moderatorAction\":\"ACTION_2\",\"actionTarget\":\"TARGET_2\"}",
          ModeratorEvent.class);

//
//      System.out.println(
//      BaseEncoding.base16()
//          .encode(
//              MessageDigest.getInstance(SHA_512).digest("password".getBytes(StandardCharsets.UTF_8)))
//          .toLowerCase());
//      // b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86
//
//    } catch (final NoSuchAlgorithmException e) {
//      throw new IllegalStateException(SHA_512 + " is not supported!", e);
//    }


    // System.out.println(BCrypt.hashpw("password", "$2a$10$vsws1i6CNhr9uPjxgihvFu"));
    // $2a$10$vsws1i6CNhr9uPjxgihvFuiPRWeHAyBRU2Wy9DMrjhhIQdqUWuF16

//    System.out.println(Hashing.sha512().hashString("password", Charsets.UTF_8));
//
//
//    System.out.println(
//        "b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86"
//            .length());
//
  }
}
