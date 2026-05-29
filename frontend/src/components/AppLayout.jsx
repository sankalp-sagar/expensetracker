import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "@/lib/auth";
import { useEffect, useState } from "react";
import { notificationsApi } from "@/lib/services";
import { Bell, LogOut, LayoutDashboard, Users, Receipt, Settings as Cog } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

const nav = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard },
  { to: "/groups", label: "Groups", icon: Users },
  { to: "/expenses", label: "Expenses", icon: Receipt },
  { to: "/settings", label: "Settings", icon: Cog },
];

export default function AppLayout() {
  const { user, logout } = useAuth();
  const nav2 = useNavigate();
  const loc = useLocation();
  const [unread, setUnread] = useState(0);
  const [notifs, setNotifs] = useState([]);

  useEffect(() => {
    let active = true;
    const refresh = async () => {
      try {
        const r = await notificationsApi.unread();
        if (active) setUnread(r.count || 0);
      } catch { /* offline */ }
    };
    refresh();
    const i = setInterval(refresh, 30000);
    return () => { active = false; clearInterval(i); };
  }, []);

  const openNotifs = async () => {
    try {
      const list = await notificationsApi.list();
      setNotifs(list || []);

      // Mark all notifications as read so they don't persist
      await Promise.all(
        (list || [])
          .filter((n) => !n.read)
          .map((n) => notificationsApi.markRead(n.id).catch(() => null))
      );

      // Refresh badge + dropdown contents
      const r = await notificationsApi.unread();
      setUnread(r.count || 0);
      const refreshed = await notificationsApi.list();
      setNotifs(refreshed || []);
    } catch {
      setNotifs([]);
    }
  };

  return (
    <div className="min-h-screen flex bg-[#F7F7F6]">
      {/* Sidebar */}
      <aside className="w-60 border-r border-zinc-200 bg-white flex flex-col">
        <div className="px-6 py-6 border-b border-zinc-200">
          <Link to="/" className="block" data-testid="nav-brand">
            <div className="font-display font-black text-[22px] tracking-tight text-zinc-950">
              expense<span className="text-[#0055FF]">tracker</span>
            </div>
            <div className="text-[10px] uppercase tracking-[0.18em] text-zinc-500 mt-1 font-mono">
              distributed · v1.0
            </div>
          </Link>
        </div>
        <nav className="flex-1 py-3" data-testid="primary-nav">
          {nav.map((n) => {
            const active = loc.pathname === n.to || (n.to !== "/" && loc.pathname.startsWith(n.to));
            const Icon = n.icon;
            return (
              <Link
                key={n.to}
                to={n.to}
                data-testid={`nav-${n.label.toLowerCase()}`}
                className={`flex items-center gap-3 px-6 py-2.5 text-sm border-l-2 transition-colors ${
                  active
                    ? "border-[#0055FF] bg-zinc-50 text-zinc-950 font-medium"
                    : "border-transparent text-zinc-600 hover:bg-zinc-50 hover:text-zinc-950"
                }`}
              >
                <Icon size={16} strokeWidth={1.75} />
                <span>{n.label}</span>
              </Link>
            );
          })}
        </nav>
        <div className="px-6 py-4 border-t border-zinc-200">
          <div className="font-medium text-sm text-zinc-950 truncate" data-testid="sidebar-user-name">
            {user?.fullName || "—"}
          </div>
          <div className="font-mono text-[11px] text-zinc-500 truncate" data-testid="sidebar-user-email">
            {user?.email}
          </div>
        </div>
      </aside>

      {/* Main */}
      <div className="flex-1 flex flex-col min-w-0">
        <header className="h-14 border-b border-zinc-200 bg-white flex items-center justify-between px-6">
          <div className="font-display font-bold text-zinc-950 text-base">
            {nav.find((n) => loc.pathname === n.to || (n.to !== "/" && loc.pathname.startsWith(n.to)))?.label || "Dashboard"}
          </div>
          <div className="flex items-center gap-2">
            <DropdownMenu onOpenChange={(o) => o && openNotifs()}>
              <DropdownMenuTrigger asChild>
                <button
                  data-testid="notifications-trigger"
                  className="relative p-2 rounded-sm hover:bg-zinc-100 transition-colors"
                  aria-label="Notifications"
                >
                  <Bell size={18} strokeWidth={1.75} />
                  {unread > 0 && (
                    <span className="absolute -top-0.5 -right-0.5 bg-[#0055FF] text-white text-[10px] font-mono font-semibold rounded-full px-1.5 py-0">
                      {unread}
                    </span>
                  )}
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent className="w-80 rounded-sm" align="end" data-testid="notifications-list">
                <DropdownMenuLabel className="font-display">Notifications</DropdownMenuLabel>
                <DropdownMenuSeparator />
                {notifs.length === 0 && (
                  <div className="px-3 py-6 text-center text-sm text-zinc-500">No notifications</div>
                )}
                {notifs.slice(0, 8).map((n) => (
                  <DropdownMenuItem key={n.id} className="flex flex-col items-start gap-0.5 py-2">
                    <div className="text-sm font-medium text-zinc-950">{n.title}</div>
                    <div className="text-xs text-zinc-500 line-clamp-2">{n.body}</div>
                  </DropdownMenuItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>

            <button
              onClick={async () => { await logout(); nav2("/login"); }}
              data-testid="logout-button"
              className="p-2 rounded-sm hover:bg-zinc-100 transition-colors"
              aria-label="Logout"
            >
              <LogOut size={18} strokeWidth={1.75} />
            </button>
          </div>
        </header>
        <main className="flex-1 overflow-auto">
          <div className="p-8 max-w-[1280px]">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
