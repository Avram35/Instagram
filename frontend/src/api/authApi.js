import { authFetch } from "./authFetch";

const AUTH_API_URL = import.meta.env.VITE_AUTH_API;

export const signin = async (usernameOrEmail, password) => {
  const response = await fetch(`${AUTH_API_URL}/api/v1/auth/signin`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ usernameOrEmail, password }),
  });
  return { data: await response.json(), ok: response.ok };
};

export const signup = async (fname, lname, username, email, password) => {
  const response = await fetch(`${AUTH_API_URL}/api/v1/auth/signup`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ fname, lname, username, email, password }),
  });
  return { data: await response.json(), ok: response.ok };
};

export const deleteAccount = async () => {
  return authFetch(`${AUTH_API_URL}/api/v1/auth/delete`, { method: "DELETE" });
};
