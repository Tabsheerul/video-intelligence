# Video Intelligence Search API

An advanced B2B Video Intelligence platform that makes video libraries fully searchable and shoppable. 

This robust Spring Boot application ingests videos, extracts audio using FFmpeg, converts it to text using the Google Cloud Speech-to-Text API, and generates 1536-dimensional vectors using Google Vertex AI. The vectors are stored in a PostgreSQL database with the `pgvector` extension, allowing users to perform low-latency semantic searches to find the exact timestamp a specific topic or product was mentioned in a video.

## Tech Stack
- **Framework:** Spring Boot 3 + Java 21
- **Database:** PostgreSQL with `pgvector` & Hibernate Vector integration
- **Messaging:** Apache Kafka (Event-Driven AI processing)
- **AI/ML:** Spring AI, Google Cloud Vertex AI (text-embedding-004), Google Cloud Speech-to-Text V1
- **Media Processing:** FFmpeg (via jave2 wrapper)

## Features
- **Asynchronous Processing:** Video uploads instantly return `202 ACCEPTED` and are placed in a Kafka queue.
- **Resilient Pipelines:** Dead-Letter Queues (DLQ) configured for automatic failure handling.
- **AI Embeddings:** Uses the latest `text-embedding-004` model to vectorize transcripts.
- **Semantic Search:** Lightning-fast vector similarity search (`<->` operator) using HNSW indices in PostgreSQL.

## Prerequisites
- Docker & Docker Compose
- Java 21 & Maven
- Google Cloud Project with the `Cloud Speech-to-Text API` and `Vertex AI API` enabled.
- A valid Google Cloud Service Account JSON Key set as `GOOGLE_APPLICATION_CREDENTIALS`.

## How to Run Locally

### 1. Start Infrastructure (Kafka, Zookeeper, PostgreSQL)
The project includes a `docker-compose.yml` to instantly spin up the required external infrastructure.
```bash
docker compose up -d
```

### 2. Configure Environment Variables
You must configure your Google Cloud credentials. You can set them in your environment variables or directly inside `src/main/resources/application.properties`.
```properties
# Example snippet
spring.ai.vertex.ai.gemini.project-id=your-gcp-project-id
spring.ai.vertex.ai.gemini.location=us-central1
```

### 3. Run the Spring Boot App
```bash
./mvnw spring-boot:run
```

## API Usage Example
Upload a video file for processing:
```bash
curl -X POST http://localhost:8081/api/v1/videos/upload \
  -F "file=@demo.mp4;type=video/mp4" \
  -F "title=Product Demo" \
  -F "tenantId=user-123"
```
*Note: Ensure the file part explicitly includes `;type=video/mp4` when using curl.*
