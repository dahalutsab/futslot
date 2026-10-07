/** Shapes returned by the FutSlot API (backend: docs/api/openapi.yaml). */

export type Role = 'PLAYER' | 'OWNER' | 'STAFF' | 'ADMIN';

export type VenuePermission =
  | 'MANAGE_VENUE'
  | 'MANAGE_BOOKINGS'
  | 'MANAGE_BLOCKS'
  | 'VIEW_CUSTOMERS'
  | 'VIEW_REPORTS'
  | 'MANAGE_STAFF';

export interface User {
  id: string;
  fullName: string;
  phone: string;
  email: string | null;
  avatarUrl: string | null;
  preferredLanguage: string;
  roles: Role[];
  createdAt: string;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  refreshToken: string;
  user: User;
}

export interface OtpSent {
  expiresInSeconds: number;
  resendAfterSeconds: number;
}

export type VenueStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'ACTIVE' | 'REJECTED' | 'SUSPENDED';

export interface VenueSummary {
  id: string;
  slug: string;
  name: string;
  area: string | null;
  city: string;
  status: VenueStatus;
  averageRating: number;
  reviewCount: number;
  fromPrice: number | null;
  courtCount: number;
  amenities: string[];
  coverPhotoUrl: string | null;
}

/** A venue the signed-in user runs or works at; {@code access} is OWNER, MANAGER or RECEPTIONIST. */
export interface MyVenue {
  venue: VenueSummary;
  access: 'OWNER' | 'MANAGER' | 'RECEPTIONIST';
  permissions: VenuePermission[];
}

/** RFC 7807 problem body; {@code code} is stable and machine-readable. */
export interface ApiProblem {
  status: number;
  detail: string;
  code?: string;
}
