import React from "react";
import { useNavigate } from "react-router-dom";
import { assets } from "../../assets/assets";
import "./NotificationItem.css";

const NotificationItem = ({ request, onAccept, onReject, onClose }) => {
  const navigate = useNavigate();

  const username = request.senderUsername || null;
  const profilePic = request.senderProfilePicture || assets.noProfilePic;

  if (!username) return null;

  const isFollowNotification =
    request.type === "FOLLOW" || request.type === "FOLLOW_REQUEST_ACCEPTED";

  return (
    <div className="notification_request">
      <img
        src={profilePic}
        className="user_img"
        alt=""
        onClick={() => {
          (navigate(`/profile/${username}`), onClose());
        }}
      />
      <div className="notification_text">
        <p>
          <span
            onClick={() => {
              (navigate(`/profile/${username}`), onClose());
            }}
          >
            {username}
          </span>{" "}
          {isFollowNotification ? "вас сада прати" : "жели да вас прати"}
        </p>

        {!isFollowNotification && (
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
