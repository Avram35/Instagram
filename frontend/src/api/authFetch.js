export const authFetch = async (url, options = {}) => {
  const token = localStorage.getItem("token");
  const response = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      Authorization: `Bearer ${token}`,
    },
  });

  if (response.status === 401) {
    window.dispatchEvent(new Event("unauthorized"));
  }

  return response;
};
