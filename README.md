# 🍕 What Should We Eat? — Group Food Decider

> *"The app that ends the eternal debate: 'Where should we eat?'"*

---

## How It Works

1. **Create a room** → share the code with friends
2. **Everyone joins** and sets dietary preferences
3. **Swipe right** on restaurants you'd eat at, left to skip
4. **When everyone swipes right** on the same place → MATCH! 🎉
5. **Navigate there** via Google Maps

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Spring Boot 3.2, Java 17, Spring WebSocket (STOMP), Spring Data JPA, H2 Database |
| **Frontend** | React 18, React Router, @stomp/stompjs, react-tinder-card, Framer Motion |
| **External API** | Google Places API (Nearby Search + Place Photos) |

---

## Prerequisites

- Java 17+
- Node.js 18+
- npm
- Google Places API key (from [Google Cloud Console](https://console.cloud.google.com))

---

## Getting Started

### Step 1 — Clone / navigate to project

```bash
cd what-should-we-eat
```

### Step 2 — Set your Google Places API key

```bash
export GOOGLE_PLACES_API_KEY=your-api-key-here
```

### Step 3 — Start the backend

```bash
./mvnw spring-boot:run
```

- Backend runs on **http://localhost:8080**
- H2 Console: **http://localhost:8080/h2-console** (JDBC URL: `jdbc:h2:mem:fooddb`)

### Step 4 — Start the frontend

```bash
cd frontend
npm install
npm start
```

- Frontend runs on **http://localhost:3000** (proxied to backend)

---

## Google Places API Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project (or select existing)
3. Enable **"Places API"**
4. Create an API key under **Credentials**
5. *(Optional)* Restrict the key to Places API only
6. Set the key as an environment variable:
   ```bash
   export GOOGLE_PLACES_API_KEY=your-key
   ```

> 💡 **Cost note:** Google gives **$200/month free credit**. This project uses ~$0.05 per room creation (1 Nearby Search + photo requests).

---

## Project Structure

```
what-should-we-eat/
├── pom.xml
├── src/main/java/com/fooddecider/app/
│   ├── WhatShouldWeEatApplication.java
│   ├── config/
│   │   ├── WebSocketConfig.java
│   │   ├── CorsConfig.java
│   │   └── GlobalExceptionHandler.java
│   ├── controller/
│   │   └── RoomController.java
│   ├── websocket/
│   │   └── SwipeController.java
│   ├── service/
│   │   ├── RoomService.java
│   │   ├── PlaceService.java
│   │   └── MatchEngine.java
│   ├── repository/
│   │   ├── RoomRepository.java
│   │   ├── RoomMemberRepository.java
│   │   ├── RoomRestaurantRepository.java
│   │   └── SwipeRepository.java
│   ├── entity/
│   │   ├── Room.java
│   │   ├── RoomMember.java
│   │   ├── RoomRestaurant.java
│   │   └── Swipe.java
│   ├── enums/
│   │   ├── RoomStatus.java
│   │   ├── SwipeDirection.java
│   │   └── PriceLevel.java
│   └── dto/
│       ├── CreateRoomRequest.java
│       ├── JoinRoomRequest.java
│       ├── SwipeRequest.java
│       ├── RoomResponse.java
│       ├── RestaurantCard.java
│       ├── MatchResult.java
│       └── WebSocketMessage.java
├── src/main/resources/
│   └── application.yml
└── frontend/
    ├── package.json
    ├── public/index.html
    └── src/
        ├── App.js
        ├── App.css
        ├── index.js
        ├── services/
        │   ├── api.js
        │   └── websocket.js
        ├── pages/
        │   ├── HomePage.js
        │   ├── LobbyPage.js
        │   ├── SwipePage.js
        │   └── MatchPage.js
        └── components/
            └── RestaurantCard.js
```

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/rooms` | Create a new room |
| `GET` | `/api/rooms/{code}` | Get room details |
| `POST` | `/api/rooms/{code}/join` | Join a room |
| `POST` | `/api/rooms/{code}/start?memberId=` | Start swiping (host only) |
| `POST` | `/api/rooms/{code}/compromise` | Get best compromise |
| `DELETE` | `/api/rooms/{code}/members/{id}` | Leave room |

---

## WebSocket Events

| Event | Channel | Description |
|-------|---------|-------------|
| `MEMBER_JOINED` | `/topic/room/{code}` | New member joined |
| `SWIPING_STARTED` | `/topic/room/{code}` | Host started the swiping phase |
| `SWIPE_PROGRESS` | `/topic/room/{code}` | Shows how many have swiped |
| `MATCH_FOUND` | `/topic/room/{code}` | Unanimous match |

---

## Stretch Goals / Ideas for V2

- [ ] User authentication (OAuth2 via Google)
- [ ] Persistent history ("we ate here last week")
- [ ] **"Surprise Me" AI mode** — skip swiping, LLM picks based on preferences
- [ ] Push notifications when it's your turn to swipe
- [ ] Photo battle mode — AI-generated restaurant descriptions
- [ ] Deploy to BTP Cloud Foundry with PostgreSQL

---

## License

[MIT](LICENSE)
