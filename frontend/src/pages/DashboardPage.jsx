import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { analyticsApi, groupsApi, expensesApi } from "@/lib/services";
import { useAuth } from "@/lib/auth";
import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Tooltip, CartesianGrid } from "recharts";
import { TrendingUp, Users, Receipt, AlertCircle } from "lucide-react";

export default function DashboardPage() {
  const { user } = useAuth();
  const [monthly, setMonthly] = useState([]);
  const [groups, setGroups] = useState([]);
  const [recentExpenses, setRecentExpenses] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    (async () => {
      try {
        const [m, g, e] = await Promise.all([
          analyticsApi.monthly().catch(() => []),
          groupsApi.mine().catch(() => []),
          expensesApi.mine().catch(() => ({ content: [] })),
        ]);
        setMonthly((m || []).slice(0, 6).reverse());
        setGroups(g || []);
        setRecentExpenses(e.content || []);
      } catch (err) {
        setError(err.message);
      }
    })();
  }, []);

  const totalThisMonth = monthly.length ? monthly[monthly.length - 1]?.total || 0 : 0;
  const totalAllTime = monthly.reduce((s, m) => s + Number(m.total || 0), 0);

  return (
    <div className="space-y-8" data-testid="dashboard-root">
      <div>
        <div className="text-xs uppercase tracking-[0.2em] text-zinc-500 font-mono mb-1.5">/dashboard</div>
        <h1 className="font-display font-black text-4xl tracking-tight text-zinc-950">
          Welcome, {user?.fullName?.split(" ")[0] || "there"}
        </h1>
        <p className="text-zinc-500 mt-2 text-sm">
          Your shared expenses at a glance.
        </p>
      </div>

      {error && (
        <div className="border border-[#E5484D]/30 bg-[#E5484D]/5 text-[#E5484D] text-sm p-4 rounded-sm flex items-start gap-3" data-testid="dashboard-error">
          <AlertCircle size={16} />
          <div>
            <div className="font-medium">Backend unreachable</div>
            <div className="text-xs mt-1 text-[#E5484D]/80">
              Make sure the gateway is running at <span className="font-mono">{process.env.REACT_APP_API_BASE || "http://localhost:8080"}</span>
            </div>
          </div>
        </div>
      )}

      {/* KPI row */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-px bg-zinc-200 border border-zinc-200 rounded-sm overflow-hidden" data-testid="dashboard-kpis">
        <KPI label="This month" value={totalThisMonth} icon={TrendingUp} accent />
        <KPI label="Total tracked" value={totalAllTime} icon={Receipt} />
        <KPI label="Active groups" value={groups.length} icon={Users} count />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Monthly chart */}
        <div className="lg:col-span-2 border border-zinc-200 bg-white p-6 rounded-sm" data-testid="monthly-chart-card">
          <div className="flex items-baseline justify-between mb-4">
            <h2 className="font-display font-bold text-lg text-zinc-950">Monthly spend</h2>
            <span className="text-xs font-mono text-zinc-500">USD</span>
          </div>
          {monthly.length === 0 ? (
            <div className="h-48 flex items-center justify-center text-sm text-zinc-500">No data yet — create an expense to start tracking.</div>
          ) : (
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={monthly}>
                <CartesianGrid stroke="#E4E4E7" vertical={false} />
                <XAxis dataKey="month" stroke="#A1A1AA" fontSize={11} tickLine={false} axisLine={false} />
                <YAxis stroke="#A1A1AA" fontSize={11} tickLine={false} axisLine={false} />
                <Tooltip contentStyle={{ border: "1px solid #E4E4E7", borderRadius: 2, fontSize: 12 }} />
                <Bar dataKey="total" fill="#0055FF" radius={[2, 2, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Groups list */}
        <div className="border border-zinc-200 bg-white rounded-sm" data-testid="dashboard-groups-card">
          <div className="px-6 py-4 border-b border-zinc-200 flex items-baseline justify-between">
            <h2 className="font-display font-bold text-lg text-zinc-950">Your groups</h2>
            <Link to="/groups" className="text-xs font-medium text-[#0055FF] hover:underline" data-testid="link-all-groups">view all</Link>
          </div>
          {groups.length === 0 ? (
            <div className="p-6 text-sm text-zinc-500">No groups yet</div>
          ) : (
            <ul>
              {groups.slice(0, 6).map((g) => (
                <li key={g.id} className="border-b border-zinc-200 last:border-0">
                  <Link
                    to={`/groups/${g.id}`}
                    data-testid={`dashboard-group-${g.id}`}
                    className="flex items-center justify-between px-6 py-3 hover:bg-zinc-50 transition-colors"
                  >
                    <div>
                      <div className="text-sm font-medium text-zinc-950">{g.name}</div>
                      <div className="text-xs text-zinc-500 font-mono">{g.members?.length || 0} members</div>
                    </div>
                    <div className="font-mono text-xs text-zinc-400">{g.defaultCurrency}</div>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {/* Recent expenses */}
      <div className="border border-zinc-200 bg-white rounded-sm" data-testid="recent-expenses-card">
        <div className="px-6 py-4 border-b border-zinc-200 flex items-baseline justify-between">
          <h2 className="font-display font-bold text-lg text-zinc-950">Recent expenses</h2>
          <Link to="/expenses" className="text-xs font-medium text-[#0055FF] hover:underline" data-testid="link-all-expenses">all expenses</Link>
        </div>
        {recentExpenses.length === 0 ? (
          <div className="p-6 text-sm text-zinc-500">No expenses yet</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-[10px] uppercase tracking-wider text-zinc-500 border-b border-zinc-200">
                <th className="px-6 py-2.5 text-left font-medium">Date</th>
                <th className="px-6 py-2.5 text-left font-medium">Description</th>
                <th className="px-6 py-2.5 text-left font-medium">Split</th>
                <th className="px-6 py-2.5 text-right font-medium">Amount</th>
              </tr>
            </thead>
            <tbody>
              {recentExpenses.slice(0, 6).map((e) => (
                <tr key={e.id} className="border-b border-zinc-100 hover:bg-zinc-50" data-testid={`dashboard-expense-row-${e.id}`}>
                  <td className="px-6 py-2.5 font-mono text-xs text-zinc-500">{e.expenseDate}</td>
                  <td className="px-6 py-2.5 text-zinc-950">{e.description}</td>
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
    </div>
  );
}

function KPI({ label, value, icon: Icon, accent, count }) {
  return (
    <div className={`p-6 bg-white ${accent ? "" : ""}`}>
      <div className="flex items-center gap-2 text-xs uppercase tracking-wider text-zinc-500 mb-3">
        <Icon size={14} strokeWidth={1.75} />
        <span>{label}</span>
      </div>
      <div className={`font-mono font-semibold text-3xl ${accent ? "text-[#0055FF]" : "text-zinc-950"}`}>
        {count ? value : `$${Number(value || 0).toFixed(2)}`}
      </div>
    </div>
  );
}
