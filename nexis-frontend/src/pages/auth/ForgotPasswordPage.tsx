import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { TerminalSquare } from 'lucide-react';
import { requestPasswordReset, submitPasswordReset } from '@/api/auth';
import { getErrorMessage } from '@/api/client';
import { toast } from '@/components/ui/Toast';
import { Button, Input } from '@/components/ui/primitives';
import { AuthLayout } from '@/components/layout/AuthLayout';

export function ForgotPasswordPage() {
  const [step, setStep] = useState<'request' | 'reset'>('request');
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  async function handleRequest(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      await requestPasswordReset(email);
      // Backend intentionally returns the same generic response whether or
      // not the account exists (anti-enumeration), so we always advance.
      toast.info('If that account exists, a reset code was sent');
      setStep('reset');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not send reset code'));
    } finally {
      setLoading(false);
    }
  }

  async function handleReset(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      await submitPasswordReset({ email, otp, newPassword });
      toast.success('Password reset — log in with your new password');
      navigate('/login', { replace: true });
    } catch (err) {
      toast.error(getErrorMessage(err, 'Invalid or expired code'));
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
      <p className="auth-subtitle">
        {step === 'request' ? 'reset your password' : 'enter the code we sent you'}
      </p>

      {step === 'request' ? (
        <form onSubmit={handleRequest} className="auth-form">
          <Input
            label="Email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoFocus
          />
          <Button type="submit" loading={loading} style={{ width: '100%' }}>
            send reset code
          </Button>
        </form>
      ) : (
        <form onSubmit={handleReset} className="auth-form">
          <Input
            label="6-digit code"
            value={otp}
            onChange={(e) => setOtp(e.target.value)}
            maxLength={6}
            required
            autoFocus
          />
          <Input
            label="New password"
            type="password"
            autoComplete="new-password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            minLength={8}
            required
          />
          <Button type="submit" loading={loading} style={{ width: '100%' }}>
            reset password
          </Button>
          <button type="button" className="auth-footer" onClick={() => setStep('request')} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
            didn't get a code? try again
          </button>
        </form>
      )}

      <p className="auth-footer">
        <Link to="/login">back to login</Link>
      </p>
    </AuthLayout>
  );
}
