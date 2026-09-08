import React from 'react';
import { UserIcon, MonitorIcon, BarChartIcon, MessageSquareIcon, TvIcon } from './Icons';

export default function Navbar({ activeRole, setActiveRole }) {
  return (
    <header className="navbar">
      <div className="brand-wrapper">
        <div className="brand-badge">SQ</div>
        <div>
          <h1 className="brand-title">SmartQueue Public Service</h1>
          <p className="brand-subtitle">Divisional Secretariat & OPD Clinical Services</p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <nav className="nav-cluster">
          <button
            className={`nav-link-btn ${activeRole === 'citizen' ? 'active' : ''}`}
            onClick={() => setActiveRole('citizen')}
          >
            <UserIcon size={16} />
            <span>Citizen Queue</span>
          </button>
          <button
            className={`nav-link-btn ${activeRole === 'operator' ? 'active' : ''}`}
            onClick={() => setActiveRole('operator')}
          >
            <MonitorIcon size={16} />
            <span>Counter Desk</span>
          </button>
          <button
            className={`nav-link-btn ${activeRole === 'admin' ? 'active' : ''}`}
            onClick={() => setActiveRole('admin')}
          >
            <BarChartIcon size={16} />
            <span>Administration</span>
          </button>
          <button
            className={`nav-link-btn ${activeRole === 'sms' ? 'active' : ''}`}
            onClick={() => setActiveRole('sms')}
          >
            <MessageSquareIcon size={16} />
            <span>SMS Gateway</span>
          </button>
        </nav>

        <button
          className="tv-badge-btn"
          onClick={() => setActiveRole('display')}
          title="Open Waiting Hall TV Big-Screen Display"
        >
          <TvIcon size={16} />
          <span>Waiting Hall TV</span>
        </button>
      </div>
    </header>
  );
}
