import { Component, type ErrorInfo, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    // eslint-disable-next-line no-console
    console.error('[Nexis] Render crash:', error, info.componentStack);
  }

  render() {
    const { error } = this.state;
    if (!error) return this.props.children;

    return (
      <div className="boot-screen" style={{ alignItems: 'stretch', padding: '3rem' }}>
        <div style={{ maxWidth: 640, margin: '0 auto', width: '100%' }}>
          <div className="boot-ascii" style={{ fontSize: 22, letterSpacing: '0.2em' }}>
            NEXIS CRASHED
          </div>
          <p style={{ color: 'var(--color-signal-red)', fontSize: 13, marginTop: '1rem' }}>{error.message}</p>
          <pre
            style={{
              fontSize: 11,
              color: 'var(--color-ink-faint)',
              whiteSpace: 'pre-wrap',
              marginTop: '1rem',
              maxHeight: 300,
              overflow: 'auto',
            }}
          >
            {error.stack}
          </pre>
          <p style={{ color: 'var(--color-ink-dim)', fontSize: 12, marginTop: '1.5rem' }}>
            Full details are also in the browser console (F12).
          </p>
        </div>
      </div>
    );
  }
}
