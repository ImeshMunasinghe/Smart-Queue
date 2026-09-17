import React, { useEffect, useRef, useState } from 'react';

/* ─── Color palette (matches index.css design tokens) ─────────────────────── */
const SERVICE_COLORS = [
  '#2563eb', // Civic Cobalt (primary)
  '#059669', // Medical Emerald (success)
  '#d97706', // Warm Amber
  '#7c3aed', // Purple
  '#dc2626', // Crimson
  '#0891b2', // Cyan
];

const PEAK_HOURS = new Set([9, 10, 11, 13, 14, 15]); // shaded band on hourly chart

/* ─── Utility helpers ──────────────────────────────────────────────────────── */
function formatHour(h) {
  if (h === 0) return '12am';
  if (h < 12) return `${h}am`;
  if (h === 12) return '12pm';
  return `${h - 12}pm`;
}

function formatDateShort(dateStr) {
  // "2026-09-17" → "Sep 17"
  const d = new Date(dateStr + 'T00:00:00');
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

/* ─── Bar Chart: Daily Token Volume (last 7 days) ─────────────────────────── */
function DailyVolumeChart({ data, serviceTypes }) {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const W = canvas.offsetWidth;
    const H = canvas.offsetHeight;
    canvas.width  = W * dpr;
    canvas.height = H * dpr;
    ctx.scale(dpr, dpr);

    ctx.clearRect(0, 0, W, H);

    if (!data || data.length === 0) {
      ctx.fillStyle = '#94a3b8';
      ctx.font = '14px Inter, sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('No data yet — issue tokens to see the chart populate.', W / 2, H / 2);
      return;
    }

    // Build unique sorted dates and service types from data
    const dates = [...new Set(data.map(d => d.date))].sort();
    const services = [...new Set(data.map(d => d.serviceTypeId))];

    // Map: date → { serviceTypeId → count }
    const matrix = {};
    dates.forEach(dt => { matrix[dt] = {}; });
    data.forEach(row => {
      if (matrix[row.date]) matrix[row.date][row.serviceTypeId] = Number(row.count);
    });

    // Build stacked max for Y axis
    const stackedTotals = dates.map(dt =>
      services.reduce((sum, sid) => sum + (matrix[dt][sid] || 0), 0)
    );
    const maxVal = Math.max(...stackedTotals, 1);

    const PAD_L = 44, PAD_R = 16, PAD_T = 16, PAD_B = 48;
    const chartW = W - PAD_L - PAD_R;
    const chartH = H - PAD_T - PAD_B;
    const groupW = chartW / dates.length;
    const barW = Math.max(12, Math.min(40, groupW * 0.55));

    // Grid lines
    const gridLines = 5;
    ctx.strokeStyle = '#e2e8f0';
    ctx.lineWidth = 1;
    ctx.setLineDash([4, 4]);
    for (let i = 0; i <= gridLines; i++) {
      const y = PAD_T + chartH - (i / gridLines) * chartH;
      ctx.beginPath();
      ctx.moveTo(PAD_L, y);
      ctx.lineTo(W - PAD_R, y);
      ctx.stroke();
      // Y-axis labels
      ctx.fillStyle = '#94a3b8';
      ctx.font = '11px Inter, sans-serif';
      ctx.textAlign = 'right';
      ctx.fillText(Math.round((i / gridLines) * maxVal), PAD_L - 6, y + 4);
    }
    ctx.setLineDash([]);

    // Stacked bars
    dates.forEach((dt, di) => {
      const cx = PAD_L + di * groupW + groupW / 2;
      let yBottom = PAD_T + chartH;

      services.forEach((sid, si) => {
        const val = matrix[dt][sid] || 0;
        const barH = (val / maxVal) * chartH;
        if (barH < 1) return;

        const x = cx - barW / 2;
        const y = yBottom - barH;
        const color = SERVICE_COLORS[si % SERVICE_COLORS.length];

        // Draw bar with rounded top on topmost segment
        const isTop = si === services.length - 1 || services.slice(si + 1).every(s => !(matrix[dt][s] > 0));
        ctx.fillStyle = color;
        if (isTop && barH > 6) {
          const r = 4;
          ctx.beginPath();
          ctx.moveTo(x + r, y);
          ctx.lineTo(x + barW - r, y);
          ctx.arcTo(x + barW, y, x + barW, y + r, r);
          ctx.lineTo(x + barW, y + barH);
          ctx.lineTo(x, y + barH);
          ctx.lineTo(x, y + r);
          ctx.arcTo(x, y, x + r, y, r);
          ctx.closePath();
          ctx.fill();
        } else {
          ctx.fillRect(x, y, barW, barH);
        }
        yBottom -= barH;
      });

      // X-axis date label
      ctx.fillStyle = '#64748b';
      ctx.font = '11px Inter, sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(formatDateShort(dt), cx, H - PAD_B + 18);
    });

    // X-axis baseline
    ctx.strokeStyle = '#cbd5e1';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(PAD_L, PAD_T + chartH);
    ctx.lineTo(W - PAD_R, PAD_T + chartH);
    ctx.stroke();

  }, [data, serviceTypes]);

  return (
    <div style={{ position: 'relative', width: '100%', height: '220px' }}>
      <canvas ref={canvasRef} style={{ width: '100%', height: '100%', display: 'block' }} />
    </div>
  );
}

