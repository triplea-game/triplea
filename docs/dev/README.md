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
(1000, 'ACTION_50', 'TARGET_50');
```

