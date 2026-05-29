import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import { groupsApi } from "@/lib/services";
import { Plus, Users, ArrowRight, Hash } from "lucide-react";
import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger,
} from "@/components/ui/dialog";

export default function GroupsPage() {
  const [groups, setGroups] = useState([]);
  const [createOpen, setCreateOpen] = useState(false);
  const [joinOpen, setJoinOpen] = useState(false);

  const load = async () => {
    try {
      setGroups(await groupsApi.mine());
    } catch (err) {
      // Keep UI from silently failing; helps diagnose missing X-User-Id
      toast.error(err?.response?.data?.message || "Failed to load your groups");
      setGroups([]);
    }
  };
  useEffect(() => { load(); }, []);

  return (
    <div className="space-y-8" data-testid="groups-page">
      <div className="flex items-end justify-between">
        <div>
          <div className="text-xs uppercase tracking-[0.2em] text-zinc-500 font-mono mb-1.5">/groups</div>
          <h1 className="font-display font-black text-4xl tracking-tight text-zinc-950">Groups</h1>
          <p className="text-zinc-500 mt-2 text-sm">Households, trips, dinner clubs — whatever you split.</p>
        </div>
        <div className="flex gap-2">
          <button
            data-testid="join-group-button"
            onClick={() => setJoinOpen(true)}
            className="bg-white border border-zinc-300 hover:bg-zinc-50 text-zinc-950 px-4 py-2 rounded-sm text-sm font-medium flex items-center gap-2 transition-all hover:-translate-y-[1px]"
          >
            <Hash size={14} /> Join by code
          </button>
          <button
            data-testid="create-group-button"
            onClick={() => setCreateOpen(true)}
            className="bg-[#0055FF] hover:bg-[#0044CC] text-white px-4 py-2 rounded-sm text-sm font-medium flex items-center gap-2 transition-all hover:-translate-y-[1px]"
          >
            <Plus size={14} /> New group
          </button>
        </div>
      </div>

      {groups.length === 0 ? (
        <div className="border border-zinc-200 border-dashed bg-white p-16 text-center rounded-sm">
          <Users size={28} className="mx-auto text-zinc-400 mb-3" strokeWidth={1.5} />
          <div className="font-display font-bold text-lg text-zinc-950">No groups yet</div>
          <div className="text-sm text-zinc-500 mt-1">Create your first group to start tracking shared expenses</div>
        </div>
      ) : (
        <div className="border border-zinc-200 bg-white rounded-sm" data-testid="groups-list">
          <ul>
            {groups.map((g) => (
              <li key={g.id} className="border-b border-zinc-200 last:border-0">
                <Link
                  to={`/groups/${g.id}`}
                  data-testid={`group-row-${g.id}`}
                  className="flex items-center justify-between px-6 py-4 hover:bg-zinc-50 transition-colors"
                >
                  <div>
                    <div className="font-medium text-zinc-950">{g.name}</div>
                    <div className="flex items-center gap-3 mt-1">
                      <span className="text-xs text-zinc-500">{g.members?.length || 0} members</span>
                      <span className="font-mono text-xs text-zinc-400">{g.defaultCurrency}</span>
                      {g.inviteCode && (
                        <span className="font-mono text-[10px] uppercase tracking-wider bg-zinc-100 px-1.5 py-0.5 rounded-sm" data-testid={`group-invite-${g.id}`}>
                          {g.inviteCode}
                        </span>
                      )}
                    </div>
                  </div>
                  <ArrowRight size={16} className="text-zinc-400" />
                </Link>
              </li>
            ))}
          </ul>
        </div>
      )}

      <CreateGroupDialog open={createOpen} onOpenChange={setCreateOpen} onCreated={load} />
      <JoinDialog open={joinOpen} onOpenChange={setJoinOpen} onJoined={load} />
    </div>
  );
}

function CreateGroupDialog({ open, onOpenChange, onCreated }) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [currency, setCurrency] = useState("USD");
  const [saving, setSaving] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await groupsApi.create({ name, description, defaultCurrency: currency });
      toast.success("Group created");
      onCreated?.();
      onOpenChange(false);
      setName(""); setDescription("");
    } catch (err) {
      toast.error(err.response?.data?.message || "Create failed");
    } finally { setSaving(false); }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="rounded-sm">
        <DialogHeader>
          <DialogTitle className="font-display">Create group</DialogTitle>
          <DialogDescription>Bring people together for shared expenses.</DialogDescription>
        </DialogHeader>
        <form onSubmit={submit} className="space-y-3" data-testid="create-group-form">
          <div>
            <label className="block text-xs uppercase tracking-wider text-zinc-500 mb-1.5">Name</label>
            <input
              data-testid="create-group-name"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3 py-2 border border-zinc-300 rounded-sm text-sm focus:outline-none focus:border-[#0055FF] focus:ring-1 focus:ring-[#0055FF]"
              placeholder="Roommates · Goa Trip · Lunch Club"
            />
          </div>
          <div>
            <label className="block text-xs uppercase tracking-wider text-zinc-500 mb-1.5">Description</label>
            <textarea
              data-testid="create-group-description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={2}
              className="w-full px-3 py-2 border border-zinc-300 rounded-sm text-sm focus:outline-none focus:border-[#0055FF] focus:ring-1 focus:ring-[#0055FF]"
              placeholder="Optional"
            />
          </div>
          <div>
            <label className="block text-xs uppercase tracking-wider text-zinc-500 mb-1.5">Default currency</label>
            <input
              data-testid="create-group-currency"
              value={currency}
              onChange={(e) => setCurrency(e.target.value.toUpperCase())}
              maxLength={3}
              className="w-32 px-3 py-2 border border-zinc-300 rounded-sm font-mono text-sm uppercase focus:outline-none focus:border-[#0055FF] focus:ring-1 focus:ring-[#0055FF]"
            />
          </div>
          <DialogFooter>
            <button
              type="submit"
              disabled={saving}
              data-testid="create-group-submit"
              className="bg-[#0055FF] hover:bg-[#0044CC] text-white px-4 py-2 rounded-sm text-sm font-medium disabled:opacity-50"
            >
              {saving ? "Creating…" : "Create"}
            </button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function JoinDialog({ open, onOpenChange, onJoined }) {
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      await groupsApi.join(code.toUpperCase());
      toast.success("Joined group");
      onJoined?.();
      onOpenChange(false);
      setCode("");
    } catch (err) {
      toast.error(err.response?.data?.message || "Join failed");
    } finally { setBusy(false); }
  };
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="rounded-sm">
        <DialogHeader>
          <DialogTitle className="font-display">Join group by invite code</DialogTitle>
        </DialogHeader>
        <form onSubmit={submit} className="space-y-3" data-testid="join-group-form">
          <input
            data-testid="join-group-code"
            required
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            className="w-full px-3 py-2 border border-zinc-300 rounded-sm font-mono uppercase tracking-widest text-center text-sm focus:outline-none focus:border-[#0055FF] focus:ring-1 focus:ring-[#0055FF]"
            placeholder="XXXXXXXX"
            maxLength={12}
          />
          <DialogFooter>
            <button
              type="submit"
              disabled={busy}
              data-testid="join-group-submit"
              className="bg-[#0055FF] hover:bg-[#0044CC] text-white px-4 py-2 rounded-sm text-sm font-medium disabled:opacity-50"
            >
              {busy ? "Joining…" : "Join"}
            </button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
