import { assets } from "../../assets/assets";

const PostLikes = ({
  isLiked,
  likeCounts,
  commentCounts,
  currentPost,
  handleLike,
}) => {
  return (
    <div className="likes">
      <div className="like_comm">
        <img
          src={isLiked ? assets.like : assets.heart}
          alt=""
          onClick={handleLike}
        />
        <div className="comment_count_wrapper">
          <img src={assets.chat} alt="" />
          {commentCounts > 0 && <span>{commentCounts}</span>}
        </div>
      </div>
      <div className="like_counts_div">
        <span className="like_counts">{likeCounts} Свиђања</span>
      </div>
      <div className="created_at_div">
        <span className="created_at">{currentPost.createdAt.slice(0, 10)}</span>
      </div>
    </div>
  );
};

export default PostLikes;