/* ─── Line Chart: Hourly Avg Wait Time (0–23h) ───────────────────────────── */
function HourlyWaitChart({ data }) {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const W = canvas.offsetWidth;
    const H = canvas.offsetHeight;
    canvas.width  = W * dpr;
    canvas.height = H * dpr;
    ctx.scale(dpr, dpr);

    ctx.clearRect(0, 0, W, H);

    // Build full 0–23 hour array (fill gaps with 0)
    const hourMap = {};
    (data || []).forEach(row => { hourMap[row.hour] = row.avgDurationSeconds; });
    const hours  = Array.from({ length: 24 }, (_, i) => i);
    const values = hours.map(h => (hourMap[h] || 0) / 60); // convert to minutes

    const maxVal = Math.max(...values, 1);
    const PAD_L = 48, PAD_R = 16, PAD_T = 20, PAD_B = 44;
    const chartW = W - PAD_L - PAD_R;
    const chartH = H - PAD_T - PAD_B;

    // Shaded peak-hour band
    ctx.fillStyle = 'rgba(217, 119, 6, 0.07)';
    [[9, 11], [13, 15]].forEach(([start, end]) => {
      const xStart = PAD_L + (start / 23) * chartW;
      const xEnd   = PAD_L + ((end + 1) / 23) * chartW;
      ctx.fillRect(xStart, PAD_T, xEnd - xStart, chartH);
    });

    // Grid lines
    const gridLines = 4;
    ctx.strokeStyle = '#e2e8f0';
    ctx.lineWidth = 1;
    ctx.setLineDash([4, 4]);
    for (let i = 0; i <= gridLines; i++) {
      const y = PAD_T + chartH - (i / gridLines) * chartH;
      ctx.beginPath();
      ctx.moveTo(PAD_L, y);
      ctx.lineTo(W - PAD_R, y);
      ctx.stroke();
      ctx.fillStyle = '#94a3b8';
      ctx.font = '11px Inter, sans-serif';
      ctx.textAlign = 'right';
      ctx.fillText(`${Math.round((i / gridLines) * maxVal)}m`, PAD_L - 6, y + 4);
    }
    ctx.setLineDash([]);

    // Gradient fill below line
    const gradient = ctx.createLinearGradient(0, PAD_T, 0, PAD_T + chartH);
    gradient.addColorStop(0, 'rgba(37, 99, 235, 0.2)');
    gradient.addColorStop(1, 'rgba(37, 99, 235, 0)');

    const xOf = (i) => PAD_L + (i / 23) * chartW;
    const yOf = (v) => PAD_T + chartH - (v / maxVal) * chartH;

    ctx.beginPath();
    ctx.moveTo(xOf(0), yOf(values[0]));
    for (let i = 1; i < 24; i++) {
      // Smooth cubic bezier
      const cp1x = xOf(i - 0.5);
      const cp2x = xOf(i - 0.5);
      ctx.bezierCurveTo(cp1x, yOf(values[i - 1]), cp2x, yOf(values[i]), xOf(i), yOf(values[i]));
    }
    ctx.lineTo(xOf(23), PAD_T + chartH);
    ctx.lineTo(xOf(0),  PAD_T + chartH);
    ctx.closePath();
    ctx.fillStyle = gradient;
    ctx.fill();

    // Main line
    ctx.beginPath();
    ctx.strokeStyle = '#2563eb';
    ctx.lineWidth = 2.5;
    ctx.lineJoin = 'round';
    ctx.moveTo(xOf(0), yOf(values[0]));
    for (let i = 1; i < 24; i++) {
      ctx.bezierCurveTo(xOf(i - 0.5), yOf(values[i - 1]), xOf(i - 0.5), yOf(values[i]), xOf(i), yOf(values[i]));
    }
    ctx.stroke();

    // Dots at data points (only where we have real data)
    hours.forEach(h => {
      if (!hourMap[h]) return;
      const x = xOf(h), y = yOf(values[h]);
      ctx.beginPath();
      ctx.arc(x, y, 4, 0, Math.PI * 2);
      ctx.fillStyle = '#2563eb';
      ctx.fill();
      ctx.strokeStyle = '#fff';
      ctx.lineWidth = 2;
      ctx.stroke();
    });

    // X-axis hour labels (every 3 hours)
    for (let h = 0; h < 24; h += 3) {
      ctx.fillStyle = PEAK_HOURS.has(h) ? '#d97706' : '#94a3b8';
      ctx.font = '11px Inter, sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(formatHour(h), xOf(h), H - PAD_B + 18);
    }

    // Baseline
    ctx.strokeStyle = '#cbd5e1';
    ctx.lineWidth = 1.5;
    ctx.setLineDash([]);
    ctx.beginPath();
    ctx.moveTo(PAD_L, PAD_T + chartH);
    ctx.lineTo(W - PAD_R, PAD_T + chartH);
    ctx.stroke();

    // Peak hour annotation
    ctx.fillStyle = '#d97706';
    ctx.font = '10px Inter, sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText('⚡ Peak hours', xOf(9) + 4, PAD_T + 14);

  }, [data]);

  return (
    <div style={{ position: 'relative', width: '100%', height: '220px' }}>
      <canvas ref={canvasRef} style={{ width: '100%', height: '100%', display: 'block' }} />
    </div>
  );
}

