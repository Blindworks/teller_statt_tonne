export type Role = 'ADMIN' | 'USER';

export interface User {
  id: number;
  email: string;
  role: Role;
  memberId: number | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: User;
}
