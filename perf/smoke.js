// Smoke test — validação rápida pós-deploy
// Thresholds and scenario params from __ENV (nfr.yaml via nfr-to-env.py)
import http from 'k6/http';
import { check, sleep } from 'k6';

const VUS = __ENV.K6_SMOKE_VUS ? parseInt(__ENV.K6_SMOKE_VUS) : 1;
const DURATION = __ENV.K6_SMOKE_DURATION || '1m';
const THRESHOLD_FAILED = parseFloat(__ENV.K6_SMOKE_THRESHOLD_HTTP_REQ_FAILED_RATE || 0.05);
const THRESHOLD_P95 = parseInt(__ENV.K6_SMOKE_THRESHOLD_HTTP_REQ_DURATION_P95 || 1000);

export const options = {
  vus: VUS,
  duration: DURATION,
  thresholds: {
    http_req_failed: [`rate<${THRESHOLD_FAILED}`],
    http_req_duration: [`p(95)<${THRESHOLD_P95}`],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://catalogo.app.svc.cluster.local:8080';

export default function () {
  const health = http.get(`${BASE_URL}/health`);
  check(health, { 'health OK': (r) => r.status === 200 });

  const list = http.get(`${BASE_URL}/api/catalogo`);
  check(list, {
    'list status 200': (r) => r.status === 200,
    'list has items': (r) => {
      try { return JSON.parse(r.body).length > 0; } catch { return false; }
    },
  });

  const create = http.post(`${BASE_URL}/api/catalogo`, JSON.stringify({
    nome: 'Smoke Test Product',
    descricao: 'Created during smoke test',
    preco: 9.99,
    categoria: 'test',
    quantidadeEstoque: 1,
  }), { headers: { 'Content-Type': 'application/json' } });
  check(create, { 'create status 201': (r) => r.status === 201 });

  sleep(1);
}

// handleSummary: writes aggregated summary JSON at test end (replaces --summary-export,
// removed in k6 v0.48+). --out json writes NDJSON incrementally and gets extracted too early.
export function handleSummary(data) {
  const summary = { metrics: {} };
  for (const [name, m] of Object.entries(data.metrics)) {
    if (m && m.values) summary.metrics[name] = { values: m.values };
  }
  const json = JSON.stringify(summary);
  const outFile = __ENV.K6_SUMMARY_FILE || '/output/summary.json';
  return { stdout: json, [outFile]: json };
}
