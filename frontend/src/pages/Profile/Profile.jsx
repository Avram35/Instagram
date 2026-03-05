import React, { useContext, useEffect, useRef, useState } from "react";
import "./Profile.css";
import { useNavigate, useParams } from "react-router-dom";
import { assets } from "../../assets/assets";

import SinglePost from "../../components/SinglePost/SinglePost";
import { AppContext } from "../../context/AppContext";
import FollowersModal from "../../components/FollowersModal/FollowersModal";

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
  const [postCount, setPostCount] = useState(0);
  const [postInfo, setPostInfo] = useState(null);
  const [isFollowing, setIsFollowing] = useState(false);
  const [isPending, setIsPending] = useState(false);
  const [followersModal, setFollowersModal] = useState(false);
  const followersModalRef = useRef(null);
  const [followersFollowing, setFollowersFollowing] = useState(null);

  const fetchProfileInfo = async () => {
    try {
      const token = localStorage.getItem("token");
      const response = await fetch(`${USER_API_URL}/${username}`, {
        headers: { Authorization: `Bearer ${token}` },
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

  const fetchPostCount = async () => {
    try {
      const token = localStorage.getItem("token");
      const response = await fetch(`${POST_API_URL}/count/${profileInfo.id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await response.json();
      setPostCount(data.count ?? 0);
    } catch (error) {
      console.error("Error fetching post count:", error);
    }
  };

  const checkFollow = async () => {
    if (!profileInfo?.id) return;
    try {
      const token = localStorage.getItem("token");

      const [followRes, pendingRes] = await Promise.all([
        fetch(`${FOLLOW_API_URL}/check/${profileInfo.id}`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
        fetch(`${FOLLOW_API_URL}/requests/check/${profileInfo.id}`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
      ]);

      const followData = await followRes.json();
      const pendingData = await pendingRes.json();

      setIsFollowing(followData.following);
      setIsPending(pendingData.pending);
    } catch (error) {
      console.error("Error checking follow:", error);
    }
  };

  const handleFollow = async () => {
    try {
      const token = localStorage.getItem("token");
      const method = isFollowing || isPending ? "DELETE" : "POST";

      const response = await fetch(`${FOLLOW_API_URL}/${profileInfo.id}`, {
        method,
        headers: { Authorization: `Bearer ${token}` },
      });

      const data = await response.json();

      if (method === "DELETE") {
        setIsFollowing(false);
        setIsPending(false);
      } else {
        if (
          data.message?.includes("захтев") ||
          data.message?.includes("послат")
        ) {
          setIsPending(true);
          setIsFollowing(false);
        } else {
          setIsFollowing(true);
          setIsPending(false);
        }
      }

      fetchFollowCount();
    } catch (error) {
      console.error("Error following/unfollowing:", error);
    }
  };

  useEffect(() => {
    fetchProfileInfo();
  }, [username]);

  useEffect(() => {
    if (!profileInfo) return;
    fetchFollowCount();
    fetchPosts();
    fetchPostCount();
    if (user.username !== username) {
      checkFollow();
    }
  }, [profileInfo]);

  useEffect(() => {
    const handleClick = (e) => {
      if (singlePost && !singlePostRef.current.contains(e.target)) {
        setSinglePost(false);
      }
      if (followersModal && !followersModalRef.current.contains(e.target)) {
        setFollowersModal(false);
      }
    };
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [singlePost, followersModal]);

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
                    <span className="number">{postCount}</span> објава
                  </span>
                  <span
                    onClick={() => {
                      if (
                        profileInfo.privateProfile &&
                        !isFollowing &&
                        user.username !== username
                      )
                        return;
                      setFollowersModal(true);
                      setFollowersFollowing("Пратиоци");
                    }}
                    className="span_followers_count"
                  >
                    <span className="number">
                      {followCount?.followersCount ?? 0}
                    </span>{" "}
                    пратилаца
                  </span>
                  <span
                    onClick={() => {
                      if (
                        profileInfo.privateProfile &&
                        !isFollowing &&
                        user.username !== username
                      )
                        return;
                      setFollowersModal(true);
                      setFollowersFollowing("Пратите");
                    }}
                    className="span_followers_count"
                  >
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
              <button
                className={
                  isFollowing || isPending
                    ? "edit_profile_button"
                    : "follow_button"
                }
                onClick={handleFollow}
              >
                {isFollowing
                  ? "Отпрати"
                  : isPending
                    ? "Захтев послат"
                    : "Прати"}
              </button>
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

      {singlePost && (
        <div className="overlay">
          <SinglePost
            singlePostRef={singlePostRef}
            postInfo={postInfo}
            onPostDeleted={(postId) => {
              setPosts((prev) => prev.filter((p) => p.id !== postId));
              setPostCount((prev) => prev - 1);
              setSinglePost(false);
            }}
            onPostUpdated={(updatedPost) => {
              setPosts((prev) =>
                prev.map((p) => (p.id === updatedPost.id ? updatedPost : p)),
              );
            }}
          />
        </div>
      )}

      {followersModal && (
        <div className="overlay">
          <FollowersModal
            followersModalRef={followersModalRef}
            followersFollowing={followersFollowing}
            profileUserId={profileInfo.id}
            isOwnProfile={user.username === username}
            onClose={() => setFollowersModal(false)}
          />
        </div>
      )}
    </div>
  );
};

export default Profile;
