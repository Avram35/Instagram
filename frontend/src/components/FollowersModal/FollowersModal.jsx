import React, { useEffect, useState, useContext } from "react";
import "./FollowersModal.css";
import { AppContext } from "../../context/AppContext";
import FollowerRow from "../FollowerRow/FollowerRow";
import {
  checkFollow,
  fetchFollowers,
  fetchFollowing,
  removeFollower,
  toggleFollow,
} from "../../api/followApi";

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

    const loadList = async () => {
      try {
        const data =
          followersFollowing === "Пратиоци"
            ? await fetchFollowers(profileUserId)
            : await fetchFollowing(profileUserId);
        setList(data);

        const map = {};
        await Promise.all(
          data.map(async (item) => {
            const otherId =
              followersFollowing === "Пратиоци"
                ? item.followerId
                : item.followingId;
            try {
              const checkData = await checkFollow(otherId);
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

    loadList();
  }, [profileUserId, followersFollowing]);

  const handleFollow = async (otherId) => {
    try {
      const isFollowingNow = followingMap[Number(otherId)];
      await toggleFollow(otherId, isFollowingNow, false);
      setFollowingMap((prev) => ({
        ...prev,
        [Number(otherId)]: !isFollowingNow,
      }));
    } catch (error) {
      console.error("Error follow/unfollow:", error);
    }
  };

  const handleRemove = async (followerId) => {
    try {
      await removeFollower(followerId);
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
