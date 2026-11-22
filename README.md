# FareX

FareX is an automated jeepney fare and transit management platform featuring a backend HTTP API server, CSV-backed data store, and cross-platform desktop/mobile client applications.

---

## Running with Docker (Backend Server)

The FareX backend server can be run inside a lightweight, containerized environment with persistence for all routes, users, cards, and transaction data.

### Prerequisites
- [Docker](https://docs.docker.com/get-docker/) & Docker Compose

### Quick Start with Docker Compose

1. **Create your settings:** copy `.env.example` to `.env`, then set `ADMIN_PASSWORD` (8 or more characters) and `CARD_KEY` (long random text).
   Docker Compose refuses to start without them. `.env` is never committed.

2. **Start the server:**
   ```bash
   docker-compose up -d
   ```
   *This builds the multi-stage image, mounts the `./data` folder to preserve transactions and cards, and starts the server on port `8080`.*

3. **Check server status and logs:**
   ```bash
   docker-compose ps
   docker-compose logs -f
   ```

4. **Access the Admin Web Dashboard:**
   Open [http://localhost:8080/admin](http://localhost:8080/admin) in your browser.
   - **Admin account:** `admin@mail.com`
   - **Password:** the `ADMIN_PASSWORD` from your `.env`
   - New operators cannot log in until the admin presses Reinstate on their account.

5. **Stop the server:**
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
| `DEV` | `0` | `1` seeds demo accounts (ana, ben) and allows `FAKE_PAY`. Never use in production |
| `FAKE_PAY` | `0` | `1` skips PayMongo and credits loads for free. The server refuses to start with it unless `DEV=1` |
| `ADMIN_PASSWORD` | *(required)* | Admin account password, 8 or more characters. Applied on every start |
| `CARD_KEY` | *(required)* | Secret used to seal card balances |
| `PAYMONGO_SECRET` | *(required unless `FAKE_PAY`)* | Secret key for PayMongo checkout integration |

The server refuses to start in production mode while a demo password (`admin123` or `1234`) is still in `users.csv`.
Run `./security-check.sh` after building to replay the launch review exploits. Expect all PASS.

---

## Connecting the Client App

Once the server is running on `localhost:8080`, start the client application:
- **From repository root:**
  ```cmd
  .\run-app.bat
  ```
- **From VS Code / IDE:**
  Open `farex-app/src/main/java/com/farex/ui/UiLauncher.java` and click **▶ Run**.
- **From `farex-app/` directory:**
  ```cmd
  cd farex-app
  mvn javafx:run
  ```
The app will connect to `http://localhost:8080` by default.
