<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0d0f12,50:1e3a5f,100:cbf14c&height=220&section=header&text=NEXIS&fontSize=90&fontColor=cbf14c&fontAlignY=40&desc=Real-Time%20Collaborative%20IDE&descSize=22&descAlignY=62&descColor=94a3b8&animation=fadeIn" width="100%"/>

<br/>

<p>
  <a href="https://youtu.be/hZI_yioSA3c">
    <img src="https://img.shields.io/badge/▶%20Live%20Collab%20Demo-FF0000?style=for-the-badge&logo=youtube&logoColor=white" alt="Live Collab Demo"/>
  </a>
  &nbsp;
  <a href="https://youtu.be/I7lwOGdrpSM">
    <img src="https://img.shields.io/badge/▶%20Feature%20Walkthrough-c4302b?style=for-the-badge&logo=youtube&logoColor=white" alt="Feature Walkthrough"/>
  </a>
</p>

<p>
  <img src="https://img.shields.io/badge/Java_17-ED8B00?style=flat-square&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=flat-square&logo=spring&logoColor=white"/>
  <img src="https://img.shields.io/badge/React_18-61DAFB?style=flat-square&logo=react&logoColor=black"/>
  <img src="https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=white"/>
  <img src="https://img.shields.io/badge/WebSocket-010101?style=flat-square&logo=socket.io&logoColor=white"/>
  <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white"/>
  <img src="https://img.shields.io/badge/RabbitMQ-FF6600?style=flat-square&logo=rabbitmq&logoColor=white"/>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white"/>
  <img src="https://img.shields.io/badge/Kubernetes-326CE5?style=flat-square&logo=kubernetes&logoColor=white"/>
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white"/>
  <img src="https://img.shields.io/badge/MinIO-C72E49?style=flat-square&logo=minio&logoColor=white"/>
</p>

<br/>

<p align="center">
  <b>Nexis is a production-oriented, real-time collaborative development platform —</b><br/>
  think Google Docs, but for code.<br/><br/>
  Multiple developers edit the same file simultaneously, see each other's live cursors,<br/>
  execute code in isolated Docker containers, chat, and replay every keystroke of their session.<br/><br/>
  Built with <b>6 Spring Boot microservices</b>, deployed on <b>Kubernetes</b>.
</p>

</div>

---

## 📋 Table of Contents

