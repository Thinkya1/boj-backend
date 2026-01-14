import { useToastStore } from '../stores/toast';

const API_BASE = import.meta.env.VITE_API_BASE || '/api';

function buildQuery(params) {
  if (!params) {
    return '';
  }
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }
    if (Array.isArray(value)) {
      value.forEach((item) => search.append(key, item));
    } else {
      search.append(key, value);
    }
  });
  const query = search.toString();
  return query ? `?${query}` : '';
}

async function request(path, options = {}) {
  const { method = 'GET', params, data, headers, silent } = options;
  const url = `${API_BASE}${path}${buildQuery(params)}`;
  const fetchOptions = {
    method,
    credentials: 'include',
    headers: {
      ...(headers || {}),
    },
  };

  if (data !== undefined) {
    if (data instanceof FormData) {
      fetchOptions.body = data;
    } else {
      fetchOptions.headers['Content-Type'] = 'application/json';
      fetchOptions.body = JSON.stringify(data);
    }
  }

  try {
    const response = await fetch(url, fetchOptions);
    const payload = await response.json().catch(() => null);
    if (!response.ok) {
      throw new Error(payload?.message || `请求失败 (${response.status})`);
    }
    if (!payload) {
      throw new Error('响应解析失败');
    }
    if (payload.code !== 0) {
      throw new Error(payload.message || '请求失败');
    }
    return payload.data;
  } catch (error) {
    if (!silent) {
      const toast = useToastStore();
      toast.push(error.message || '请求异常', 'error');
    }
    throw error;
  }
}

export function get(path, options = {}) {
  return request(path, { ...options, method: 'GET' });
}

export function post(path, data, options = {}) {
  return request(path, { ...options, method: 'POST', data });
}

export function upload(path, formData, options = {}) {
  return request(path, { ...options, method: 'POST', data: formData });
}

export { request };
