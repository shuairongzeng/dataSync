-- Cache database schema for DbDataSync
-- This SQLite database will store cached data to improve performance

-- Cache metadata table
CREATE TABLE IF NOT EXISTS cache_metadata (
                                              id INTEGER PRIMARY KEY AUTOINCREMENT,
                                              cache_key VARCHAR(255) UNIQUE NOT NULL,
    cache_type VARCHAR(50) NOT NULL, -- 'table_list', 'query_result', 'schema', 'connection'
    data_source VARCHAR(100) NOT NULL, -- database connection identifier
    created_at TIMESTAMP DEFAULT (datetime('now')),
    updated_at TIMESTAMP DEFAULT (datetime('now')),
    expires_at TIMESTAMP,
    hit_count INTEGER DEFAULT 0,
    data_size INTEGER DEFAULT 0,
    checksum VARCHAR(64) -- for data integrity
    );

-- Cache data table (stores actual cached content)
CREATE TABLE IF NOT EXISTS cache_data (
                                          id INTEGER PRIMARY KEY AUTOINCREMENT,
                                          cache_key VARCHAR(255) NOT NULL,
    data_content TEXT NOT NULL, -- JSON serialized data
    compression_type VARCHAR(20) DEFAULT 'none', -- 'none', 'gzip', 'lz4'
    created_at TIMESTAMP DEFAULT (datetime('now')),
    FOREIGN KEY (cache_key) REFERENCES cache_metadata(cache_key) ON DELETE CASCADE
    );

-- Database connection cache
CREATE TABLE IF NOT EXISTS connection_cache (
                                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                connection_id VARCHAR(100) UNIQUE NOT NULL,
    database_type VARCHAR(50) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    database_name VARCHAR(255) NOT NULL,
    schema_info TEXT, -- JSON serialized schema information
    table_count INTEGER DEFAULT 0,
    last_accessed TIMESTAMP DEFAULT (datetime('now')),
    is_active BOOLEAN DEFAULT 1
    );

-- Query result cache
CREATE TABLE IF NOT EXISTS query_cache (
                                           id INTEGER PRIMARY KEY AUTOINCREMENT,
                                           query_hash VARCHAR(64) UNIQUE NOT NULL, -- MD5/SHA256 of normalized query
    original_query TEXT NOT NULL,
    normalized_query TEXT NOT NULL,
    connection_id VARCHAR(100) NOT NULL,
    result_data TEXT NOT NULL, -- JSON serialized result
    row_count INTEGER DEFAULT 0,
    execution_time_ms INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT (datetime('now')),
    expires_at TIMESTAMP,
    access_count INTEGER DEFAULT 0,
    FOREIGN KEY (connection_id) REFERENCES connection_cache(connection_id)
    );

-- Table schema cache
CREATE TABLE IF NOT EXISTS table_schema_cache (
                                                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                  connection_id VARCHAR(100) NOT NULL,
    table_name VARCHAR(255) NOT NULL,
    schema_name VARCHAR(255),
    column_info TEXT NOT NULL, -- JSON serialized column information
    index_info TEXT, -- JSON serialized index information
    constraint_info TEXT, -- JSON serialized constraint information
    row_count_estimate INTEGER,
    table_size_bytes BIGINT,
    last_analyzed TIMESTAMP DEFAULT (datetime('now')),
    created_at TIMESTAMP DEFAULT (datetime('now')),
    UNIQUE(connection_id, table_name, schema_name),
    FOREIGN KEY (connection_id) REFERENCES connection_cache(connection_id)
    );

-- Cache statistics
CREATE TABLE IF NOT EXISTS cache_statistics (
                                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                date DATE DEFAULT (date('now')),
    cache_type VARCHAR(50) NOT NULL,
    hit_count INTEGER DEFAULT 0,
    miss_count INTEGER DEFAULT 0,
    eviction_count INTEGER DEFAULT 0,
    total_size_bytes BIGINT DEFAULT 0,
    avg_response_time_ms REAL DEFAULT 0,
    UNIQUE(date, cache_type)
    );

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_cache_metadata_key ON cache_metadata(cache_key);
CREATE INDEX IF NOT EXISTS idx_cache_metadata_type ON cache_metadata(cache_type);
CREATE INDEX IF NOT EXISTS idx_cache_metadata_expires ON cache_metadata(expires_at);
CREATE INDEX IF NOT EXISTS idx_cache_data_key ON cache_data(cache_key);
CREATE INDEX IF NOT EXISTS idx_query_cache_hash ON query_cache(query_hash);
CREATE INDEX IF NOT EXISTS idx_query_cache_connection ON query_cache(connection_id);
CREATE INDEX IF NOT EXISTS idx_table_schema_connection ON table_schema_cache(connection_id);
CREATE INDEX IF NOT EXISTS idx_connection_cache_active ON connection_cache(is_active);

-- Triggers for automatic timestamp updates
CREATE TRIGGER IF NOT EXISTS update_cache_metadata_timestamp
AFTER UPDATE ON cache_metadata
                            FOR EACH ROW
BEGIN
UPDATE cache_metadata SET updated_at = datetime('now') WHERE id = NEW.id;
END;

-- Cleanup expired cache entries (can be called periodically)
CREATE VIEW IF NOT EXISTS expired_cache AS
SELECT cache_key FROM cache_metadata
WHERE expires_at IS NOT NULL AND expires_at < datetime('now')
