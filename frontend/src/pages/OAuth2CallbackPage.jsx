import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";

/**
 * Receives Google OAuth2 redirect from auth-service.
 * Tokens are passed in URL fragment (#access_token=..&refresh_token=..&user_id=..)
 * so they don't end up in server logs / referer headers.
 */
export default function OAuth2CallbackPage() {
  const nav = useNavigate();

  useEffect(() => {
    const fragment = window.location.hash.substring(1);
    const params = new URLSearchParams(fragment);
    const access = params.get("access_token");
    const refresh = params.get("refresh_token");
    const userId = params.get("user_id");

    if (!access) {
      toast.error("Google login failed");
      nav("/login", { replace: true });
      return;
    }
    localStorage.setItem("accessToken", access);
    if (refresh) localStorage.setItem("refreshToken", refresh);
    if (userId) localStorage.setItem("user", JSON.stringify({ userId }));

    toast.success("Signed in with Google");
    // Reload to let AuthProvider rehydrate the user
    window.location.href = "/";
  }, [nav]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-white" data-testid="oauth2-callback">
      <div className="text-sm font-mono text-zinc-500">Completing Google sign-in…</div>
    </div>
  );
}
