import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { TerminalSquare } from 'lucide-react';
import { login, fetchCurrentUser } from '@/api/auth';
import { getErrorMessage } from '@/api/client';
import { refreshTokenStorage } from '@/lib/refreshTokenStorage';
import { useAuthStore } from '@/store/authStore';
import { toast } from '@/components/ui/Toast';
import { Button, Input } from '@/components/ui/primitives';
import { AuthLayout } from '@/components/layout/AuthLayout';
import { API_BASE_URL } from '@/lib/config';

interface LocationState {
  from?: { pathname: string };
  prefillEmail?: string;
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const locationState = location.state as LocationState | null;

  const [email, setEmail] = useState(locationState?.prefillEmail ?? '');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await login({ email, password });
      refreshTokenStorage.set(res.refreshToken);
      useAuthStore.getState().setAccessToken(res.accessToken);
      const profile = await fetchCurrentUser();
      useAuthStore.getState().setUser(profile);
      navigate(locationState?.from?.pathname || '/dashboard', { replace: true });
    } catch (err) {
      toast.error(getErrorMessage(err, 'Invalid email or password'));
    } finally {
      setLoading(false);
    }
  }

  function handleOAuth(provider: 'google' | 'github') {
    window.location.href = `${API_BASE_URL}/oauth2/authorization/${provider}`;
  }

  return (
    <AuthLayout>
      <div className="mb-8">
        <div className="flex items-center gap-2 font-display text-2xl font-bold tracking-tight text-zinc-100">
          <TerminalSquare size={24} className="text-blue-500" />
          <span>NEXIS</span>
        </div>
        <p className="mt-2 text-[14px] text-zinc-500">Sign in to your workspaces</p>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <Input
          label="Email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          autoFocus={!locationState?.prefillEmail}
        />
        <Input
          label="Password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          autoFocus={!!locationState?.prefillEmail}
        />
        <Button type="submit" loading={loading} className="mt-2 w-full">
          Log In
        </Button>
      </form>

      <div className="my-6 flex items-center gap-4">
        <div className="h-px flex-1 bg-zinc-800" />
        <span className="text-[10px] font-semibold uppercase tracking-widest text-zinc-600">or</span>
        <div className="h-px flex-1 bg-zinc-800" />
      </div>

      <div className="grid grid-cols-2 gap-3">
        <Button type="button" variant="ghost" onClick={() => handleOAuth('google')}>Google</Button>
        <Button type="button" variant="ghost" onClick={() => handleOAuth('github')}>GitHub</Button>
      </div>

      <p className="mt-8 text-center text-[13px] text-zinc-500">
        <Link to="/forgot-password" className="text-zinc-400 hover:text-zinc-100">Forgot password?</Link>
        <span className="mx-2 text-zinc-700">·</span>
        <Link to="/signup" className="text-zinc-400 hover:text-zinc-100">Create account</Link>
      </p>
    </AuthLayout>
  );
}