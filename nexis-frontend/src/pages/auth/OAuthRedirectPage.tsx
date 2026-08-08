import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { apiClient } from '@/api/client';
import { refreshTokenStorage } from '@/lib/refreshTokenStorage';
import { toast } from '@/components/ui/Toast';

export function OAuthRedirectPage() {
  const navigate = useNavigate();
  const ran = useRef(false);

  useEffect(() => {
    // StrictMode invokes effects twice in dev; guard so we don't consume
    // the query params (and hit the API) twice for one redirect.
    if (ran.current) return;
    ran.current = true;

    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');
    const refreshToken = params.get('refreshToken');

    if (!token || !refreshToken) {
      toast.error('OAuth sign-in did not complete');
      navigate('/login', { replace: true });
      return;
    }

    (async () => {
      try {
        refreshTokenStorage.set(refreshToken);
        useAuthStore.getState().setAccessToken(token);
        const me = await apiClient.get('/api/auth/me');
        useAuthStore.getState().setUser(me.data);
        // replace: true strips the tokens out of the URL bar immediately.
        navigate('/dashboard', { replace: true });
      } catch {
        toast.error('Could not complete sign-in');
        navigate('/login', { replace: true });
      }
    })();
  }, [navigate]);

  return (
    <div className="boot-screen">
      <div className="boot-ascii">NEXIS</div>
      <p className="boot-text">
        completing sign-in<span className="cursor-blink">_</span>
      </p>
    </div>
  );
}
