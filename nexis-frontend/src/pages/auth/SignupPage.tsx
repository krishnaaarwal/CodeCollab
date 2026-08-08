import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { TerminalSquare } from 'lucide-react';
import { signup } from '@/api/auth';
import { getErrorMessage } from '@/api/client';
import { toast } from '@/components/ui/Toast';
import { Button, Input } from '@/components/ui/primitives';
import { AuthLayout } from '@/components/layout/AuthLayout';

export function SignupPage() {
  const [fullname, setFullname] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (password !== confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }
    setLoading(true);
    try {
      await signup({ fullname, email, password });
      toast.success('Account created — log in to continue');
      navigate('/login', { replace: true, state: { prefillEmail: email } });
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not create account'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout>
      <div className="auth-brand glow-text">
        <TerminalSquare size={24} />
        <span>NEXIS</span>
      </div>
      <p className="auth-subtitle">create your account</p>

      <form onSubmit={handleSubmit} className="auth-form">
        <Input
          label="Full name"
          value={fullname}
          onChange={(e) => setFullname(e.target.value)}
          required
          autoFocus
        />
        <Input
          label="Email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <Input
          label="Password"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          minLength={8}
          required
        />
        <Input
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          minLength={8}
          required
        />
        <Button type="submit" loading={loading} style={{ width: '100%' }}>
          create account
        </Button>
      </form>

      <p className="auth-footer">
        already have an account?
        <Link to="/login">log in</Link>
      </p>
    </AuthLayout>
  );
}
