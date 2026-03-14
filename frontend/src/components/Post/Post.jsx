import React from "react";
import "./Post.css";
import { assets } from "../../assets/assets";
import { useNavigate } from "react-router-dom";

const Post = ({
  profilePic,
  profileUserName,
  postMedia,
  description,
  isLiked,
  likeCount,
  commentCount,
  onLikeClick,
  onCommentClick,
}) => {
  const navigate = useNavigate();

  return (
    <div className="post">
      <div
        className="profile"
        onClick={() => navigate(`/profile/${profileUserName}`)}
      >
        <img
          src={profilePic === "" ? assets.noProfilePic : profilePic}
          alt=""
        />
        <span>{profileUserName}</span>
      </div>
      {postMedia?.match(/\.(mp4|mov|avi|webm)$/i) ? (
        <video src={postMedia} className="post_img" controls />
      ) : (
        <img src={postMedia} alt="" className="post_img" />
      )}
      <div className="like_comm">
        <img
          src={isLiked ? assets.like : assets.heart}
          alt=""
          onClick={onLikeClick}
          style={{ cursor: "pointer" }}
        />
        <img
          src={assets.chat}
          alt=""
          onClick={onCommentClick}
          style={{ cursor: "pointer" }}
        />
      </div>
      <div className="like_counts">
        <span>{likeCount} свиђања</span>
        <span onClick={onCommentClick} style={{ cursor: "pointer" }}>
          Погледај све коментаре ({commentCount})
        </span>
      </div>
      <div className="desc">
        <span>{profileUserName}</span>
        <p>{description}</p>
      </div>
    </div>
  );
};

export default Post;
