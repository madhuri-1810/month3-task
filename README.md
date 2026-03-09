# 📱 Microservices-Based Social Media Platform

> **Month 3 Project** — Advanced Full Stack Integration  
> Built with Spring Boot, Spring Cloud, React 18, RabbitMQ, Docker

---

## 🏗️ Architecture Overview

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
```

---

## 🔧 Microservices

| Service | Port | Responsibility |
|---------|------|----------------|
| API Gateway | 8080 | Routing, Auth, Rate Limiting |
| Service Discovery | 8761 | Eureka Server |
| User Service | 8081 | Auth, Profiles, Follow |
| Post Service | 8082 | CRUD Posts, Feed |
| Notification Service | 8083 | Real-time Notifications |
| Chat Service | 8084 | WebSocket Messaging |
| Media Service | 8085 | Image/Video Upload |
| Analytics Service | 8086 | User Behavior Analytics |

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Maven 3.8+

### Run with Docker Compose (Recommended)

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

### Access Points
- **Frontend:** http://localhost:3000
- **API Gateway:** http://localhost:8080
- **Eureka Dashboard:** http://localhost:8761
- **RabbitMQ Management:** http://localhost:15672 (guest/guest)
- **Zipkin Tracing:** http://localhost:9411
- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3001

---

## 📡 API Gateway Routes

| Route | Service | Description |
|-------|---------|-------------|
| `/api/users/**` | User Service | User management |
| `/api/auth/**` | User Service | Authentication |
| `/api/posts/**` | Post Service | Post CRUD |
| `/api/feed/**` | Post Service | User feed |
| `/api/notifications/**` | Notification Service | Notifications |
| `/api/chat/**` | Chat Service | Messaging |
| `/api/media/**` | Media Service | File uploads |
| `/api/analytics/**` | Analytics Service | Analytics data |

---

## 🐰 RabbitMQ Events

| Exchange | Routing Key | Publisher | Consumer |
|----------|-------------|-----------|----------|
| social-media.exchange | post.created | Post Service | Notification, Analytics |
| social-media.exchange | user.followed | User Service | Notification, Analytics |
| social-media.exchange | message.sent | Chat Service | Notification, Analytics |
| social-media.exchange | media.uploaded | Media Service | Analytics |

---

## 🛡️ Resilience Patterns

- **Circuit Breaker:** Resilience4j on all inter-service calls
- **Retry:** Exponential backoff (3 retries)
- **Bulkhead:** Thread pool isolation per service
- **Rate Limiting:** Redis-based per user/IP
- **Timeout:** Configured per service type

---

## 📊 Monitoring Stack

- **Metrics:** Prometheus + Grafana
- **Tracing:** Spring Cloud Sleuth + Zipkin
- **Logging:** ELK Stack (Elasticsearch, Logstash, Kibana)
- **Health:** Spring Boot Actuator endpoints

---

## 🏗️ Project Structure

```
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
```

---

## 📤 Submission Info

- **Due:** April 9, 2026
- **Version:** Standard (6 services + real-time)
- **Stack:** Spring Boot 3 + React 18 + Docker + RabbitMQ
