DROP TABLE IF EXISTS SPRING_AI_CHAT_MEMORY;

CREATE TABLE SPRING_AI_CHAT_MEMORY (
                                       conversation_id VARCHAR(255) NOT NULL,
                                       sequence_id BIGINT AUTO_INCREMENT,
                                       type VARCHAR(50) NOT NULL,
                                       content CLOB NOT NULL,
                                       timestamp TIMESTAMP NOT NULL,
                                       PRIMARY KEY (conversation_id, sequence_id)
);

CREATE INDEX idx_chat_memory_conversation_id
    ON SPRING_AI_CHAT_MEMORY(conversation_id);