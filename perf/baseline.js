// Baseline test — ramping VUs até 25 para validar SLAs críticos
// Thresholds and scenario params from __ENV (nfr.yaml via nfr-to-env.py)
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://catalogo.app.svc.cluster.local:8080';

// Métricas personalizadas
const catalogoListDuration = new Trend('catalogo_list_duration');
const catalogoGetDuration = new Trend('catalogo_get_duration');
const catalogoCreateDuration = new Trend('catalogo_create_duration');
const catalogoErrors = new Rate('catalogo_errors');

// Thresholds from nfr.yaml (via pre-processor)
const ERR_RATE = parseFloat(__ENV.K6_BASELINE_THRESHOLD_HTTP_REQ_FAILED || 0.01);
const P95_THRESH = parseInt(__ENV.K6_BASELINE_THRESHOLD_P95 || 300);
const P99_THRESH = parseInt(__ENV.K6_BASELINE_THRESHOLD_P99 || 800);
const THROUGHPUT_MIN = parseInt(__ENV.K6_BASELINE_THRESHOLD_THROUGHPUT || 50);
const BIZ_ERR_RATE = parseFloat(__ENV.K6_BASELINE_THRESHOLD_BUSINESS_ERRORS || 0.05);

function parseStages(envStr) {
  if (!envStr) return [
    { duration: '1m', target: 25 },
    { duration: '3m', target: 25 },
    { duration: '1m', target: 0 },
  ];
  try { return JSON.parse(envStr); } catch { return []; }
}

export const options = {
  thresholds: {
    http_req_failed: [`rate<${ERR_RATE}`],
    http_req_duration: [`p(95)<${P95_THRESH}`, `p(99)<${P99_THRESH}`],
    http_reqs: [`rate>=${THROUGHPUT_MIN}`],
    catalogo_errors: [`rate<${BIZ_ERR_RATE}`],
  },
  scenarios: {
    baseline: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: parseStages(__ENV.K6_BASELINE_STAGES),
      gracefulStop: '30s',
    },
  },
};

// IDs capturados para operações de GET
let productIds = [];

export default function () {
  const r = Math.random();

  // Listar produtos (70%)
  if (r < 0.70) {
    const listRes = http.get(`${BASE_URL}/api/catalogo`, { tags: { operation: 'list' } });
    catalogoListDuration.add(listRes.timings.duration);
    check(listRes, {
      'GET list 200': (r2) => r2.status === 200,
    }) || catalogoErrors.add(1);

    try {
      const ids = JSON.parse(listRes.body).map((p) => p.id);
      if (ids.length > 0) productIds = ids.slice(0, 5);
    } catch (e) { /* ignore */ }
  }
  // Buscar por ID (15%)
  else if (r < 0.85) {
    if (productIds.length > 0) {
      const id = productIds[Math.floor(Math.random() * productIds.length)];
      const getRes = http.get(`${BASE_URL}/api/catalogo/${id}`, { tags: { operation: 'get' } });
      catalogoGetDuration.add(getRes.timings.duration);
      check(getRes, {
        'GET id 200 or 404': (r2) => r2.status === 200 || r2.status === 404,
      }) || catalogoErrors.add(1);
    } else {
      const listRes = http.get(`${BASE_URL}/api/catalogo`, { tags: { operation: 'list' } });
      catalogoListDuration.add(listRes.timings.duration);
      check(listRes, { 'GET list fallback 200': (r2) => r2.status === 200 }) || catalogoErrors.add(1);
    }
  }
  // Criar produto (10%)
  else if (r < 0.95) {
    const createRes = http.post(`${BASE_URL}/api/catalogo`, JSON.stringify({
      nome: `Produto ${Date.now()}`,
      descricao: 'Produto de teste de carga',
      preco: Math.random() * 1000 + 10,
      categoria: 'Teste',
      quantidadeEstoque: Math.floor(Math.random() * 100),
    }), {
      headers: { 'Content-Type': 'application/json' },
      tags: { operation: 'create' },
    });
    catalogoCreateDuration.add(createRes.timings.duration);
    check(createRes, {
      'POST status 201': (r2) => r2.status === 201,
    }) || catalogoErrors.add(1);
  }
  // Health check (5%)
  else {
    http.get(`${BASE_URL}/health`, { tags: { operation: 'health' } });
  }

  sleep(0.5 + Math.random() * 1.5);
}
