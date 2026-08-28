#!/bin/zsh

set -e

echo "Creating Payment Processing frontend..."

mkdir -p payment-frontend/src

cd payment-frontend

cat > package.json <<'EOF'
{
  "name": "payment-frontend",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^19.1.1",
    "react-dom": "^19.1.1"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^5.0.2",
    "vite": "^7.1.3"
  }
}
EOF

cat > index.html <<'EOF'
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Payment Processing Platform</title>
  </head>

  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.jsx"></script>
  </body>
</html>
EOF

cat > vite.config.js <<'EOF'
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173
  }
});
EOF

cat > src/main.jsx <<'EOF'
import React, { useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

const API_BASE_URL = "http://localhost:4000";

function App() {
  const [customerId, setCustomerId] = useState("C101");
  const [amount, setAmount] = useState("5000");
  const [currency, setCurrency] = useState("INR");
  const [idempotencyKey, setIdempotencyKey] = useState("");

  const [paymentId, setPaymentId] = useState("");

  const [payment, setPayment] = useState(null);
  const [response, setResponse] = useState(null);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const generateIdempotencyKey = () => {
    const key = crypto.randomUUID();
    setIdempotencyKey(key);
  };

  const clearMessages = () => {
    setError("");
    setResponse(null);
  };

  const apiRequest = async (url, options = {}) => {
      clearMessages();
      setLoading(true);

      try {
          const res = await fetch(url, {
              ...options,
              headers: {
                  "Content-Type": "application/json",
                  ...(options.headers || {})
              }
          });

          let data = null;

          const contentType = res.headers.get("content-type");

          if (contentType && contentType.includes("application/json")) {
              data = await res.json();
          } else {
              data = await res.text();
          }

          setResponse({
              status: res.status,
              statusText: res.statusText,
              data
          });

          if (!res.ok) {
              throw new Error(
                  data?.message ||
                  data?.error ||
                  `Request failed with status ${res.status}`
              );
          }

          return data;

      } catch (err) {
          setError(err.message || "Something went wrong");
          throw err;

      } finally {
          setLoading(false);
      }
  };

  const createPayment = async () => {
    if (!idempotencyKey.trim()) {
      setError("Please provide an Idempotency-Key.");
      return;
    }

    try {
      const data = await apiRequest(
        `${API_BASE_URL}/payments`,
        {
          method: "POST",
          headers: {
            "Idempotency-Key": idempotencyKey
          },
          body: JSON.stringify({
            customerId,
            amount: Number(amount),
            currency
          })
        }
      );

      setPayment(data);

      if (data?.id) {
        setPaymentId(data.id);
      }

    } catch {
      // Error is already displayed by apiRequest()
    }
  };

  const getPayment = async () => {
    if (!paymentId.trim()) {
      setError("Please enter a Payment ID.");
      return;
    }

    try {
      const data = await apiRequest(
        `${API_BASE_URL}/payments/${paymentId}`
      );

      setPayment(data);

    } catch {
      // Error already handled
    }
  };

  const getStatus = async () => {
    if (!paymentId.trim()) {
      setError("Please enter a Payment ID.");
      return;
    }

    try {
      const data = await apiRequest(
        `${API_BASE_URL}/payments/${paymentId}/status`
      );

      setPayment(data);

    } catch {
      // Error already handled
    }
  };

  const authorizePayment = async () => {
    await changePaymentState("authorize");
  };

  const capturePayment = async () => {
    await changePaymentState("capture");
  };

  const refundPayment = async () => {
    await changePaymentState("refund");
  };

  const changePaymentState = async (operation) => {
    if (!paymentId.trim()) {
      setError("Please enter a Payment ID.");
      return;
    }

    try {
      const data = await apiRequest(
        `${API_BASE_URL}/payments/${paymentId}/${operation}`,
        {
          method: "POST"
        }
      );

      setPayment(data);

    } catch {
      // Error already handled
    }
  };

  const statusClass = (status) => {
    if (!status) {
      return "";
    }

    return status.toLowerCase();
  };

  return (
    <div className="app">

      <header className="header">
        <div>
          <h1>Payment Processing Platform</h1>
          <p>Distributed payment system dashboard</p>
        </div>

        <div className="system-status">
          <span className="status-dot"></span>
          Payment Service
          <span className="status-port">:4000</span>
        </div>
      </header>

      <main className="container">

        {/* CREATE PAYMENT */}

        <section className="card">

          <div className="section-title">
            <div>
              <h2>Create Payment</h2>
              <p>Create a new payment using idempotency protection.</p>
            </div>
          </div>

          <div className="form-grid">

            <div className="field">
              <label>Customer ID</label>
              <input
                value={customerId}
                onChange={(e) => setCustomerId(e.target.value)}
                placeholder="C101"
              />
            </div>

            <div className="field">
              <label>Amount</label>
              <input
                type="number"
                min="0"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="5000"
              />
            </div>

            <div className="field">
              <label>Currency</label>
              <select
                value={currency}
                onChange={(e) => setCurrency(e.target.value)}
              >
                <option value="INR">INR</option>
                <option value="USD">USD</option>
                <option value="EUR">EUR</option>
              </select>
            </div>

            <div className="field field-wide">
              <label>
                Idempotency Key
                <span className="label-hint">
                  Required for POST /payments
                </span>
              </label>

              <div className="input-action">
                <input
                  value={idempotencyKey}
                  onChange={(e) =>
                    setIdempotencyKey(e.target.value)
                  }
                  placeholder="abc-123"
                />

                <button
                  className="secondary"
                  onClick={generateIdempotencyKey}
                >
                  Generate
                </button>
              </div>
            </div>

          </div>

          <div className="button-row">
            <button
              className="primary"
              onClick={createPayment}
              disabled={loading}
            >
              {loading ? "Processing..." : "Create Payment"}
            </button>
          </div>

        </section>


        {/* PAYMENT LOOKUP */}

        <section className="card">

          <div className="section-title">
            <div>
              <h2>Payment Operations</h2>
              <p>Retrieve and change the payment state.</p>
            </div>
          </div>

          <div className="lookup-row">

            <div className="field">
              <label>Payment ID</label>

              <input
                value={paymentId}
                onChange={(e) =>
                  setPaymentId(e.target.value)
                }
                placeholder="301d15f2-d925-4a86-b641-c5875e9431bb"
              />
            </div>

            <button
              className="secondary"
              onClick={getPayment}
              disabled={loading}
            >
              Get Payment
            </button>

            <button
              className="secondary"
              onClick={getStatus}
              disabled={loading}
            >
              Get Status
            </button>

          </div>


          <div className="operation-buttons">

            <button
              className="authorize"
              onClick={authorizePayment}
              disabled={loading}
            >
              Authorize
            </button>

            <button
              className="capture"
              onClick={capturePayment}
              disabled={loading}
            >
              Capture
            </button>

            <button
              className="refund"
              onClick={refundPayment}
              disabled={loading}
            >
              Refund
            </button>

          </div>

        </section>


        {/* PAYMENT DETAILS */}

        <section className="card">

          <div className="section-title">
            <div>
              <h2>Payment Details</h2>
              <p>Current payment state.</p>
            </div>
          </div>

          {!payment && (
            <div className="empty-state">
              No payment selected.
            </div>
          )}

          {payment && (
            <div className="payment-details">

              <div className="detail">
                <span>Payment ID</span>
                <strong>{payment.id || payment.paymentId || "-"}</strong>
              </div>

              <div className="detail">
                <span>Customer</span>
                <strong>{payment.customerId || "-"}</strong>
              </div>

              <div className="detail">
                <span>Amount</span>
                <strong>
                  {payment.amount !== undefined
                    ? `${payment.currency || ""} ${payment.amount}`
                    : "-"}
                </strong>
              </div>

              <div className="detail">
                <span>Currency</span>
                <strong>{payment.currency || "-"}</strong>
              </div>

              <div className="detail">
                <span>Status</span>

                <strong>
                  <span
                    className={`status-badge ${statusClass(
                      payment.status
                    )}`}
                  >
                    {payment.status || "-"}
                  </span>
                </strong>
              </div>

            </div>
          )}

        </section>


        {/* IDEMPOTENCY DEMO */}

        <section className="card idempotency-card">

          <div className="section-title">
            <div>
              <h2>Idempotency Demo</h2>
              <p>
                Send the same request multiple times using the
                same Idempotency-Key.
              </p>
            </div>
          </div>

          <div className="idempotency-flow">

            <div className="flow-step">
              <span className="flow-number">1</span>
              <div>
                <strong>First request</strong>
                <p>Creates the payment.</p>
              </div>
            </div>

            <div className="flow-arrow">→</div>

            <div className="flow-step">
              <span className="flow-number">2</span>
              <div>
                <strong>Retry same request</strong>
                <p>Uses the same Idempotency-Key.</p>
              </div>
            </div>

            <div className="flow-arrow">→</div>

            <div className="flow-step">
              <span className="flow-number">3</span>
              <div>
                <strong>Same payment</strong>
                <p>No duplicate payment is created.</p>
              </div>
            </div>

          </div>

          <div className="demo-key">
            <span>Current Idempotency-Key:</span>
            <code>{idempotencyKey || "Not generated"}</code>
          </div>

        </section>


        {/* ERROR */}

        {error && (
          <section className="alert error-alert">
            <strong>Request Failed</strong>
            <span>{error}</span>
          </section>
        )}


        {/* API RESPONSE */}

        {response && (
          <section className="card response-card">

            <div className="response-header">

              <div>
                <h2>API Response</h2>
                <p>Latest backend response.</p>
              </div>

              <span
                className={
                  response.status >= 200 &&
                  response.status < 300
                    ? "http-success"
                    : "http-error"
                }
              >
                HTTP {response.status}
              </span>

            </div>

            <pre>
              {JSON.stringify(
                response.data,
                null,
                2
              )}
            </pre>

          </section>
        )}

      </main>

      <footer>
        Distributed Payment Processing Platform
        <span>•</span>
        React + Spring Boot
      </footer>

    </div>
  );
}

createRoot(
  document.getElementById("root")
).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
EOF

cat > src/styles.css <<'EOF'
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  font-family:
    Inter,
    -apple-system,
    BlinkMacSystemFont,
    "Segoe UI",
    sans-serif;

  background: #f4f6f8;
  color: #17202a;
}

