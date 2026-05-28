import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { useAuth } from "@/lib/auth";

export default function RegisterPage() {
  const { register, loading } = useAuth();
  const nav = useNavigate();
  const [form, setForm] = useState({ email: "", password: "", fullName: "" });

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const submit = async (e) => {
    e.preventDefault();
    if (form.password.length < 8) return toast.error("Password must be ≥ 8 chars");
    try {
      await register(form.email, form.password, form.fullName);
      toast.success("Account created");
      nav("/");
    } catch (err) {
      toast.error(err.response?.data?.message || "Registration failed");
    }
  };

  return (
    <div className="min-h-screen grid grid-cols-1 md:grid-cols-2 bg-white">
      <div className="hidden md:flex relative bg-zinc-950 text-white p-12 flex-col justify-between">
        <div>
          <div className="font-display font-black text-[28px] tracking-tight">
            expense<span className="text-[#3D7BFF]">tracker</span>
          </div>
          <div className="text-[11px] uppercase tracking-[0.2em] text-zinc-400 mt-1 font-mono">
            distributed · production grade
          </div>
        </div>
        <div className="space-y-6 max-w-md">
          <h1 className="font-display font-black text-5xl leading-[0.95] tracking-tight">
            Stop chasing<br/>roommates for<br/><span className="text-[#3D7BFF]">$23.50.</span>
          </h1>
          <p className="text-zinc-400 leading-relaxed text-sm">
            Track shared expenses, settle smartly with auto-generated minimum-transaction plans, and never lose track again.
          </p>
        </div>
        <div className="font-mono text-[11px] text-zinc-500 uppercase tracking-wider">
          jwt · refresh-rotation · bcrypt-12
        </div>
      </div>

      <div className="flex items-center justify-center p-8">
        <form onSubmit={submit} className="w-full max-w-sm space-y-6" data-testid="register-form">
          <div>
            <div className="text-xs uppercase tracking-[0.2em] text-zinc-500 font-mono mb-2">/register</div>
            <h2 className="font-display font-black text-3xl tracking-tight text-zinc-950">Create an account</h2>
          </div>

          <div className="space-y-3">
            <div>
              <label className="block text-xs uppercase tracking-wider text-zinc-500 mb-1.5">Full name</label>
              <input
                data-testid="register-fullname-input"
                required minLength={2}
                value={form.fullName}
                onChange={(e) => set("fullName", e.target.value)}
                className="w-full px-3 py-2.5 border border-zinc-300 rounded-sm text-sm focus:outline-none focus:border-[#0055FF] focus:ring-1 focus:ring-[#0055FF]"
                placeholder="Alice Smith"
              />
            </div>
            <div>
              <label className="block text-xs uppercase tracking-wider text-zinc-500 mb-1.5">Email</label>
              <input
                data-testid="register-email-input"
                type="email" required
                value={form.email}
                onChange={(e) => set("email", e.target.value)}
                className="w-full px-3 py-2.5 border border-zinc-300 rounded-sm font-mono text-sm focus:outline-none focus:border-[#0055FF] focus:ring-1 focus:ring-[#0055FF]"
                placeholder="alice@example.com"
              />
            </div>
            <div>
              <label className="block text-xs uppercase tracking-wider text-zinc-500 mb-1.5">Password</label>
              <input
                data-testid="register-password-input"
                type="password" required minLength={8}
                value={form.password}
                onChange={(e) => set("password", e.target.value)}
                className="w-full px-3 py-2.5 border border-zinc-300 rounded-sm font-mono text-sm focus:outline-none focus:border-[#0055FF] focus:ring-1 focus:ring-[#0055FF]"
                placeholder="≥ 8 characters"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            data-testid="register-submit-button"
            className="w-full bg-[#0055FF] hover:bg-[#0044CC] text-white font-medium text-sm py-2.5 rounded-sm transition-all hover:-translate-y-[1px] disabled:opacity-50"
          >
            {loading ? "Creating…" : "Create account"}
          </button>

          <div className="text-sm text-zinc-500">
            Already have an account?{" "}
            <Link to="/login" data-testid="link-login" className="text-[#0055FF] hover:underline font-medium">
              Sign in
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}
