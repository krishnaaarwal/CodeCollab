import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import { ErrorBoundary } from './components/ErrorBoundary';
import './styles/globals.css';

// Catches anything outside React's own render cycle (event handlers, async
// code, module-load-time errors) — the ErrorBoundary below only covers
// render-time throws.
window.addEventListener('error', (e) => {
  // eslint-disable-next-line no-console
  console.error('[Nexis] Uncaught error:', e.error ?? e.message);
});
window.addEventListener('unhandledrejection', (e) => {
  // eslint-disable-next-line no-console
  console.error('[Nexis] Unhandled promise rejection:', e.reason);
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </StrictMode>
);
