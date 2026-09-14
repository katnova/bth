package db

import cinterop.NativeSqlite3

object Schema {
    fun createTablesIfNotExists(sql: NativeSqlite3) {
        sql.rawExec(
            """             
create table if not exists commands
(
    id      INTEGER not null
        primary key autoincrement,
    command TEXT    not null
        unique
)
    strict;

create table if not exists groups
(
    id   integer not null
        constraint groups_pk
            primary key autoincrement,
    name text    not null
) strict;

create table if not exists bookmarks
(
    id      integer not null
        constraint bookmarks_pk
            primary key autoincrement,
    cmd_id  integer not null
        constraint bookmarks_commands_id_fk
            references commands,
    "group" integer
        constraint bookmarks_groups_id_fk
            references groups
) strict;

create table if not exists directories
(
    id   INTEGER not null
        primary key autoincrement,
    path TEXT    not null
        unique
)
    strict;
    
create table if not exists sessions
(
    id   INTEGER not null
        primary key autoincrement,
    start_epoch     REAL              not null
)
    strict;

create table if not exists history
(
    id              INTEGER           not null
        primary key autoincrement,
    start_epoch     REAL              not null,
    start_dir_id    INTEGER           not null
        constraint command_history_starting_directory_fk
            references directories
            on update cascade on delete restrict,
    cmd_id          INTEGER           not null
        constraint command_history_command_fk
            references commands
            on update cascade on delete restrict,
    elevated_before INTEGER default 0 not null,
    elevated_after  INTEGER,
    end_dir_id      INTEGER
        constraint command_history_ending_directory_fk
            references directories
            on update cascade on delete set null,
    exit_code       INTEGER,
    end_epoch       REAL,
    session         integer
        constraint command_history_session_fk
            references sessions
            on update cascade on delete set null,
    constraint command_history_started_before_ended_check
        check (end_epoch IS NULL
            OR end_epoch >= start_epoch),
    constraint command_history_was_elevated_after_check
        check (elevated_after IS NULL
            OR elevated_after IN (0, 1)),
    constraint command_history_was_elevated_before_check
        check (elevated_before IN (0, 1))
)
    strict;

create index if not exists command_history_command_started_at_desc_idx
    on history (cmd_id asc, start_epoch desc);

create index if not exists command_history_ending_directory_started_at_desc_idx
    on history (end_dir_id asc, start_epoch desc)
    where end_dir_id IS NOT NULL;

create index if not exists command_history_exit_code_started_at_desc_idx
    on history (exit_code asc, start_epoch desc)
    where exit_code IS NOT NULL;

create index if not exists command_history_started_at_desc_idx
    on history (start_epoch desc);

create index if not exists command_history_starting_directory_started_at_desc_idx
    on history (start_dir_id asc, start_epoch desc);

create index if not exists command_history_unfinished_started_at_desc_idx
    on history (start_epoch desc)
    where end_epoch IS NULL;
        """.trimIndent()
        )
    }
}
