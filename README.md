Payment Project (multi-module Spring Boot)

Modules:

- common: shared DTOs and Kafka event models
- auth-service: authentication & authorization (JWT)
- card-service: add card API, persists to SQL, publishes events to Kafka, caches card data in Redis
- loan-service: loan microservice, consumes events from Kafka, persists to SQL

Infra (suggested): Postgres for SQL, Redis for cache, Kafka for events.

Next steps:

- Review the scaffolded modules
- Provide DB credentials and infra preferences (docker-compose or cloud)
- I can add full JWT implementation and Docker Compose if you want
