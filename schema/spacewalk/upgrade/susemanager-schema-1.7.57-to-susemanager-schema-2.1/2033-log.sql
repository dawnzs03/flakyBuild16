CREATE TABLE log
(
    id INTEGER NOT NULL PRIMARY KEY,
    stamp TIMESTAMP WITH TIME ZONE,
    user_id INTEGER REFERENCES web_contact_all(id)
)

;

CREATE SEQUENCE log_seq;
