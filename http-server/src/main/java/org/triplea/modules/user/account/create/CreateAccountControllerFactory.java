package org.triplea.modules.user.account.create;

import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.BadWordsDao;
import org.triplea.db.dao.UserJdbiDao;
import org.triplea.domain.data.UserName;
import org.triplea.modules.user.account.NameValidation;
import org.triplea.modules.user.account.PasswordBCrypter;

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
