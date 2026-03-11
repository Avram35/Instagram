import React, { useEffect, useState } from "react";
import "./Notification.css";
import NotificationItem from "../NotificationItem/NotificationItem";
import {
  fetchPendingRequests,
  fetchNotifications,
  acceptFollowRequest,
  rejectFollowRequest,
} from "../../api/followApi";
import { fetchUserById } from "../../api/userApi";

const Notification = ({ setSearchNotification }) => {
  const [requests, setRequests] = useState([]);
  const [notifications, setNotifications] = useState([]);

  const loadAll = async () => {
    try {
      const [pending, notifs] = await Promise.all([
        fetchPendingRequests(),
        fetchNotifications(),
      ]);

      const enriched = await Promise.all(
        pending.map(async (r) => {
          try {
            const userData = await fetchUserById(r.senderId);
            return {
              ...r,
              type: "request",
              senderUsername: userData.username,
              senderProfilePicture: userData.profilePictureUrl,
            };
          } catch {
            return { ...r, type: "request" };
          }
        }),
      );

      setRequests(enriched);
      const pendingSenderIds = enriched.map((r) => r.senderId);
      setNotifications(
        notifs.filter((n) => !pendingSenderIds.includes(n.senderId)),
      );
    } catch (error) {
      console.error("Error fetching notifications:", error);
    }
  };

  const handleAccept = async (requestId) => {
    try {
      await acceptFollowRequest(requestId);
      setRequests((prev) => prev.filter((r) => r.id !== requestId));
      const notifs = await fetchNotifications();
      setNotifications(notifs);
    } catch (error) {
      console.error("Error accepting request:", error);
    }
  };

  const handleReject = async (requestId) => {
    try {
      await rejectFollowRequest(requestId);
      setRequests((prev) => prev.filter((r) => r.id !== requestId));
    } catch (error) {
      console.error("Error rejecting request:", error);
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  const allItems = [...requests, ...notifications];

  return (
    <div className="notification_div">
      <h1>Обавештења</h1>
      {allItems.length === 0 ? (
        <p className="no_notifications">Нема нових обавештења</p>
      ) : (
        allItems.map((item) => (
          <NotificationItem
            key={`${item.type}-${item.id}`}
            request={item}
            onAccept={handleAccept}
            onReject={handleReject}
            onClose={() => setSearchNotification(false)}
          />
        ))
      )}
    </div>
  );
};

export default Notification;
