import { useEffect, useState } from "react";
import { toast } from "sonner";
import { usersApi } from "@/lib/services";

export default function SettingsPage() {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({
    statusMessage: "", phone: "",
    preferredCurrency: "USD", preferredLanguage: "en", privacy: "PUBLIC",
  });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const p = await usersApi.me();
        setProfile(p);
        setForm({
          statusMessage: p.statusMessage || "",
          phone: p.phone || "",
          preferredCurrency: p.preferredCurrency || "USD",
          preferredLanguage: p.preferredLanguage || "en",
          privacy: p.privacy || "PUBLIC",
        });
      } catch { /* unauthenticated handled by interceptor */ }
    })();
  }, []);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const updated = await usersApi.update(form);
      setProfile(updated);
      toast.success("Profile saved");
    } catch (err) {
      toast.error(err.response?.data?.message || "Save failed");
    } finally { setSaving(false); }
  };

  return (
    <div className="space-y-8 max-w-2xl" data-testid="settings-page">
      <div>
        <div className="text-xs uppercase tracking-[0.2em] text-zinc-500 font-mono mb-1.5">/settings</div>
        <h1 className="font-display font-black text-4xl tracking-tight text-zinc-950">Settings</h1>
        <p className="text-zinc-500 mt-2 text-sm">Profile and preferences.</p>
      </div>

      <form onSubmit={save} className="border border-zinc-200 bg-white p-8 rounded-sm space-y-5">
        <Field label="Full name" hint="from signup">
          <input
            data-testid="settings-fullname"
            value={profile?.fullName || ""}
            disabled
            className="w-full px-3 py-2 border border-zinc-200 bg-zinc-50 rounded-sm text-sm text-zinc-500"
          />
        </Field>
        <Field label="Email" hint="immutable">
          <input value={profile?.email || ""} disabled className="w-full px-3 py-2 border border-zinc-200 bg-zinc-50 rounded-sm font-mono text-sm text-zinc-500" />
        </Field>
        <Field label="Status message">
          <input
            data-testid="settings-status"
            value={form.statusMessage}
            onChange={(e) => set("statusMessage", e.target.value)}
            className="w-full px-3 py-2 border border-zinc-300 rounded-sm text-sm focus:outline-none focus:border-[#0055FF] focus:ring-1 focus:ring-[#0055FF]"
            placeholder="What are you up to?"
          />
        </Field>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Preferred currency">
            <input
              data-testid="settings-currency"
              value={form.preferredCurrency}
              onChange={(e) => set("preferredCurrency", e.target.value.toUpperCase())}
              maxLength={3}
              className="w-full px-3 py-2 border border-zinc-300 rounded-sm font-mono text-sm uppercase focus:outline-none focus:border-[#0055FF]"
            />
          </Field>
          <Field label="Privacy">
            <select
              data-testid="settings-privacy"
              value={form.privacy}
              onChange={(e) => set("privacy", e.target.value)}
              className="w-full px-3 py-2 border border-zinc-300 rounded-sm text-sm focus:outline-none focus:border-[#0055FF]"
            >
              <option>PUBLIC</option>
              <option>FRIENDS_ONLY</option>
              <option>PRIVATE</option>
            </select>
          </Field>
        </div>
        <div>
          <button
            type="submit"
            disabled={saving}
            data-testid="settings-save"
            className="bg-[#0055FF] hover:bg-[#0044CC] text-white px-4 py-2 rounded-sm text-sm font-medium disabled:opacity-50 transition-all hover:-translate-y-[1px]"
          >
            {saving ? "Saving…" : "Save changes"}
          </button>
        </div>
      </form>
    </div>
  );
}

function Field({ label, hint, children }) {
  return (
    <div>
      <div className="flex items-baseline justify-between mb-1.5">
        <label className="text-xs uppercase tracking-wider text-zinc-500">{label}</label>
        {hint && <span className="text-[10px] font-mono text-zinc-400">{hint}</span>}
      </div>
      {children}
    </div>
  );
}
