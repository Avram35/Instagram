import { authFetch } from "./authFetch";

const BLOCK_API_URL = import.meta.env.VITE_BLOCK_API;

export const checkBlock = async (userId) => {
  const response = await authFetch(
    `${BLOCK_API_URL}/api/v1/block/check/${userId}`,
  );
  return response.json();
};

export const toggleBlock = async (userId, isBlocked) => {
  await authFetch(`${BLOCK_API_URL}/api/v1/block/${userId}`, {
    method: isBlocked ? "DELETE" : "POST",
  });
};
