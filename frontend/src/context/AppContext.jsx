import { createContext, useEffect, useState } from "react";

import { fetchProfileInfo } from "../api/userApi";
import { signin, signup } from "../api/authApi";

export const AppContext = createContext();

const AppContextProvider = (props) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [onPostCreated, setOnPostCreated] = useState(null);

  const login = async (usernameOrEmail, password) => {
    try {
      const { data, ok } = await signin(usernameOrEmail, password);

      if (ok) {
        localStorage.setItem("token", data.token);

        const userData = await fetchProfileInfo(data.username);
        setUser(userData);
        localStorage.setItem("user", JSON.stringify(userData));
        return { success: true, message: "Успешна пријава!" };
      } else {
        return { success: false, message: data.error };
      }
    } catch (error) {
      console.error("Login error:", error);
      return { success: false, message: "Грешка при повезивању са сервером" };
    }
  };

  const register = async (fname, lname, username, email, password) => {
    try {
      const { data, ok } = await signup(
        fname,
        lname,
        username,
        email,
        password,
      );

      if (ok) {
        return await login(username, password);
      } else {
        if (data.error) {
          return { success: false, message: data.error };
        } else {
          const firstError = Object.values(data)[0];
          return { success: false, message: firstError };
        }
      }
    } catch (error) {
      console.error("Register error:", error);
      return { success: false, message: "Грешка при повезивању са сервером" };
    }
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem("token");
    localStorage.removeItem("user");
  };

  const updateUser = (updatedData) => {
    const newUser = { ...user, ...updatedData };
    setUser(newUser);
    localStorage.setItem("user", JSON.stringify(newUser));
  };

  useEffect(() => {
    const storedUser = localStorage.getItem("user");
    if (storedUser) {
      const parsed = JSON.parse(storedUser);
      fetchProfileInfo(parsed.username)
        .then((freshUser) => {
          if (!freshUser || !freshUser.username) {
            setUser(null);
            localStorage.removeItem("token");
            localStorage.removeItem("user");
            return;
          }
          setUser(freshUser);
          localStorage.setItem("user", JSON.stringify(freshUser));
        })
        .catch(() => {
          setUser(null);
          localStorage.removeItem("token");
          localStorage.removeItem("user");
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const handleUnauthorized = () => logout();
    window.addEventListener("unauthorized", handleUnauthorized);
    return () => window.removeEventListener("unauthorized", handleUnauthorized);
  }, []);

  const value = {
    user,
    login,
    register,
    logout,
    updateUser,
    loading,
    onPostCreated,
    setOnPostCreated,
  };

  return (
    <AppContext.Provider value={value}>{props.children}</AppContext.Provider>
  );
};

export default AppContextProvider;
