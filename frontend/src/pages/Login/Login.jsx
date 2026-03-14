import React, { useContext, useEffect, useState } from "react";
import "./Login.css";
import { assets } from "../../assets/assets";
import { AppContext } from "../../context/AppContext";

const Login = () => {
  const [fname, setFname] = useState("");
  const [lname, setLname] = useState("");
  const [username, setUserName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [enableShowPassword, setEnableShowPassword] = useState(false);
  const [enableShowConfirmPassword, setEnableShowConfirmPassword] =
    useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isRegister, setIsRegister] = useState(false);
  const { user, register, login } = useContext(AppContext);
  const [error, setError] = useState("");

  useEffect(() => {
    if (password !== "") {
      setEnableShowPassword(true);
    } else {
      setEnableShowPassword(false);
    }
  }, [password]);

  useEffect(() => {
    if (confirmPassword !== "") {
      setEnableShowConfirmPassword(true);
    } else {
      setEnableShowConfirmPassword(false);
    }
  }, [confirmPassword]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isRegister) {
      if (
        !fname ||
        !lname ||
        !username ||
        !email ||
        !password ||
        !confirmPassword
      ) {
        setError("Сва поља су обавезна!");
        return;
      }

      if (password !== confirmPassword) {
        setError("Лозинке се не подударају!");
        return;
      }

      if (password.length < 6) {
        setError("Лозинка мора имати минимум 6 карактера!");
        return;
      }

      const result = await register(fname, lname, username, email, password);

      if (!result.success) {
        setError(result.message);
      }
    } else {
      if (!username || !password) {
        setError("Сва поља су обавезна!");
        return;
      }

      const result = await login(username, password);

      if (!result.success) {
        setError(result.message);
      }
    }
  };

  const toggleRegister = () => {
    setIsRegister((prev) => !prev);
    setError("");
    setFname("");
    setLname("");
    setUserName("");
    setEmail("");
    setPassword("");
    setConfirmPassword("");
  };
  return (
    <div className="login">
      <img src={assets.logo} alt="" />
      <form className="login-form" onSubmit={handleSubmit}>
        {isRegister && (
          <div className="input_login">
            <input
              type="text"
              placeholder="Име"
              value={fname}
              onChange={(e) => setFname(e.target.value)}
            />
          </div>
        )}
        {isRegister && (
          <div className="input_login">
            <input
              type="text"
              placeholder="Презиме"
              value={lname}
              onChange={(e) => setLname(e.target.value)}
            />
          </div>
        )}
        <div className="input_login">
          <input
            type="text"
            placeholder={
              isRegister ? "Корисничко име" : "Корисничко име или е-адреса"
            }
            value={username}
            onChange={(e) => setUserName(e.target.value)}
          />
        </div>
        {isRegister && (
          <div className="input_login">
            <input
              type="text"
              placeholder="Е-адреса"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
        )}
        <div className="input_login input_password">
          <input
            type={showPassword ? "text" : "password"}
            placeholder="Лозинка"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {enableShowPassword ? (
            <button
              className="show_button"
              type="button"
              onClick={() => {
                setShowPassword((prev) => !prev);
              }}
            >
              {showPassword ? "Сакриј" : "Прикажи"}
            </button>
          ) : (
            ""
          )}
        </div>
        {isRegister && (
          <div className="input_login input_password">
            <input
              type={showConfirmPassword ? "text" : "password"}
              placeholder="Потврда лозинке"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
            {enableShowConfirmPassword ? (
              <button
                className="show_button"
                type="button"
                onClick={() => {
                  setShowConfirmPassword((prev) => !prev);
                }}
              >
                {showConfirmPassword ? "Сакриј" : "Прикажи"}
              </button>
            ) : (
              ""
            )}
          </div>
        )}
        {error && <p className="error">{error}</p>}
        <button className="login_button" type="submit">
          {isRegister ? "Региструј се" : "Пријави се"}
        </button>
        {!isRegister ? (
          <p className="no-nalog">
            Немате налог?{" "}
            <span className="register-span" onClick={() => toggleRegister()}>
              Региструјте се
            </span>
          </p>
        ) : (
          <p className="no-nalog">
            Имате налог?{" "}
            <span className="register-span" onClick={() => toggleRegister()}>
              Пријавите се
            </span>
          </p>
        )}
      </form>
    </div>
  );
};

export default Login;