button,
input,
select {
  font: inherit;
}

button {
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.app {
  min-height: 100vh;
}

.header {
  background: #111827;
  color: white;

  padding: 26px 48px;

  display: flex;
  justify-content: space-between;
  align-items: center;

  border-bottom: 1px solid #293241;
}

.header h1 {
  margin: 0;

  font-size: 25px;
  font-weight: 650;
}

.header p {
  margin: 6px 0 0;

  color: #9ca3af;
  font-size: 14px;
}

.system-status {
  display: flex;
  align-items: center;
  gap: 8px;

  font-size: 13px;

  background: #1f2937;
  padding: 9px 13px;

  border-radius: 8px;
}

.status-port {
  color: #9ca3af;
}

.status-dot {
  width: 8px;
  height: 8px;

  border-radius: 50%;

  background: #22c55e;
}

.container {
  width: min(1180px, calc(100% - 40px));

  margin: 32px auto;

  display: flex;
  flex-direction: column;

  gap: 20px;
}

.card {
  background: white;

  border: 1px solid #e1e5ea;

  border-radius: 12px;

  padding: 25px;

  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.04);
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;

  margin-bottom: 22px;
}

.section-title h2 {
  margin: 0;

  font-size: 18px;
}

.section-title p {
  margin: 5px 0 0;

  font-size: 13px;

  color: #6b7280;
}

