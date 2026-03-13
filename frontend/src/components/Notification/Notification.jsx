import React, { useEffect, useState } from "react";
import "./Notification.css";
import NotificationItem from "../NotificationItem/NotificationItem";

const FOLLOW_API_URL = "http://localhost:8083/api/v1/follow";
const USER_API_URL = "http://localhost:8082/api/v1/user/id";

const Notification = ({ setSearchNotification }) => {
  const [requests, setRequests] = useState([]);
  const [notifications, setNotifications] = useState([]);

  const fetchAll = async () => {
    try {
      const token = localStorage.getItem("token");
      const headers = { Authorization: `Bearer ${token}` };

      const [pendingRes, notifRes] = await Promise.all([
        fetch(`${FOLLOW_API_URL}/requests/pending`, { headers }),
        fetch(`${FOLLOW_API_URL}/notifications`, { headers }),
      ]);

      const pending = await pendingRes.json();
      const notifs = await notifRes.json();

      const enriched = await Promise.all(
        pending.map(async (r) => {
          try {
            const userRes = await fetch(`${USER_API_URL}/${r.senderId}`, {
              headers,
            });
            const userData = await userRes.json();
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
      const token = localStorage.getItem("token");
      await fetch(`${FOLLOW_API_URL}/requests/${requestId}/accept`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });

      setRequests((prev) => prev.filter((r) => r.id !== requestId));

      const res = await fetch(`${FOLLOW_API_URL}/notifications`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const notifs = await res.json();
      setNotifications(notifs);
    } catch (error) {
      console.error("Error accepting request:", error);
    }
  };

  const handleReject = async (requestId) => {
    try {
      const token = localStorage.getItem("token");
      await fetch(`${FOLLOW_API_URL}/requests/${requestId}/reject`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });
      setRequests((prev) => prev.filter((r) => r.id !== requestId));
    } catch (error) {
      console.error("Error rejecting request:", error);
    }
  };

  useEffect(() => {
    fetchAll();
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
