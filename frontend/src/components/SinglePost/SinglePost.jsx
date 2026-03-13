import React, { useContext, useEffect, useState } from "react";
import "./SinglePost.css";
import { AppContext } from "../../context/AppContext";
import { assets } from "../../assets/assets";

const USER_API_URL = "http://localhost:8082/api/v1/user/id/";
const LIKE_API_URL = "http://localhost:8087/api/v1/like/count/";
const COMMENT_API_URL = "http://localhost:8087/api/v1/comment/count/";

const SinglePost = ({ singlePostRef, postInfo }) => {
  const [author, setAuthor] = useState();
  const [likeCounts, setLikeCounts] = useState(0);
  const [commentCounts, setCommentCounts] = useState(0);
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    if (!postInfo?.userId) return;

    const fetchUser = async () => {
      try {
        const token = localStorage.getItem("token");

        const response = await fetch(`${USER_API_URL}${postInfo.userId}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (!response.ok) throw new Error("Failed to fetch user");

        const data = await response.json();
        setAuthor(data);
      } catch (error) {
        console.error("Error fetching user:", error);
      }
    };

    fetchUser();
  }, [postInfo?.userId]);

  useEffect(() => {
    if (!postInfo?.id) return;

    const fetchCounts = async () => {
      try {
        const token = localStorage.getItem("token");
        const headers = { Authorization: `Bearer ${token}` };

        const [likesRes, commentsRes] = await Promise.all([
          fetch(`${LIKE_API_URL}${postInfo.id}`, {
            headers,
          }),
          fetch(`${COMMENT_API_URL}${postInfo.id}`, {
            headers,
          }),
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
  }, [postInfo?.id]);

  if (!author) {
    return <div>Loading...</div>;
  }

  return (
    <div className="single_post" ref={singlePostRef}>
      <div className="single_post_img_wrapper">
        <img
          src={`http://localhost:8086${postInfo.media[currentIndex].mediaUrl}`}
          alt=""
          className="single_post_img"
        />
        {postInfo.media.length > 1 && (
          <>
            {currentIndex > 0 && (
              <button
                className="carousel_btn carousel_btn_left"
                onClick={() => setCurrentIndex((prev) => prev - 1)}
              >
                <img src={assets.left_arrow} alt="" />
              </button>
            )}
            {currentIndex < postInfo.media.length - 1 && (
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
          <img src={assets.more} alt="" className="img_more" />
        </div>

        <div className="comments">
          <div className="comment">
            <div className="single_post_profile_comment">
              <img
                src={author.profilePictureUrl || assets.noProfilePic}
                alt=""
              />
              <div>
                <span className="single_post_username">{author.username}</span>
                <span className="single_post_comment">
                  {postInfo.description}
                </span>
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
              {postInfo.createdAt.slice(0, 10)}
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
