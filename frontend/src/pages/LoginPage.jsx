import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { useAuth } from "@/lib/auth";

export default function LoginPage() {
  const { login, loading } = useAuth();
  const nav = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const submit = async (e) => {
    e.preventDefault();
    try {
      await login(email, password);
      toast.success("Welcome back");
      nav("/");
    } catch (err) {
      toast.error(err.response?.data?.message || "Login failed");
    }
  };

  return (
    <div className="min-h-screen grid grid-cols-1 md:grid-cols-2 bg-white">
      {/* Left: hero panel */}
      <div className="hidden md:flex relative bg-zinc-950 text-white p-12 flex-col justify-between overflow-hidden">
        <div>
          <div className="font-display font-black text-[28px] tracking-tight">
            expense<span className="text-[#3D7BFF]">tracker</span>
          </div>
          <div className="text-[11px] uppercase tracking-[0.2em] text-zinc-400 mt-1 font-mono">
            distributed expense settlement platform
          </div>
        </div>
        <div className="space-y-6 max-w-md">
          <h1 className="font-display font-black text-5xl leading-[0.95] tracking-tight">
            Split bills.<br/>Settle smartly.<br/>
            <span className="text-[#3D7BFF]">At any scale.</span>
          </h1>
          <p className="text-zinc-400 leading-relaxed text-sm">
            8 microservices · Spring Cloud Gateway · Kafka event-driven · PostgreSQL per service · debt minimisation algorithm · real-time balances over WebSocket.
          </p>
        </div>
        <div className="flex gap-6 font-mono text-[11px] text-zinc-500 uppercase tracking-wider">
          <div>java 21</div>
          <div>spring boot 3.4</div>
          <div>kafka</div>
          <div>postgres</div>
          <div>redis</div>
        </div>
      </div>

      {/* Right: form */}
      <div className="flex items-center justify-center p-8">
        <form onSubmit={submit} className="w-full max-w-sm space-y-6" data-testid="login-form">
          <div>
            <div className="text-xs uppercase tracking-[0.2em] text-zinc-500 font-mono mb-2">/login</div>
            <h2 className="font-display font-black text-3xl tracking-tight text-zinc-950">Sign in</h2>
          </div>

          <div className="space-y-3">
            <div>
              <label className="block text-xs uppercase tracking-wider text-zinc-500 mb-1.5">Email</label>
              <input
                data-testid="login-email-input"
                type="email"
                required
                autoFocus
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full px-3 py-2.5 border border-zinc-300 rounded-sm font-mono text-sm focus:outline-none focus:border-[#0055FF] focus:ring-1 focus:ring-[#0055FF]"
                placeholder="alice@example.com"
              />
            </div>
            <div>
              <label className="block text-xs uppercase tracking-wider text-zinc-500 mb-1.5">Password</label>
              <input
                data-testid="login-password-input"
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full px-3 py-2.5 border border-zinc-300 rounded-sm font-mono text-sm focus:outline-none focus:border-[#0055FF] focus:ring-1 focus:ring-[#0055FF]"
                placeholder="••••••••"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            data-testid="login-submit-button"
            className="w-full bg-[#0055FF] hover:bg-[#0044CC] text-white font-medium text-sm py-2.5 rounded-sm transition-all hover:-translate-y-[1px] disabled:opacity-50"
          >
            {loading ? "Signing in…" : "Sign in"}
          </button>

          {process.env.REACT_APP_GOOGLE_OAUTH_ENABLED === "true" && (
            <>
              <div className="relative my-2">
                <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-zinc-200" /></div>
                <div className="relative flex justify-center text-[10px] uppercase tracking-wider font-mono">
                  <span className="bg-white px-2 text-zinc-500">or</span>
                </div>
              </div>
              <a
                href={`${process.env.REACT_APP_API_BASE || "http://localhost:8080"}/oauth2/authorization/google`}
                data-testid="google-login-button"
                className="w-full block text-center bg-white border border-zinc-300 hover:bg-zinc-50 text-zinc-950 font-medium text-sm py-2.5 rounded-sm transition-all hover:-translate-y-[1px]"
              >
                Continue with Google
              </a>
            </>
          )}

          <div className="text-sm text-zinc-500">
            New here?{" "}
            <Link to="/register" data-testid="link-register" className="text-[#0055FF] hover:underline font-medium">
              Create an account
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}
