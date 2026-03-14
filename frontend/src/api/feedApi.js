import { authFetch } from "./authFetch";

const FEED_API_URL = import.meta.env.VITE_FEED_API;

export const fetchFeed = async (page = 0, size = 20) => {
  const response = await authFetch(
    `${FEED_API_URL}/api/v1/feed?page=${page}&size=${size}`,
  );
  return response.json();
};
