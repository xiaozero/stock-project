-- 为 chat_message 表添加 image_data 列，支持图片消息持久化
ALTER TABLE chat_message ADD COLUMN image_data MEDIUMTEXT AFTER content;
