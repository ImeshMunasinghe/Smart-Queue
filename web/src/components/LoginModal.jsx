import React, { useState, useEffect, useRef } from 'react';

/**
 * LoginModal — displayed when an unauthenticated user tries to access
 * a protected tab (Operator or Admin). Stores JWT in sessionStorage.
 */
export default function LoginModal({ targetRole, onSuccess, onClose, apiBase }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const usernameRef = useRef(null);

  // Auto-focus username field when modal opens
  useEffect(() => {
    usernameRef.current?.focus();
  }, []);

  // Close on Escape key
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [onClose]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const res = await fetch(`${apiBase}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.message || 'Login failed.');
      }

      // Persist token and user info in sessionStorage
      sessionStorage.setItem('sq_token', data.token);
      sessionStorage.setItem('sq_username', data.username);
      sessionStorage.setItem('sq_role', data.role);

      onSuccess({ token: data.token, username: data.username, role: data.role });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const roleLabel  = targetRole === 'operator' ? 'Counter Operator' : 'Administrator';
  const demoUser   = targetRole === 'operator' ? 'operator1' : 'admin';
  const demoPass   = targetRole === 'operator' ? 'operator123' : 'admin123';

  return (
    /* Backdrop */
    <div
      onClick={onClose}
      style={{
        position: 'fixed', inset: 0, zIndex: 1000,
        background: 'rgba(15, 23, 42, 0.55)',
        backdropFilter: 'blur(4px)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: '1rem',
      }}
    >
      {/* Modal card */}
      <div
        onClick={e => e.stopPropagation()}
        style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-medium)',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-lg)',
          padding: '2rem',
          width: '100%',
          maxWidth: '400px',
          animation: 'modal-slide-in 0.18s ease-out',
        }}
      >
        {/* Header */}
        <div style={{ marginBottom: '1.5rem' }}>
          <div style={{
            width: '44px', height: '44px',
            background: 'linear-gradient(135deg, #1d4ed8, #7c3aed)',
            borderRadius: 'var(--radius-md)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            marginBottom: '1rem',
          }}>
            <svg width="22" height="22" fill="none" stroke="white" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <h2 style={{ fontSize: '1.3rem', fontWeight: 700, fontFamily: 'var(--font-display)', color: 'var(--text-main)' }}>
            Staff Login
          </h2>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
            Sign in as <strong>{roleLabel}</strong> to access this section.
          </p>
        </div>

        {/* Demo hint */}
        <div style={{
          background: 'var(--primary-subtle)', border: '1px solid var(--primary-border)',
          borderRadius: 'var(--radius-sm)', padding: '0.65rem 0.85rem',
          fontSize: '0.8rem', color: 'var(--primary)', marginBottom: '1.25rem',
          display: 'flex', gap: '0.5rem', alignItems: 'flex-start',
        }}>
          <span>💡</span>
          <span>
            Demo: <strong>{demoUser}</strong> / <strong>{demoPass}</strong>
          </span>
        </div>

        {error && (
          <div style={{
            padding: '0.7rem 1rem',
            background: 'var(--danger-subtle)', border: '1px solid var(--danger-border)',
            borderRadius: 'var(--radius-sm)', color: 'var(--danger)',
            fontSize: '0.85rem', marginBottom: '1.25rem',
          }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-field">
            <label className="field-label">Username</label>
            <input
              ref={usernameRef}
              id="login-username"
              type="text"
              className="field-input"
              value={username}
              onChange={e => setUsername(e.target.value)}
              placeholder={demoUser}
              autoComplete="username"
              required
            />
          </div>

          <div className="form-field">
            <label className="field-label">Password</label>
            <input
              id="login-password"
              type="password"
              className="field-input"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="••••••••"
              autoComplete="current-password"
              required
            />
          </div>

          <button
            id="login-submit-btn"
            type="submit"
            className="btn-civic btn-primary"
            style={{ width: '100%', padding: '0.75rem', marginTop: '0.25rem' }}
            disabled={loading}
          >
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>

        <button
          onClick={onClose}
          style={{
            display: 'block', width: '100%', marginTop: '0.75rem',
            padding: '0.5rem', fontSize: '0.85rem', color: 'var(--text-muted)',
            background: 'none', border: 'none', cursor: 'pointer',
          }}
        >
          Cancel
        </button>
      </div>

      <style>{`
        @keyframes modal-slide-in {
          from { opacity: 0; transform: translateY(-12px) scale(0.97); }
          to   { opacity: 1; transform: translateY(0) scale(1); }
        }
      `}</style>
    </div>
  );
}
