import React, { useState } from 'react';
import { SendIcon } from './Icons';

export default function SmsSandbox({ apiBase }) {
  const [senderPhone, setSenderPhone] = useState('+94771234567');
  const [commandText, setCommandText] = useState('STATUS NI-001');
  const [history, setHistory] = useState([
    { from: 'Telco Gateway', text: 'SMS Sandbox initialized. Type STATUS <Token> or CANCEL <Token> to test.', time: '09:00 AM' }
  ]);
  const [loading, setLoading] = useState(false);

  const handleSend = async (e) => {
    e.preventDefault();
    if (!commandText.trim()) return;

    const userEntry = {
      from: senderPhone,
      text: commandText,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };
    setHistory(prev => [userEntry, ...prev]);
    setLoading(true);

    try {
      const res = await fetch(`${apiBase}/webhooks/sms/inbound`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          sender: senderPhone,
          text: commandText
        })
      });
      const data = await res.json();
      const replyEntry = {
        from: 'SmartQueue Auto-Reply',
        text: data.reply || data.status,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };
      setHistory(prev => [replyEntry, ...prev]);
    } catch (err) {
      setHistory(prev => [{ from: 'Error', text: err.message, time: 'Now' }, ...prev]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="grid-cols-2">
      {/* 1. Telco Simulator Control Panel */}
      <div className="civic-card">
        <div style={{ borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.85rem', marginBottom: '1.25rem' }}>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-main)', fontFamily: 'var(--font-display)' }}>
            Telco SMS Command Simulator
          </h2>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
            Test inbound telco SMS parsing and turn alerts with zero carrier fees.
          </p>
        </div>

        <form onSubmit={handleSend}>
          <div className="form-field">
            <label className="field-label">
              <span>Simulated Citizen Mobile Number</span>
            </label>
            <input
              type="text"
              className="field-input"
              value={senderPhone}
              onChange={e => setSenderPhone(e.target.value)}
              required
            />
          </div>

          <div className="form-field">
            <label className="field-label">
              <span>Inbound Message Text</span>
            </label>
            <input
              type="text"
              className="field-input"
              value={commandText}
              onChange={e => setCommandText(e.target.value)}
              placeholder="e.g. STATUS NI-001"
              required
            />
          </div>

          {/* Quick Command Chips */}
          <div style={{ display: 'flex', gap: '0.4rem', marginBottom: '1.25rem', flexWrap: 'wrap' }}>
            <button
              type="button"
              className="btn-civic btn-secondary"
              style={{ padding: '0.3rem 0.65rem', fontSize: '0.75rem' }}
              onClick={() => setCommandText('STATUS NI-001')}
            >
              STATUS NI-001
            </button>
            <button
              type="button"
              className="btn-civic btn-secondary"
              style={{ padding: '0.3rem 0.65rem', fontSize: '0.75rem' }}
              onClick={() => setCommandText('CANCEL NI-001')}
            >
              CANCEL NI-001
            </button>
            <button
              type="button"
              className="btn-civic btn-secondary"
              style={{ padding: '0.3rem 0.65rem', fontSize: '0.75rem' }}
              onClick={() => setCommandText('HELP')}
            >
              HELP
            </button>
          </div>

          <button
            type="submit"
            className="btn-civic btn-primary"
            style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}
            disabled={loading}
          >
            <SendIcon size={16} />
            <span>{loading ? 'Transmitting to Gateway...' : 'Dispatch Inbound SMS'}</span>
          </button>
        </form>
      </div>

      {/* 2. Realistic Message Log Viewer */}
      <div className="civic-card">
        <div style={{ borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.75rem', marginBottom: '1rem' }}>
          <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-main)' }}>
            Carrier Message Stream
          </h3>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', maxHeight: '420px', overflowY: 'auto' }}>
          {history.map((item, idx) => {
            const isGateway = item.from.includes('Gateway') || item.from.includes('Auto-Reply');
            return (
              <div
                key={idx}
                style={{
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-sm)',
                  backgroundColor: isGateway ? 'var(--primary-subtle)' : 'var(--bg-subtle)',
                  border: isGateway ? '1px solid var(--primary-border)' : '1px solid var(--border-subtle)',
                  alignSelf: isGateway ? 'flex-start' : 'flex-end',
                  maxWidth: '90%'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1.5rem', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '0.25rem' }}>
                  <strong>{item.from}</strong>
                  <span>{item.time}</span>
                </div>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-main)' }}>{item.text}</p>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
