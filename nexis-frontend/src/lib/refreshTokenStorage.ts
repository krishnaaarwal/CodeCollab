
const KEY = 'refreshToken';

export const refreshTokenStorage = {
  get: (): string | null => sessionStorage.getItem(KEY),
  set: (token: string): void => sessionStorage.setItem(KEY, token),
  clear: (): void => sessionStorage.removeItem(KEY),
};
