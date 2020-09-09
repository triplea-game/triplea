create table lobby_chat_history
(
    id               serial primary key,
    date             timestamp without time zone not null default now(),
    username         character varying(40)       not null,
    lobby_api_key_id int                         not null references lobby_api_key (id),
    message          character varying(240)      not null
);

comment on table lobby_chat_history is
    $$Table recording history of all chat messages in the lobby$$;
comment on column lobby_chat_history.username is
    $$De-normalized column representing the name of the user that
    sent the chat message. It is denormalized for query convenience.$$;
comment on column lobby_chat_history.lobby_api_key_id is
    $$Foreign key reference to the API-key used by the user when logging in. Useful to
    know the users IP and system id information for cross-reference.$$;

comment on column lobby_chat_history.message is
    $$The contents of the chat message sent by the user$$;

create index lobby_chat_date on lobby_chat_history (date);
create index lobby_chat_username on lobby_chat_history (username);
