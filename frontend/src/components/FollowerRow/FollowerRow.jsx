import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { assets } from "../../assets/assets";
import "./FollowerRow.css";
import { fetchUserById, getUserAvatarUrl } from "../../api/userApi";

const FollowerRow = ({
  userId,
  isFollowing,
  isOwnProfile,
  isFollowersList,
  isMe,
  onFollow,
  onRemove,
  onNavigate,
}) => {
  const [userData, setUserData] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const data = await fetchUserById(userId);
        setUserData(data);
      } catch (e) {
        console.error(e);
      }
    };
    fetchUser();
  }, [userId]);

  if (!userData) return null;

  const handleNavigate = () => {
    onNavigate();
    navigate(`/profile/${userData.username}`);
  };

  return (
    <div className="follower_row">
      <img
        src={getUserAvatarUrl(userData.profilePictureUrl, assets.noProfilePic)}
        className="follower_img"
        alt=""
        onClick={handleNavigate}
      />
      <span className="follower_username" onClick={handleNavigate}>
        {userData.username}
      </span>

      {!isMe && (
        <>
          {isOwnProfile && isFollowersList ? (
            <button className="remove_btn" onClick={onRemove}>
              Уклони
            </button>
          ) : (
            <button
              className={isFollowing ? "unfollow_btn" : "follow_btn"}
              onClick={onFollow}
            >
              {isFollowing ? "Отпрати" : "Прати"}
            </button>
          )}
        </>
      )}
    </div>
  );
};

export default FollowerRow;
