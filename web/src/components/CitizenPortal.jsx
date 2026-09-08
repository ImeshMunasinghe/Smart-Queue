import React, { useState, useEffect } from 'react';
import { TicketIcon, FileTextIcon, ClockIcon, UserIcon } from './Icons';

export default function CitizenPortal({
  serviceTypes,
  selectedOfficeId,
  activeToken,
  setActiveToken,
  refreshOfficeData,
  apiBase
}) {
  const [form, setForm] = useState({
    serviceTypeId: '',
    nic: '145896235V',
    phone: '0771234567'
  });
  const [nicValid, setNicValid] = useState(true);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);

  useEffect(() => {
    if (serviceTypes.length > 0 && !form.serviceTypeId) {
      setForm(prev => ({ ...prev, serviceTypeId: serviceTypes[0].id }));
    }
  }, [serviceTypes]);

  // Dual Sri Lankan NIC Regex: legacy 9-digit + V/X or modern 12-digit
  const validateNic = (val) => {
    const clean = val.trim();
    const regex = /^([0-9]{9}[vVxX]|[0-9]{12})$/;
    const isValid = clean === '' || regex.test(clean);
    setNicValid(isValid);
    setForm(prev => ({ ...prev, nic: clean }));
  };

  const handleIssue = async (e) => {
    e.preventDefault();
    if (!nicValid || !form.nic) {
      setErrorMsg('Please enter a valid NIC format (9 digits + V/X or 12 digits).');
      return;
    }
    setLoading(true);
    setErrorMsg(null);
    try {
      const res = await fetch(`${apiBase}/tokens`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          officeId: selectedOfficeId,
          serviceTypeId: form.serviceTypeId,
          citizenNic: form.nic,
          citizenPhone: form.phone,
          channel: 'WEB'
        })
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.message || 'Capacity reached for this session.');
      }
      const token = await res.json();
      setActiveToken(token);
      refreshOfficeData();
    } catch (err) {
      setErrorMsg(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async () => {
    if (!activeToken?.id) return;
    if (!confirm('Are you sure you want to cancel your queue ticket? This releases your slot to other citizens.')) return;

    try {
      const res = await fetch(`${apiBase}/tokens/${activeToken.id}/cancel`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason: 'Citizen self-cancellation from portal' })
      });
      if (res.ok) {
        const updated = await res.json();
        setActiveToken(updated);
        refreshOfficeData();
      }
    } catch (err) {
      alert('Error cancelling ticket: ' + err.message);
    }
  };

  return (
    <div className="grid-cols-2">
      {/* 1. Issuance Section */}
      <div className="civic-card">
        <div style={{ borderBottom: '1px solid var(--border-subtle)', paddingBottom: '1rem', marginBottom: '1.25rem' }}>
          <h2 style={{ fontSize: '1.35rem', fontWeight: 700, color: 'var(--text-main)', fontFamily: 'var(--font-display)' }}>
            Request Virtual Queue Token
          </h2>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
            Book your slot remotely and track wait time in real-time. No need to stand in physical queues.
          </p>
        </div>

        {errorMsg && (
          <div style={{ padding: '0.75rem 1rem', background: 'var(--danger-subtle)', border: '1px solid var(--danger-border)', borderRadius: 'var(--radius-sm)', color: 'var(--danger)', fontSize: '0.85rem', marginBottom: '1.25rem' }}>
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleIssue}>
          <div className="form-field">
            <label className="field-label">
              <span>Service Category</span>
            </label>
            <select
              className="field-select"
              value={form.serviceTypeId}
              onChange={e => setForm({ ...form, serviceTypeId: e.target.value })}
            >
              {serviceTypes.map(st => (
                <option key={st.id} value={st.id}>
                  {st.name} — ~{st.defaultDurationMinutes} min duration
                </option>
              ))}
            </select>
          </div>

          <div className="form-field">
            <label className="field-label">
              <span>National Identity Card (NIC)</span>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Legacy or Modern Format</span>
            </label>
            <input
              type="text"
              className="field-input"
              value={form.nic}
              onChange={e => validateNic(e.target.value)}
              placeholder="e.g. 145896235V or 144756235896"
              style={{ borderColor: !nicValid ? 'var(--danger)' : undefined }}
              required
            />
            {!nicValid && (
              <p style={{ fontSize: '0.75rem', color: 'var(--danger)', marginTop: '0.3rem' }}>
                Invalid NIC. Enter 9 digits followed by V/X (e.g. 145896235V) or 12 digits (e.g. 144756235896).
              </p>
            )}
          </div>

          <div className="form-field">
            <label className="field-label">
              <span>Mobile Phone Number</span>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>For turn alerts</span>
            </label>
            <input
              type="tel"
              className="field-input"
              value={form.phone}
              onChange={e => setForm({ ...form, phone: e.target.value })}
              placeholder="0771234567"
              required
            />
          </div>

          <button
            type="submit"
            className="btn-civic btn-primary"
            style={{ width: '100%', padding: '0.75rem' }}
            disabled={loading || !nicValid}
          >
            <TicketIcon size={18} />
            <span>{loading ? 'Reserving Session Slot...' : 'Issue Queue Token'}</span>
          </button>
        </form>
      </div>

      {/* 2. Live Token Tracking Section */}
      <div className="civic-card" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
        {activeToken ? (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '1rem', marginBottom: '1.25rem' }}>
              <div>
                <span className={`pill pill-${activeToken.state.toLowerCase()}`}>
                  ● {activeToken.state}
                </span>
                <h3 style={{ fontSize: '3.5rem', fontFamily: 'var(--font-display)', fontWeight: 800, color: 'var(--primary)', lineHeight: 1.1, marginTop: '0.4rem' }}>
                  {activeToken.tokenNumber}
                </h3>
                <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', fontWeight: 500 }}>
                  {activeToken.serviceTypeName}
                </p>
              </div>

              <div style={{ textAlign: 'right' }}>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  Counter Allocation
                </span>
                <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-main)', marginTop: '0.15rem' }}>
                  {activeToken.assignedCounterNumber || 'Pending Allocation'}
                </div>
                {activeToken.isProvisionalCounter && (
                  <span style={{ fontSize: '0.7rem', color: 'var(--warning)', background: 'var(--warning-subtle)', border: '1px solid var(--warning-border)', padding: '0.15rem 0.45rem', borderRadius: '4px', fontWeight: 600 }}>
                    Provisional
                  </span>
                )}
              </div>
            </div>

            {/* Queue Metrics Dashboard */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', background: 'var(--bg-subtle)', padding: '1rem', borderRadius: 'var(--radius-sm)', marginBottom: '1.5rem' }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                  <UserIcon size={14} />
                  <span>Queue Ahead</span>
                </div>
                <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-main)', marginTop: '0.15rem' }}>
                  {activeToken.queuePosition > 0 ? `${activeToken.queuePosition} citizens` : 'Now Serving'}
                </div>
              </div>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                  <ClockIcon size={14} />
                  <span>Estimated Wait</span>
                </div>
                <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--success)', marginTop: '0.15rem' }}>
                  ~{activeToken.estimatedWaitMinutes} mins
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8rem', color: 'var(--success)', marginBottom: '1.25rem', fontWeight: 500 }}>
              <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--success)' }} />
              Live Server-Sent Events (SSE) Stream Active • Reconnect Resilient
            </div>

            {(activeToken.state === 'WAITING' || activeToken.state === 'CALLED') && (
              <button
                className="btn-civic btn-danger"
                style={{ width: '100%' }}
                onClick={handleCancel}
              >
                Release Token & Exit Queue
              </button>
            )}
          </div>
        ) : (
          <div style={{ textAlign: 'center', padding: '3rem 1rem', color: 'var(--text-muted)' }}>
            <div style={{ color: 'var(--border-strong)', marginBottom: '0.75rem', display: 'flex', justifyContent: 'center' }}>
              <FileTextIcon size={48} />
            </div>
            <h3 style={{ color: 'var(--text-main)', fontSize: '1.15rem', marginBottom: '0.25rem' }}>
              No Active Ticket
            </h3>
            <p style={{ fontSize: '0.85rem' }}>
              Request a virtual token using the form to track your position live.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
