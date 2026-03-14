import React, { useContext, useEffect, useRef, useState } from "react";
import Navbar from "./components/Navbar/Navbar";
import { Navigate, Route, Routes } from "react-router-dom";
import Feed from "./pages/Feed/Feed";
import Profile from "./pages/Profile/Profile";
import { AppContext } from "./context/AppContext";
import Login from "./pages/Login/Login";
import MorePanel from "./components/MorePanel/MorePanel";
import Panel from "./components/Panel/Panel";
import EditProfile from "./pages/EditProfile/EditProfile";
import CreatePost from "./components/CreatePost/CreatePost";

const App = () => {
  const { user, loading, onPostCreated } = useContext(AppContext);
  const [morePanel, setMorePanel] = React.useState(false);
  const [searchNotification, setSearchNotification] = React.useState(null);
  const [createPost, setCreatePost] = useState(false);
  const createPostRef = useRef(null);

  const morePanRef = useRef(null);
  const panRef = useRef(null);

  useEffect(() => {
    const handleClick = (e) => {
      if (createPost) {
        if (!createPostRef.current.contains(e.target)) {
          setCreatePost(false);
        }
      }
    };

    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [createPost]);

  if (loading)
    return (
      <div className="loading_screen">
        <p>Учитава се...</p>
      </div>
    );

  return (
    <div className="app">
      {user && (
        <>
          <Navbar
            setSearchNotification={setSearchNotification}
            searchNotification={searchNotification}
            morePanel={morePanel}
            setMorePanel={setMorePanel}
            panRef={panRef}
            morePanRef={morePanRef}
            setCreatePost={setCreatePost}
            createPost={createPost}
          />
          <Panel
            searchNotification={searchNotification}
            panRef={panRef}
            setSearchNotification={setSearchNotification}
          />
          <MorePanel
            morePanel={morePanel}
            setMorePanel={setMorePanel}
            morePanRef={morePanRef}
          />
          {createPost && (
            <div className="overlay">
              <CreatePost
                createPostRef={createPostRef}
                setCreatePost={setCreatePost}
                onPostCreated={onPostCreated}
              />
            </div>
          )}
        </>
      )}

      <Routes>
        <Route path="/" element={user ? <Feed /> : <Navigate to="/login" />} />
        <Route
          path="/profile/:username"
          element={user ? <Profile /> : <Navigate to="/login" />}
        />
        <Route path="/login" element={user ? <Navigate to="/" /> : <Login />} />
        <Route
          path="/edit-profile"
          element={user ? <EditProfile /> : <Navigate to="/login" />}
        />
      </Routes>
    </div>
  );
};

export default App;
