import React, { useContext, useRef } from "react";
import Navbar from "./components/Navbar/Navbar";
import { Navigate, Route, Routes } from "react-router-dom";
import Feed from "./pages/Feed/Feed";
import Profile from "./pages/Profile/Profile";
import { AppContext } from "./context/AppContext";
import Login from "./pages/Login/Login";
import MorePanel from "./components/MorePanel/MorePanel";
import Panel from "./components/Panel/Panel";

const App = () => {
  const { user } = useContext(AppContext);
  const [morePanel, setMorePanel] = React.useState(false);
  const [searchNotification, setSearchNotification] = React.useState(null);

  const morePanRef = useRef(null);
  const panRef = useRef(null);

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
          />
          <Panel searchNotification={searchNotification} panRef={panRef} />
          <MorePanel morePanel={morePanel} morePanRef={morePanRef} />
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
