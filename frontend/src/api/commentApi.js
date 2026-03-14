import { authFetch } from "./authFetch";

const INTERACTION_API_URL = import.meta.env.VITE_INTERACTION_API;

export const fetchCommentCount = async (postId) => {
  const response = await authFetch(
    `${INTERACTION_API_URL}/api/v1/comment/count/${postId}`,
  );
  return response.json();
};

export const fetchCommentList = async (postId) => {
  const response = await authFetch(
    `${INTERACTION_API_URL}/api/v1/comment/${postId}/list`,
  );
  return response.json();
};

export const addComment = async (postId, content) => {
  const response = await authFetch(
    `${INTERACTION_API_URL}/api/v1/comment/${postId}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ content }),
    },
  );
  return response.json();
};

export const deleteComment = async (commentId) => {
  await authFetch(`${INTERACTION_API_URL}/api/v1/comment/${commentId}`, {
    method: "DELETE",
  });
};

export const updateComment = async (commentId, content) => {
  const response = await authFetch(
    `${INTERACTION_API_URL}/api/v1/comment/${commentId}`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ content }),
    },
  );
  return response.json();
};
