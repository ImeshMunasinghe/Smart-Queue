import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import CitizenPortal from './components/CitizenPortal';
import OperatorTerminal from './components/OperatorTerminal';
import PublicDisplayBoard from './components/PublicDisplayBoard';
import AdminConsole from './components/AdminConsole';
import SmsSandbox from './components/SmsSandbox';
import LoginModal from './components/LoginModal';

const API_BASE = '/api/v1';

// Synthesizes a clean two-tone chime via Web Audio API (D5 -> A5)
function playChime() {
  try {
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(587.33, audioCtx.currentTime); // D5
    osc.frequency.exponentialRampToValueAtTime(880, audioCtx.currentTime + 0.15); // A5
    gain.gain.setValueAtTime(0.25, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.6);
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.start();
    osc.stop(audioCtx.currentTime + 0.6);
  } catch (e) {
    console.log('Audio chime not available:', e);
  }
}

export default function App() {
  const [activeRole, setActiveRole] = useState('citizen'); // 'citizen' | 'operator' | 'admin' | 'sms' | 'display'

  // ── Auth state ──────────────────────────────────────────────────────────────
  const [authToken,   setAuthToken]   = useState(() => sessionStorage.getItem('sq_token') || null);
  const [authUsername, setAuthUsername] = useState(() => sessionStorage.getItem('sq_username') || null);
  const [authRole,    setAuthRole]    = useState(() => sessionStorage.getItem('sq_role') || null);

  // Which tab is pending login (null = no modal open)
  const [pendingRole, setPendingRole] = useState(null);

  // Master Office & Counter Data
  const [offices, setOffices] = useState([]);
  const [selectedOfficeId, setSelectedOfficeId] = useState('');
  const [serviceTypes, setServiceTypes] = useState([]);
  const [counters, setCounters] = useState([]);
  const [slots, setSlots] = useState([]);
  const [analytics, setAnalytics] = useState(null);

  // Active Citizen Token
  const [activeToken, setActiveToken] = useState(null);

  // Operator Active State
  const [selectedCounterId, setSelectedCounterId] = useState('');
  const [currentServingToken, setCurrentServingToken] = useState(null);
  const [upcomingQueue, setUpcomingQueue] = useState([]);

  const [backendConnected, setBackendConnected] = useState(null);

  // ── Auth helpers ─────────────────────────────────────────────────────────────
  const handleLoginSuccess = ({ token, username, role }) => {
    sessionStorage.setItem('sq_token', token);
    sessionStorage.setItem('sq_username', username);
    sessionStorage.setItem('sq_role', role);
    setAuthToken(token);
    setAuthUsername(username);
    setAuthRole(role);
    // Reload office data with the new token so admin/slots/analytics fetch correctly
    if (selectedOfficeId) loadOfficeData(selectedOfficeId, token);
    // Navigate to the tab that triggered the login
    if (pendingRole) setActiveRole(pendingRole);
    setPendingRole(null);
  };

  const handleLogout = () => {
    sessionStorage.removeItem('sq_token');
    sessionStorage.removeItem('sq_username');
    sessionStorage.removeItem('sq_role');
    setAuthToken(null);
    setAuthUsername(null);
    setAuthRole(null);
    setActiveRole('citizen');
  };

  /**
   * Returns an Authorization header object when a token is present.
   * Used for all protected API fetches.
   */
  const authHeaders = () =>
    authToken ? { Authorization: `Bearer ${authToken}` } : {};

  /**
   * Intercepts tab changes for protected roles.
   * If the user is not authenticated (or has wrong role), show the login modal.
   */
  const handleRoleChange = (role) => {
    if (role === 'operator') {
      const hasOperatorOrAdmin = authRole === 'ROLE_OPERATOR' || authRole === 'ROLE_ADMIN';
      if (!authToken || !hasOperatorOrAdmin) {
        setPendingRole('operator');
        return;
      }
    }
    if (role === 'admin') {
      if (!authToken || authRole !== 'ROLE_ADMIN') {
        setPendingRole('admin');
        return;
      }
    }
    setActiveRole(role);
  };

  // ── Load Offices with auto-retry if backend is booting ─────────────────────
  useEffect(() => {
    let cancelled = false;

    const fetchOffices = () => {
      fetch(`${API_BASE}/admin/offices`)
        .then(res => {
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          return res.json();
        })
        .then(data => {
          if (cancelled) return;
          if (Array.isArray(data) && data.length > 0) {
            setOffices(data);
            setBackendConnected(true);
            setSelectedOfficeId(prev => {
              const officeId = prev || data[0].id;
              loadOfficeData(officeId);
              return officeId;
            });
          }
        })
        .catch(() => {
          if (cancelled) return;
          setBackendConnected(false);
        });
    };

    fetchOffices();
    const timer = setInterval(() => {
      if (!backendConnected) fetchOffices();
    }, 3500);

    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [backendConnected]);

  const loadOfficeData = (officeId, token) => {
    if (!officeId) return;
    // Use passed token OR fall back to current state (for scheduled refresh calls)
    const hdrs = (token || authToken) ? { Authorization: `Bearer ${token || authToken}` } : {};

    // Public endpoints — no auth needed
    fetch(`${API_BASE}/admin/offices/${officeId}/service-types`)
      .then(res => res.json())
      .then(setServiceTypes)
      .catch(() => setServiceTypes([]));

    fetch(`${API_BASE}/operator/offices/${officeId}/counters`)
      .then(res => res.json())
      .then(list => {
        setCounters(list);
        if (list.length > 0 && !selectedCounterId) {
          setSelectedCounterId(list[0].id);
          loadCounterQueue(list[0].id);
        }
      })
      .catch(() => setCounters([]));

    // Admin-protected endpoints — require ROLE_ADMIN token
    fetch(`${API_BASE}/admin/offices/${officeId}/slots`, { headers: hdrs })
      .then(res => res.ok ? res.json() : [])
      .then(setSlots)
      .catch(() => setSlots([]));

    fetch(`${API_BASE}/admin/offices/${officeId}/analytics`, { headers: hdrs })
      .then(res => res.ok ? res.json() : null)
      .then(setAnalytics)
      .catch(() => setAnalytics(null));
  };

  const loadCounterQueue = (counterId) => {
    if (!counterId) return;
    fetch(`${API_BASE}/operator/counters/${counterId}/queue`, { headers: authHeaders() })
      .then(res => res.ok ? res.json() : [])
      .then(data => setUpcomingQueue(Array.isArray(data) ? data : []))
      .catch(() => setUpcomingQueue([]));
  };

  // ── Real-time SSE Stream for Active Citizen Token ──────────────────────────
  useEffect(() => {
    if (!activeToken?.id) return;
    const sse = new EventSource(`${API_BASE}/tokens/${activeToken.id}/stream`);

    sse.addEventListener('state_change', (e) => {
      try {
        const updated = JSON.parse(e.data);
        setActiveToken(updated);
        if (updated.state === 'CALLED') {
          playChime();
        }
      } catch (err) {
        console.error('SSE JSON error', err);
      }
    });

    return () => sse.close();
  }, [activeToken?.id]);

  // ── Auto-refresh admin/operator data every 15 seconds while logged in ───────
  useEffect(() => {
    if (!authToken || !selectedOfficeId) return;
    const interval = setInterval(() => {
      loadOfficeData(selectedOfficeId);
      if (selectedCounterId) loadCounterQueue(selectedCounterId);
    }, 15000);
    return () => clearInterval(interval);
  }, [authToken, selectedOfficeId, selectedCounterId]);



  // ── Full-Screen TV Waiting Hall Display Mode ───────────────────────────────
  if (activeRole === 'display') {
    return (
      <PublicDisplayBoard
        counters={counters}
        offices={offices}
        selectedOfficeId={selectedOfficeId}
        onExit={() => setActiveRole('citizen')}
        playChime={playChime}
        apiBase={API_BASE}
      />
    );
  }

  return (
    <div>
      {/* Login modal (shown when accessing protected tab without token) */}
      {pendingRole && (
        <LoginModal
          targetRole={pendingRole}
          onSuccess={handleLoginSuccess}
          onClose={() => setPendingRole(null)}
          apiBase={API_BASE}
        />
      )}

      <Navbar
        activeRole={activeRole}
        setActiveRole={handleRoleChange}
        authUsername={authUsername}
        authRole={authRole}
        onLogout={handleLogout}
      />

      <main className="page-container">
        {activeRole === 'citizen' && (
          <CitizenPortal
            serviceTypes={serviceTypes}
            selectedOfficeId={selectedOfficeId}
            activeToken={activeToken}
            setActiveToken={setActiveToken}
            refreshOfficeData={() => loadOfficeData(selectedOfficeId)}
            apiBase={API_BASE}
            backendConnected={backendConnected}
          />
        )}

        {activeRole === 'operator' && (
          <OperatorTerminal
            counters={counters}
            selectedCounterId={selectedCounterId}
            setSelectedCounterId={setSelectedCounterId}
            currentServingToken={currentServingToken}
            setCurrentServingToken={setCurrentServingToken}
            upcomingQueue={upcomingQueue}
            loadCounterQueue={loadCounterQueue}
            refreshOfficeData={() => loadOfficeData(selectedOfficeId)}
            apiBase={API_BASE}
            playChime={playChime}
            authToken={authToken}
          />
        )}

        {activeRole === 'admin' && (
          <AdminConsole
            slots={slots}
            analytics={analytics}
            serviceTypes={serviceTypes}
            counters={counters}
            selectedOfficeId={selectedOfficeId}
            refreshOfficeData={() => loadOfficeData(selectedOfficeId)}
            apiBase={API_BASE}
            authToken={authToken}
          />
        )}

        {activeRole === 'sms' && (
          <SmsSandbox apiBase={API_BASE} />
        )}
      </main>
    </div>
  );
}