.form-grid {
  display: grid;

  grid-template-columns:
    repeat(3, 1fr);

  gap: 18px;
}

.field {
  display: flex;
  flex-direction: column;

  gap: 7px;
}

.field-wide {
  grid-column: span 2;
}

label {
  font-size: 13px;

  font-weight: 600;

  color: #374151;
}

.label-hint {
  margin-left: 8px;

  font-weight: 400;

  color: #9ca3af;
}

input,
select {
  width: 100%;

  height: 42px;

  border: 1px solid #d1d5db;

  border-radius: 7px;

  padding: 0 12px;

  background: white;

  outline: none;

  transition: border-color 0.15s;
}

input:focus,
select:focus {
  border-color: #4f46e5;
}

.input-action {
  display: flex;
  gap: 8px;
}

.input-action input {
  flex: 1;
}

button {
  border: none;

  border-radius: 7px;

  padding: 10px 17px;

  font-weight: 600;

  font-size: 13px;
}

.primary {
  background: #4f46e5;
  color: white;
}

.primary:hover {
  background: #4338ca;
}

.secondary {
  background: #eef0f3;
  color: #27303a;
}

.secondary:hover {
  background: #e2e5e9;
}

.button-row {
  margin-top: 22px;

  display: flex;
  justify-content: flex-end;
}

.lookup-row {
  display: grid;

  grid-template-columns: 1fr auto auto;

  align-items: end;

  gap: 10px;
}

.operation-buttons {
  display: flex;

  gap: 10px;

  margin-top: 20px;

  padding-top: 20px;

  border-top: 1px solid #edf0f2;
}

.authorize {
  background: #eef2ff;
  color: #3730a3;
}

.capture {
  background: #ecfdf5;
  color: #047857;
}

.refund {
  background: #fff7ed;
  color: #c2410c;
}

.payment-details {
  display: grid;

  grid-template-columns:
    repeat(5, 1fr);

  gap: 15px;
}

