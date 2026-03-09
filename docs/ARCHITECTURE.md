# 📋 Architecture Documentation
## Microservices-Based Social Media Platform

---

## 1. Architecture Design

### Microservices Architecture Pattern
This platform follows the **microservices architecture** pattern where each service:
- Has a **single responsibility** (SRP)
- Has its **own database** (Database-per-Service pattern)
- Communicates via **REST APIs** (sync) or **message queues** (async)
- Can be **deployed independently**

### Architecture Diagram
```
Client (React SPA)
       │
       ▼
API Gateway (Spring Cloud Gateway)
  ├─ JWT Authentication
  ├─ Rate Limiting (Redis)
  ├─ Load Balancing (Ribbon)
  ├─ Circuit Breaker (Resilience4j)
  └─ Routing

Service Discovery (Eureka Server)
  └─ All services register here

Microservices:
  ├─ User Service     (8081) - PostgreSQL user_db
  ├─ Post Service     (8082) - PostgreSQL post_db
  ├─ Notification Svc (8083) - PostgreSQL notification_db
  ├─ Chat Service     (8084) - PostgreSQL chat_db
  ├─ Media Service    (8085) - PostgreSQL media_db
  └─ Analytics Svc    (8086) - PostgreSQL analytics_db

Message Broker (RabbitMQ)
  └─ Exchange: social-media.exchange
       ├─ post.created    → notification.queue, analytics.queue
       ├─ user.followed   → notification.queue, analytics.queue
       ├─ post.liked      → notification.queue, analytics.queue
       ├─ message.sent    → notification.queue, analytics.queue
       └─ media.uploaded  → analytics.queue

Real-time Communication (WebSocket/STOMP)
  └─ Through Chat Service
       ├─ /user/{id}/queue/messages
       ├─ /user/{id}/queue/notifications
       ├─ /user/{id}/queue/typing
       └─ /topic/feed (public feed updates)
```

---

## 2. API Contracts

### Authentication Endpoints (User Service)
```
POST /api/auth/register
Body: { username, email, password, displayName }
Response: { accessToken, refreshToken, userId, username, email, role }

POST /api/auth/login
Body: { usernameOrEmail, password }
Response: { accessToken, refreshToken, userId, username, email, role }

POST /api/auth/refresh
Header: X-Refresh-Token: <token>
Response: { accessToken, refreshToken }

POST /api/auth/logout
Header: Authorization: Bearer <token>
Response: 200 OK
```

### Post Endpoints (Post Service)
```
POST   /api/posts               - Create post
GET    /api/posts/{id}          - Get post by ID
PUT    /api/posts/{id}          - Update post
DELETE /api/posts/{id}          - Delete post
POST   /api/posts/{id}/like     - Like post
DELETE /api/posts/{id}/like     - Unlike post
GET    /api/feed                - Get user feed (paginated)
GET    /api/posts/trending      - Get trending posts
GET    /api/posts/hashtag/{tag} - Get posts by hashtag
```

### User Endpoints (User Service)
```
GET    /api/users/{id}                - Get user profile
GET    /api/users/username/{username} - Get user by username
PUT    /api/users/profile             - Update own profile
GET    /api/users/search?query=       - Search users
POST   /api/users/{id}/follow         - Follow user
DELETE /api/users/{id}/follow         - Unfollow user
GET    /api/users/{id}/followers      - Get user's followers
GET    /api/users/{id}/following      - Get users they follow
```

### Notification Endpoints
```
GET  /api/notifications               - Get notifications (paginated)
GET  /api/notifications/unread-count  - Get unread count
PUT  /api/notifications/{id}/read     - Mark as read
PUT  /api/notifications/read-all      - Mark all as read
DELETE /api/notifications/{id}        - Delete notification
```

### Chat Endpoints
```
GET    /api/chat/conversations          - Get all conversations
GET    /api/chat/messages/{userId}      - Get messages with user
DELETE /api/chat/messages/{messageId}  - Delete message
GET    /api/chat/unread-count          - Get unread message count

WebSocket (STOMP):
SEND   /app/chat.send    - Send message
SEND   /app/chat.typing  - Typing indicator
SEND   /app/chat.read    - Mark as read
```

---

## 3. Real-time Communication Protocols

### WebSocket / STOMP
- **Protocol:** STOMP over SockJS (fallback for WebSocket)
- **Endpoint:** `ws://host/ws`
- **Authentication:** JWT in connection headers

### Event Types
| Topic | Direction | Payload |
|-------|-----------|---------|
| `/user/{id}/queue/messages` | Server→Client | Message object |
| `/user/{id}/queue/notifications` | Server→Client | Notification object |
| `/user/{id}/queue/typing` | Server→Client | {senderId, typing} |
| `/user/{id}/queue/read-receipts` | Server→Client | {userId, conversationId} |
| `/topic/feed` | Server→All | New post object |
| `/app/chat.send` | Client→Server | {recipientId, content} |
| `/app/chat.typing` | Client→Server | {recipientId, typing} |