- [Demo](#-demo)
- [Features](#-feature-overview)
- [Architecture](#️-architecture)
- [Service Breakdown](#-service-breakdown)
- [How Real-Time Editing Works](#-how-real-time-editing-works)
- [Code Execution](#-code-execution--zero-polling-architecture)
- [Repository Structure](#-repository-structure)
- [Getting Started](#-getting-started)
- [API Reference](#-api-reference)
- [Kubernetes](#️-kubernetes)
- [Security](#-security)
- [Key Engineering Decisions](#-key-engineering-decisions)

---

## 🎬 Demo

<div align="center">
<table>
<tr>
<td align="center" width="50%">

### Real-Time Collaboration
[![Nexis Collab](https://img.youtube.com/vi/hZI_yioSA3c/mqdefault.jpg)](https://youtu.be/hZI_yioSA3c)
*Two users. Same file. Live cursors. Zero conflict.*

</td>
<td align="center" width="50%">

### Feature Walkthrough
[![Nexis Features](https://img.youtube.com/vi/I7lwOGdrpSM/mqdefault.jpg)](https://youtu.be/I7lwOGdrpSM)
*Login → Workspace → Code Execution*

</td>
</tr>
</table>
</div>

---

## ✨ Feature Overview

| Feature | Description |
|---------|-------------|
| 🔄 **Real-Time Collaborative Editing** | Conflict-free concurrent editing via Operational Transform with Redisson distributed locks |
| 🖱️ **Live Cursor Tracking** | Every collaborator's cursor position broadcast in near real-time via Redis pub/sub |
| ⚡ **Sandboxed Code Execution** | Isolated Docker container per job — CPU, memory, network, and timeout constrained |
| 💬 **In-IDE Chat** | Real-time messaging via RabbitMQ consistent-hash sharding for ordered delivery |
| 🎬 **Session Recording & Playback** | Every keystroke stored as events; replayed as a streaming NDJSON response |
| 📁 **File Management & Versioning** | 3-step MinIO presigned upload with version tracking per file |
| 🔐 **Auth (JWT + OAuth2 + RBAC)** | Spring Security with refresh token rotation and workspace-level roles |
| ☸️ **Kubernetes Deployment** | Full K8s manifests — CoreDNS service discovery, persistent volumes, Nginx ingress |

---

## 🏗️ Architecture

```
                        ┌─────────────────────────────────┐
                        │         Nginx Ingress            │
                        │    (SSL + path-based routing)    │
                        └──────────────┬──────────────────┘
                                       │
              ┌────────────────────────┼────────────────────────┐
              │                        │                        │
              ▼                        ▼                        ▼
      ┌───────────────┐      ┌──────────────────┐     ┌────────────────┐
      │    Frontend   │      │   API Gateway    │     │   WebSocket    │
      │  React + TS   │      │  :8080           │     │   Service      │
      │  Monaco Editor│      │  Spring Cloud GW │     │   :8082        │
      └───────────────┘      └────────┬─────────┘     └───────┬────────┘
                                      │                        │
                   ┌──────────────────┼──────────────┐         │
                   │                  │              │         │
                   ▼                  ▼              ▼         │
           ┌────────────┐   ┌──────────────┐  ┌──────────┐    │
           │    Auth    │   │   Storage    │  │Execution │    │
           │  Service   │   │   Service    │  │ Service  │    │
           │   :8081    │   │    :8084     │  │  :8083   │    │
           └────────────┘   └──────────────┘  └────┬─────┘    │
                                    │               │         │
                                    ▼               ▼         │
                               ┌────────┐     ┌──────────┐    │
                               │ MinIO  │     │  Docker  │    │
                               │  S3    │     │  Sandbox │    │
                               └────────┘     └──────────┘    │
                                                               │
                                              ┌───────────────┘
                                              │  Redis pub/sub
                                              │  (cross-instance broadcast)
                                              ▼
                                       ┌────────────┐
                                       │  Recording │
                                       │  Service   │
                                       │   :8085    │
                                       └────────────┘

  ━━━━━━━━━━━━━━━━━━━━━━━━━ SHARED INFRASTRUCTURE ━━━━━━━━━━━━━━━━━━━━━━━━━

  PostgreSQL × 4       Redis 7            RabbitMQ 3           MinIO
  ─────────────        ──────────         ──────────────        ─────────────
  auth-db   :5433      Sessions           nexis.code.*          S3-compatible
  exec-db   :5434      OT history         (shard 0/1/2)         Presigned URLs
  storage-db:5435      Pub/Sub            nexis.chat.queue      File versioning
  record-db :5436      Redisson locks     nexis.exec.queue
                                          nexis.result.queue

  ──────────────── Service Discovery: Kubernetes CoreDNS ─────────────────────
```

---

## 🔧 Service Breakdown

| Service | Port | Key Responsibilities |
|---------|------|---------------------|
| **api-gateway** | 8080 | Route all HTTP traffic, validate JWT on every request, rate-limit via Redis, circuit-break with Resilience4j |
| **auth-service** | 8081 | Signup/Login/OAuth2, JWT issuance & rotation, workspace CRUD, member management, RBAC |
| **websocket-service** | 8082 | OT engine with Redisson locking, Redis pub/sub for cross-instance broadcast, RabbitMQ code/chat publishing |
| **execution-service** | 8083 | RabbitMQ consumer, Docker SDK container lifecycle, multi-language execution, result routing |
| **storage-service** | 8084 | MinIO presigned upload (3-step), file versioning, workspace file listing, Feign-based auth checks |
| **recording-service** | 8085 | RabbitMQ consumer for code + chat events, PostgreSQL event store, NDJSON streaming playback |

---

## 🧠 How Real-Time Editing Works

Two users type simultaneously. No conflicts. Here's exactly how:

```
  User A types "H"                        User B types "W"
  at position 0                           at position 0
        │                                       │
        ▼                                       ▼
   CodeOperation                          CodeOperation
   INSERT pos=0 "H"                       INSERT pos=0 "W"
        │                                       │
        └─────────────┐       ┌─────────────────┘
                      ▼       ▼
                 WebSocket Service
                      │
               Redisson.tryLock()          ← distributed lock per workspace
               OTEngine.transform()        ← adjust conflicting positions
               version++ → Redis           ← server assigns canonical version
               Redisson.unlock()
                      │
               Redis pub/sub publish       ← fan-out to ALL WS instances
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
     Browser A               Browser B
  receives op(W, v2)       receives op(H, v1)
  skips own echo           skips own echo
  applies remote op        applies remote op
          │                       │
          ▼                       ▼
       "HW" ✅                 "HW" ✅
     Consistent              Consistent
```

The Redisson lock (500ms wait, 5s lease) guarantees operations are serialized per workspace without blocking across workspaces. The OT transform adjusts positions so both users converge to identical document state.

---

## ⚡ Code Execution — Zero-Polling Architecture

```
  User clicks ▶ Run
        │
        ▼
  POST /api/execute/run   ──►   Job saved (status: QUEUED)
        │                       Published to nexis.execution.queue
        │
        │  ← HTTP returns immediately with jobId
        │    Frontend shows "running…" in terminal
        │    Result arrives via WebSocket — no polling
        │
        │  (background)
        ▼
  CodeExecutionWorker (RabbitMQ consumer)
        │
        ▼
  Docker SDK creates container:
  ┌─────────────────────────────────────────────────────┐
  │  Image:    python:3.11-slim | openjdk:17-alpine      │
  │            node:18-alpine  | gcc:latest | dart:stable│
  │  CPU:      50% quota                                 │
  │  Memory:   128 MB hard limit                         │
  │  Network:  NONE (fully isolated)                     │
  │  FS:       Read-only except /tmp                     │
  │  Timeout:  30 second watchdog → SIGKILL              │
  └─────────────────────────────────────────────────────┘
        │
        ▼
  stdout + stderr captured → container destroyed
        │
        ▼
  Publish ExecutionResult → nexis.exchange (RabbitMQ)
        │
        ▼
  WebSocket Service consumes → broadcasts to:
  /topic/workspace/{id}/terminal
        │
        ▼
  Terminal panel updates in ALL connected browsers ✅
```

---

## 📦 Repository Structure

```
nexis/
├── api-gateway/              Spring Cloud Gateway
│   └── src/                  JWT filter, rate limiting, circuit breakers, fallback controller
│
├── auth-service/             Spring Boot + Spring Security
│   └── src/                  JWT, OAuth2, workspaces, workspace members, RBAC
│
├── websocket-service/        Spring WebSocket + STOMP
│   └── src/                  OT engine, Redisson locks, Redis pub/sub, RabbitMQ publisher
│
├── execution-service/        Spring Boot + Docker SDK
│   └── src/                  Job queue consumer, container lifecycle, result routing
│
├── storage-service/          Spring Boot + MinIO
│   └── src/                  Presigned upload (3-step), versioning, Feign auth client
│
├── recording-service/        Spring Boot
│   └── src/                  Event consumer, PostgreSQL event store, NDJSON streaming
│
├── nexis-frontend/           React 18 + TypeScript + Vite
│   └── src/
│       ├── pages/            LoginPage, DashboardPage, WorkspacePage
│       ├── components/       CodeEditor (Monaco), Terminal, FileTree,
│       │                     ChatPanel, UserList, Toolbar
│       ├── services/         api.ts (axios + auto-refresh), websocket.ts (STOMP)
│       └── store/            useStore.ts (Zustand)
│
├── k8s/
│   ├── api-gateway.yaml
│   ├── auth-service.yaml
│   ├── execution-service.yaml
│   ├── frontend.yaml
│   ├── ingress.yaml
│   ├── minio.yaml
│   ├── nexis-config-secrets.yaml    ← secrets
│   ├── postgres.yaml
│   ├── rabbitmq.yaml
│   ├── recording-service.yaml
│   ├── redis.yaml
│   ├── storage-service.yaml
│   └── websocket-service.yaml
│
└── docker-compose.yaml       Full local stack
```

---

## 🚀 Getting Started

### Prerequisites

```
Java 17+          Docker & Docker Compose
Node.js 18+       npm 9+
# For K8s:
Minikube + kubectl
```

### Run with Docker Compose

```bash
# 1. Clone
git clone https://github.com/krishnaaarwal/nexis.git && cd nexis

# 2. Start infrastructure
docker-compose up -d

# 3. Start services (each in its own terminal)
cd auth-service      && ./mvnw spring-boot:run
cd api-gateway       && ./mvnw spring-boot:run
cd websocket-service && ./mvnw spring-boot:run
cd execution-service && ./mvnw spring-boot:run
cd storage-service   && ./mvnw spring-boot:run
cd recording-service && ./mvnw spring-boot:run

# 4. Start frontend
cd nexis-frontend && npm install && npm run dev

# Open http://localhost:5173
```

### Run on Kubernetes (Minikube)

```bash
minikube start --cpus=4 --memory=8192
minikube addons enable ingress

kubectl apply -f k8s/
kubectl get pods -n nexis -w     # wait for all Running

minikube tunnel                   # exposes LoadBalancer IPs
# Open http://nexis.local
```

### Infrastructure at a Glance

| Component | Port | Notes |
|-----------|------|-------|
| API Gateway | 8080 | All HTTP goes here |
| WebSocket Service | 8082 | Direct WS connection (not via gateway) |
| PostgreSQL (auth) | 5433 | |
| PostgreSQL (exec) | 5434 | |
| PostgreSQL (storage) | 5435 | |
| PostgreSQL (recording) | 5436 | |
| Redis | 6379 | Password required |
| RabbitMQ | 5672 | Management UI: 15672 |
| MinIO | 9000 | Console: 9001 |

---

## 🌐 API Reference

<details>
<summary><b>🔐 Auth  ·  /api/auth</b></summary>

```http
POST   /api/auth/signup              { email, password, fullname }
POST   /api/auth/login               { email, password } → { accessToken, refreshToken }
POST   /api/auth/refresh             { token: refreshToken } → { accessToken, refreshToken }
POST   /api/auth/logout              { refreshToken } + Authorization header
GET    /api/auth/me                  → { id, fullname, email, avatar }
POST   /api/auth/forgot-password     { email } → sends OTP
POST   /api/auth/reset-password      { email, otp, newPassword }
```
</details>

<details>
<summary><b>🏢 Workspaces  ·  /api/workspaces</b></summary>

```http
GET    /api/workspaces                           List user's workspaces
POST   /api/workspaces                           Create workspace
GET    /api/workspaces/{id}                      Get workspace by ID
PUT    /api/workspaces/{id}                      Update workspace
GET    /api/workspaces/{id}/members              List members with roles
POST   /api/workspaces/{id}/members?memberId={}  Add member
DELETE /api/workspaces/{id}/members/{userId}     Remove member
PUT    /api/workspaces/{id}/transfer-ownership   Transfer to new owner
```
</details>

<details>
<summary><b>📁 Files  ·  /api/files</b></summary>

```http
# 3-step presigned upload:
POST   /api/files/upload               { workspaceId, fileName, size }
                                       → { fileId, url }  ← presigned MinIO URL
# (client PUTs file bytes directly to MinIO URL — backend never touches the bytes)
POST   /api/files/upload-complete      { workspaceId, fileId, versionNum, fileName, sizeBytes }

GET    /api/files/{id}/download        → { url }  ← presigned download URL
GET    /api/files/workspace/{id}       List all files with metadata
```
</details>

<details>
<summary><b>⚡ Execution  ·  /api/execute</b></summary>

```http
POST   /api/execute/run               { userId, workspaceId, codeLanguage, code }
                                      → { id (jobId), status: "QUEUED" }
                                      # Result delivered via WebSocket to /topic/workspace/{id}/terminal
POST   /api/execute/kill/{jobId}      Force-kill running container
```
</details>

<details>
<summary><b>🎬 Recording  ·  /api/sessions</b></summary>

```http
POST   /api/sessions/start            { workspaceId, participants: [UUID] }
POST   /api/sessions/{id}/end
GET    /api/sessions/{id}             Session metadata
GET    /api/sessions/{id}/events      NDJSON stream → replay every event in order
```
</details>

<details>
<summary><b>🔌 WebSocket  ·  ws://host:8082/ws (SockJS + STOMP)</b></summary>

```
# Connect with JWT in STOMP CONNECT header:
Authorization: Bearer {accessToken}

# Send (client → server):
SEND /app/workspace/{id}/code        CodeOperation  { version, userId, operationType, position, code, length }
SEND /app/workspace/{id}/cursor      CursorPayload  { userId, line, characterIndex }
SEND /app/workspace/{id}/chat        ChatMessage    { userId, workspaceId, time, message }
SEND /app/workspace/{id}/typing      TypingPayload  { userId, isTyping }

# Subscribe (server → client):
/topic/workspace/{id}/code           Remote code operations (already OT-transformed)
/topic/workspace/{id}/cursor         Remote cursor positions
/topic/workspace/{id}/chat           Chat messages
/topic/workspace/{id}/typing         Typing indicators
/topic/workspace/{id}/presence       JOINED / LEFT events (auto-fired by backend)
/topic/workspace/{id}/terminal       Execution results (async, pushed after container exits)
```
</details>

---

## ☸️ Kubernetes

**Service discovery uses Kubernetes CoreDNS.** Every service resolves others by name within the cluster — no external registry required.

```bash
# CoreDNS resolution example
# From storage-service pod:
curl http://auth-service:8081/api/auth/internal/...
# Resolves to auth-service ClusterIP via K8s DNS automatically
```

### K8s Manifests Summary

| Resource | Count | Notes |
|----------|-------|-------|
| Deployments | 7 | 6 services + frontend (2 replicas each) |
| Services | 7 | ClusterIP for internal, LoadBalancer for gateway + frontend |
| ConfigMaps | 1 | DB URLs, Redis host, RabbitMQ host, MinIO endpoint |
| Secrets | 1 | JWT secret, DB passwords, MinIO credentials |
| PersistentVolumeClaims | 4 | One per PostgreSQL instance |
| Ingress | 1 | Nginx — routes by path prefix |

---

## 🔐 Security

| Concern | Approach |
|---------|----------|
| **API authentication** | JWT validated at Gateway — services never receive unauthenticated requests |
| **Code execution** | Docker: no network, read-only FS, 128MB RAM, 50% CPU cap, 30s kill |
| **Credentials** | Kubernetes Secrets, never in source code |
| **Internal service calls** | Direct CoreDNS (bypasses Gateway) — `/api/auth/internal/**` is `permitAll()` |
| **CORS** | Explicit origin whitelist per service |
| **Refresh tokens** | Single-use rotation — each refresh invalidates the old token |

---

## 💡 Key Engineering Decisions

**1. Consistent-Hash RabbitMQ Sharding**

Code operations route to one of 3 queues (`nexis.code.queue.shard0/1/2`) via RabbitMQ's `x-consistent-hash` exchange, keyed by `workspaceId`. Operations for the same workspace always land on the same shard — guaranteeing ordered delivery without a single-queue bottleneck.

**2. Redisson Distributed OT Locks**

Before transforming any incoming operation, the OT engine acquires a per-workspace Redisson lock (`tryLock(500ms wait, 5s lease)`). This serializes concurrent edits at the server level, ensuring two operations arriving within the same millisecond are transformed correctly rather than corrupting the document.

**3. Zero-Polling Execution**

`POST /api/execute/run` returns immediately with a `jobId`. When the container finishes, the result travels: Execution Service → RabbitMQ → WebSocket Service → `/topic/workspace/{id}/terminal` → every browser in the workspace. No HTTP polling, no client-side timeouts.

**4. NDJSON Streaming Playback**

Session replay uses Spring MVC's `StreamingResponseBody` to stream session events directly from a PostgreSQL cursor (fetch size = 100). The entire session event history never loads into memory — it flows row by row to the client as Newline-Delimited JSON.

**5. 3-Step MinIO Presigned Upload**

The backend never touches actual file bytes. Step 1 generates a presigned PUT URL. Step 2 is client → MinIO directly. Step 3 notifies the backend to persist metadata. This removes the backend as a bottleneck for large file uploads entirely.

**6. Native Kubernetes Service Discovery**

Services reference each other by name (`http://auth-service:8081`) via Kubernetes CoreDNS. No separate service registry to deploy, configure, or scale — the cluster handles it natively.

---

## 👨‍💻 Built By

<div align="center">

**Krishna Agarwal**

3rd Year B.Tech CSE · AKGEC Ghaziabad
Backend Developer · GDG on Campus AKGEC

[![GitHub](https://img.shields.io/badge/GitHub-Krishna_Agarwal-181717?style=for-the-badge&logo=github)](https://github.com/krishnaaarwal)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Krishna_Agarwal-0A66C2?style=for-the-badge&logo=linkedin)](https://www.linkedin.com/in/krishna-agarwal-9b2a06367/)

</div>

---

## 📄 License

MIT — use freely, credit appreciated.

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:cbf14c,50:1e3a5f,100:0d0f12&height=140&section=footer&text=Built%20in%205%20months.%206%20services.%201%20working%20IDE.&fontSize=16&fontColor=94a3b8&animation=fadeIn" width="100%"/>

*If this impressed you, consider giving it a* ⭐

</div>