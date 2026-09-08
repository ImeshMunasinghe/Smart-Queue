import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import CitizenPortal from './components/CitizenPortal';
import OperatorTerminal from './components/OperatorTerminal';
import PublicDisplayBoard from './components/PublicDisplayBoard';
import AdminConsole from './components/AdminConsole';
import SmsSandbox from './components/SmsSandbox';

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

  // Load Offices
  useEffect(() => {
    fetch(`${API_BASE}/admin/offices`)
      .then(res => res.json())
      .then(data => {
        setOffices(data);
        if (data.length > 0) {
          const defaultOfficeId = data[0].id;
          setSelectedOfficeId(defaultOfficeId);
          loadOfficeData(defaultOfficeId);
        }
      })
      .catch(err => console.error('Error fetching offices:', err));
  }, []);

  const loadOfficeData = (officeId) => {
    if (!officeId) return;

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

    fetch(`${API_BASE}/admin/offices/${officeId}/slots`)
      .then(res => res.json())
      .then(setSlots)
      .catch(() => setSlots([]));

    fetch(`${API_BASE}/admin/offices/${officeId}/analytics`)
      .then(res => res.json())
      .then(setAnalytics)
      .catch(() => setAnalytics(null));
  };

  const loadCounterQueue = (counterId) => {
    if (!counterId) return;
    fetch(`${API_BASE}/operator/counters/${counterId}/queue`)
      .then(res => res.json())
      .then(setUpcomingQueue)
      .catch(() => setUpcomingQueue([]));
  };

  // Real-time SSE Stream for Active Citizen Token
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

  // Full-Screen TV Waiting Hall Display Mode
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
      <Navbar activeRole={activeRole} setActiveRole={setActiveRole} />

      <main className="page-container">
        {activeRole === 'citizen' && (
          <CitizenPortal
            serviceTypes={serviceTypes}
            selectedOfficeId={selectedOfficeId}
            activeToken={activeToken}
            setActiveToken={setActiveToken}
            refreshOfficeData={() => loadOfficeData(selectedOfficeId)}
            apiBase={API_BASE}
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
          />
        )}

        {activeRole === 'sms' && (
          <SmsSandbox apiBase={API_BASE} />
        )}
      </main>
    </div>
  );
}
