# SmartQueue — Web Portal

A high-performance, modern web application serving as the omnichannel interface for the **Smart Queue Token System**. Built with React, Vite, and Vanilla CSS tokens, it provides dedicated portals for citizens, counter operators, office administrators, and telco SMS testing.

---

## 🚀 Portals & Interfaces

### 1. 👤 Citizen Virtual Queue Portal
- **Remote Token Issuance**: Citizens select their service category, input their National Identity Card (NIC) number, and provide a phone number for turn alerts.
- **Dual NIC Validation**: Real-time client validation supporting both:
  - **Legacy 9-digit format** with suffix (`145896235V` / `v` / `x` / `X`)
  - **Modern 12-digit format** (`144756235896`)
- **Real-Time Position Tracker**: Displays live queue position countdown, estimated wait time in minutes, and provisional counter routing.
- **Server-Sent Events (SSE)**: Auto-connects to `/api/v1/tokens/{id}/stream` with native reconnection and audio chime notifications when called.
- **Self-Service Cancellation**: Citizens can cancel active tokens directly, atomically freeing up slot capacity for others.

### 2. 🖥️ Desktop Counter Operator Station
- **Workstation Ergonomics**: High-density desktop layout tailored for standard 1080p and 720p monitors across government offices and OPD clinics.
- **Rapid Non-Mouse Hotkeys**:
  - <kbd>Space</kbd> : **Call Next Token** in eligible queue
  - <kbd>S</kbd> or <kbd>Enter</kbd> : **Start Serving** citizen
  - <kbd>C</kbd> : **Complete Consultation** & record duration
  - <kbd>K</kbd> : **Mark Skip** (temporary deferral)
  - <kbd>R</kbd> : **Recall** previously skipped citizen
  - <kbd>X</kbd> : **Mark No-Show** on absence
- **Web Audio Chime**: Synthesizes a two-tone alert chime (D5 ➔ A5) upon calling tokens without external audio assets.
- **Live Queue Sidebar**: Displays upcoming citizens in line with priority and channel indicators.

### 3. 📊 Admin & Overbooking Console
- **Session Telemetry**: Today's total issued count, active waiting queue, and online counter occupancy.
- **Probabilistic Overbooking Bounds**: View raw capacity vs. computed overbooking limits under the configured risk threshold (e.g. 10% risk).
- **Manual Overrides**: 1-click administrator ceiling overrides for peak or emergency sessions.

### 4. 📱 Telco SMS Gateway Sandbox
- **Zero-Cost Simulator**: Simulates telco inbound SMS commands without requiring active telco credit or subscriptions.
- **Command Grammar**:
  - `STATUS <TokenNumber>` (e.g. `STATUS NI-001`) ➔ Real-time position & ETA reply
  - `CANCEL <TokenNumber>` (e.g. `CANCEL NI-001`) ➔ Confirms release & updates database
  - `HELP` ➔ Command syntax assistance

---

## 🛠️ Tech Stack & Design System

- **Framework**: React 19 + Vite
- **Styling**: Vanilla CSS Design System with dark radial gradients, glassmorphism (`backdrop-filter: blur`), and accessible contrast tokens.
- **Typography**: Google Fonts — **Outfit** (headings & numbers) and **Inter** (body & telemetry).
- **Audio**: Web Audio API synthesizer.

---

## 💻 Development & Build Scripts

### Prerequisites
- Node.js `v18+` (tested on Node `v25.1.0`)
- npm `v9+`

### Setup & Development
```bash
# Install dependencies
npm install

# Start development server with proxy to backend (http://localhost:8080)
npm run dev
```
The application will start at `http://localhost:5173`.

### Production Build
```bash
# Compile and bundle assets
npm run build

# Preview production build locally
npm run preview
```

---

## ⚙️ Proxy Configuration (`vite.config.js`)
All `/api` requests are automatically proxied to the Spring Boot core service:
```javascript
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```
