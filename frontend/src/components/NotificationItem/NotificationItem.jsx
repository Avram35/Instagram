import React from "react";
import { useNavigate } from "react-router-dom";
import { assets } from "../../assets/assets";
import "./NotificationItem.css";
import { getUserAvatarUrl } from "../../api/userApi";

const NotificationItem = ({ request, onAccept, onReject, onClose }) => {
  const navigate = useNavigate();

  const username = request.senderUsername || null;
  const profilePic = getUserAvatarUrl(
    request.senderProfilePicture,
    assets.noProfilePic,
  );

  if (!username) return null;

  const isFollowNotification =
    request.type === "FOLLOW" || request.type === "FOLLOW_REQUEST_ACCEPTED";
  const isLikeNotification = request.type === "LIKE";
  const isCommentNotification = request.type === "COMMENT";
  const isFollowRequest =
    !isFollowNotification && !isLikeNotification && !isCommentNotification;

  const getText = () => {
    if (isFollowNotification) return "вас сада прати";
    if (isLikeNotification) return "је лајковао/ла вашу објаву";
    if (isCommentNotification) return "је коментарисао/ла вашу објаву";
    return "жели да вас прати";
  };

  return (
    <div className="notification_request">
      <img
        src={profilePic}
        className="user_img"
        alt=""
        onClick={() => {
          navigate(`/profile/${username}`);
          onClose();
        }}
      />
      <div className="notification_text">
        <p>
          <span
            onClick={() => {
              navigate(`/profile/${username}`);
              onClose();
            }}
          >
            {username}
          </span>{" "}
          {getText()}
        </p>

        {isFollowRequest && (
          <div className="notification_actions">
            <button className="accept_btn" onClick={() => onAccept(request.id)}>
              Прихвати
            </button>
            <img
              src={assets.close}
              alt="odbij"
              className="reject_request"
              onClick={() => onReject(request.id)}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default NotificationItem;
