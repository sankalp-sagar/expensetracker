import { useEffect, useState } from "react";
import { expensesApi } from "@/lib/services";

export default function ExpensesPage() {
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try { const r = await expensesApi.mine(); setExpenses(r.content || []); }
      catch { setExpenses([]); }
      finally { setLoading(false); }
    })();
  }, []);

  return (
    <div className="space-y-8" data-testid="expenses-page">
      <div>
        <div className="text-xs uppercase tracking-[0.2em] text-zinc-500 font-mono mb-1.5">/expenses</div>
        <h1 className="font-display font-black text-4xl tracking-tight text-zinc-950">All my expenses</h1>
        <p className="text-zinc-500 mt-2 text-sm">Everything you've paid for or owe across all groups.</p>
      </div>

      <div className="border border-zinc-200 bg-white rounded-sm">
        {loading ? (
          <div className="p-6 text-sm text-zinc-500">Loading…</div>
        ) : expenses.length === 0 ? (
          <div className="p-12 text-center text-sm text-zinc-500">No expenses yet</div>
        ) : (
          <table className="w-full text-sm" data-testid="all-expenses-table">
            <thead>
              <tr className="text-[10px] uppercase tracking-wider text-zinc-500 border-b border-zinc-200">
                <th className="px-6 py-2.5 text-left">Date</th>
                <th className="px-6 py-2.5 text-left">Description</th>
                <th className="px-6 py-2.5 text-left">Group</th>
                <th className="px-6 py-2.5 text-left">Split</th>
                <th className="px-6 py-2.5 text-right">Amount</th>
              </tr>
            </thead>
            <tbody>
              {expenses.map((e) => (
                <tr key={e.id} className="border-b border-zinc-100 hover:bg-zinc-50">
                  <td className="px-6 py-3 font-mono text-xs text-zinc-500">{e.expenseDate}</td>
                  <td className="px-6 py-3 text-zinc-950">{e.description}</td>
                  <td className="px-6 py-3 font-mono text-xs text-zinc-500">
                    {e.groupId ? e.groupId.split("-")[0] : "—"}
                  </td>
                  <td className="px-6 py-3 text-xs font-mono text-zinc-500">{e.splitType}</td>
                  <td className="px-6 py-3 text-right font-mono font-semibold text-zinc-950">
                    {e.currency} {Number(e.amount).toFixed(2)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
