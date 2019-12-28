package org.triplea.server.user.account.create;

import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.domain.data.UserName;
import org.triplea.lobby.server.db.dao.BadWordsDao;
import org.triplea.lobby.server.db.dao.UserJdbiDao;
import org.triplea.server.user.account.NameValidation;
import org.triplea.server.user.account.PasswordBCrypter;

@UtilityClass
public class CreateAccountControllerFactory {

  public static CreateAccountController buildController(final Jdbi jdbi) {
    return CreateAccountController.builder()
        .createAccountModule(
            CreateAccountModule.builder()
                .accountCreator(
                    AccountCreator.builder()
                        .userJdbiDao(jdbi.onDemand(UserJdbiDao.class))
                        .passwordEncryptor(new PasswordBCrypter())
                        .build())
                .createAccountValidation(
                    CreateAccountValidation.builder()
                        .nameValidator(
                            NameValidation.builder()
                                .userJdbiDao(jdbi.onDemand(UserJdbiDao.class))
                                .syntaxValidation(
                                    name -> Optional.ofNullable(UserName.validate(name)))
                                .badWordsDao(jdbi.onDemand(BadWordsDao.class))
                                .build())
                        .emailValidator(new EmailValidation())
                        .passwordValidator(new PasswordValidation())
                        .build())
                .build())
        .build();
  }
}
