import catalogApi from './catalogClient';
import { UserComplaint } from './adminUserService';

export interface ComplaintPayload {
  targetUserId: number;
  category: string;
  description: string;
}

export const complaintService = {
  submit: (payload: ComplaintPayload) =>
    catalogApi.post<UserComplaint>('/api/complaints', payload).then((res) => res.data)
};
