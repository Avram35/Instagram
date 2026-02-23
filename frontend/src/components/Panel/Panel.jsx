import React from "react";
import "./Panel.css";
import Search from "../Search/Search";
import Notification from "../Notification/Notification";

const Panel = ({ searchNotification, panRef }) => {
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
        <Search />
      ) : searchNotification === "obavestenja" ? (
        <Notification />
      ) : (
        ""
      )}
    </div>
  );
};

export default Panel;
