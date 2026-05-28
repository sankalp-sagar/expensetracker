import { createContext, useContext, useEffect, useState } from "react";
import { api } from "./api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem("user");
    return raw ? JSON.parse(raw) : null;
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (user) localStorage.setItem("user", JSON.stringify(user));
    else localStorage.removeItem("user");
  }, [user]);

  const login = async (email, password) => {
    setLoading(true);
    try {
      const { data } = await api.post("/api/auth/login", { email, password });
      const t = data.data;
      localStorage.setItem("accessToken", t.accessToken);
      localStorage.setItem("refreshToken", t.refreshToken);
      const u = { userId: t.userId, email: t.email, fullName: t.fullName, roles: t.roles };
      setUser(u);
      return u;
    } finally {
      setLoading(false);
    }
  };

  const register = async (email, password, fullName) => {
    setLoading(true);
    try {
      const { data } = await api.post("/api/auth/register", { email, password, fullName });
      const t = data.data;
      localStorage.setItem("accessToken", t.accessToken);
      localStorage.setItem("refreshToken", t.refreshToken);
      const u = { userId: t.userId, email: t.email, fullName: t.fullName, roles: t.roles };
      setUser(u);
      return u;
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    try { await api.post("/api/auth/logout"); } catch (e) { /* ignore */ }
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
