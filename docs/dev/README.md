## Prerequisites

- Local installation/environment setup, see [Dev Setup](setup/dev_setup.md) to get started.


## Building and Running the Code  / Typical gradle commands

Launch the headed-game client:
```
./gradlew run 
```

Perform all code formatting, checks and tests (run **this** to verify PR builds):
```
./verify
```

## Building installers

- Install [Install4j7](https://www.ej-technologies.com/download/install4j/files)
- Create a `triplea/gradle.properties` file with:
```
install4jHomeDir = /path/to/install4j7/
```
- Obtain install47 license key (can get from maintainers or an open-source one from install4j)
- Run release task
```
export INSTALL4J_LICENSE_KEY={License key here}
./gradlew release
```

Installers will be created in `triplea/build/releases/`


## Docker Images

The following project-specific Docker images, which may be useful during development and testing, are available:

  - [Lobby database](https://github.com/triplea-game/triplea/tree/master/lobby-db/Dockerfile)



## Local Testing

SQL:
```sql

delete from moderator_action_history;
delete from moderator_api_key;
delete from lobby_user;

insert into lobby_user(id, username, email, admin, bcrypt_password) 
values 
  (1000, 'moderator', 'email@email.com', true,
   '$2a$10$C4rHfjK/seKexc6KlyknP.oFVBZ7Wi.kp91qUQFgmkKajwgczXzcS');

insert into moderator_api_key(lobby_user_id, api_key)
values(
 1000,
'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86'
);

insert into moderator_action_history(lobby_user_id, action_name, action_target)
values
(1000, 'ACTION_1', 'TARGET_1'),
(1000, 'ACTION_2', 'TARGET_2'),
(1000, 'ACTION_3', 'TARGET_3'),
(1000, 'ACTION_4', 'TARGET_4'),
(1000, 'ACTION_5', 'TARGET_5'),
(1000, 'ACTION_6', 'TARGET_6'),
(1000, 'ACTION_7', 'TARGET_7'),
(1000, 'ACTION_8', 'TARGET_8'),
(1000, 'ACTION_9', 'TARGET_9'),
(1000, 'ACTION_10', 'TARGET_10'),
(1000, 'ACTION_11', 'TARGET_11'),
(1000, 'ACTION_12', 'TARGET_12'),
(1000, 'ACTION_13', 'TARGET_13'),
(1000, 'ACTION_14', 'TARGET_14'),
(1000, 'ACTION_15', 'TARGET_15'),
(1000, 'ACTION_16', 'TARGET_16'),
(1000, 'ACTION_17', 'TARGET_17'),
(1000, 'ACTION_18', 'TARGET_18'),
(1000, 'ACTION_19', 'TARGET_19'),
(1000, 'ACTION_20', 'TARGET_20'),
(1000, 'ACTION_21', 'TARGET_21'),
(1000, 'ACTION_22', 'TARGET_22'),
(1000, 'ACTION_23', 'TARGET_23'),
(1000, 'ACTION_24', 'TARGET_24'),
(1000, 'ACTION_25', 'TARGET_25'),
(1000, 'ACTION_26', 'TARGET_26'),
(1000, 'ACTION_27', 'TARGET_27'),
(1000, 'ACTION_28', 'TARGET_28'),
(1000, 'ACTION_29', 'TARGET_29'),
(1000, 'ACTION_30', 'TARGET_30'),
(1000, 'ACTION_31', 'TARGET_31'),
(1000, 'ACTION_32', 'TARGET_32'),
(1000, 'ACTION_33', 'TARGET_33'),
(1000, 'ACTION_34', 'TARGET_34'),
(1000, 'ACTION_35', 'TARGET_35'),
(1000, 'ACTION_36', 'TARGET_36'),
(1000, 'ACTION_37', 'TARGET_37'),
(1000, 'ACTION_38', 'TARGET_38'),
(1000, 'ACTION_39', 'TARGET_39'),
(1000, 'ACTION_40', 'TARGET_40'),
(1000, 'ACTION_41', 'TARGET_41'),
(1000, 'ACTION_42', 'TARGET_42'),
(1000, 'ACTION_43', 'TARGET_43'),
(1000, 'ACTION_44', 'TARGET_44'),
(1000, 'ACTION_45', 'TARGET_45'),
(1000, 'ACTION_46', 'TARGET_46'),
(1000, 'ACTION_47', 'TARGET_47'),
(1000, 'ACTION_48', 'TARGET_48'),
(1000, 'ACTION_49', 'TARGET_49'),
(1000, 'ACTION_50', 'TARGET_50'),
(1000, 'ACTION_51', 'TARGET_51'),
(1000, 'ACTION_52', 'TARGET_52'),
(1000, 'ACTION_53', 'TARGET_53'),
(1000, 'ACTION_54', 'TARGET_54'),
(1000, 'ACTION_55', 'TARGET_55'),
(1000, 'ACTION_56', 'TARGET_56'),
(1000, 'ACTION_57', 'TARGET_57'),
(1000, 'ACTION_58', 'TARGET_58'),
(1000, 'ACTION_59', 'TARGET_59'),
(1000, 'ACTION_60', 'TARGET_60'),
(1000, 'ACTION_61', 'TARGET_61'),
(1000, 'ACTION_62', 'TARGET_62'),
(1000, 'ACTION_63', 'TARGET_63'),
(1000, 'ACTION_64', 'TARGET_64'),
(1000, 'ACTION_65', 'TARGET_65'),
(1000, 'ACTION_66', 'TARGET_66'),
(1000, 'ACTION_67', 'TARGET_67'),
(1000, 'ACTION_68', 'TARGET_68'),
(1000, 'ACTION_69', 'TARGET_69'),
(1000, 'ACTION_70', 'TARGET_70'),
(1000, 'ACTION_71', 'TARGET_71'),
(1000, 'ACTION_72', 'TARGET_72'),
(1000, 'ACTION_73', 'TARGET_73'),
(1000, 'ACTION_74', 'TARGET_74'),
(1000, 'ACTION_75', 'TARGET_75'),
(1000, 'ACTION_76', 'TARGET_76'),
(1000, 'ACTION_77', 'TARGET_77'),
(1000, 'ACTION_78', 'TARGET_78'),
(1000, 'ACTION_79', 'TARGET_79'),
(1000, 'ACTION_80', 'TARGET_80'),
(1000, 'ACTION_81', 'TARGET_81'),
(1000, 'ACTION_82', 'TARGET_82'),
(1000, 'ACTION_83', 'TARGET_83'),
(1000, 'ACTION_84', 'TARGET_84'),
(1000, 'ACTION_85', 'TARGET_85'),
(1000, 'ACTION_86', 'TARGET_86'),
(1000, 'ACTION_87', 'TARGET_87'),
(1000, 'ACTION_88', 'TARGET_88'),
(1000, 'ACTION_89', 'TARGET_89'),
(1000, 'ACTION_90', 'TARGET_90'),
(1000, 'ACTION_91', 'TARGET_91'),
(1000, 'ACTION_92', 'TARGET_92'),
(1000, 'ACTION_93', 'TARGET_93'),
(1000, 'ACTION_94', 'TARGET_94'),
(1000, 'ACTION_95', 'TARGET_95'),
(1000, 'ACTION_96', 'TARGET_96'),
(1000, 'ACTION_97', 'TARGET_97'),
(1000, 'ACTION_98', 'TARGET_98'),
(1000, 'ACTION_99', 'TARGET_99'),
(1000, 'ACTION_100', 'TARGET_100');
``

