insert into rhnServerGroupTypeFeature (server_group_type_id, feature_id,
                                       created, modified)
values (lookup_sg_type('salt_entitled'), lookup_feature_type('ftr_package_updates'),
        current_timestamp,current_timestamp);

insert into rhnServerGroupTypeFeature (server_group_type_id, feature_id,
                                       created, modified)
values (lookup_sg_type('salt_entitled'), lookup_feature_type('ftr_package_remove'),
        current_timestamp,current_timestamp);

