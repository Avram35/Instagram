import React, { useContext, useEffect, useRef, useState } from "react";
import "./Navbar.css";
import { assets } from "../../assets/assets";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { AppContext } from "../../context/AppContext";
import {
  fetchNotifications,
  fetchPendingRequests,
  markNotificationsAsRead,
} from "../../api/followApi";
import { getUserAvatarUrl } from "../../api/userApi";

const Navbar = ({
  setSearchNotification,
  searchNotification,
  morePanel,
  setMorePanel,
  panRef,
  morePanRef,
  setCreatePost,
  createPost,
}) => {
  const [active, setActive] = useState("pocetak");
  const navigate = useNavigate();
  const location = useLocation();
  const isFeed = location.pathname === "/";
  const moreDivRef = useRef(null);
  const searchDivRef = useRef(null);
  const notificationDivRef = useRef(null);
  const [hasUnread, setHasUnread] = useState(false);

  const { user } = useContext(AppContext);

  const isProfile = location.pathname === `/profile/${user.username}`;

  useEffect(() => {
    if (!isFeed && !isProfile) {
      setActive("");
    } else if (isFeed) {
      setActive("pocetak");
    } else if (isProfile) {
      setActive("profil");
    }
  }, [isFeed, isProfile]);

  useEffect(() => {
    const handleClick = (e) => {
      if (morePanel) {
        if (
          morePanRef.current &&
          !morePanRef.current.contains(e.target) &&
          !moreDivRef.current.contains(e.target)
        ) {
          setMorePanel(false);
          setActive((prev) =>
            prev === "jos"
              ? isFeed
                ? "pocetak"
                : isProfile
                  ? "profil"
                  : ""
              : "",
          );
        }
      }
      if (searchNotification) {
        if (
          panRef.current &&
          !panRef.current.contains(e.target) &&
          !searchDivRef.current.contains(e.target) &&
          !notificationDivRef.current.contains(e.target)
        ) {
          setSearchNotification(null);
          setActive((prev) =>
            prev === "pretraga"
              ? isFeed
                ? "pocetak"
                : isProfile
                  ? "profil"
                  : ""
              : prev === "obavestenja"
                ? isFeed
                  ? "pocetak"
                  : isProfile
                    ? "profil"
                    : ""
                : "",
          );
        }
      }
    };

    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [morePanel, searchNotification]);

  useEffect(() => {
    const checkUnread = async () => {
      try {
        const [notifs, pending] = await Promise.all([
          fetchNotifications(),
          fetchPendingRequests(),
        ]);
        const lastNotif = notifs[0];
        setHasUnread(pending.length > 0 || (lastNotif && !lastNotif.read));
      } catch (e) {}
    };
    checkUnread();
  }, []);

  useEffect(() => {
    if (!createPost && active === "objavi") {
      setActive(isFeed ? "pocetak" : isProfile ? "profil" : "");
    }
  }, [createPost]);

  return (
    <div className="navbar">
      <Link
        to="/"
        onClick={() => {
          setActive("pocetak");
          setSearchNotification(null);
          setMorePanel(false);
        }}
        className="logo_div"
      >
        <img src={assets.logo} alt="" className="logo" />
      </Link>
      <ul className="nav">
        <li>
          <Link
            to="/"
            className={
              active === "pocetak" ? "navbar-element active" : "navbar-element"
            }
            onClick={() => {
              setActive("pocetak");
              setSearchNotification(null);
              setMorePanel(false);
            }}
          >
            <img
              src={active === "pocetak" ? assets.home1 : assets.home}
              alt=""
            />
            <span>Почетак</span>
          </Link>
        </li>
        <li>
          <div
            id="search"
            ref={searchDivRef}
            className={
              active === "pretraga" ? "navbar-element active" : "navbar-element"
            }
            onClick={() => {
              setActive((prev) =>
                prev === "pretraga"
                  ? isFeed
                    ? "pocetak"
                    : isProfile
                      ? "profil"
                      : ""
                  : "pretraga",
              );
              if (searchNotification === "pretraga") {
                setSearchNotification(null);
              } else {
                setSearchNotification("pretraga");
              }
              setMorePanel(false);
            }}
          >
            <img
              src={active === "pretraga" ? assets.search1 : assets.search}
              alt=""
            />
            <span>Претрага</span>
          </div>
        </li>
        <li>
          <div
            id="notification"
            ref={notificationDivRef}
            className={
              active === "obavestenja"
                ? "navbar-element active"
                : "navbar-element"
            }
            onClick={() => {
              if (searchNotification !== "obavestenja") {
                setHasUnread(false);
                markNotificationsAsRead();
              }
              setActive((prev) =>
                prev === "obavestenja"
                  ? isFeed
                    ? "pocetak"
                    : isProfile
                      ? "profil"
                      : ""
                  : "obavestenja",
              );
              if (searchNotification === "obavestenja") {
                setSearchNotification(null);
              } else {
                setSearchNotification("obavestenja");
              }
              setMorePanel(false);
            }}
          >
            <div style={{ position: "relative", display: "inline-flex" }}>
              <img
                src={active === "obavestenja" ? assets.heart1 : assets.heart}
                alt=""
              />
              {hasUnread && (
                <span
                  className="unread_notifications"
                  style={{
                    position: "absolute",
                    top: -3,
                    right: -3,
                    width: 8,
                    height: 8,
                    borderRadius: "50%",
                    backgroundColor: "red",
                  }}
                />
              )}
            </div>
            <span>Обавештења</span>
          </div>
        </li>
        <li>
          <div
            className={
              active === "objavi" ? "navbar-element active" : "navbar-element"
            }
            onClick={() => {
              setActive("objavi");
              setSearchNotification(null);
              setMorePanel(false);
              setCreatePost(true);
            }}
          >
            <img
              src={active === "objavi" ? assets.plus1 : assets.plus}
              alt=""
            />
            <span>Објави</span>
          </div>
        </li>
        <li>
          <div
            className={
              active === "profil" ? "navbar-element active" : "navbar-element"
            }
            onClick={() => {
              setActive("profil");
              setSearchNotification(null);
              setMorePanel(false);
              navigate(`/profile/${user.username}`);
            }}
          >
            <div
              className={`img_wrapper ${active === "profil" ? "active" : ""}`}
            >
              <img
                src={getUserAvatarUrl(
                  user.profilePictureUrl,
                  assets.noProfilePic,
                )}
                alt=""
                className="profile_pic"
              />{" "}
            </div>
            <span>Профил</span>
          </div>
        </li>
      </ul>

      <div
        id="more"
        ref={moreDivRef}
        className={
          active === "jos" ? "navbar-element active" : "navbar-element"
        }
        onClick={() => {
          setMorePanel((prev) => !prev);
          setActive((prev) =>
            prev === "jos"
              ? isFeed
                ? "pocetak"
                : isProfile
                  ? "profil"
                  : ""
              : "jos",
          );
        }}
      >
        <img src={active === "jos" ? assets.menu1 : assets.menu} />
        <span>Још</span>
      </div>
    </div>
  );
};

export default Navbar;
