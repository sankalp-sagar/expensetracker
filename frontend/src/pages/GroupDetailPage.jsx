import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { toast } from "sonner";
import { groupsApi, expensesApi, settlementsApi } from "@/lib/services";
import { useAuth } from "@/lib/auth";
import { ArrowLeft, Plus, Copy, Zap } from "lucide-react";
import ExpenseFormDialog from "@/components/ExpenseFormDialog";
import SettleUpDialog from "@/components/SettleUpDialog";

export default function GroupDetailPage() {
  const { groupId } = useParams();
  const { user } = useAuth();
  const [group, setGroup] = useState(null);
  const [expenses, setExpenses] = useState([]);
  const [balances, setBalances] = useState([]);
  const [suggestions, setSuggestions] = useState([]);
  const [createOpen, setCreateOpen] = useState(false);
  const [settleOpen, setSettleOpen] = useState(false);
  const [activeSuggestion, setActiveSuggestion] = useState(null);

  const load = async () => {
    try {
      const [g, ex, bs, ss] = await Promise.all([
        groupsApi.get(groupId),
        expensesApi.byGroup(groupId).catch(() => ({ content: [] })),
        settlementsApi.groupBalances(groupId).catch(() => []),
        settlementsApi.suggestions(groupId).catch(() => []),
      ]);
      setGroup(g);
      setExpenses(ex.content || []);
      setBalances(bs || []);
      setSuggestions(ss || []);
    } catch (err) {
      toast.error("Failed to load group");
    }
  };

  useEffect(() => { load(); }, [groupId]);

  const copyInvite = () => {
    if (!group?.inviteCode) return;
    navigator.clipboard.writeText(group.inviteCode);
    toast.success("Invite code copied");
  };

  if (!group) return <div className="text-zinc-500 text-sm">Loading…</div>;

  return (
    <div className="space-y-8" data-testid="group-detail-page">
      <div>
        <Link to="/groups" className="text-xs text-zinc-500 hover:text-zinc-950 inline-flex items-center gap-1 mb-3" data-testid="back-to-groups">
          <ArrowLeft size={12} /> Groups
        </Link>
        <div className="flex items-end justify-between">
          <div>
            <h1 className="font-display font-black text-4xl tracking-tight text-zinc-950">{group.name}</h1>
            <div className="flex items-center gap-4 mt-2">
              <span className="text-sm text-zinc-500">{group.members?.length || 0} members</span>
              <span className="font-mono text-xs text-zinc-400">{group.defaultCurrency}</span>
              {group.inviteCode && (
                <button onClick={copyInvite} className="font-mono text-[11px] uppercase tracking-wider bg-zinc-100 hover:bg-zinc-200 px-2 py-1 rounded-sm inline-flex items-center gap-1.5" data-testid="copy-invite">
                  <Copy size={11} /> {group.inviteCode}
                </button>
              )}
            </div>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => { setActiveSuggestion(null); setSettleOpen(true); }}
              data-testid="settle-up-button"
              className="bg-white border border-zinc-300 hover:bg-zinc-50 text-zinc-950 px-4 py-2 rounded-sm text-sm font-medium flex items-center gap-2 transition-all hover:-translate-y-[1px]"
            >
              <Zap size={14} /> Settle up
            </button>
            <button
              onClick={() => setCreateOpen(true)}
              data-testid="add-expense-button"
              className="bg-[#0055FF] hover:bg-[#0044CC] text-white px-4 py-2 rounded-sm text-sm font-medium flex items-center gap-2 transition-all hover:-translate-y-[1px]"
            >
              <Plus size={14} /> Add expense
            </button>
          </div>
        </div>
      </div>

      {/* Settlement suggestions */}
      {suggestions.length > 0 && (
        <div className="border border-[#30A46C]/40 bg-[#30A46C]/5 rounded-sm" data-testid="settlement-suggestions-card">
          <div className="px-6 py-3 border-b border-[#30A46C]/30 flex items-center gap-2">
            <Zap size={14} className="text-[#30A46C]" />
            <span className="font-display font-bold text-sm text-zinc-950">Smart settle-up</span>
            <span className="text-xs text-zinc-500 ml-2 font-mono">({suggestions.length} payments to clear all debts)</span>
          </div>
          <ul>
            {suggestions.map((s, i) => (
              <li key={i} className="px-6 py-3 flex items-center justify-between border-b border-[#30A46C]/20 last:border-0" data-testid={`suggestion-${i}`}>
                <div className="text-sm">
                  <span className="font-mono text-xs text-zinc-500">{shortId(s.from)}</span>
                  <span className="mx-2 text-zinc-400">→</span>
                  <span className="font-mono text-xs text-zinc-500">{shortId(s.to)}</span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="font-mono font-semibold text-[#30A46C]">${Number(s.amount).toFixed(2)}</span>
                  <button
                    onClick={() => { setActiveSuggestion(s); setSettleOpen(true); }}
                    data-testid={`mark-paid-${i}`}
                    className="text-xs px-2 py-1 bg-white border border-zinc-300 hover:bg-zinc-50 rounded-sm">
                    Mark paid
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Balances */}
      <div className="border border-zinc-200 bg-white rounded-sm" data-testid="balances-card">
        <div className="px-6 py-4 border-b border-zinc-200">
          <h2 className="font-display font-bold text-lg text-zinc-950">Pairwise balances</h2>
        </div>
        {balances.length === 0 ? (
          <div className="p-6 text-sm text-zinc-500">All settled up · no outstanding balances</div>
        ) : (
          <table className="w-full text-sm" data-testid="balances-table">
            <thead>
              <tr className="text-[10px] uppercase tracking-wider text-zinc-500 border-b border-zinc-200">
                <th className="px-6 py-2.5 text-left">User A</th>
                <th className="px-6 py-2.5 text-left">User B</th>
                <th className="px-6 py-2.5 text-right">Amount</th>
                <th className="px-6 py-2.5 text-right">Currency</th>
              </tr>
            </thead>
            <tbody>
              {balances.map((b, i) => (
                <tr key={i} className="border-b border-zinc-100 hover:bg-zinc-50">
                  <td className="px-6 py-2.5 font-mono text-xs text-zinc-700">{shortId(b.userA)}</td>
                  <td className="px-6 py-2.5 font-mono text-xs text-zinc-700">{shortId(b.userB)}</td>
                  <td className={`px-6 py-2.5 text-right font-mono font-semibold ${Number(b.amount) > 0 ? "text-[#E5484D]" : "text-[#30A46C]"}`}>
                    {Number(b.amount).toFixed(2)}
                  </td>
                  <td className="px-6 py-2.5 text-right font-mono text-xs text-zinc-500">{b.currency}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Expenses */}
      <div className="border border-zinc-200 bg-white rounded-sm" data-testid="expenses-card">
        <div className="px-6 py-4 border-b border-zinc-200">
          <h2 className="font-display font-bold text-lg text-zinc-950">Expenses</h2>
        </div>
        {expenses.length === 0 ? (
          <div className="p-6 text-sm text-zinc-500">No expenses yet — add one above.</div>
        ) : (
          <table className="w-full text-sm" data-testid="expenses-table">
            <thead>
              <tr className="text-[10px] uppercase tracking-wider text-zinc-500 border-b border-zinc-200">
                <th className="px-6 py-2.5 text-left">Date</th>
                <th className="px-6 py-2.5 text-left">Description</th>
                <th className="px-6 py-2.5 text-left">Payer</th>
                <th className="px-6 py-2.5 text-left">Split</th>
                <th className="px-6 py-2.5 text-right">Amount</th>
              </tr>
            </thead>
            <tbody>
              {expenses.map((e) => (
                <tr key={e.id} className="border-b border-zinc-100 hover:bg-zinc-50">
                  <td className="px-6 py-2.5 font-mono text-xs text-zinc-500">{e.expenseDate}</td>
                  <td className="px-6 py-2.5 text-zinc-950">{e.description}</td>
                  <td className="px-6 py-2.5 font-mono text-xs text-zinc-500">{shortId(e.payerId)}</td>
                  <td className="px-6 py-2.5 text-xs font-mono text-zinc-500">{e.splitType}</td>
                  <td className="px-6 py-2.5 text-right font-mono font-semibold text-zinc-950">
                    {e.currency} {Number(e.amount).toFixed(2)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <ExpenseFormDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        group={group}
        payerId={user?.userId}
        onCreated={load}
      />
      <SettleUpDialog
        open={settleOpen}
        onOpenChange={setSettleOpen}
        group={group}
        currentUserId={user?.userId}
        suggestion={activeSuggestion}
        onSettled={load}
      />
    </div>
  );
}

function shortId(uuid) {
  if (!uuid) return "—";
  return uuid.split("-")[0];
}
