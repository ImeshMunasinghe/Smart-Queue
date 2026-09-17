import React, { useState, useEffect } from 'react';
import {
  MegaphoneIcon,
  PlayIcon,
  CheckCircleIcon,
  SkipForwardIcon,
  RotateCcwIcon,
  XCircleIcon,
  ClockIcon,
  ArmchairIcon
} from './Icons';

export default function OperatorTerminal({
  counters,
  selectedCounterId,
  setSelectedCounterId,
  currentServingToken,
  setCurrentServingToken,
  upcomingQueue,
  loadCounterQueue,
  refreshOfficeData,
  apiBase,
  playChime,
  authToken
}) {
  const [loading, setLoading] = useState(false);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  // Timer for active consultation
  useEffect(() => {
    let interval = null;
    if (currentServingToken?.state === 'SERVING') {
      interval = setInterval(() => {
        setElapsedSeconds(prev => prev + 1);
      }, 1000);
    } else {
      setElapsedSeconds(0);
    }
    return () => clearInterval(interval);
  }, [currentServingToken?.state]);

  const authHeaders = () => authToken ? { Authorization: `Bearer ${authToken}` } : {};

  const handleCallNext = async () => {
    if (!selectedCounterId) return;
    setLoading(true);
    try {
      const res = await fetch(`${apiBase}/operator/counters/${selectedCounterId}/call-next`, {
        method: 'POST',
        headers: authHeaders()
      });
      if (res.status === 204) {
        alert('Eligible queue is clear. No waiting citizens.');
      } else if (res.status === 401 || res.status === 403) {
        alert('Session expired or insufficient permissions. Please log out and log in again.');
      } else if (res.ok) {
        const token = await res.json();
        setCurrentServingToken(token);
        playChime();
        loadCounterQueue(selectedCounterId);
        refreshOfficeData();
      } else {
        const err = await res.json().catch(() => ({}));
        alert('Error calling next: ' + (err.message || res.status));
      }
    } finally {
      setLoading(false);
    }
  };

  const handleAction = async (actionPath) => {
    if (!currentServingToken?.id) return;
    setLoading(true);
    try {
      const res = await fetch(`${apiBase}/operator/tokens/${currentServingToken.id}/${actionPath}`, {
        method: 'POST',
        headers: authHeaders()
      });
      if (res.status === 401 || res.status === 403) {
        alert('Session expired or insufficient permissions. Please log out and log in again.');
      } else if (res.ok) {
        const updated = await res.json();
        if (['complete', 'no-show', 'skip'].includes(actionPath)) {
          setCurrentServingToken(null);
        } else {
          setCurrentServingToken(updated);
        }
        loadCounterQueue(selectedCounterId);
        refreshOfficeData();
      } else {
        const err = await res.json().catch(() => ({}));
        alert('Error performing action: ' + (err.message || res.status));
      }
    } finally {
      setLoading(false);
    }
  };

  // Keyboard shortcut listener
  useEffect(() => {
    const onKey = (e) => {
      if (['INPUT', 'SELECT', 'TEXTAREA'].includes(document.activeElement.tagName)) return;

      if (e.code === 'Space') {
        e.preventDefault();
        handleCallNext();
      } else if (e.key === 's' || e.key === 'S' || e.key === 'Enter') {
        e.preventDefault();
        handleAction('serve');
      } else if (e.key === 'c' || e.key === 'C') {
        e.preventDefault();
        handleAction('complete');
      } else if (e.key === 'k' || e.key === 'K') {
        e.preventDefault();
        handleAction('skip');
      } else if (e.key === 'r' || e.key === 'R') {
        e.preventDefault();
        handleAction('recall');
      } else if (e.key === 'x' || e.key === 'X') {
        e.preventDefault();
        handleAction('no-show');
      }
    };

    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [selectedCounterId, currentServingToken, authToken]);


  const formatTimer = (sec) => {
    const mins = Math.floor(sec / 60);
    const s = sec % 60;
    return `${mins}:${s < 10 ? '0' : ''}${s}`;
  };

  const currentCounter = counters.find(c => c.id === selectedCounterId);

  return (
    <div>
      {/* 1. Counter Control Header */}
      <div className="civic-card" style={{ padding: '0.85rem 1.25rem', marginBottom: '1.25rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <label style={{ fontSize: '0.85rem', fontWeight: 600 }}>Active Service Point:</label>
          <select
            className="field-select"
            style={{ width: 'auto', padding: '0.4rem 0.8rem' }}
            value={selectedCounterId}
            onChange={e => {
              setSelectedCounterId(e.target.value);
              loadCounterQueue(e.target.value);
            }}
            disabled={!counters || counters.length === 0}
          >
            {!counters || counters.length === 0 ? (
              <option value="" disabled>⏳ Loading service counters...</option>
            ) : (
              counters.map(c => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.status})
                </option>
              ))
            )}
          </select>
          <span className="pill pill-serving" style={{ fontSize: '0.7rem' }}>
            ● {currentCounter?.status || 'ONLINE'}
          </span>
        </div>

        <button
          className="btn-civic btn-primary"
          onClick={handleCallNext}
          disabled={loading}
        >
          <MegaphoneIcon size={16} />
          <span>Call Next Citizen</span>
          <span className="kbd-key" style={{ color: '#ffffff', background: 'rgba(255,255,255,0.2)' }}>Space</span>
        </button>
      </div>

      {/* 2. Main Terminal Layout */}
      <div className="grid-cols-2">
        {/* Active Ticket Terminal */}
        <div className="civic-card" style={{ textAlign: 'center', padding: '2rem' }}>
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Active Citizen Ticket
          </span>

          {currentServingToken ? (
            <div style={{ marginTop: '1rem' }}>
              <span className={`pill pill-${currentServingToken.state.toLowerCase()}`}>
                ● {currentServingToken.state}
              </span>
              <div style={{ fontSize: '5rem', fontFamily: 'var(--font-display)', fontWeight: 800, color: 'var(--primary)', lineHeight: 1.1, margin: '0.5rem 0' }}>
                {currentServingToken.tokenNumber}
              </div>
              <p style={{ fontSize: '1.1rem', color: 'var(--text-secondary)', fontWeight: 600 }}>
                {currentServingToken.serviceTypeName}
              </p>

              {currentServingToken.state === 'SERVING' && (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.4rem', margin: '1rem 0', fontSize: '1.25rem', fontWeight: 700, color: 'var(--success)' }}>
                  <ClockIcon size={18} />
                  <span>Duration: {formatTimer(elapsedSeconds)}</span>
                </div>
              )}

              {/* Action Buttons with Keyboard Keys */}
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.6rem', justifyContent: 'center', marginTop: '1.5rem' }}>
                {currentServingToken.state === 'CALLED' && (
                  <button
                    className="btn-civic btn-success"
                    onClick={() => handleAction('serve')}
                    disabled={loading}
                  >
                    <PlayIcon size={14} />
                    <span>Start Consultation</span>
                    <span className="kbd-key">S</span>
                  </button>
                )}

                {currentServingToken.state === 'SERVING' && (
                  <button
                    className="btn-civic btn-success"
                    onClick={() => handleAction('complete')}
                    disabled={loading}
                  >
                    <CheckCircleIcon size={16} />
                    <span>Complete</span>
                    <span className="kbd-key">C</span>
                  </button>
                )}

                <button
                  className="btn-civic btn-secondary"
                  onClick={() => handleAction('skip')}
                  disabled={loading}
                >
                  <SkipForwardIcon size={16} />
                  <span>Skip</span>
                  <span className="kbd-key">K</span>
                </button>

                <button
                  className="btn-civic btn-secondary"
                  onClick={() => handleAction('recall')}
                  disabled={loading}
                >
                  <RotateCcwIcon size={16} />
                  <span>Recall</span>
                  <span className="kbd-key">R</span>
                </button>

                <button
                  className="btn-civic btn-danger"
                  onClick={() => handleAction('no-show')}
                  disabled={loading}
                >
                  <XCircleIcon size={16} />
                  <span>No-Show</span>
                  <span className="kbd-key">X</span>
                </button>
              </div>
            </div>
          ) : (
            <div style={{ padding: '3.5rem 1rem', color: 'var(--text-muted)' }}>
              <div style={{ color: 'var(--border-strong)', marginBottom: '0.75rem', display: 'flex', justifyContent: 'center' }}>
                <ArmchairIcon size={48} />
              </div>
              <h3 style={{ color: 'var(--text-main)', fontSize: '1.25rem' }}>Counter Idle</h3>
              <p style={{ fontSize: '0.85rem', marginTop: '0.35rem' }}>
                Press <span className="kbd-key">Space</span> or click "Call Next Citizen" to advance the queue.
              </p>
            </div>
          )}
        </div>

        {/* Real-time Eligible Queue Sidebar */}
        <div className="civic-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.75rem', marginBottom: '1rem' }}>
            <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-main)' }}>
              Waiting Citizens for this Service
            </h3>
            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)' }}>
              {(upcomingQueue || []).length} in queue
            </span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem', maxHeight: '420px', overflowY: 'auto' }}>
            {Array.isArray(upcomingQueue) && upcomingQueue.length > 0 ? (
              upcomingQueue.map((item, idx) => (
                <div
                  key={item.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '0.65rem 0.85rem',
                    backgroundColor: 'var(--bg-subtle)',
                    border: '1px solid var(--border-subtle)',
                    borderRadius: 'var(--radius-sm)',
                    borderLeft: '4px solid var(--primary)'
                  }}
                >
                  <div>
                    <strong style={{ fontSize: '1.05rem', color: 'var(--text-main)' }}>{item.tokenNumber}</strong>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginLeft: '0.5rem' }}>
                      Channel: {item.channel}
                    </span>
                  </div>
                  <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                    Position #{idx + 1}
                  </span>
                </div>
              ))
            ) : (
              <p style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '2.5rem 0', fontSize: '0.85rem' }}>
                No citizens currently waiting in this line.
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
