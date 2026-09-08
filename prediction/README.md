# SmartQueue — Prediction & Intelligence Microservice

FastAPI microservice providing stateless machine learning predictions for citizen wait times, no-show probabilities, and exact binomial probabilistic overbooking limits.

---

## 🧮 Mathematical Engine: Probabilistic Overbooking

Rather than applying a crude flat multiplier, the system computes the maximum safe issuance limit $N$ based on the **Binomial Distribution**:

Let:
- $C$: Raw physical counter capacity for the session
- $N$: Overbooked token issuance target ($N \ge C$)
- $p$: Probability that an issued citizen shows up ($p = 1 - \text{no\_show\_rate}$)
- $\alpha$: Administrator's maximum acceptable overflow risk (default $\alpha = 0.10$ or 10%)

The number of citizens who show up $S$ follows:
$$S \sim \text{Binomial}(N, p)$$

The engine finds the maximum integer $N$ satisfying:
$$P(S > C) = \sum_{k=C+1}^{N} \binom{N}{k} p^k (1-p)^{N-k} \le \alpha$$

This guarantees mathematically that the probability of counter overcrowding never exceeds the office's risk policy.

---

## ❄️ Cold-Start & Heuristic Fallbacks

- **Peak Hour Calibration**: Heuristically scales duration estimates by 15% during peak hours (09:00–11:30 and 13:30–15:00).
- **Lead Time Decay**: Modulates no-show probabilities based on lead time (citizens arriving within 30 minutes are 20% more likely to attend).
- **Resilient Fallback**: If the microservice is temporarily unreachable, the Spring Boot core service automatically falls back to baseline duration lookups without service disruption.

---

## 📡 Microservice Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Health check & model status indicator |
| `POST` | `/predict/wait-time` | Predicts token wait time (seconds) with P50/P90 confidence bounds |
| `POST` | `/predict/no-show` | Predicts no-show probability $[0.0 - 1.0]$ based on citizen history & lead time |
| `POST` | `/calculate/overbooking` | Calculates maximum safe token limit $N$ using the exact binomial tail model |

### Interactive Swagger Docs
Interactive API documentation is available at:
`http://localhost:8000/docs`

---

## 🧪 Testing & Verification

Unit tests assert peak-hour weight adjustments, binomial tail probability edge cases, and that computed limits strictly respect risk bounds:

```bash
# Run pytest in virtual environment
.\venv\Scripts\python -m pytest
```

**Results**: 3 passed in 0.55s (**100% passing**).

---

## 🏃 Running the Microservice Locally

```bash
# Activate virtualenv and start Uvicorn server on port 8000
.\venv\Scripts\uvicorn main:app --port 8000 --reload
```
