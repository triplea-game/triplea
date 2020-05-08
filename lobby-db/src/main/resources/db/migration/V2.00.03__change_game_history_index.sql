drop index lobby_game_host_name_game_id;
create index lobby_game_game_id on lobby_game (game_id);

alter table lobby_chat_history
    alter column date type timestamptz;
alter table game_chat_history
    alter column date type timestamptz;
alter table lobby_game
    alter column date_created type timestamptz;
alter table bad_word
    alter column date_created type timestamptz;