/* ─── Legend ──────────────────────────────────────────────────────────────── */
function ChartLegend({ items }) {
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem 1.25rem', marginTop: '0.75rem' }}>
      {items.map((item, i) => (
        <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
          <span style={{ width: '10px', height: '10px', borderRadius: '2px', background: SERVICE_COLORS[i % SERVICE_COLORS.length], flexShrink: 0 }} />
          {item}
        </div>
      ))}
    </div>
  );
}

/* ─── Main export ─────────────────────────────────────────────────────────── */
export default function AnalyticsCharts({ selectedOfficeId, serviceTypes, apiBase, authToken }) {
  const [dailyData,  setDailyData]  = useState([]);
  const [hourlyData, setHourlyData] = useState([]);
  const [loading,    setLoading]    = useState(true);
  const [days,       setDays]       = useState(7);

  useEffect(() => {
    if (!selectedOfficeId) return;
    setLoading(true);

    const headers = authToken ? { Authorization: `Bearer ${authToken}` } : {};

    Promise.all([
      fetch(`${apiBase}/admin/offices/${selectedOfficeId}/analytics/daily-volume?days=${days}`, { headers })
        .then(r => r.ok ? r.json() : []),
      fetch(`${apiBase}/admin/offices/${selectedOfficeId}/analytics/hourly-wait?days=${days}`, { headers })
        .then(r => r.ok ? r.json() : []),
    ]).then(([daily, hourly]) => {
      setDailyData(daily);
      setHourlyData(hourly);
    }).catch(() => {
      setDailyData([]);
      setHourlyData([]);
    }).finally(() => setLoading(false));
  }, [selectedOfficeId, days, apiBase, authToken]);

  const serviceNames = [...new Set(dailyData.map(d => d.serviceTypeName))];

  return (
    <div style={{ marginTop: '1.5rem', display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '1.5rem' }}>
      {/* ── Chart 1: Daily Volume ── */}
      <div className="civic-card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
          <div>
            <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-main)', fontFamily: 'var(--font-display)' }}>
              Daily Token Volume
            </h3>
            <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '0.1rem' }}>
              Issuances per service type
            </p>
          </div>
          <select
            value={days}
            onChange={e => setDays(Number(e.target.value))}
            style={{
              fontSize: '0.78rem', padding: '0.25rem 0.5rem', border: '1px solid var(--border-medium)',
              borderRadius: 'var(--radius-sm)', background: 'var(--bg-subtle)', color: 'var(--text-secondary)',
              cursor: 'pointer',
            }}
          >
            <option value={7}>Last 7 days</option>
            <option value={14}>Last 14 days</option>
            <option value={30}>Last 30 days</option>
          </select>
        </div>

        {loading
          ? <div style={{ height: '220px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>Loading…</div>
          : <DailyVolumeChart data={dailyData} serviceTypes={serviceTypes} />
        }
        {serviceNames.length > 0 && <ChartLegend items={serviceNames} />}
      </div>

      {/* ── Chart 2: Hourly Wait Time ── */}
      <div className="civic-card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
          <div>
            <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-main)', fontFamily: 'var(--font-display)' }}>
              Avg Service Duration by Hour
            </h3>
            <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '0.1rem' }}>
              Minutes per consultation window
            </p>
          </div>
          <div style={{
            display: 'flex', alignItems: 'center', gap: '0.4rem',
            fontSize: '0.75rem', color: 'var(--warning)', fontWeight: 600,
            background: 'var(--warning-subtle)', border: '1px solid var(--warning-border)',
            padding: '0.2rem 0.55rem', borderRadius: 'var(--radius-full)',
          }}>
            <span>⚡</span> Peak bands shaded
          </div>
        </div>

        {loading
          ? <div style={{ height: '220px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>Loading…</div>
          : <HourlyWaitChart data={hourlyData} />
        }

        <div style={{ display: 'flex', gap: '1rem', marginTop: '0.75rem', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.78rem', color: 'var(--text-muted)' }}>
            <span style={{ width: '16px', height: '3px', background: '#2563eb', display: 'inline-block', borderRadius: '2px' }} />
            Avg duration (mins)
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.78rem', color: 'var(--text-muted)' }}>
            <span style={{ width: '14px', height: '10px', background: 'rgba(217,119,6,0.18)', display: 'inline-block', borderRadius: '2px' }} />
            Peak hours
          </div>
        </div>
      </div>

      {/* Responsive: collapse to single column below 860px (via global grid media query) */}
      <style>{`
        @media (max-width: 860px) {
          .analytics-grid { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
}
