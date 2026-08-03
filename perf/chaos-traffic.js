// Minimal traffic generator for chaos experiments — GET only, no writes.
import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
  vus: 1,
  duration: '1m',
  thresholds: {
    http_req_failed: ['rate<1.0'],
    http_req_duration: ['p(95)<5000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://catalogo.app.svc.cluster.local:8080';

export default function () {
  http.get(`${BASE_URL}/health`);
  http.get(`${BASE_URL}/api/catalogo`);
  sleep(1);
}
