export type Role = 'ADMIN' | 'USER';

export interface User {
  id: string;
  email: string;
  role: Role;
  memberId: string | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: User;
}
