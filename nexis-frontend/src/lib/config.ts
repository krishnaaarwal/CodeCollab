// If the .env fails to load, it will safely fall back to the Ingress paths!
export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL || '/api';
export const WS_URL: string = import.meta.env.VITE_WS_URL || '/ws';
export const FRONTEND_URL: string = import.meta.env.VITE_FRONTEND_URL || '';