import { assets } from "../../assets/assets";
import { getUserAvatarUrl } from "../../api/userApi";
import { useNavigate } from "react-router-dom";

const PostHeader = ({
  author,
  isOwner,
  showMenu,
  setShowMenu,
  setIsEditMode,
  handleDeletePost,
}) => {
  const navigate = useNavigate();
  return (
    <div className="right_side_top">
      <div
        className="single_post_profile"
        onClick={() => navigate(`/profile/${author.username}`)}
      >
        <img
          src={getUserAvatarUrl(author.profilePictureUrl, assets.noProfilePic)}
          alt=""
          onError={(e) => {
            e.target.src = assets.noProfilePic;
          }}
        />
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
                  setIsEditMode(true);
                  setShowMenu(false);
                }}
              >
                Измените објаву
              </span>
              <span className="delete_option" onClick={handleDeletePost}>
                Обришите објаву
              </span>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default PostHeader;
