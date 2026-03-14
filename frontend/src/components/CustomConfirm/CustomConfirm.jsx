import React from "react";
import "./CustomConfirm.css";

const CustomConfirm = ({ message, onConfirm, onCancel }) => {
  return (
    <div className="custom_confirm_overlay">
      <div className="custom_confirm_modal">
        <p className="custom_confirm_message">{message}</p>
        <div className="custom_confirm_actions">
          <button className="custom_confirm_btn_cancel" onClick={onCancel}>
            Откажи
          </button>
          <button className="custom_confirm_btn_confirm" onClick={onConfirm}>
            Потврди
          </button>
        </div>
      </div>
    </div>
  );
};

export default CustomConfirm;
