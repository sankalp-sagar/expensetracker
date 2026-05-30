import { api } from "./api";

// All API methods returning the unwrapped data (since backend wraps in ApiResponse<T>).

export const usersApi = {
  me: () => api.get("/api/users/me").then((r) => r.data.data),
  update: (payload) => api.put("/api/users/me", payload).then((r) => r.data.data),
  search: (q) => api.get(`/api/users/search?q=${encodeURIComponent(q || "")}`).then((r) => r.data.data),
  lookup: (userIds = []) => {
    const ids = [...new Set(userIds.filter(Boolean))];
    if (ids.length === 0) return Promise.resolve([]);
    const query = ids.map((id) => `userIds=${encodeURIComponent(id)}`).join("&");
    return api.get(`/api/users/lookup?${query}`).then((r) => r.data.data);
  },
  listFriends: () => api.get("/api/users/friends").then((r) => r.data.data),
  pending: () => api.get("/api/users/friends/pending").then((r) => r.data.data),
  addFriend: (addresseeId) => api.post(`/api/users/friends/${addresseeId}`).then((r) => r.data.data),
};

export const groupsApi = {
  mine: () => api.get("/api/groups/me").then((r) => r.data.data),
  create: (payload) => api.post("/api/groups", payload).then((r) => r.data.data),
  get: (id) => api.get(`/api/groups/${id}`).then((r) => r.data.data),
  join: (code) => api.post(`/api/groups/join/${code}`).then((r) => r.data.data),
  addMember: (groupId, memberId) =>
    api.post(`/api/groups/${groupId}/members/${memberId}`).then((r) => r.data.data),
};

export const expensesApi = {
  create: (payload) => api.post("/api/expenses", payload).then((r) => r.data.data),
  mine: (page = 0, size = 20) => api.get(`/api/expenses/me?page=${page}&size=${size}`).then((r) => r.data.data),
  byGroup: (groupId, page = 0, size = 50) =>
    api.get(`/api/expenses/group/${groupId}?page=${page}&size=${size}`).then((r) => r.data.data),
  remove: (id) => api.delete(`/api/expenses/${id}`).then((r) => r.data.data),
  categories: () => api.get("/api/categories").then((r) => r.data.data),
  uploadReceipt: (expenseId, file) => {
    const fd = new FormData();
    fd.append("file", file);
    return api.post(`/api/receipts/${expenseId}`, fd, {
      headers: { "Content-Type": "multipart/form-data" },
    }).then((r) => r.data.data);
  },
};

export const settlementsApi = {
  record: (payload) => api.post("/api/settlements", payload).then((r) => r.data.data),
  myHistory: (page = 0) => api.get(`/api/settlements/me?page=${page}&size=20`).then((r) => r.data.data),
  groupHistory: (groupId) =>
    api.get(`/api/settlements/group/${groupId}?page=0&size=50`).then((r) => r.data.data),
  groupBalances: (groupId) => api.get(`/api/balances/group/${groupId}`).then((r) => r.data.data),
  suggestions: (groupId) =>
    api.get(`/api/balances/group/${groupId}/suggestions`).then((r) => r.data.data),
};

export const analyticsApi = {
  monthly: () => api.get("/api/analytics/monthly").then((r) => r.data.data),
  spent: (from, to) =>
    api.get(`/api/analytics/spent?from=${from}&to=${to}`).then((r) => r.data.data),
  contributions: (groupId) =>
    api.get(`/api/analytics/group/${groupId}/contributions`).then((r) => r.data.data),
};

export const notificationsApi = {
  list: () => api.get("/api/notifications").then((r) => r.data.data),
  unread: () => api.get("/api/notifications/unread-count").then((r) => r.data.data),
  markRead: (id) => api.post(`/api/notifications/${id}/read`).then((r) => r.data.data),
};
