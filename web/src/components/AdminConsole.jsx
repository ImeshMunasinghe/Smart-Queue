import React, { useState } from 'react';
import { TicketIcon, ClockIcon, MonitorIcon } from './Icons';
import AnalyticsCharts from './AnalyticsCharts';

export default function AdminConsole({
  slots,
  analytics,
  serviceTypes,
  counters,
  selectedOfficeId,
  refreshOfficeData,
  apiBase,
  authToken
}) {
  const [overrideInput, setOverrideInput] = useState({});
  const [savingId, setSavingId] = useState(null);

  const handleSaveOverride = async (slotId) => {
    const val = parseInt(overrideInput[slotId], 10);
    if (isNaN(val) || val < 1) {
      alert('Please enter a valid positive number.');
      return;
    }

    setSavingId(slotId);
    try {
      const res = await fetch(`${apiBase}/admin/slots/${slotId}/override`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
        },
        body: JSON.stringify({ manualOverrideLimit: val })
      });
      if (res.ok) {
        alert('Capacity override limit updated.');
        refreshOfficeData();
      } else if (res.status === 401 || res.status === 403) {
        alert('Session expired or insufficient permissions. Please log in again.');
      }
    } catch (err) {
      alert('Failed to update capacity: ' + err.message);
    } finally {
      setSavingId(null);
    }
  };

  return (
    <div>
      {/* 1. High-Level Telemetry Cards */}
      <div className="grid-cols-3" style={{ marginBottom: '1.5rem' }}>
        <div className="civic-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Today's Total Issued
            </span>
            <div style={{ color: 'var(--primary)' }}>
              <TicketIcon size={20} />
            </div>
          </div>
          <div style={{ fontSize: '2.25rem', fontFamily: 'var(--font-display)', fontWeight: 800, color: 'var(--primary)', marginTop: '0.2rem' }}>
            {analytics?.totalIssuedToday || 0}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Tokens requested across all channels</span>
        </div>

        <div className="civic-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Active Citizens Waiting
            </span>
            <div style={{ color: 'var(--warning)' }}>
              <ClockIcon size={20} />
            </div>
          </div>
          <div style={{ fontSize: '2.25rem', fontFamily: 'var(--font-display)', fontWeight: 800, color: 'var(--warning)', marginTop: '0.2rem' }}>
            {analytics?.totalActiveWaiting || 0}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Currently in waiting queues</span>
        </div>

        <div className="civic-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Service Counters Online
            </span>
            <div style={{ color: 'var(--success)' }}>
              <MonitorIcon size={20} />
            </div>
          </div>
          <div style={{ fontSize: '2.25rem', fontFamily: 'var(--font-display)', fontWeight: 800, color: 'var(--success)', marginTop: '0.2rem' }}>
            {counters.filter(c => c.status === 'ONLINE' || c.status === 'BUSY').length} / {counters.length}
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Active workstations serving citizens</span>
        </div>
      </div>

      {/* 2. Session Slot Capacities & Overbooking Table */}
      <div className="civic-card">
        <div style={{ borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.85rem', marginBottom: '1rem' }}>
          <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-main)', fontFamily: 'var(--font-display)' }}>
            Session Slot Capacities &amp; Binomial Overbooking Bounds
          </h3>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
            Issuance limits are computed dynamically using predicted no-show probabilities to prevent overflow (Risk ≤ 10%).
          </p>
        </div>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid var(--border-subtle)', color: 'var(--text-muted)', fontWeight: 700 }}>
                <th style={{ padding: '0.75rem' }}>Service Category</th>
                <th style={{ padding: '0.75rem' }}>Operating Hours</th>
                <th style={{ padding: '0.75rem' }}>Raw Capacity</th>
                <th style={{ padding: '0.75rem' }}>Computed Limit (10% Risk)</th>
                <th style={{ padding: '0.75rem' }}>Issued Count</th>
                <th style={{ padding: '0.75rem' }}>Active Waiting</th>
                <th style={{ padding: '0.75rem' }}>Admin Manual Override</th>
              </tr>
            </thead>
            <tbody>
              {slots.map(slot => {
                const service = serviceTypes.find(s => s.id === slot.serviceTypeId);
                const overbookedCount = slot.computedLimit - slot.rawCapacity;

                return (
                  <tr key={slot.id} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                    <td style={{ padding: '0.75rem', fontWeight: 600, color: 'var(--text-main)' }}>
                      {service?.name || 'General Service'}
                    </td>
                    <td style={{ padding: '0.75rem', color: 'var(--text-secondary)' }}>
                      {slot.startTime} – {slot.endTime}
                    </td>
                    <td style={{ padding: '0.75rem', fontWeight: 600 }}>{slot.rawCapacity}</td>
                    <td style={{ padding: '0.75rem' }}>
                      <strong style={{ color: 'var(--primary)' }}>{slot.computedLimit}</strong>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginLeft: '0.35rem' }}>
                        (+{overbookedCount} overbooked)
                      </span>
                    </td>
                    <td style={{ padding: '0.75rem', fontWeight: 700 }}>{slot.issuedCount}</td>
                    <td style={{ padding: '0.75rem', color: 'var(--warning)', fontWeight: 700 }}>
                      {slot.activeWaitingCount}
                    </td>
                    <td style={{ padding: '0.75rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <input
                          type="number"
                          className="field-input"
                          style={{ width: '85px', padding: '0.3rem 0.5rem', fontSize: '0.85rem' }}
                          placeholder={slot.manualOverrideLimit ? String(slot.manualOverrideLimit) : String(slot.computedLimit)}
                          onChange={e => setOverrideInput({ ...overrideInput, [slot.id]: e.target.value })}
                        />
                        <button
                          className="btn-civic btn-secondary"
                          style={{ padding: '0.3rem 0.65rem', fontSize: '0.75rem' }}
                          onClick={() => handleSaveOverride(slot.id)}
                          disabled={savingId === slot.id}
                        >
                          {savingId === slot.id ? '...' : 'Set'}
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* 3. Historical Analytics Charts */}
      <AnalyticsCharts
        selectedOfficeId={selectedOfficeId}
        serviceTypes={serviceTypes}
        apiBase={apiBase}
        authToken={authToken}
      />
    </div>
  );
}
