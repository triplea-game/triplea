create table game_chat_history
(
    id        serial primary key,
    date      timestamp without time zone not null default now(),
    host_name character varying(40)       not null,
    game_id   character varying(36)       not null,
    username  character varying(40)       not null,
    message   character varying(240)      not null
);

comment on table game_chat_history is
    $$Table recording history of chat messages in games connected to the lobby.
    Of note, players can connect directly to lobby games and may not necessarily have an API key.$$;
comment on column game_chat_history.host_name is
    $$Name of the game host, can be a bot or a player name$$;
comment on column game_chat_history.game_id is
    $$ID of the game as posted in the lobby. A single game session can have multiple game IDs
        if disconnected and then reconnected to lobby$$;
comment on column game_chat_history.username is
    $$The name of the player sending a chat message$$;
comment on column game_chat_history.message is
    $$Contents of the chat message sent$$;

create index game_chat_history_host_game_id on game_chat_history (host_name, game_id);
create index game_chat_history_username on game_chat_history (username);
