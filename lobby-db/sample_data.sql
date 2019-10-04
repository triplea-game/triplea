delete
from moderator_action_history;
delete
from api_key;
delete
from lobby_user;
delete
from access_log;

insert into lobby_user(id, username, email, role,  bcrypt_password)
values (1000, 'test', 'email@email.com', 'ADMIN',
        '$2a$10$Ut3tvElEhPPr4s5wPd4dFuOvY25fa4r5XH3T7ucFTr5gJsotZl5d6'), -- password = 'test'
       (1001, 'user1', 'email@email.com', 'PLAYER',
        '$2a$10$C4rHfjK/seKexc6KlyknP.oFVBZ7Wi.kp91qUQFgmkKajwgczXzcS');

insert into access_log(access_time, username, ip, mac, registered)
values (now() - interval '1 days', 'user1', '1.1.1.1', '$1$AA$AA7qDBliIofq8jOm4nM0H/', false),
       (now() - interval '2 days', 'user2', '1.1.1.2', '$1$BA$AA7qDBliIofq8jOm4nM0H/', false),
       (now() - interval '3 days', 'user3', '1.1.1.3', '$1$CA$AA7qDBliIofq8jOm4nM0H/', true),
       (now() - interval '4 days', 'user1', '1.1.1.2', '$1$DA$AA7qDBliIofq8jOm4nM0H/', false),
       (now() - interval '5 days', 'user2', '1.1.1.4', '$1$EA$AA7qDBliIofq8jOm4nM0H/', false);

insert into moderator_action_history(lobby_user_id, action_name, action_target)
values (1000, 'ACTION_1', 'TARGET_1'),
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
