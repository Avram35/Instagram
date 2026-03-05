import React, { useContext, useEffect, useState } from "react";
import "./SinglePost.css";
import { AppContext } from "../../context/AppContext";
import { assets } from "../../assets/assets";

const USER_API_URL = "http://localhost:8082/api/v1/user/id/";
const LIKE_API_URL = "http://localhost:8087/api/v1/like/count/";
const COMMENT_API_URL = "http://localhost:8087/api/v1/comment/count/";
const POST_API_URL = "http://localhost:8086/api/v1/post";

const SinglePost = ({
  singlePostRef,
  postInfo,
  onPostDeleted,
  onPostUpdated,
}) => {
  const [author, setAuthor] = useState();
  const [likeCounts, setLikeCounts] = useState(0);
  const [commentCounts, setCommentCounts] = useState(0);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [showMenu, setShowMenu] = useState(false);
  const [showEditDesc, setShowEditDesc] = useState(false);
  const [editDescription, setEditDescription] = useState(
    postInfo.description || "",
  );
  const [currentPost, setCurrentPost] = useState(postInfo);
  const { user } = useContext(AppContext);

  const isOwner =
    user?.id === currentPost.userId || user?.username === author?.username;

  useEffect(() => {
    if (!currentPost?.userId) return;
    const fetchUser = async () => {
      try {
        const token = localStorage.getItem("token");
        const response = await fetch(`${USER_API_URL}${currentPost.userId}`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        const data = await response.json();
        setAuthor(data);
      } catch (error) {
        console.error("Error fetching user:", error);
      }
    };
    fetchUser();
  }, [currentPost?.userId]);

  useEffect(() => {
    if (!currentPost?.id) return;
    const fetchCounts = async () => {
      try {
        const token = localStorage.getItem("token");
        const headers = { Authorization: `Bearer ${token}` };
        const [likesRes, commentsRes] = await Promise.all([
          fetch(`${LIKE_API_URL}${currentPost.id}`, { headers }),
          fetch(`${COMMENT_API_URL}${currentPost.id}`, { headers }),
        ]);
        const likesData = await likesRes.json();
        const commentsData = await commentsRes.json();
        setLikeCounts(likesData.count);
        setCommentCounts(commentsData.count);
      } catch (error) {
        console.error("Error fetching counts:", error);
      }
    };
    fetchCounts();
  }, [currentPost?.id]);

  const handleUpdateDescription = async () => {
    try {
      const token = localStorage.getItem("token");
      const response = await fetch(`${POST_API_URL}/${currentPost.id}`, {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ description: editDescription }),
      });
      const data = await response.json();
      setCurrentPost(data);
      setShowEditDesc(false);
      if (onPostUpdated) onPostUpdated(data);
    } catch (error) {
      console.error("Error updating description:", error);
    }
  };

  const handleDeletePost = async () => {
    if (!window.confirm("Да ли сте сигурни да желите да обришете ову објаву?"))
      return;
    try {
      const token = localStorage.getItem("token");
      await fetch(`${POST_API_URL}/${currentPost.id}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
      });
      if (onPostDeleted) onPostDeleted(currentPost.id);
    } catch (error) {
      console.error("Error deleting post:", error);
    }
  };

  const handleDeleteMedia = async (mediaId) => {
    const currentMedia = currentPost.media[currentIndex];
    const isVideo = currentMedia.mediaUrl.match(/\.(mp4|mov|avi|webm)$/i);
    const tip = isVideo ? "видео" : "слику";

    if (!window.confirm(`Да ли сте сигурни да желите да уклоните овај ${tip}?`))
      return;
    try {
      const token = localStorage.getItem("token");
      const response = await fetch(
        `${POST_API_URL}/${currentPost.id}/media/${mediaId}`,
        {
          method: "DELETE",
          headers: { Authorization: `Bearer ${token}` },
        },
      );
      const data = await response.json();

      if (data.message) {
        if (onPostDeleted) onPostDeleted(currentPost.id);
        return;
      }

      setCurrentPost(data);
      if (onPostUpdated) onPostUpdated(data);
      if (currentIndex >= data.media.length) {
        setCurrentIndex(data.media.length - 1);
      }
    } catch (error) {
      console.error("Error deleting media:", error);
    }
  };

  if (!author) return <div>Loading...</div>;

  return (
    <div className="single_post" ref={singlePostRef}>
      <div className="single_post_img_wrapper">
        <img
          src={`http://localhost:8086${currentPost.media[currentIndex].mediaUrl}`}
          alt=""
          className="single_post_img"
        />

        {isOwner && currentPost.media.length > 1 && (
          <button
            className="delete_media_btn"
            onClick={() =>
              handleDeleteMedia(currentPost.media[currentIndex].id)
            }
            title="Уклони овај медија"
          >
            ✕
          </button>
        )}

        {currentPost.media.length > 1 && (
          <>
            {currentIndex > 0 && (
              <button
                className="carousel_btn carousel_btn_left"
                onClick={() => setCurrentIndex((prev) => prev - 1)}
              >
                <img src={assets.left_arrow} alt="" />
              </button>
            )}
            {currentIndex < currentPost.media.length - 1 && (
              <button
                className="carousel_btn carousel_btn_right"
                onClick={() => setCurrentIndex((prev) => prev + 1)}
              >
                <img src={assets.right_arrow} alt="" />
              </button>
            )}
          </>
        )}
      </div>

      <div className="right_side">
        <div className="right_side_top">
          <div className="single_post_profile">
            <img src={author.profilePictureUrl || assets.noProfilePic} alt="" />
            <span>{author.username}</span>
          </div>

          {isOwner && (
            <div className="post_menu_wrapper">
              <img
                src={assets.more}
                alt=""
                className="img_more"
                onClick={() => setShowMenu((prev) => !prev)}
              />
              {showMenu && (
                <div className="post_menu">
                  <span
                    onClick={() => {
                      setShowEditDesc(true);
                      setShowMenu(false);
                    }}
                  >
                    Измените опис
                  </span>
                  <span className="delete_option" onClick={handleDeletePost}>
                    Обришите објаву
                  </span>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="comments">
          <div className="comment">
            <div className="single_post_profile_comment">
              <img
                src={author.profilePictureUrl || assets.noProfilePic}
                alt=""
              />
              <div className="single_post_div_comment_info">
                <span className="single_post_username">{author.username}</span>
                {showEditDesc ? (
                  <div className="edit_desc_wrapper">
                    <textarea
                      className="edit_desc_input"
                      value={editDescription}
                      onChange={(e) => setEditDescription(e.target.value)}
                      maxLength={2200}
                      autoFocus
                    />
                    <div className="edit_desc_actions">
                      <span
                        className="save_desc"
                        onClick={handleUpdateDescription}
                      >
                        Сачувај
                      </span>
                      <span
                        className="cancel_desc"
                        onClick={() => {
                          setShowEditDesc(false);
                          setEditDescription(currentPost.description || "");
                        }}
                      >
                        Одустани
                      </span>
                    </div>
                  </div>
                ) : (
                  <span className="single_post_comment">
                    {currentPost.description}
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="likes">
          <div className="like_comm">
            <img src={assets.heart} alt="" />
            <img src={assets.chat} alt="" />
          </div>
          <div className="like_counts_div">
            <span className="like_counts">{likeCounts} Свиђања</span>
          </div>
          <div className="created_at_div">
            <span className="created_at">
              {currentPost.createdAt.slice(0, 10)}
            </span>
          </div>
        </div>

        <div className="input_comments">
          <input type="text" placeholder="Унесите коментар..." />
        </div>
      </div>
    </div>
  );
};

export default SinglePost;
