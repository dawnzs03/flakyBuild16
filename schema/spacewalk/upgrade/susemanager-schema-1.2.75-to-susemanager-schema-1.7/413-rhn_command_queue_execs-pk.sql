alter table rhn_command_queue_execs disable constraint rhn_cqexe_inst_id_nsaint_pk;
drop index rhn_cqexe_inst_id_nsaint_pk;
alter table rhn_command_queue_execs enable constraint rhn_cqexe_inst_id_nsaint_pk;
