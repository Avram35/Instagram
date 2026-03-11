import { useNavigate } from "react-router-dom";
import { assets } from "../../assets/assets";
import { getUserAvatarUrl } from "../../api/userApi";

const PostDescription = ({
  author,
  currentPost,
  isEditMode,
  editDescription,
  setEditDescription,
  descLoading,
  handleUpdateDescription,
  setIsEditMode,
  onClose,
}) => {
  const navigate = useNavigate();

  if (!currentPost.description && !isEditMode) return null;

  return (
    <div className="comment">
      <div
        className={`single_post_profile_comment ${isEditMode ? "comment_editing" : ""}`}
      >
        <img
          src={getUserAvatarUrl(author.profilePictureUrl, assets.noProfilePic)}
          alt=""
          onClick={() => {
            navigate(`/profile/${author.username}`);
            onClose();
          }}
          onError={(e) => {
            e.target.src = assets.noProfilePic;
          }}
        />
        <div className="single_post_div_comment_info">
          <span
            className="single_post_username"
            style={{ cursor: "pointer" }}
            onClick={() => {
              navigate(`/profile/${author.username}`);
              onClose();
            }}
          >
            {author.username}
          </span>

          {isEditMode ? (
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
                  className={`save_desc ${descLoading ? "post_comment_btn_disabled" : ""}`}
                  onClick={!descLoading ? handleUpdateDescription : undefined}
                >
                  {descLoading ? "Чува се..." : "Сачувај"}
                </span>
                <span
                  className="cancel_desc"
                  onClick={() => {
                    setIsEditMode(false);
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
  );
};

export default PostDescription;
