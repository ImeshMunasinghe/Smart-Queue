import React, { useState, useEffect } from 'react';
import { BellIcon, CheckCircleIcon, MegaphoneIcon, XCircleIcon } from './Icons';

export default function PublicDisplayBoard({
  counters,
  offices,
  selectedOfficeId,
  onExit,
  playChime,
  apiBase
}) {
  const [time, setTime] = useState(new Date());
  const [activeCalls, setActiveCalls] = useState({});

  // Digital clock
  useEffect(() => {
    const timer = setInterval(() => setTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  // Poll active counter states every 2.5s for the public display
  useEffect(() => {
    const fetchBoardData = async () => {
      try {
        const res = await fetch(`${apiBase}/operator/offices/${selectedOfficeId}/counters`);
        if (res.ok) {
          const data = await res.json();
          // Map each counter to its current token if active
          const callsMap = {};
          for (const c of data) {
            if (c.currentTokenId) {
              const tokenRes = await fetch(`${apiBase}/tokens/${c.currentTokenId}/status`);
              if (tokenRes.ok) {
                callsMap[c.id] = await tokenRes.json();
              }
            }
          }
          setActiveCalls(callsMap);
        }
      } catch (e) {
        console.error('Error refreshing display board', e);
      }
    };

    fetchBoardData();
    const interval = setInterval(fetchBoardData, 3000);
    return () => clearInterval(interval);
  }, [selectedOfficeId]);

  const office = offices.find(o => o.id === selectedOfficeId);

  return (
    <div className="tv-display-wrapper">
      {/* 1. TV Board Header */}
      <div className="tv-header">
        <div>
          <div style={{ display: 'inline-block', background: '#2563eb', padding: '0.25rem 0.65rem', borderRadius: '4px', fontSize: '0.8rem', fontWeight: 800, letterSpacing: '0.05em' }}>
            PUBLIC SERVICE QUEUE DISPLAY
          </div>
          <h1 style={{ fontSize: '2.25rem', fontFamily: 'var(--font-display)', fontWeight: 800, marginTop: '0.25rem', color: '#ffffff' }}>
            {office?.name || 'Divisional Secretariat / OPD Clinic'}
          </h1>
        </div>

        <div style={{ textAlign: 'right', display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          <div>
            <div style={{ fontSize: '2rem', fontFamily: 'var(--font-display)', fontWeight: 700, color: '#38bdf8' }}>
              {time.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
            </div>
            <div style={{ fontSize: '0.85rem', color: '#94a3b8' }}>
              {time.toLocaleDateString([], { weekday: 'long', month: 'short', day: 'numeric', year: 'numeric' })}
            </div>
          </div>

          <button
            className="btn-civic btn-secondary"
            onClick={onExit}
            style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.8rem', padding: '0.4rem 0.75rem', background: '#1e293b', color: '#f8fafc', borderColor: '#334155' }}
          >
            <XCircleIcon size={14} />
            <span>Exit Display Mode</span>
          </button>
        </div>
      </div>

      {/* 2. Counter Display Grid */}
      <div style={{ flex: 1, display: 'grid', gridTemplateColumns: `repeat(${Math.min(3, Math.max(1, counters.length))}, 1fr)`, gap: '2rem', alignItems: 'stretch' }}>
        {counters.map(counter => {
          const call = activeCalls[counter.id];
          const isCalling = call?.state === 'CALLED';
          const isServing = call?.state === 'SERVING';

          return (
            <div
              key={counter.id}
              className={`tv-counter-card ${isCalling ? 'active-call' : ''}`}
              style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}
            >
              <div>
                <div style={{ fontSize: '1.1rem', fontWeight: 800, color: '#94a3b8', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
                  {counter.name}
                </div>
                <div style={{ fontSize: '0.85rem', color: '#64748b', marginTop: '0.25rem' }}>
                  Status: <span style={{ color: counter.status === 'ONLINE' ? '#34d399' : '#94a3b8', fontWeight: 700 }}>{counter.status}</span>
                </div>
              </div>

              <div style={{ padding: '2rem 0' }}>
                <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.9rem', color: isCalling ? '#fbbf24' : isServing ? '#34d399' : '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 700 }}>
                  {isCalling && <BellIcon size={16} />}
                  {isServing && <CheckCircleIcon size={16} />}
                  <span>{isCalling ? 'NOW CALLING' : isServing ? 'NOW SERVING' : 'WAITING FOR CITIZEN'}</span>
                </div>

                <div className={`tv-ticket-number ${isCalling ? 'calling' : ''}`}>
                  {call ? call.tokenNumber : '---'}
                </div>

                <div style={{ fontSize: '1.1rem', color: '#cbd5e1', fontWeight: 600 }}>
                  {call ? call.serviceTypeName : 'Counter Available'}
                </div>
              </div>

              <div style={{ borderTop: '1px solid #1e293b', paddingTop: '1rem', color: '#94a3b8', fontSize: '0.85rem' }}>
                {isCalling && (
                  <span style={{ color: '#fbbf24', fontWeight: 700 }}>
                    Please proceed to {counter.counterNumber} immediately
                  </span>
                )}
                {isServing && (
                  <span style={{ color: '#34d399', fontWeight: 600 }}>
                    Consultation in progress
                  </span>
                )}
                {!call && 'Next ticket will appear here'}
              </div>
            </div>
          );
        })}
      </div>

      {/* 3. Audio & Footer Marquee */}
      <div style={{ marginTop: '2rem', borderTop: '1px solid #1e293b', paddingTop: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.85rem', color: '#64748b' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <MegaphoneIcon size={16} />
          <span>Audio Chime Announcements Active • Keep your virtual or SMS ticket ready</span>
        </div>
        <div>
          Powered by SmartQueue Civic Service Engine
        </div>
      </div>
    </div>
  );
}
