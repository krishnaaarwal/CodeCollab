<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0d0f12,50:1a73e8,100:cbf14c&height=200&section=header&text=NEXIS&fontSize=80&fontColor=cbf14c&fontAlignY=38&desc=Real-Time%20Collaborative%20Development%20Platform&descAlignY=60&descColor=94a3b8&animation=fadeIn" width="100%" />

<br/>

[![Demo Video](https://img.shields.io/badge/▶_Demo_Video-FF0000?style=for-the-badge&logo=youtube&logoColor=white)](https://youtu.be/I7lwOGdrpSM)
[![Made With Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React_18-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)](https://reactjs.org)
[![TypeScript](https://img.shields.io/badge/TypeScript-007ACC?style=for-the-badge&logo=typescript&logoColor=white)](https://typescriptlang.org)
[![Docker](https://img.shields.io/badge/Docker-2CA5E0?style=for-the-badge&logo=docker&logoColor=white)](https://docker.com)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)](https://kubernetes.io)

<br/>

> **Nexis** is a production-grade, real-time collaborative IDE — think Google Docs, but for code.
> Two developers can edit the same file simultaneously, see each other's cursors, run code in sandboxed containers, chat, and replay every keystroke of their session.

<br/>

<a href="https://youtu.be/I7lwOGdrpSM">
  <img src="https://img.shields.io/badge/🎬_Watch_Full_Demo-click_here-cbf14c?style=for-the-badge&labelColor=0d0f12" />
</a>

</div>

---

## ✨ What Makes Nexis Different

| Feature | What it does | How it works |
|--------|--------------|-------------|
| 🔄 **Real-time Editing** | Multiple users edit the same file without conflict | Operational Transform (OT) algorithm with Redisson distributed locks |
| 🖱️ **Live Cursors** | See every collaborator's cursor position | WebSocket STOMP + Redis pub/sub broadcasts positions at 50ms |
| ⚡ **Code Execution** | Run Python, Java, C++, JS, Dart instantly | Docker-in-Docker sandboxed containers with CPU/memory limits |
| 💬 **Collaborative Chat** | Chat panel inside the IDE | RabbitMQ consistent-hash sharding for ordered delivery |
| 🎬 **Session Recording** | Replay every keystroke of a session | NDJSON streaming from PostgreSQL ordered event store |
| 📁 **File Management** | Upload, version, and browse project files | 3-step MinIO presigned upload with version tracking |
| 🔐 **Auth System** | JWT + OAuth2 (Google/GitHub), RBAC | Spring Security with refresh token rotation |
| 🚀 **Production Ready** | Full K8s deployment with HPA | Minikube tested, cloud-ready manifests |

---

## 🎬 Demo

<div align="center">

[![Nexis Demo](https://img.youtube.com/vi/I7lwOGdrpSM/maxresdefault.jpg)](https://youtu.be/I7lwOGdrpSM)

*Click to watch — two users collaboratively coding in real time*

</div>

---

## 🏗️ System Architecture

```
╔══════════════════════════════════════════════════════════════════════════╗
║                          NEXIS ARCHITECTURE                              ║
╠══════════════════════════════════════════════════════════════════════════╣
║                                                                          ║
║   Browser A          Browser B          Browser C                        ║
║      │                  │                  │                             ║
║      └──────────────────┴──────────────────┘                             ║
║                          │                                               ║
║              ┌───────────▼───────────┐                                   ║
║              │     Nginx Ingress     │  ← SSL termination                ║
║              └───────────┬───────────┘                                   ║
║                          │                                               ║
║         ┌────────────────┼─────────────────┐                             ║
║         │                │                 │                             ║
║         ▼                ▼                 ▼                             ║
║   ┌──────────┐    ┌──────────┐    ┌──────────────┐                       ║
║   │  React   │    │   API    │    │  WebSocket   │                       ║
║   │ Frontend │    │ Gateway  │    │   Service    │                       ║
║   │  :3000   │    │  :8080   │    │   :8082      │                       ║
║   └──────────┘    └────┬─────┘    └──────┬───────┘                       ║
║                        │                 │                               ║
║           ┌────────────┼──────────┐      │ Redis pub/sub                 ║
║           │            │          │      │ (cross-instance broadcast)    ║
║           ▼            ▼          ▼      ▼                               ║
║    ┌──────────┐ ┌──────────┐ ┌──────────────┐                            ║
║    │  Auth    │ │ Storage  │ │  Execution   │                            ║
║    │ Service  │ │ Service  │ │  Service     │                            ║
║    │  :8081   │ │  :8084   │ │   :8083      │                            ║
║    └──────────┘ └──────────┘ └──────────────┘                            ║
║          │            │              │                                   ║
║          │            │         ┌───▼───────┐                            ║
║          │            │         │ Docker SDK│ ← spins up sandboxed       ║
║          │            │         │ containers│   execution containers     ║
║          │            │         └───────────┘                            ║
║          │            │                                                  ║
║    ┌─────▼────────────▼──────────────────────────────────────┐           ║
║    │                  INFRASTRUCTURE                           │           ║
║    │                                                           │           ║
║    │   PostgreSQL    Redis      RabbitMQ      MinIO            │           ║
║    │   (per svc)     :6379      :5672         :9000            │           ║
║    │   auth-db    pub/sub    consistent     S3-compat          │           ║
║    │   storage-db sessions   hash shards    file storage       │           ║
║    │   exec-db              CHAT_QUEUE       presigned         │           ║
║    │   record-db            CODE_SHARD_0/1/2  URLs            │           ║
║    └───────────────────────────────────────────────────────────┘           ║
║                                                                          ║
╚══════════════════════════════════════════════════════════════════════════╝
```

---

## 🔧 Tech Stack

<div align="center">

### Backend
| Layer | Technology | Purpose |
|-------|-----------|---------|
| Services | **Spring Boot 3** | All 6 microservices |
| Gateway | **Spring Cloud Gateway** | Routing, JWT validation, rate limiting, circuit breakers |
| Real-time | **Spring WebSocket + STOMP** | Bidirectional communication |
| Consistency | **Operational Transform** | Conflict-free concurrent editing |
| Distributed Locks | **Redisson** | Prevents race conditions in OT processing |
| Messaging | **RabbitMQ** | Async code events (consistent-hash), chat queue, execution results |
| Cache + Pub/Sub | **Redis 7** | Session state, cross-instance WebSocket broadcast |
| Databases | **PostgreSQL 15** | One database per service |
| Object Storage | **MinIO** | S3-compatible file storage |
| Service Discovery | **Eureka** | Service registration and discovery |
| Resilience | **Resilience4j** | Circuit breakers, retry |
| Auth | **Spring Security + JWT + OAuth2** | Authentication and authorization |

### Frontend
| Layer | Technology | Purpose |
|-------|-----------|---------|
| Framework | **React 18 + TypeScript** | SPA |
| Editor | **Monaco Editor** | VS Code-powered code editing |
| Real-time | **STOMP.js + SockJS** | WebSocket client |
| State | **Zustand** | Global state management |
| HTTP | **Axios** | API client with auto-refresh |
| Styling | **Inline CSS / CSS-in-JS** | Dark terminal aesthetic |

### DevOps
| Tool | Purpose |
|------|---------|
| **Docker** | Containerization (multi-stage builds) |
| **Docker Compose** | Local development |
| **Kubernetes** | Production orchestration (Minikube) |
| **HPA** | Auto-scaling for WebSocket + Execution pods |
| **Nginx Ingress** | Load balancing + SSL |

</div>

---

## ⚡ Real-Time Collaboration — How It Works

```
User A types "H"                    User B types "W" simultaneously
      │                                       │
      ▼                                       ▼
 Monaco onChange                        Monaco onChange
      │                                       │
      ▼                                       ▼
 Calculate OT op:                      Calculate OT op:
 INSERT pos=0 "H"                      INSERT pos=0 "W"
      │                                       │
      └──────────► WebSocket Service ◄────────┘
                        │
                   OTEngine.transform()
                   (with Redisson lock)
                        │
                   Assign versions:
                   opA → v:1
                   opB → v:2 (position adjusted)
                        │
                   Redis pub/sub
                        │
              ┌─────────┴──────────┐
              ▼                    ▼
         Browser A            Browser B
     receives opB(v2)     receives opA(v1)
     applies remotely     applies remotely
              │                    │
              ▼                    ▼
        Result: "HW"          Result: "HW"
         ✅ Consistent         ✅ Consistent
```

---

## 🐳 Code Execution Pipeline

```
User clicks "Run"
      │
      ▼
POST /api/execute/run
      │
      ▼
Execution Service creates Job
status: QUEUED → saved to PostgreSQL
      │
      ▼
Job published to nexis.execution.queue (RabbitMQ)
      │
      ▼
CodeExecutionWorker picks up job
status: PROCESSING
      │
      ▼
Docker SDK creates container:
  Image:   python:3.11-slim (or java/node/cpp/dart)
  CPU:     50% limit
  Memory:  128MB limit
  Timeout: 30 seconds
  Network: DISABLED (security)
      │
      ▼
Code runs inside sandbox
stdout + stderr captured
Container destroyed immediately
      │
      ▼
Result published to nexis.exchange (RabbitMQ)
      │
      ▼
WebSocket Service consumes result
Broadcasts to /topic/workspace/{id}/terminal
      │
      ▼
Terminal panel updates in ALL connected browsers
No polling. Pure WebSocket delivery.
```

---

## 📁 Project Structure

```
nexis/
├── 📦 api-gateway/          Spring Cloud Gateway — routing, auth, rate limiting
├── 📦 auth-service/         JWT, OAuth2, workspaces, members, RBAC
├── 📦 websocket-service/    OT engine, Redis pub/sub, RabbitMQ events
├── 📦 execution-service/    Docker-in-Docker, job queue, multi-language
├── 📦 storage-service/      MinIO integration, file versioning, Feign client
├── 📦 recording-service/    Session events, NDJSON streaming playback
├── 🎨 nexis-frontend/       React 18, Monaco Editor, STOMP WebSocket
├── ☸️  k8s/                  Kubernetes manifests
│   ├── deployments/         One deployment per service
│   ├── services/            ClusterIP + LoadBalancer
│   ├── configmaps/          Environment configuration
│   ├── secrets/             JWT secret, DB passwords
│   ├── hpa/                 HorizontalPodAutoscaler
│   └── ingress.yaml         Nginx ingress with SSL
├── 🐳 docker-compose.yaml   Full local stack
└── 📖 README.md
```

---

## 🚀 Getting Started

### Prerequisites

```bash
# Required
Java 17+
Docker & Docker Compose
Node.js 18+
npm 9+

# For Kubernetes
Minikube
kubectl
Helm
```

### Local Development (Docker Compose)

```bash
# 1. Clone the repository
git clone https://github.com/krishnaaarwal/nexis.git
cd nexis

# 2. Start infrastructure (PostgreSQL, Redis, RabbitMQ, MinIO)
docker-compose up -d

# 3. Start each service (in separate terminals or via IDE)
cd auth-service      && ./mvnw spring-boot:run
cd api-gateway       && ./mvnw spring-boot:run
cd websocket-service && ./mvnw spring-boot:run
cd execution-service && ./mvnw spring-boot:run
cd storage-service   && ./mvnw spring-boot:run
cd recording-service && ./mvnw spring-boot:run

# 4. Start frontend
cd nexis-frontend
npm install
npm run dev

# 5. Open http://localhost:5173
```

### Kubernetes (Minikube)

```bash
# 1. Start Minikube
minikube start --cpus=4 --memory=8192

# 2. Enable addons
minikube addons enable ingress
minikube addons enable metrics-server

# 3. Apply all manifests
kubectl apply -f k8s/

# 4. Check all pods are running
kubectl get pods -n nexis

# 5. Access the app
minikube tunnel
# Open http://nexis.local
```

### Environment Variables

| Variable | Service | Description |
|----------|---------|-------------|
| `JWT_SECRET` | All | Shared JWT signing key |
| `POSTGRES_PASSWORD` | All | Database password |
| `REDIS_PASSWORD` | Gateway, WebSocket, Recording | Redis auth |
| `RABBITMQ_PASSWORD` | WebSocket, Execution, Recording | RabbitMQ auth |
| `MINIO_ROOT_PASSWORD` | Storage | MinIO credentials |

---

## 🌐 API Overview

<details>
<summary><b>Auth Service (via :8080/api/auth)</b></summary>

```http
POST   /api/auth/signup              → Register user
POST   /api/auth/login               → Login, returns JWT
POST   /api/auth/refresh             → Refresh access token
POST   /api/auth/logout              → Invalidate tokens
GET    /api/auth/me                  → Get current user
POST   /api/auth/forgot-password     → Request OTP
POST   /api/auth/reset-password      → Reset with OTP
```
</details>

<details>
<summary><b>Workspace Service (via :8080/api/workspaces)</b></summary>

```http
GET    /api/workspaces               → List user's workspaces
POST   /api/workspaces               → Create workspace
GET    /api/workspaces/{id}          → Get workspace
PUT    /api/workspaces/{id}          → Update workspace
GET    /api/workspaces/{id}/members  → List members
POST   /api/workspaces/{id}/members  → Add member
DELETE /api/workspaces/{id}/members/{memberId}
```
</details>

<details>
<summary><b>Storage Service (via :8080/api/files)</b></summary>

```http
POST   /api/files/upload             → Step 1: get presigned URL
POST   /api/files/upload-complete    → Step 3: confirm upload
GET    /api/files/{id}/download      → Get download URL
GET    /api/files/workspace/{id}     → List workspace files
```
</details>

<details>
<summary><b>Execution Service (via :8080/api/execute)</b></summary>

```http
POST   /api/execute/run              → Submit code job
GET    /api/execute/status/{jobId}   → Check status
POST   /api/execute/kill/{jobId}     → Force kill container
```
</details>

<details>
<summary><b>Recording Service (via :8080/api/sessions)</b></summary>

```http
POST   /api/sessions/start           → Begin recording
POST   /api/sessions/{id}/end        → End recording
GET    /api/sessions/{id}            → Session metadata
GET    /api/sessions/{id}/events     → NDJSON event stream (playback)
```
</details>

<details>
<summary><b>WebSocket Topics (direct :8082)</b></summary>

```
CONNECT  → Authorization: Bearer {jwt}

SEND     /app/workspace/{id}/code     → Code operation
SEND     /app/workspace/{id}/cursor   → Cursor position
SEND     /app/workspace/{id}/chat     → Chat message
SEND     /app/workspace/{id}/typing   → Typing indicator

SUBSCRIBE /topic/workspace/{id}/code      → Receive code ops
SUBSCRIBE /topic/workspace/{id}/cursor    → Receive cursors
SUBSCRIBE /topic/workspace/{id}/chat      → Receive chat
SUBSCRIBE /topic/workspace/{id}/typing    → Receive typing
SUBSCRIBE /topic/workspace/{id}/presence  → Join/leave events
SUBSCRIBE /topic/workspace/{id}/terminal  → Execution results
```
</details>

---

## ☸️ Kubernetes Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Nexis K8s Cluster                             │
│                   Namespace: nexis                               │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    Ingress (Nginx)                           │ │
│  │              nexis.local → services                         │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  Deployments (2 replicas each, except where noted):             │
│                                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │
│  │  auth    │ │ gateway  │ │ storage  │ │   websocket      │  │
│  │  svc     │ │  svc     │ │  svc     │ │  svc (HPA 2-10)  │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘  │
│                                                                  │
│  ┌──────────┐ ┌──────────────────────────────────────────────┐  │
│  │recording │ │  execution-svc (HPA 3-20, scales on queue)   │  │
│  │  svc     │ └──────────────────────────────────────────────┘  │
│  └──────────┘                                                    │
│                                                                  │
│  StatefulSets:  PostgreSQL × 4 │ Redis │ RabbitMQ │ MinIO       │
│  ConfigMaps:    Per-service environment                          │
│  Secrets:       JWT secret, DB creds, MinIO creds               │
│  PVCs:          Persistent storage for all databases             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Auto-Scaling Rules

| Service | Min | Max | Trigger |
|---------|-----|-----|---------|
| WebSocket Service | 2 | 10 | CPU > 70% OR Memory > 80% |
| Execution Service | 3 | 20 | CPU > 60% (code runs are CPU-heavy) |
| Auth Service | 2 | 4 | CPU > 70% |

---

## 🔐 Security

- **JWT Authentication** — 15-minute access tokens, 7-day refresh tokens with rotation
- **API Gateway Validation** — Every request JWT-validated before reaching services
- **Sandboxed Execution** — Docker containers with no network access, read-only FS, resource limits
- **Internal Service Communication** — Feign clients bypass gateway, direct service-to-service
- **Secrets Management** — All credentials in Kubernetes Secrets (base64), never in code
- **CORS** — Configured per service, whitelisted origins only

---

## 🏆 Engineering Highlights

**1. Operational Transform with Distributed Locking**
The OT engine uses Redisson distributed locks to prevent race conditions when multiple users send operations simultaneously. Lock timeout is 5 seconds with 500ms wait — preventing deadlocks under high concurrency.

**2. Consistent-Hash RabbitMQ Sharding**
Code operation events are sharded across 3 queues using RabbitMQ's `x-consistent-hash` exchange with `workspaceId` as the routing key. This guarantees ordered delivery of operations per workspace without a single queue bottleneck.

**3. Non-Blocking Execution Flow**
Code execution is 100% asynchronous. The frontend submits a job and immediately returns. Results arrive via WebSocket `/topic/workspace/{id}/terminal`. Zero polling.

**4. NDJSON Streaming Playback**
Session replay streams events as Newline-Delimited JSON from PostgreSQL using Spring MVC's `StreamingResponseBody` with cursor-based pagination. No memory explosion for long sessions.

**5. 3-Step Presigned Upload**
Files upload directly to MinIO (bypassing the backend for the actual bytes) using presigned URLs. Backend only handles metadata. This removes backend as a bottleneck for large file uploads.

---

## 👨‍💻 Author

<div align="center">

**Krishna Agarwal**

3rd Year B.Tech CSE @ AKGEC, Ghaziabad
Backend Developer @ GDG AKGEC

[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/krishnaaarwal)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://linkedin.com/in/krishnaaarwal)

</div>

---

## 📄 License

This project is licensed under the MIT License.

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:cbf14c,50:1a73e8,100:0d0f12&height=120&section=footer&animation=fadeIn" width="100%" />

**Built with 5 months of determination, debugging, and distributed systems.**

*If this project helped you, consider giving it a ⭐*

</div>