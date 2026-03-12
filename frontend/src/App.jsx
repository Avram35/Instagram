import React from "react";
import Navbar from "./components/Navbar/Navbar";
import { Route, Routes } from "react-router-dom";
import Feed from "./pages/Feed/Feed";
import Profile from "./pages/Profile/Profile";

const App = () => {
  return (
    <div className="app">
      <Navbar />

      <Routes>
        <Route path="/" element={<Feed />} />
        <Route path="/profile/:profileId" element={<Profile />} />
      </Routes>
    </div>
  );
};

export default App;
