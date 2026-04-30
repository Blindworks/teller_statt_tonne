export type { OnlineStatus, Role, User, UserStatus } from '../users/user.model';
import type { User } from '../users/user.model';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: User;
}
