/** Shapes aligned with docs/api-contract.md — Agent A will wire real JWT auth. */

export type AuthProvider = 'LOCAL' | 'GOOGLE';
export type UserRole = 'USER' | 'ADMIN';

export interface UserDto {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  authProvider: AuthProvider;
  phoneNumber: string | null;
  phoneVerified: boolean;
}

export interface AuthConfig {
  googleOAuthEnabled: boolean;
}

export interface AuthResponse {
  accessToken: string;
  expiresIn: number;
  user: UserDto;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  passwordConfirmation: string;
}
