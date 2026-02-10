--liquibase formatted sql
--changeSet alexgls:initialize-functions runOnChange:true splitStatements:false stripComments:false

CREATE OR REPLACE FUNCTION set_updated_at_in_chats()
    RETURNS TRIGGER AS $func$
DECLARE
    lastChatId INTEGER := (SELECT chat_id FROM messages ORDER BY message_id DESC LIMIT 1);
BEGIN
    UPDATE chats SET updated_at = NOW() WHERE chat_id = lastChatId;
    RETURN NEW;
END;
$func$ LANGUAGE plpgsql;

CREATE TRIGGER after_add_message_trigger
    AFTER INSERT ON messages
    FOR EACH ROW
EXECUTE FUNCTION set_updated_at_in_chats();