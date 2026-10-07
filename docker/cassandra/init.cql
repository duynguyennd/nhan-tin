-- Message Store: keyspace + bảng messages theo thiết kế (mục 5.2)
-- Local dev dùng replication factor 1; production đổi thành 3.
CREATE KEYSPACE IF NOT EXISTS chat_system
WITH replication = {
    'class': 'NetworkTopologyStrategy',
    'datacenter1': 1
};

CREATE TABLE IF NOT EXISTS chat_system.messages (
    conversation_id uuid,
    bucket_id text,        -- YYYY-MM: phân vùng tránh partition quá lớn
    message_id timeuuid,   -- duy nhất + chứa mốc thời gian
    sender_id uuid,
    content text,
    media_urls list<text>,
    status text,           -- 'SENT', 'DELIVERED', 'READ'
    reply_to_id timeuuid,
    reply_to_content text,
    reply_to_sender_id uuid,
    PRIMARY KEY ((conversation_id, bucket_id), message_id)
) WITH CLUSTERING ORDER BY (message_id DESC);

CREATE TABLE IF NOT EXISTS chat_system.message_reactions (
    conversation_id uuid,
    bucket_id text,
    message_id timeuuid,
    user_id uuid,
    emoji text,
    created_at timestamp,
    PRIMARY KEY ((conversation_id, bucket_id, message_id), user_id)
);
