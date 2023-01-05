create table lobby_message
(
    message varchar(256) not null
);
comment on table lobby_message is 'Stores the message that we display to users on login';

insert into lobby_message(message)
values ('Welcome to the TripleA lobby!\n' ||
        'Please no politics, stay respectful, be welcoming, have fun.');
