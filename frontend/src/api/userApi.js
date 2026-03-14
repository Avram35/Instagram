import { authFetch } from "./authFetch";

const USER_API_URL = import.meta.env.VITE_USER_API;

export const fetchProfileInfo = async (username) => {
  const response = await authFetch(`${USER_API_URL}/api/v1/user/${username}`);
  return response.json();
};

export const fetchUserById = async (userId) => {
  const response = await authFetch(`${USER_API_URL}/api/v1/user/id/${userId}`);
  return response.json();
};

export const getUserAvatarUrl = (profilePictureUrl, fallback = "") =>
  profilePictureUrl ? `${USER_API_URL}${profilePictureUrl}` : fallback;

export const searchUsers = async (query) => {
  const response = await authFetch(
    `${USER_API_URL}/api/v1/user/search?query=${query}`,
  );
  return response.json();
};

export const updateProfile = async (userId, profileData) => {
  return authFetch(`${USER_API_URL}/api/v1/user/${userId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(profileData),
  });
};

export const updateProfilePicture = async (file) => {
  const formData = new FormData();
  formData.append("file", file);
  await authFetch(`${USER_API_URL}/api/v1/user/profile-pic`, {
    method: "POST",
    body: formData,
  });
};
