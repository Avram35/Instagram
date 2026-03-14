import React, { useContext } from "react";
import "./MorePanel.css";
import { AppContext } from "../../context/AppContext";

const MorePanel = ({ morePanel, morePanRef, setMorePanel }) => {
  const { logout } = useContext(AppContext);
  return (
    <div
      id="more_panel"
      ref={morePanRef}
      className={`more_panel ${morePanel ? "show_more_panel" : ""}`}
    >
      {morePanel ? (
        <button
          className="button_logout"
          onClick={() => {
            logout();
            setMorePanel(false);
          }}
        >
          Одјавите се
        </button>
      ) : (
        ""
      )}
    </div>
  );
};

export default MorePanel;
