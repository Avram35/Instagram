import React, { useEffect, useState, useContext } from "react";
import "./FollowersModal.css";
import { AppContext } from "../../context/AppContext";
import FollowerRow from "../FollowerRow/FollowerRow";

const FOLLOW_API_URL = "http://localhost:8083/api/v1/follow";

const FollowersModal = ({
  followersModalRef,
  followersFollowing,
  profileUserId,
  isOwnProfile,
  onClose,
}) => {
  const [list, setList] = useState([]);
  const [followingMap, setFollowingMap] = useState({});
  const { user } = useContext(AppContext);

  useEffect(() => {
    if (!profileUserId) return;

    const fetchList = async () => {
      try {
        const token = localStorage.getItem("token");
        const endpoint =
          followersFollowing === "Пратиоци"
            ? `${FOLLOW_API_URL}/${profileUserId}/followers`
            : `${FOLLOW_API_URL}/${profileUserId}/following`;

        const res = await fetch(endpoint, {
          headers: { Authorization: `Bearer ${token}` },
        });
        const data = await res.json();
        setList(data);

        const map = {};
        await Promise.all(
          data.map(async (item) => {
            const otherId =
              followersFollowing === "Пратиоци"
                ? item.followerId
                : item.followingId;
            try {
              const checkRes = await fetch(
                `${FOLLOW_API_URL}/check-internal/${user.id}/${otherId}`,
                { headers: { Authorization: `Bearer ${token}` } },
              );
              const checkData = await checkRes.json();
              map[Number(otherId)] = checkData.following;
            } catch {
              map[Number(otherId)] = false;
            }
          }),
        );
        setFollowingMap(map);
      } catch (error) {
        console.error("Error fetching list:", error);
      }
    };

    fetchList();
  }, [profileUserId, followersFollowing]);

  const handleFollow = async (otherId) => {
    try {
      const token = localStorage.getItem("token");
      const isFollowing = followingMap[Number(otherId)];
      await fetch(`${FOLLOW_API_URL}/${otherId}`, {
        method: isFollowing ? "DELETE" : "POST",
        headers: { Authorization: `Bearer ${token}` },
      });
      setFollowingMap((prev) => ({ ...prev, [Number(otherId)]: !isFollowing }));
    } catch (error) {
      console.error("Error follow/unfollow:", error);
    }
  };

  const handleRemove = async (followerId) => {
    try {
      const token = localStorage.getItem("token");
      await fetch(`${FOLLOW_API_URL}/remove/${followerId}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
      });
      setList((prev) => prev.filter((item) => item.followerId !== followerId));
    } catch (error) {
      console.error("Error removing follower:", error);
    }
  };

  return (
    <div className="followers_modal_div" ref={followersModalRef}>
      <div className="followers_modal_header">
        <h2>{followersFollowing}</h2>
      </div>
      <div className="followers_modal_list">
        {list.length === 0 ? (
          <p className="empty_list">Нема корисника</p>
        ) : (
          list.map((item) => {
            const otherId =
              followersFollowing === "Пратиоци"
                ? item.followerId
                : item.followingId;
            const isMe = Number(otherId) === Number(user.id);

            return (
              <FollowerRow
                key={otherId}
                userId={otherId}
                isFollowing={followingMap[Number(otherId)]}
                isOwnProfile={isOwnProfile}
                isFollowersList={followersFollowing === "Пратиоци"}
                isMe={isMe}
                onFollow={() => handleFollow(otherId)}
                onRemove={() => handleRemove(otherId)}
                onNavigate={onClose}
              />
            );
          })
        )}
      </div>
    </div>
  );
};

export default FollowersModal;
