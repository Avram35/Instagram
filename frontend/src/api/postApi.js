import { authFetch } from "./authFetch";

const POST_API_URL = import.meta.env.VITE_POST_API;

export const fetchPosts = async (userId) => {
  const response = await authFetch(
    `${POST_API_URL}/api/v1/post/user/${userId}`,
  );

  if (response.status === 403) return null;
  if (!response.ok) throw new Error("Failed to fetch posts");

  return response.json();
};

export const fetchPostCount = async (userId) => {
  const response = await authFetch(
    `${POST_API_URL}/api/v1/post/count/${userId}`,
  );
  return response.json();
};

export const getPostMediaUrl = (mediaUrl) => `${POST_API_URL}${mediaUrl}`;

export const updatePost = async (postId, description) => {
  const response = await authFetch(`${POST_API_URL}/api/v1/post/${postId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ description }),
  });
  return response.json();
};

export const deletePost = async (postId) => {
  await authFetch(`${POST_API_URL}/api/v1/post/${postId}`, {
    method: "DELETE",
  });
};

export const deletePostMedia = async (postId, mediaId) => {
  const response = await authFetch(
    `${POST_API_URL}/api/v1/post/${postId}/media/${mediaId}`,
    {
      method: "DELETE",
    },
  );
  return response.json();
};

export const createPost = async (files, description) => {
  const formData = new FormData();
  files.forEach((file) => formData.append("files", file));
  if (description) formData.append("description", description);
  return authFetch(`${POST_API_URL}/api/v1/post`, {
    method: "POST",
    body: formData,
  });
};
