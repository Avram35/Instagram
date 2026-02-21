import React, { useContext } from "react";
import Navbar from "./components/Navbar/Navbar";
import { Navigate, Route, Routes } from "react-router-dom";
import Feed from "./pages/Feed/Feed";
import Profile from "./pages/Profile/Profile";
import { AppContext } from "./context/AppContext";
import Login from "./pages/Login/Login";

const App = () => {
  const { user } = useContext(AppContext);

  return (
    <div className="app">
      {user && (
        <>
          <Navbar />
        </>
      )}

      <Routes>
        <Route path="/" element={user ? <Feed /> : <Navigate to="/login" />} />
        <Route
          path="/profile/:profileId"
          element={user ? <Profile /> : <Navigate to="/login" />}
        />
        <Route path="/login" element={user ? <Navigate to="/" /> : <Login />} />
      </Routes>
    </div>
  );
};

export default App;
