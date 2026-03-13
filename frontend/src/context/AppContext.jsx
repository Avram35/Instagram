import { createContext, useEffect, useState } from "react";

export const AppContext = createContext();

const API_URL = "http://localhost:8081/api/v1/auth";
const USER_API_URL = "http://localhost:8082/api/v1/user";

const AppContextProvider = (props) => {
  const [user, setUser] = useState(null);

  const login = async (usernameOrEmail, password) => {
    try {
      const response = await fetch(`${API_URL}/signin`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ usernameOrEmail, password }),
      });

      const data = await response.json();

      if (response.ok) {
        localStorage.setItem("token", data.token);

        // const userData = data.user || { username: usernameOrEmail };
        const userResponse = await fetch(`${USER_API_URL}/${usernameOrEmail}`, {
          headers: {
            Authorization: `Bearer ${data.token}`,
          },
        });
        const userData = await userResponse.json();

        setUser(userData);
        localStorage.setItem("user", JSON.stringify(userData));

        return { success: true, message: "Успешна пријава!" };
      } else {
        return { success: false, message: data.error };
      }
    } catch (error) {
      console.error("Login error:", error);
      return {
        success: false,
        message: "Грешка при повезивању са сервером",
      };
    }
  };

  const register = async (fname, lname, username, email, password) => {
    try {
      const response = await fetch(`${API_URL}/signup`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ fname, lname, username, email, password }),
      });

      const data = await response.json();

      if (response.ok) {
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
      return {
        success: false,
        message: "Грешка при повезивању са сервером",
      };
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
      setUser(JSON.parse(storedUser));
    }
  }, []);

  const value = {
    user,
    login,
    register,
    logout,
    updateUser,
  };

  return (
    <AppContext.Provider value={value}>{props.children}</AppContext.Provider>
  );
};

export default AppContextProvider;
