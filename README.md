# FareX

FareX is an automated jeepney fare and transit management platform featuring a backend HTTP API server, CSV-backed data store, and cross-platform desktop/mobile client applications.

---

## Running with Docker (Backend Server)

The FareX backend server can be run inside a lightweight, containerized environment with persistence for all routes, users, cards, and transaction data.

### Prerequisites
- [Docker](https://docs.docker.com/get-docker/) & Docker Compose

### Quick Start with Docker Compose

1. **Start the server:**
   ```bash
   docker-compose up -d
   ```
   *This builds the multi-stage image, mounts the `./data` folder to preserve transactions and cards, and starts the server on port `8080`.*

2. **Check server status and logs:**
   ```bash
   docker-compose ps
   docker-compose logs -f
   ```

3. **Access the Admin Web Dashboard:**
   Open [http://localhost:8080/admin](http://localhost:8080/admin) in your browser.
   - **Default Admin Account:** `admin@mail.com`
   - **Default Password:** `admin123`

4. **Stop the server:**
   ```bash
   docker-compose down
   ```

---

### Running with Plain Docker CLI

You can also build and run the image directly without Docker Compose:

1. **Build the image:**
   ```bash
   docker build -t farex-server .
   ```

2. **Run the container with persistent storage:**
   ```bash
   docker run -d \
     --name farex-server \
     -p 8080:8080 \
     -v "$(pwd)/data:/app/data" \
     --env-file .env \
     farex-server
   ```

---

## Configuration & Environment Variables

The server loads configuration from environment variables (or `.env` in the root):

| Variable | Default | Description |
|---|---|---|
| `PORT` | `8080` | Port for the HTTP server |
| `DATA_DIR` | `/app/data` (Docker) / `data` (Local) | Directory containing persistent CSV stores |
| `FAKE_PAY` | `1` | `1` to bypass PayMongo API for local testing, `0` for production |
| `ADMIN_PASSWORD` | `admin123` | Default administrator account password |
| `CARD_KEY` | `farex-dev-card-key` | Cryptographic secret key used to seal/sign card balances |
| `PAYMONGO_SECRET` | *(optional)* | Secret key for PayMongo checkout integration |

---

## Connecting the Client App

Once the Docker container is up and running on `localhost:8080`, start the client application:
```cmd
cd farex-app
mvn javafx:run -Djavafx.mainClass=com.farex.Main
```
The app will connect to `http://localhost:8080` by default.
