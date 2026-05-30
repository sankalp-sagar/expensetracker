import { useEffect, useState } from "react";
import { toast } from "sonner";
import { usersApi } from "@/lib/services";
import { normalizeCurrency } from "@/lib/currency";

export default function SettingsPage() {
  const [form, setForm] = useState({
    preferredCurrency: "USD",
  });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const p = await usersApi.me();
        setForm({
          preferredCurrency: normalizeCurrency(p?.preferredCurrency),
        });
      } catch (err) {
        toast.error(err.response?.data?.message || "Failed to load settings");
      }
    })();
  }, []);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const save = async (e) => {
    e.preventDefault();
    const nextCurrency = form.preferredCurrency.trim().toUpperCase();
    if (!/^[A-Z]{3}$/.test(nextCurrency)) {
      toast.error("Use a 3-letter currency code");
      return;
    }

    setSaving(true);
    try {
      const updated = await usersApi.update({
        preferredCurrency: nextCurrency,
      });
      setForm({
        preferredCurrency: normalizeCurrency(updated?.preferredCurrency, nextCurrency),
      });
      toast.success("Settings saved");
    } catch (err) {
      toast.error(err.response?.data?.message || "Save failed");
    } finally { setSaving(false); }
  };

  return (
    <div className="space-y-8 w-full" data-testid="settings-page">
      <div>
        <div className="text-xs uppercase tracking-[0.2em] text-zinc-500 font-mono mb-1.5">/settings</div>
        <h1 className="font-display font-black text-4xl tracking-tight text-zinc-950">Settings</h1>
      </div>

      <form onSubmit={save} className="border border-zinc-200 bg-white rounded-sm">
        <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-end sm:justify-between">
          <Field label="Default currency">
            <input
              data-testid="settings-currency"
              value={form.preferredCurrency}
              onChange={(e) => set("preferredCurrency", e.target.value.toUpperCase())}
              maxLength={3}
              pattern="[A-Z]{3}"
              className="w-32 px-3 py-2 border border-zinc-300 rounded-sm font-mono text-sm uppercase focus:outline-none focus:border-[#0055FF] focus:ring-1 focus:ring-[#0055FF]"
            />
          </Field>
          <button
            type="submit"
            disabled={saving}
            data-testid="settings-save"
            className="bg-[#0055FF] hover:bg-[#0044CC] text-white px-4 py-2 rounded-sm text-sm font-medium disabled:opacity-50 transition-all hover:-translate-y-[1px] sm:mb-0"
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
