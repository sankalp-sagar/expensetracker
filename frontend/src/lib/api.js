import axios from "axios";

// In the Emergent preview no Java backend is reachable, so we default to localhost:8080
// (where docker compose exposes the gateway). Override via REACT_APP_API_BASE.
const API_BASE = process.env.REACT_APP_API_BASE || "http://localhost:8080";

export const api = axios.create({
  baseURL: API_BASE,
  headers: { "Content-Type": "application/json" },
  timeout: 15000,
});

// Inject Bearer token + required X-User-Id header from stored auth state
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) config.headers.Authorization = `Bearer ${token}`;

  const storedUser = localStorage.getItem("user");
  if (storedUser) {
    try {
      const parsed = JSON.parse(storedUser);
      if (parsed?.userId) config.headers["X-User-Id"] = parsed.userId;
    } catch {
      // ignore malformed local user
    }
  }

  return config;
});

// On 401: try to refresh, else logout
let refreshing = null;
api.interceptors.response.use(
  (r) => r,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true;
      const refreshToken = localStorage.getItem("refreshToken");
      if (!refreshToken) {
        localStorage.clear();
        window.location.href = "/login";
        return Promise.reject(error);
      }
      try {
        refreshing = refreshing || axios.post(`${API_BASE}/api/auth/refresh`, { refreshToken });
        const { data } = await refreshing;
        refreshing = null;
        const t = data.data;
        localStorage.setItem("accessToken", t.accessToken);
        localStorage.setItem("refreshToken", t.refreshToken);
        original.headers.Authorization = `Bearer ${t.accessToken}`;
        return api(original);
      } catch (e) {
        refreshing = null;
        localStorage.clear();
        window.location.href = "/login";
        return Promise.reject(e);
      }
    }
    return Promise.reject(error);
  }
);

export const apiBase = API_BASE;
