import { useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { useAuth } from "@/lib/auth";

function getOAuthParam(name) {
  const fragmentParams = new URLSearchParams(window.location.hash.substring(1));
  const queryParams = new URLSearchParams(window.location.search.substring(1));
  return fragmentParams.get(name) || queryParams.get(name);
}

/**
 * Receives Google OAuth2 redirect from auth-service.
 * Tokens are passed in URL fragment (#access_token=..&refresh_token=..&user_id=..)
 * so they don't end up in server logs / referer headers.
 */
export default function OAuth2CallbackPage() {
  const nav = useNavigate();
  const { completeOAuthLogin } = useAuth();
  const handled = useRef(false);

  useEffect(() => {
    if (handled.current) return;
    handled.current = true;

    const access = getOAuthParam("access_token");
    const refresh = getOAuthParam("refresh_token");
    const userId = getOAuthParam("user_id");
    if (!access) {
      toast.error("Google login failed");
      nav("/login", { replace: true });
      return;
    }

    try {
      completeOAuthLogin(access, refresh, userId);
      toast.success("Signed in with Google");
      nav("/", { replace: true });
    } catch {
      toast.error("Google login failed");
      nav("/login", { replace: true });
    }
  }, [completeOAuthLogin, nav]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-white" data-testid="oauth2-callback">
      <div className="text-sm font-mono text-zinc-500">Completing Google sign-in…</div>
    </div>
  );
}
