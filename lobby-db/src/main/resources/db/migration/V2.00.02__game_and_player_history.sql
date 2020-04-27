create table lobby_game
(
    id                      serial primary key,
    host_name               character varying(40)       not null,
    game_id                 character varying(36)       not null,
    game_hosting_api_key_id int                         not null references game_hosting_api_key (id),
    date_created            timestamp without time zone not null default now()
);

comment on table lobby_game is
    $$Records games that have been posted to the lobby$$;
comment on column lobby_game.host_name is
    $$Name of the game host, can be a bot or a player name$$;
comment on column game_chat_history.game_id is
    $$ID of the game as assigned by the lobby. 'game_id' is a publicly known field and is sent to players.
    A single game can have multiple game-ids if it is disconnected and reconnects.$$;

create index lobby_game_host_name_game_id on lobby_game (host_name, game_id);


create table game_chat_history
(
    id            serial primary key,
    lobby_game_id int                         not null references lobby_game (id),
    date          timestamp without time zone not null default now(),
    username      character varying(40)       not null,
    message       character varying(240)      not null
);

comment on table game_chat_history is
    $$Table recording history of chat messages in games connected to the lobby.
    Of note, players can connect directly to lobby games and may not necessarily have an API key.$$;
comment on column game_chat_history.username is
    $$The name of the player sending a chat message$$;
comment on column game_chat_history.message is
    $$Contents of the chat message sent$$;

create index game_chat_history_username on game_chat_history (username);
create index game_chat_history_date on game_chat_history (date);
