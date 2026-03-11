import { useNavigate } from "react-router-dom";
import { assets } from "../../assets/assets";
import { getUserAvatarUrl } from "../../api/userApi";

const CommentItem = ({
  comment,
  user,
  isOwner,
  commentMenuId,
  setCommentMenuId,
  editingCommentId,
  editingCommentText,
  setEditingCommentId,
  setEditingCommentText,
  handleDeleteComment,
  handleUpdateComment,
  onClose,
}) => {
  const navigate = useNavigate();
  const isEditingThis = editingCommentId === comment.id;
  const isMyComment = comment.username === user?.username;

  return (
    <div className="comment">
      <div
        className={`single_post_profile_comment ${isEditingThis ? "comment_editing" : ""}`}
      >
        <img
          src={getUserAvatarUrl(comment.profilePictureUrl, assets.noProfilePic)}
          alt=""
          style={{ cursor: "pointer" }}
          onClick={() => {
            navigate(`/profile/${comment.username}`);
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
              navigate(`/profile/${comment.username}`);
              onClose();
            }}
          >
            {comment.username}
          </span>

          {isEditingThis ? (
            <div className="edit_desc_wrapper">
              <textarea
                className="edit_desc_input"
                value={editingCommentText}
                onChange={(e) => setEditingCommentText(e.target.value)}
                autoFocus
              />
              <div className="edit_desc_actions">
                <span
                  className="save_desc"
                  onClick={() => handleUpdateComment(comment.id)}
                >
                  Сачувај
                </span>
                <span
                  className="cancel_desc"
                  onClick={() => {
                    setEditingCommentId(null);
                    setEditingCommentText("");
                  }}
                >
                  Одустани
                </span>
              </div>
            </div>
          ) : (
            <span className="single_post_comment">{comment.content}</span>
          )}
        </div>
      </div>

      {!isEditingThis &&
        (isMyComment ? (
          <div className="post_menu_wrapper">
            <img
              src={assets.more}
              alt=""
              className="img_more"
              onClick={() =>
                setCommentMenuId((prev) =>
                  prev === comment.id ? null : comment.id,
                )
              }
            />
            {commentMenuId === comment.id && (
              <div className="post_menu">
                <span
                  onClick={() => {
                    setEditingCommentId(comment.id);
                    setEditingCommentText(comment.content);
                    setCommentMenuId(null);
                  }}
                >
                  Измените
                </span>
                <span
                  className="delete_option"
                  onClick={() => handleDeleteComment(comment.id)}
                >
                  Обришите
                </span>
              </div>
            )}
          </div>
        ) : isOwner ? (
          <span
            className="delete_comment_btn"
            onClick={() => handleDeleteComment(comment.id)}
          >
            ✕
          </span>
        ) : null)}
    </div>
  );
};

export default CommentItem;
