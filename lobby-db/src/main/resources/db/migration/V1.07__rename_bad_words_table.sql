create table bad_word as select word, now() as date_created from bad_words;
alter table bad_word add primary key (word);
alter table bad_word alter column date_created set not null;
alter table bad_word alter column date_created set default now();
