import React, { useContext, useEffect, useRef, useState } from "react";
import "./Profile.css";
import { useNavigate, useParams } from "react-router-dom";
import { assets } from "../../assets/assets";

import SinglePost from "../../components/SinglePost/SinglePost";
import { AppContext } from "../../context/AppContext";
import FollowersModal from "../../components/FollowersModal/FollowersModal";

import { fetchProfileInfo, getUserAvatarUrl } from "../../api/userApi";
import {
  fetchFollowCount,
  checkFollow,
  toggleFollow,
} from "../../api/followApi";
import { fetchPosts, fetchPostCount, getPostMediaUrl } from "../../api/postApi";
import { checkBlock, toggleBlock } from "../../api/blockApi";
import CustomConfirm from "../../components/CustomConfirm/CustomConfirm";

const Profile = () => {
  const { username } = useParams();
  const [profileInfo, setProfileInfo] = useState(null);
  const [singlePost, setSinglePost] = useState(false);
  const singlePostRef = useRef(null);
  const [postId, setPostId] = useState();
  const { user, setOnPostCreated } = useContext(AppContext);
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
  const [showMenu, setShowMenu] = useState(false);
  const menuRef = useRef(null);
  const [isBlocked, setIsBlocked] = useState(false);
  const [blockChecked, setBlockChecked] = useState(false);
  const [confirm, setConfirm] = useState(null);

  const formatPost = (count) => {
    const last2 = count % 100;
    const last1 = count % 10;
    if (last2 >= 11 && last2 <= 14) return `${count} објава`;
    if (last1 === 1) return `${count} објава`;
    if (last1 >= 2 && last1 <= 4) return `${count} објаве`;
    return `${count} објава`;
  };

  const formatFollowers = (count) => {
    const last2 = count % 100;
    const last1 = count % 10;
    if (last2 >= 11 && last2 <= 14) return `${count} пратилаца`;
    if (last1 === 1) return `${count} пратилац`;
    if (last1 >= 2 && last1 <= 4) return `${count} пратиоца`;
    return `${count} пратилаца`;
  };

  const renderPostThumbnail = (post, index) => {
    const isVideo = post.media[0].mediaUrl.match(/\.(mp4|mov|avi|webm)$/i);
    return (
      <div
        key={index}
        className="post_thumbnail_wrapper"
        onClick={() => {
          setSinglePost(true);
          setPostInfo(post);
        }}
      >
        {isVideo ? (
          <video src={getPostMediaUrl(post.media[0].mediaUrl)} />
        ) : (
          <img src={getPostMediaUrl(post.media[0].mediaUrl)} alt="" />
        )}
        {post.media.length > 1 && (
          <img src={assets.more_media} alt="" className="multi_media_icon" />
        )}
      </div>
    );
  };

  const loadProfileInfo = async () => {
    setPosts(null);
    setIsBlocked(false);
    setIsFollowing(false);
    setIsPending(false);
    setBlockChecked(false);
    try {
      const data = await fetchProfileInfo(username);
      setProfileInfo(data);
    } catch (error) {
      console.error("Error fetching profile:", error);
    }
  };

  const loadFollowCount = async () => {
    try {
      const data = await fetchFollowCount(profileInfo.id);
      setFollowCount(data);
    } catch (error) {
      console.error("Error fetching follow count:", error);
    }
  };

  const loadPosts = async () => {
    try {
      const data = await fetchPosts(profileInfo.id);
      setPosts(data);
    } catch (error) {
      console.error("Error fetching posts: ", error);
    }
  };

  const loadPostCount = async () => {
    try {
      const data = await fetchPostCount(profileInfo.id);
      setPostCount(data.count ?? 0);
    } catch (error) {
      console.error("Error fetching post count:", error);
    }
  };

  const loadCheckFollow = async () => {
    if (!profileInfo?.id) return;
    try {
      const { following, pending } = await checkFollow(profileInfo.id);
      setIsFollowing(following);
      setIsPending(pending);
    } catch (error) {
      console.error("Error checking follow:", error);
    }
  };

  const loadCheckBlock = async () => {
    if (!profileInfo?.id) return;
    try {
      const data = await checkBlock(profileInfo.id);
      setIsBlocked(data.blocked);
      setBlockChecked(true);
    } catch (error) {
      console.error("Error checking block:", error);
      setBlockChecked(true);
    }
  };

  const handleBlock = async () => {
    const action = isBlocked ? "одблокирате" : "блокирате";
    const confirmed = await new Promise((resolve) =>
      setConfirm({
        message: `Да ли сте сигурни да желите да ${action} корисника ${profileInfo.username}?`,
        resolve,
      }),
    );
    if (!confirmed) return;
    try {
      await toggleBlock(profileInfo.id, isBlocked);
      const newBlocked = !isBlocked;
      setIsBlocked(newBlocked);
      setShowMenu(false);
      if (!newBlocked) {
        loadPosts();
      } else {
        setPosts(null);
      }
    } catch (error) {
      console.error("Error blocking/unblocking:", error);
    }
  };

  const handleFollow = async () => {
    try {
      const { data, method } = await toggleFollow(
        profileInfo.id,
        isFollowing,
        isPending,
      );
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
      loadFollowCount();
    } catch (error) {
      console.error("Error following/unfollowing:", error);
    }
  };

  useEffect(() => {
    loadProfileInfo();
  }, [username]);

  useEffect(() => {
    if (!profileInfo) return;
    loadFollowCount();
    loadPostCount();
    if (user.username !== username) {
      const init = async () => {
        const followData = await checkFollow(profileInfo.id);
        setIsFollowing(followData.following);
        setIsPending(followData.pending);

        const blockData = await checkBlock(profileInfo.id);
        setIsBlocked(blockData.blocked);
        setBlockChecked(true);

        if (
          !blockData.blocked &&
          (!profileInfo.privateProfile || followData.following)
        ) {
          await loadPosts();
        }
      };
      init();
    } else {
      loadPosts();
    }
  }, [profileInfo]);

  useEffect(() => {
    const handleClick = (e) => {
      if (
        singlePost &&
        singlePostRef.current &&
        !singlePostRef.current.contains(e.target)
      ) {
        setSinglePost(false);
      }
      if (
        followersModal &&
        followersModalRef.current &&
        !followersModalRef.current.contains(e.target)
      ) {
        setFollowersModal(false);
      }
      if (showMenu && menuRef.current && !menuRef.current.contains(e.target)) {
        setShowMenu(false);
      }
    };
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [singlePost, followersModal, showMenu]);

  useEffect(() => {
    if (user.username === username) {
      setOnPostCreated(() => () => {
        loadPosts();
        loadPostCount();
      });
      return () => setOnPostCreated(null);
    }
  }, [profileInfo]);

  return (
    <div>
      {profileInfo ? (
        <div className="profile_div">
          <div className="profile_top">
            <div className="profile_info_div">
              <img
                src={getUserAvatarUrl(
                  profileInfo.profilePictureUrl,
                  assets.noProfilePic,
                )}
                alt=""
              />
              <div className="profile_info">
                <div className="username_row">
                  <span className="username">{profileInfo.username}</span>
                  {user.username !== username && (
                    <div className="post_menu_wrapper" ref={menuRef}>
                      <img
                        src={assets.more}
                        alt=""
                        className="img_more"
                        onClick={() => setShowMenu((prev) => !prev)}
                      />
                      {showMenu && (
                        <div className="post_menu">
                          <span className="delete_option" onClick={handleBlock}>
                            {isBlocked ? "Одблокирај" : "Блокирај"}
                          </span>
                        </div>
                      )}
                    </div>
                  )}
                </div>
                <span className="name">
                  {profileInfo.fname} {profileInfo.lname}
                </span>
                <div className="followers">
                  <span>
                    <span className="number">{postCount}</span>{" "}
                    {formatPost(postCount).replace(postCount, "").trim()}
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
                    {formatFollowers(followCount?.followersCount ?? 0)
                      .replace(followCount?.followersCount ?? 0, "")
                      .trim()}
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
            ) : isBlocked ? (
              <button className="edit_profile_button" disabled>
                Блокиран корисник
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
                {posts.map((post, index) => renderPostThumbnail(post, index))}
              </div>
            ) : (
              <div className="no_pictures_div">
                <img src={assets.instagramPhoto} alt="" />
                <h2>Поделите фотографије</h2>
                <p>Кад поделите фотографије, приказаће се на вашем профилу.</p>
              </div>
            )
          ) : isBlocked ? null : profileInfo.privateProfile && !isFollowing ? (
            <div className="no_pictures_div">
              <img src={assets.instagramPhoto} alt="" />
              <h2>Овај налог је приватан</h2>
              <p>
                Пратите овај налог да бисте видели фотографије и видео запусе.
              </p>
            </div>
          ) : posts?.length > 0 ? (
            <div className="profile_bottom">
              {posts.map((post, index) => renderPostThumbnail(post, index))}
            </div>
          ) : posts?.length === 0 ? (
            <div className="no_pictures_yet_div">
              <img src={assets.instagramPhoto} alt="" />
              <h2>Још нема објава</h2>
            </div>
          ) : null}
        </div>
      ) : (
        <div className="loading_screen">
          <p>Учитава се...</p>
        </div>
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
            onClose={() => setSinglePost(false)}
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
      {confirm && (
        <CustomConfirm
          message={confirm.message}
          onConfirm={() => {
            confirm.resolve(true);
            setConfirm(null);
          }}
          onCancel={() => {
            confirm.resolve(false);
            setConfirm(null);
          }}
        />
      )}
    </div>
  );
};

export default Profile;
