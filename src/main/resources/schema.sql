-- Enable the pgvector extension if it's not already enabled
CREATE EXTENSION IF NOT EXISTS vector;

-- Create the videos table to store overall video metadata
CREATE TABLE IF NOT EXISTS videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL, -- For multi-tenancy
    title VARCHAR(255),
    description TEXT,
    file_path VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL, -- UPLOADED, PROCESSING, COMPLETED, FAILED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create the video_chunks table to store individual transcribed segments and their vectors
CREATE TABLE IF NOT EXISTS video_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id UUID NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
    start_timestamp DOUBLE PRECISION NOT NULL,
    end_timestamp DOUBLE PRECISION NOT NULL,
    transcript TEXT NOT NULL,
    visual_tags JSONB,
    embedding vector(1536), -- 1536 dimensions for Google Vertex AI text-embedding-004
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create an HNSW index on the embedding column using cosine distance (vector_cosine_ops)
-- HNSW (Hierarchical Navigable Small World) provides fast Approximate Nearest Neighbor (ANN) search.
CREATE INDEX IF NOT EXISTS idx_video_chunks_embedding
ON video_chunks USING hnsw (embedding vector_cosine_ops);

-- Index for finding chunks by video_id quickly
CREATE INDEX IF NOT EXISTS idx_video_chunks_video_id ON video_chunks(video_id);
