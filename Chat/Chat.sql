CREATE TABLE Users_Chat(
id number PRIMARY KEY,
login varchar2(50) NOT NULL,
password varchar2(50) NOT NULL
);

CREATE TABLE Message_Chat(
id number PRIMARY KEY,
login varchar2(50) NOT NULL,
message_text varchar2(1000) NOT NULL,
message_date date NOT NULL
);

ALTER TABLE Message_Chat ADD CONSTRAINT fk_id_message_chat FOREIGN KEY (login) REFERENCES Users_Chat (login);

CREATE SEQUENCE users_chat_seq start with 1 increment by 1;
CREATE SEQUENCE message_chat_seq start with 1 increment by 1;

CREATE OR REPLACE TRIGGER users_chat_trigger_seq
BEFORE INSERT ON Users_Chat
FOR EACH ROW
BEGIN
SELECT users_chat_seq.NEXTVAL
INTO :new.id FROM DUAL;
END;
/

CREATE OR REPLACE TRIGGER message_chat_trigger_seq
BEFORE INSERT ON Message_Chat
FOR EACH ROW
BEGIN
SELECT message_chat_seq.NEXTVAL
INTO :new.id FROM DUAL;
END;
/

INSERT INTO Users_Chat(login, password) VALUES ('katya', '111111');

--DROP TABLE Users_Chat CASCADE CONSTRAINTS PURGE;
--DROP TABLE Message_Chat CASCADE CONSTRAINTS PURGE;
--DROP TRIGGER users_chat_trigger_seq;
--DROP TRIGGER message_chat_trigger_seq;
--DROP SEQUENCE users_chat_seq;
--DROP SEQUENCE message_chat_seq;