---

## 4. Deployment Procedures

### Local Development with Docker Compose
```bash
# Start all services
docker-compose up -d

# Scale a service
docker-compose up -d --scale post-service=3

# View logs
docker-compose logs -f post-service

# Rebuild a service
docker-compose up -d --build user-service

# Stop all
docker-compose down
```

### Production Deployment with Kubernetes
```bash
# Apply all manifests
kubectl apply -f kubernetes/

# Check deployments
kubectl get pods -n social-media

# Scale a deployment
kubectl scale deployment post-service --replicas=5 -n social-media

# Rolling update (zero downtime)
kubectl set image deployment/post-service post-service=social-media/post-service:v2 -n social-media

# Check HPA status
kubectl get hpa -n social-media
```

---

## 5. Scalability Considerations

### Horizontal Scaling
- All services are **stateless** (JWT auth, no server-side sessions)
- Post Service scales to 3+ replicas (highest traffic)
- API Gateway scales to 2+ replicas
- HPA configured: auto-scale at 70% CPU

### Database Sharding
- User data: shard by `userId` hash
- Post data: shard by `authorId` 
- Message data: shard by `conversationId`

### Caching Strategy
```
Redis Cache Layers:
├─ User profiles (TTL: 1 hour)
├─ Post data (TTL: 5 minutes)
├─ Feed cache (TTL: 30 seconds)
├─ Rate limit counters (TTL: 1 minute)
└─ JWT blacklist (TTL: token expiry)
```

### CDN Integration
- Media files served via CDN (CloudFront/Cloudflare)
- Static frontend assets cached at edge
- Image optimization via media service before CDN upload

---

## 6. Monitoring Strategy

### Metrics (Prometheus + Grafana)
- **RED Method:** Rate, Errors, Duration per service
- **USE Method:** Utilization, Saturation, Errors per resource
- **Business Metrics:** DAU, Posts/day, Messages/day

### Distributed Tracing (Zipkin + Sleuth)
- Trace IDs propagated across all services via HTTP headers
- End-to-end request visibility
- Performance bottleneck identification

### Logging (ELK Stack)
```
All Services → Logstash → Elasticsearch → Kibana
Log Format: JSON structured logs
Fields: timestamp, service, traceId, spanId, level, message
```

### Alerting Rules
- Response time > 500ms → Warning
- Error rate > 1% → Critical
- Service down → PagerDuty alert
- Queue depth > 10K → Scale notification

---

## 7. Resilience Patterns

### Circuit Breaker (Resilience4j)
```java
// Applied at API Gateway for all inter-service calls
circuitBreaker:
  slidingWindowSize: 10 calls
  failureRateThreshold: 50%
  waitDurationInOpenState: 10s
  permittedCallsInHalfOpen: 3
```

### Retry with Backoff
```
Retry attempts: 3
Backoff: 1s, 2s, 4s (exponential)
Applied to: external API calls, DB connections
```

### Bulkhead Pattern
- Separate thread pools per service category
- Prevents cascade failures
- Max concurrent calls: 25 per service

### Rate Limiting
- Per-user: 100 req/min
- Per-IP: 500 req/min
- Post creation: 10 posts/hour
- Login attempts: 5/15min

---

## 8. Security

### Authentication Flow
```
1. Client sends credentials → User Service
2. User Service validates → Returns JWT (15min) + Refresh Token (7 days)
3. Client sends Bearer token with each request → API Gateway
4. API Gateway validates JWT → Extracts user info → Forwards to service
5. Service uses X-User-Id header (never trusts client-provided user ID)
```

### Data Protection
- Passwords: BCrypt (cost factor 12)
- JWT: HS256 signed with 256-bit secret
- HTTPS enforced on all endpoints
- SQL injection prevention via JPA parameterized queries
- XSS prevention via input sanitization
- File upload: virus scanning, type validation, size limits

---

## 9. Analysis Questions (Answered)

**Q: How does microservices architecture support social media scalability?**
Each service scales independently based on load. Post Service (high read) scales to 10+ replicas while Analytics (batch) runs with 1. This is impossible with monolith.

**Q: What are the challenges of real-time features in distributed systems?**
WebSocket connections are stateful — connection stickiness needed. Message ordering across nodes requires careful design. STOMP over a message broker solves distribution.

**Q: How can WebSocket connections be managed at scale?**
Use sticky sessions via load balancer OR externalize session state to Redis pub/sub. Each app node subscribes to user-specific Redis channels and forwards to connected WebSocket.

**Q: What data consistency patterns work for social graphs?**
Eventual consistency: follow/unfollow propagates asynchronously. Feed is eventually consistent — acceptable for social media. Use CQRS: write to normalized DB, read from denormalized feed cache.

**Q: How does event-driven architecture improve system resilience?**
Services decouple via RabbitMQ. If Notification Service is down, events queue up and replay on recovery. No data loss. Post Service doesn't fail because Notification Service is unavailable.