.detail {
  background: #f8fafc;

  border: 1px solid #edf0f2;

  border-radius: 8px;

  padding: 15px;
}

.detail span:first-child {
  display: block;

  color: #6b7280;

  font-size: 12px;

  margin-bottom: 7px;
}

.detail strong {
  font-size: 14px;

  word-break: break-word;
}

.status-badge {
  display: inline-block;

  padding: 5px 9px;

  border-radius: 999px;

  font-size: 11px;

  background: #eef2ff;

  color: #3730a3;
}

.status-badge.created {
  background: #f3f4f6;
  color: #374151;
}

.status-badge.authorized {
  background: #eff6ff;
  color: #1d4ed8;
}

.status-badge.captured {
  background: #ecfdf5;
  color: #047857;
}

.status-badge.refunded {
  background: #fff7ed;
  color: #c2410c;
}

.empty-state {
  text-align: center;

  padding: 35px;

  color: #9ca3af;

  font-size: 14px;
}

.idempotency-card {
  background:
    linear-gradient(
      135deg,
      #ffffff 0%,
      #fafaff 100%
    );
}

.idempotency-flow {
  display: flex;

  align-items: center;

  gap: 15px;
}

.flow-step {
  flex: 1;

  display: flex;

  align-items: center;

  gap: 12px;

  padding: 15px;

  border: 1px solid #e5e7eb;

  border-radius: 9px;

  background: white;
}

.flow-step p {
  margin: 4px 0 0;

  color: #6b7280;

  font-size: 12px;
}

.flow-number {
  width: 27px;
  height: 27px;

  flex-shrink: 0;

  display: flex;

  justify-content: center;
  align-items: center;

  border-radius: 50%;

  background: #eef2ff;

  color: #4338ca;

  font-weight: 700;

  font-size: 12px;
}

.flow-arrow {
  color: #9ca3af;

  font-size: 20px;
}

.demo-key {
  margin-top: 18px;

  padding: 12px 15px;

  background: #111827;

  border-radius: 7px;

  color: #d1d5db;

  font-size: 12px;
}

.demo-key code {
  margin-left: 8px;

  color: white;

  font-family: monospace;
}

.response-card {
  background: #111827;

  color: white;

  border-color: #1f2937;
}

.response-header {
  display: flex;

  justify-content: space-between;

  align-items: center;
}

.response-header h2 {
  margin: 0;

  font-size: 17px;
}

.response-header p {
  margin: 5px 0 0;

  color: #9ca3af;

  font-size: 12px;
}

.response-header span {
  padding: 6px 10px;

  border-radius: 6px;

  font-size: 12px;

  font-weight: 700;
}

.http-success {
  background: #064e3b;

  color: #a7f3d0;
}

.http-error {
  background: #7f1d1d;

  color: #fecaca;
}

pre {
  margin: 20px 0 0;

  padding: 18px;

  overflow-x: auto;

  background: #030712;

  border-radius: 8px;

  color: #d1d5db;

  font-size: 12px;

  line-height: 1.6;
}

.alert {
  padding: 15px 18px;

  border-radius: 9px;

  display: flex;

  flex-direction: column;

  gap: 4px;

  font-size: 13px;
}

.error-alert {
  background: #fef2f2;

  border: 1px solid #fecaca;

  color: #991b1b;
}

footer {
  text-align: center;

  padding: 30px;

  color: #9ca3af;

  font-size: 12px;
}

footer span {
  margin: 0 8px;
}

@media (max-width: 900px) {

  .header {
    padding: 22px;

    flex-direction: column;

    align-items: flex-start;

    gap: 15px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .field-wide {
    grid-column: span 1;
  }

  .lookup-row {
    grid-template-columns: 1fr;
  }

  .payment-details {
    grid-template-columns: repeat(2, 1fr);
  }

  .idempotency-flow {
    flex-direction: column;
    align-items: stretch;
  }

  .flow-arrow {
    text-align: center;
    transform: rotate(90deg);
  }
}

@media (max-width: 550px) {

  .container {
    width: calc(100% - 20px);

    margin: 15px auto;
  }

  .card {
    padding: 18px;
  }

  .payment-details {
    grid-template-columns: 1fr;
  }

  .operation-buttons {
    flex-direction: column;
  }

  .button-row {
    justify-content: stretch;
  }

  .button-row button {
    width: 100%;
  }
}
EOF

echo ""
echo "Frontend created successfully."
echo ""
echo "Next:"
echo "  cd payment-frontend"
echo "  npm install"
echo "  npm run dev"
echo ""
echo "Open:"
echo "  http://localhost:5173"
echo ""

