package org.triplea.server.moderator.toolbox.api.key.validation;


import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

public class BcryptTest {

  public static void main(String[] args) {
    // System.out.println(BCrypt.hashpw("password", "$2a$10$vsws1i6CNhr9uPjxgihvFu"));
    // $2a$10$vsws1i6CNhr9uPjxgihvFuiPRWeHAyBRU2Wy9DMrjhhIQdqUWuF16

    System.out.println(Hashing.sha512().hashString("password", Charsets.UTF_8));


    System.out.println(
        "b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86"
            .length());

  }
}
