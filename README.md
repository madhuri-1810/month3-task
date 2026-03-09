 HEAD
Microservices-Based Social Media Platform

> Advanced Full Stack Integration  
> Built with Spring Boot, Spring Cloud, React 18, RabbitMQ, Docker

---
 Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENTS                              │
│              React Web App (port 3000)                      │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP / WebSocket
┌─────────────────────────▼───────────────────────────────────┐
│                   API GATEWAY (8080)                        │
│              Spring Cloud Gateway + JWT Auth                │
└──┬────────┬──────────┬──────────┬──────────┬───────────────┘
   │        │          │          │          │
┌──▼──┐ ┌──▼──┐  ┌────▼──┐  ┌───▼──┐  ┌────▼──┐  ┌──────────┐
│User │ │Post │  │Notif  │  │Chat  │  │Media  │  │Analytics │
│8081 │ │8082 │  │8083   │  │8084  │  │8085   │  │8086      │
└──┬──┘ └──┬──┘  └───────┘  └──────┘  └───────┘  └──────────┘
   │        │         ▲           ▲
   └────────┴─────────┴───────────┘
                  RabbitMQ (5672)
                  
┌─────────────────────────────────────────────────────────────┐
│              Service Discovery — Eureka (8761)              │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│         PostgreSQL (5432) — Per-service databases           │
└─────────────────────────────────────────────────────────────┘
`

Quick Start

Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Maven 3.8+

Run with Docker Compose (Recommended)

```bash
# Clone the repository
git clone https://github.com/yourusername/social-media-platform.git
cd social-media-platform

# Start all services
docker-compose up -d

# Check service health
docker-compose ps

# View logs
docker-compose logs -f api-gateway
```
 API Gateway Routes

| Route | Service | Description |
|-------|---------|-------------|
| `/api/users/**` | User Service | User management |
| `/api/auth/**` | User Service | Authentication |
| `/api/posts/**` | Post Service | Post CRUD |
| `/api/feed/**` | Post Service | User feed |
| `/api/notifications/**` | Notification Service | Notifications |
| `/api/chat/**` | Chat Service | Messaging |
| `/api/media/**` | Media Service | File uploads |
| `/api/analytics/**` | Analytics Service | Analytics data 
|


social-media-platform/
├── README.md
├── docker-compose.yml
├── frontend/                   # React 18 app
├── api-gateway/                # Spring Cloud Gateway
├── service-discovery/          # Eureka Server
├── user-service/               # User management
├── post-service/               # Post management
├── notification-service/       # Notifications
├── chat-service/               # Real-time chat
├── media-service/              # Media handling
├── analytics-service/          # Analytics
├── kubernetes/                 # K8s manifests
├── monitoring/                 # Prometheus/Grafana
└── docs/                       # Architecture docs

