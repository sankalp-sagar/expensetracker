import { createContext, useContext, useState } from "react";
import { api } from "./api";

const AuthContext = createContext(null);

function decodeJwtPayload(token) {
  const [, payload] = token.split(".");
  if (!payload) throw new Error("Invalid access token");

  const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(normalized.length + (4 - normalized.length % 4) % 4, "=");
  const json = decodeURIComponent(
    atob(padded)
      .split("")
      .map((char) => `%${(`00${char.charCodeAt(0).toString(16)}`).slice(-2)}`)
      .join(""),
  );

  return JSON.parse(json);
}

function userFromAccessToken(accessToken, fallbackUserId) {
  const payload = decodeJwtPayload(accessToken);
  const userId = payload.sub || fallbackUserId;
  if (!userId) throw new Error("Access token is missing subject");

  return {
    userId,
    email: payload.email || "",
    fullName: payload.fullName || payload.name || payload.email || "",
    roles: Array.isArray(payload.roles) ? payload.roles : [],
  };
}

function getStoredUser() {
  const accessToken = localStorage.getItem("accessToken");
  const rawUser = localStorage.getItem("user");

  if (accessToken) {
    try {
      const tokenUser = userFromAccessToken(accessToken);
      let storedUser = null;

      if (rawUser) {
        try {
          storedUser = JSON.parse(rawUser);
        } catch {
          localStorage.removeItem("user");
        }
      }

      const user = storedUser?.userId === tokenUser.userId
        ? { ...tokenUser, ...storedUser }
        : tokenUser;
      localStorage.setItem("user", JSON.stringify(user));
      return user;
    } catch {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("user");
      return null;
    }
  }

  if (rawUser) {
    try {
      const storedUser = JSON.parse(rawUser);
      if (storedUser?.email) return storedUser;
    } catch {
      localStorage.removeItem("user");
    }
  }

  return null;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getStoredUser);
  const [loading, setLoading] = useState(false);

  const storeSession = (tokenResponse) => {
    localStorage.setItem("accessToken", tokenResponse.accessToken);
    localStorage.setItem("refreshToken", tokenResponse.refreshToken);
    const nextUser = {
      userId: tokenResponse.userId,
      email: tokenResponse.email,
      fullName: tokenResponse.fullName,
      roles: tokenResponse.roles || [],
    };
    localStorage.setItem("user", JSON.stringify(nextUser));
    setUser(nextUser);
    return nextUser;
  };

  const completeOAuthLogin = (accessToken, refreshToken, fallbackUserId) => {
    const nextUser = userFromAccessToken(accessToken, fallbackUserId);
    localStorage.setItem("accessToken", accessToken);
    if (refreshToken) {
      localStorage.setItem("refreshToken", refreshToken);
    } else {
      localStorage.removeItem("refreshToken");
    }
    localStorage.setItem("user", JSON.stringify(nextUser));
    setUser(nextUser);
    return nextUser;
  };

  const login = async (email, password) => {
    setLoading(true);
    try {
      const { data } = await api.post("/api/auth/login", { email, password });
      return storeSession(data.data);
    } finally {
      setLoading(false);
    }
  };

  const register = async (email, password, fullName) => {
    setLoading(true);
    try {
      const { data } = await api.post("/api/auth/register", { email, password, fullName });
      return storeSession(data.data);
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
    <AuthContext.Provider value={{ user, loading, login, register, completeOAuthLogin, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
