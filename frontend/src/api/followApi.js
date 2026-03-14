import { authFetch } from "./authFetch";

const FOLLOW_API_URL = import.meta.env.VITE_FOLLOW_API;

export const fetchFollowCount = async (userId) => {
  const response = await authFetch(
    `${FOLLOW_API_URL}/api/v1/follow/${userId}/count`,
  );
  return response.json();
};

export const checkFollow = async (userId) => {
  const [followRes, pendingRes] = await Promise.all([
    authFetch(`${FOLLOW_API_URL}/api/v1/follow/check/${userId}`),
    authFetch(`${FOLLOW_API_URL}/api/v1/follow/requests/check/${userId}`),
  ]);
  const followData = await followRes.json();
  const pendingData = await pendingRes.json();
  return { following: followData.following, pending: pendingData.pending };
};

export const toggleFollow = async (userId, isFollowing, isPending) => {
  const method = isFollowing || isPending ? "DELETE" : "POST";
  const response = await authFetch(
    `${FOLLOW_API_URL}/api/v1/follow/${userId}`,
    { method },
  );
  return { data: await response.json(), method };
};

export const fetchFollowers = async (profileUserId) => {
  const response = await authFetch(
    `${FOLLOW_API_URL}/api/v1/follow/${profileUserId}/followers`,
  );
  return response.json();
};

export const fetchFollowing = async (profileUserId) => {
  const response = await authFetch(
    `${FOLLOW_API_URL}/api/v1/follow/${profileUserId}/following`,
  );
  return response.json();
};

export const removeFollower = async (followerId) => {
  await authFetch(`${FOLLOW_API_URL}/api/v1/follow/remove/${followerId}`, {
    method: "DELETE",
  });
};

export const fetchPendingRequests = async () => {
  const response = await authFetch(
    `${FOLLOW_API_URL}/api/v1/follow/requests/pending`,
  );
  return response.json();
};

export const fetchNotifications = async () => {
  const response = await authFetch(
    `${FOLLOW_API_URL}/api/v1/follow/notifications`,
  );
  return response.json();
};

export const acceptFollowRequest = async (requestId) => {
  await authFetch(
    `${FOLLOW_API_URL}/api/v1/follow/requests/${requestId}/accept`,
    { method: "POST" },
  );
};

export const rejectFollowRequest = async (requestId) => {
  await authFetch(
    `${FOLLOW_API_URL}/api/v1/follow/requests/${requestId}/reject`,
    { method: "POST" },
  );
};

export const markNotificationsAsRead = async () => {
  await authFetch(`${FOLLOW_API_URL}/api/v1/follow/notifications/read-all`, {
    method: "PUT",
  });
};
