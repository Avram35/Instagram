import React from "react";
import "./Panel.css";
import Search from "../Search/Search";
import Notification from "../Notification/Notification";

const Panel = ({ searchNotification, panRef, setSearchNotification }) => {
  return (
    <div
      id="panel"
      ref={panRef}
      className={`panel ${
        searchNotification === "pretraga" ||
        searchNotification === "obavestenja"
          ? "show"
          : ""
      }`}
    >
      {searchNotification === "pretraga" ? (
        <Search setSearchNotification={setSearchNotification} />
      ) : searchNotification === "obavestenja" ? (
        <Notification setSearchNotification={setSearchNotification} />
      ) : (
        ""
      )}
    </div>
  );
};

export default Panel;
