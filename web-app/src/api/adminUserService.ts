import catalogApi from './catalogClient';

export type AdminRoleFilter = 'ADMIN' | 'USER' | undefined;
export type BlockedFilter = 'BLOCKED' | 'ACTIVE' | undefined;
export type AuditAction = 'ROLE_UPDATED' | 'BLOCK_UPDATED' | 'USER_DELETED';

export interface AdminUser {
  id: number;
  name: string;
  email: string;
  role: 'USER' | 'ADMIN';
  blocked: boolean;
  complaintsCount: number;
  createdAt: string;
  updatedAt: string;
  roleChangedAt?: string;
  roleChangedBy?: string;
  blockedChangedAt?: string;
  blockedChangedBy?: string;
}

export interface UserComplaint {
  id: number;
  category: string;
  description: string;
  status: 'PENDING' | 'REVIEWING' | 'RESOLVED';
  reporterName?: string;
  reporterEmail?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminUserComplaints {
  openCount: number;
  reviewingCount: number;
  resolvedCount: number;
  complaints: UserComplaint[];
}

export interface UserAuditLogEntry {
  id: number;
  action: AuditAction;
  details?: string;
  performedById?: number;
  performedByEmail?: string;
  performedByName?: string;
  createdAt: string;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface AdminUserQuery {
  query?: string;
  role?: AdminRoleFilter;
  blocked?: BlockedFilter;
  page?: number;
  size?: number;
}

export interface BlockStatusPayload {
  blocked: boolean;
  reason?: string;
}

export interface RoleChangePayload {
  role: 'USER' | 'ADMIN';
  reason?: string;
}

export type ComplaintStatus = UserComplaint['status'];

const mapBlockedFilter = (value?: BlockedFilter): boolean | undefined => {
  if (value === 'BLOCKED') return true;
  if (value === 'ACTIVE') return false;
  return undefined;
};

export const adminUserService = {
  listUsers: (query: AdminUserQuery = {}) => {
    const params: Record<string, string | number | boolean> = {};
    if (query.query) params.query = query.query;
    if (query.role) params.role = query.role;
    const blocked = mapBlockedFilter(query.blocked);
    if (typeof blocked === 'boolean') params.blocked = blocked;
    if (typeof query.page === 'number') params.page = query.page;
    if (typeof query.size === 'number') params.size = query.size;

    return catalogApi
      .get<PageResponse<AdminUser>>('/api/admin/users', { params })
      .then((res) => res.data);
  },
  updateBlockStatus: (userId: number, payload: BlockStatusPayload) =>
    catalogApi.patch<AdminUser>(`/api/admin/users/${userId}/block`, payload).then((res) => res.data),
  updateRole: (userId: number, payload: RoleChangePayload) =>
    catalogApi.patch<AdminUser>(`/api/admin/users/${userId}/role`, payload).then((res) => res.data),
  deleteUser: (userId: number) => catalogApi.delete(`/api/admin/users/${userId}`),
  getComplaints: (userId: number) =>
    catalogApi.get<AdminUserComplaints>(`/api/admin/users/${userId}/complaints`).then((res) => res.data),
  getAuditLog: (userId: number) =>
    catalogApi.get<UserAuditLogEntry[]>(`/api/admin/users/${userId}/audit`).then((res) => res.data),
  updateComplaintStatus: (userId: number, complaintId: number, status: ComplaintStatus) =>
    catalogApi
      .patch<UserComplaint>(`/api/admin/users/${userId}/complaints/${complaintId}`, { status })
      .then((res) => res.data)
};
