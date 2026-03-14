import { authFetch } from "./authFetch";

const INTERACTION_API_URL = import.meta.env.VITE_INTERACTION_API;

export const fetchLikeCount = async (postId) => {
  const response = await authFetch(
    `${INTERACTION_API_URL}/api/v1/like/count/${postId}`,
  );
  return response.json();
};

export const checkLiked = async (postId) => {
  const response = await authFetch(
    `${INTERACTION_API_URL}/api/v1/like/check/${postId}`,
  );
  return response.json();
};

export const toggleLike = async (postId, isLiked) => {
  await authFetch(`${INTERACTION_API_URL}/api/v1/like/${postId}`, {
    method: isLiked ? "DELETE" : "POST",
  });
};
