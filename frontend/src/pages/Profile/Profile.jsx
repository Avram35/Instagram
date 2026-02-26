import React, { useContext, useEffect, useRef, useState } from "react";
import "./Profile.css";
import { Navigate, useNavigate, useParams } from "react-router-dom";
import { assets } from "../../assets/assets";
import { AppContext } from "../../context/AppContext";

const USER_API_URL = "http://localhost:8082/api/v1/user";
const FOLLOW_API_URL = "http://localhost:8083/api/v1/follow";

const Profile = () => {
  const { username } = useParams();
  const [profileInfo, setProfileInfo] = useState(null);
  const { user } = useContext(AppContext);
  const navigate = useNavigate();
  const [followCount, setFollowCount] = useState(null);

  const fetchProfileInfo = async () => {
    try {
      const token = localStorage.getItem("token");
      const response = await fetch(`${USER_API_URL}/${username}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      const data = await response.json();
      setProfileInfo(data);
    } catch (error) {
      console.error("Error fetching profile:", error);
    }
  };

  const fetchFollowCount = async () => {
    try {
      const token = localStorage.getItem("token");
      const response = await fetch(
        `${FOLLOW_API_URL}/${profileInfo.id}/count`,
        {
          headers: { Authorization: `Bearer ${token}` },
        },
      );
      const data = await response.json();
      setFollowCount(data);
    } catch (error) {
      console.error("Error fetching follow count:", error);
    }
  };

  useEffect(() => {
    fetchProfileInfo();
  }, [username]);

  useEffect(() => {
    if (profileInfo) {
      fetchFollowCount();
    }
  }, [profileInfo]);

  return (
    <div>
      {profileInfo ? (
        <div className="profile_div">
          <div className="profile_top">
            <div className="profile_info_div">
              <img
                src={profileInfo.profilePictureUrl || assets.noProfilePic}
                alt=""
              />
              <div className="profile_info">
                <span className="username">{profileInfo.username}</span>
                <span className="name">
                  {profileInfo.fname} {profileInfo.lname}
                </span>
                <div className="followers">
                  <span>
                    <span className="number">0</span> објава
                  </span>
                  <span>
                    <span className="number">
                      {followCount?.followersCount ?? 0}
                    </span>{" "}
                    пратилаца
                  </span>
                  <span>
                    <span className="number">
                      {followCount?.followingCount ?? 0}
                    </span>{" "}
                    прати
                  </span>
                </div>
                <span className="bio">{profileInfo.bio}</span>
              </div>
            </div>

            {user.username === username ? (
              <button
                className="edit_profile_button"
                onClick={() => navigate(`/edit-profile`)}
              >
                Измените профил
              </button>
            ) : (
              <button className="follow_button">Прати</button>
            )}
          </div>

          <h2 className="profile-privacy">
            {profileInfo.privateProfile
              ? "Налог је приватан"
              : "Налог је јаван"}
          </h2>
        </div>
      ) : (
        <h1>Loading...</h1>
      )}
    </div>
  );
};

export default Profile;
