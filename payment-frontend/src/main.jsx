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
        headers: {
          "Content-Type": "application/json",
          ...(options.headers || {})
        },
        ...options
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
