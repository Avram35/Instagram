import React, { useContext, useEffect, useRef, useState } from "react";
import "./Profile.css";
import { Navigate, useNavigate, useParams } from "react-router-dom";
import { assets } from "../../assets/assets";

import SinglePost from "../../components/SinglePost/SinglePost";
import { AppContext } from "../../context/AppContext";

const USER_API_URL = "http://localhost:8082/api/v1/user";
const FOLLOW_API_URL = "http://localhost:8083/api/v1/follow";
const POST_API_URL = "http://localhost:8086/api/v1/post";

const Profile = () => {
  const { username } = useParams();
  const [profileInfo, setProfileInfo] = useState(null);
  const [singlePost, setSinglePost] = useState(false);
  const singlePostRef = useRef(null);
  const [postId, setPostId] = useState();
  const { user } = useContext(AppContext);
  const navigate = useNavigate();
  const [followCount, setFollowCount] = useState(null);
  const [posts, setPosts] = useState(null);
  const [postInfo, setPostInfo] = useState(null);

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

  const fetchPosts = async () => {
    try {
      const token = localStorage.getItem("token");
      const response = await fetch(`${POST_API_URL}/user/${profileInfo.id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      const data = await response.json();
      setPosts(data);
    } catch (error) {
      console.error("Error fetching posts: ", error);
    }
  };

  useEffect(() => {
    fetchProfileInfo();
  }, [username]);

  useEffect(() => {
    if (profileInfo) {
      fetchFollowCount();
    }
    if (profileInfo) {
      fetchPosts();
    }
  }, [profileInfo]);

  useEffect(() => {
    console.log(posts);
  }, [posts]);

  useEffect(() => {
    const handleClick = (e) => {
      if (singlePost) {
        if (!singlePostRef.current.contains(e.target)) {
          setSinglePost(false);
        }
      }
    };

    document.addEventListener("mousedown", handleClick);

    return () => {
      document.removeEventListener("mousedown", handleClick);
    };
  }, [singlePost]);

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

          {user.username === username ? (
            posts && posts.length > 0 ? (
              <div className="profile_bottom">
                {posts.map((post, index) => (
                  <img
                    src={`http://localhost:8086${post.media[0].mediaUrl}`}
                    key={index}
                    onClick={() => {
                      setSinglePost(true);
                      setPostInfo(post);
                    }}
                  />
                ))}
              </div>
            ) : (
              <div className="no_pictures_div">
                <img src={assets.instagramPhoto} alt="" />
                <h2>Поделите фотографије</h2>
                <p>Кад поделите фотографије, приказаће се на вашем профилу.</p>
              </div>
            )
          ) : profileInfo.privateProfile ? (
            <div className="no_pictures_div">
              <img src={assets.instagramPhoto} alt="" />
              <h2>Овај налог је приватан</h2>
              <p>
                Пратите овај налог да бисте видели фотографије и видео запусе.
              </p>
            </div>
          ) : posts && posts.length > 0 ? (
            <div className="profile_bottom">
              {posts.map((post, index) => (
                <img
                  src={`http://localhost:8086${post.media[0].mediaUrl}`}
                  key={index}
                  onClick={() => {
                    setSinglePost(true);
                    setPostInfo(post);
                  }}
                />
              ))}
            </div>
          ) : (
            <div className="no_pictures_yet_div">
              <img src={assets.instagramPhoto} alt="" />
              <h2>Још нема објава</h2>
            </div>
          )}
        </div>
      ) : (
        <h1>Loading...</h1>
      )}
      {singlePost ? (
        <div className="overlay">
          <SinglePost singlePostRef={singlePostRef} postInfo={postInfo} />
        </div>
      ) : (
        ""
      )}
    </div>
  );
};

export default Profile;
