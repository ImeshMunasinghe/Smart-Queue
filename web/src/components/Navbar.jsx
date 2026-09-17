import React from 'react';
import { UserIcon, MonitorIcon, BarChartIcon, MessageSquareIcon, TvIcon } from './Icons';

export default function Navbar({ activeRole, setActiveRole, authUsername, authRole, onLogout }) {
  const roleLabel = authRole === 'ROLE_ADMIN' ? 'Admin' : authRole === 'ROLE_OPERATOR' ? 'Operator' : null;
  const roleBadgeColor = authRole === 'ROLE_ADMIN' ? 'var(--purple)' : 'var(--success)';
  const roleBadgeBg = authRole === 'ROLE_ADMIN' ? 'var(--purple-subtle)' : 'var(--success-subtle)';
  const roleBadgeBorder = authRole === 'ROLE_ADMIN' ? 'var(--purple-border)' : 'var(--success-border)';

  return (
    <header className="navbar">
      <div className="brand-wrapper">
        <div className="brand-badge">SQ</div>
        <div>
          <h1 className="brand-title">SmartQueue Public Service</h1>
          <p className="brand-subtitle">Divisional Secretariat &amp; OPD Clinical Services</p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <nav className="nav-cluster">
          {/* Citizen — always public */}
          <button
            id="nav-citizen"
            className={`nav-link-btn ${activeRole === 'citizen' ? 'active' : ''}`}
            onClick={() => setActiveRole('citizen')}
          >
            <UserIcon size={16} />
            <span>Citizen Queue</span>
          </button>

          {/* Operator — requires ROLE_OPERATOR or ROLE_ADMIN */}
          <button
            id="nav-operator"
            className={`nav-link-btn ${activeRole === 'operator' ? 'active' : ''}`}
            onClick={() => setActiveRole('operator')}
            title={!authUsername ? 'Login required' : undefined}
          >
            <MonitorIcon size={16} />
            <span>Counter Desk</span>
            {!authUsername && (
              <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)', marginLeft: '2px' }}>🔒</span>
            )}
          </button>

          {/* Admin — requires ROLE_ADMIN */}
          <button
            id="nav-admin"
            className={`nav-link-btn ${activeRole === 'admin' ? 'active' : ''}`}
            onClick={() => setActiveRole('admin')}
            title={!authUsername ? 'Login required' : undefined}
          >
            <BarChartIcon size={16} />
            <span>Administration</span>
            {!authUsername && (
              <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)', marginLeft: '2px' }}>🔒</span>
            )}
          </button>

          {/* SMS Sandbox — public */}
          <button
            id="nav-sms"
            className={`nav-link-btn ${activeRole === 'sms' ? 'active' : ''}`}
            onClick={() => setActiveRole('sms')}
          >
            <MessageSquareIcon size={16} />
            <span>SMS Gateway</span>
          </button>
        </nav>

        {/* TV Display button */}
        <button
          id="nav-tv"
          className="tv-badge-btn"
          onClick={() => setActiveRole('display')}
          title="Open Waiting Hall TV Big-Screen Display"
        >
          <TvIcon size={16} />
          <span>Waiting Hall TV</span>
        </button>

        {/* ── Auth area ── */}
        {authUsername ? (
          /* Logged-in user badge */
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <div style={{
              display: 'flex', alignItems: 'center', gap: '0.45rem',
              padding: '0.35rem 0.7rem',
              background: roleBadgeBg,
              border: `1px solid ${roleBadgeBorder}`,
              borderRadius: 'var(--radius-full)',
            }}>
              <span style={{
                width: '22px', height: '22px', borderRadius: '50%',
                background: roleBadgeColor, color: '#fff',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: '0.7rem', fontWeight: 700, flexShrink: 0,
              }}>
                {authUsername[0].toUpperCase()}
              </span>
              <div style={{ lineHeight: 1.2 }}>
                <div style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-main)' }}>
                  {authUsername}
                </div>
                {roleLabel && (
                  <div style={{ fontSize: '0.68rem', color: roleBadgeColor, fontWeight: 600 }}>
                    {roleLabel}
                  </div>
                )}
              </div>
            </div>

            <button
              id="logout-btn"
              onClick={onLogout}
              style={{
                padding: '0.35rem 0.65rem',
                fontSize: '0.78rem', fontWeight: 600,
                color: 'var(--text-muted)',
                background: 'var(--bg-subtle)',
                border: '1px solid var(--border-medium)',
                borderRadius: 'var(--radius-sm)',
                cursor: 'pointer',
                transition: 'all 0.15s',
              }}
              title="Sign out"
            >
              Sign out
            </button>
          </div>
        ) : (
          /* Not logged in — subtle "Staff Login" hint */
          <button
            id="staff-login-hint-btn"
            onClick={() => setActiveRole('operator')}
            style={{
              padding: '0.35rem 0.75rem',
              fontSize: '0.78rem', fontWeight: 600,
              color: 'var(--text-muted)',
              background: 'none',
              border: '1px solid var(--border-medium)',
              borderRadius: 'var(--radius-sm)',
              cursor: 'pointer',
              display: 'flex', alignItems: 'center', gap: '0.35rem',
              transition: 'all 0.15s',
            }}
          >
            <svg width="13" height="13" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            Staff Login
          </button>
        )}
      </div>
    </header>
  );
}
