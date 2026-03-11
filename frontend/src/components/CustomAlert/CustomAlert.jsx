import React, { useEffect } from "react";
import "./CustomAlert.css";

const CustomAlert = ({ message, type = "success", onClose }) => {
  useEffect(() => {
    const timer = setTimeout(onClose, 3000);
    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <div className={`custom_alert custom_alert_${type}`}>
      <span className="custom_alert_message">{message}</span>
      <span className="custom_alert_close" onClick={onClose}>
        ✕
      </span>
    </div>
  );
};

export default CustomAlert;
