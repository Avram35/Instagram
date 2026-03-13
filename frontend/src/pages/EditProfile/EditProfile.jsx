import React, { useContext, useState } from "react";
import "./EditProfile.css";
import { AppContext } from "../../context/AppContext";
import { assets } from "../../assets/assets";
import { useNavigate } from "react-router-dom";

const USER_API_URL = "http://localhost:8082/api/v1/user";

const EditProfile = () => {
  const { user, updateUser } = useContext(AppContext);

  const [selectedImage, setSelectedImage] = useState(null);
  const [preview, setPreview] = useState(null);
  const [bio, setBio] = useState(user.bio || "");
  const [fname, setFname] = useState(user.fname || "");
  const [lname, setLname] = useState(user.lname || "");
  const [username, setUsername] = useState(user.username || "");
  const [privateProfile, setPrivateProfile] = useState(
    user.privateProfile || false,
  );
  const navigate = useNavigate();

  const handleImageChange = (e) => {
    const file = e.target.files[0];

    if (file) {
      if (file.size > 50 * 1024 * 1024) {
        alert("Фајл не сме бити већи од 50MB!");
        return;
      }
      setSelectedImage(file);
      setPreview(URL.createObjectURL(file));
    }
  };

  const handleOnSubmit = async (e) => {
    e.preventDefault();

    const token = localStorage.getItem("token");

    try {
      const response = await fetch(`${USER_API_URL}/${user.id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          fname: fname,
          lname: lname,
          username: username,
          bio: bio,
          privateProfile: privateProfile,
        }),
      });

      if (response.ok) {
        if (selectedImage) {
          const formData = new FormData();
          formData.append("file", selectedImage);

          await fetch(`${USER_API_URL}/${user.id}/profile-pic`, {
            method: "POST",
            headers: {
              Authorization: `Bearer ${token}`,
            },
            body: formData,
          });
        }

        updateUser({ fname, lname, username, bio, privateProfile });
        alert("Профил успешно ажуриран!");
        navigate(`/profile/${username}`);
      } else {
        alert("Грешка при ажурирању профила!");
      }
    } catch (error) {
      console.error("Update error:", error);
    }
  };
  return (
    <form className="edit-profile-div" onSubmit={handleOnSubmit}>
      <div className="edit-profile-container">
        <h2 className="title-edit-profile">Измените профил</h2>
        <div className="edit-image">
          <img src={preview || assets.noProfilePic} alt="" />
          <button
            type="button"
            onClick={() => document.getElementById("imageInput").click()}
            className="change-image-btn"
          >
            Промените фотографију
          </button>
          <input
            type="file"
            id="imageInput"
            accept="image/*"
            style={{ display: "none" }}
            onChange={handleImageChange}
          />
        </div>
        <div className="edit-bio">
          <h3 className="title-bio">Биографија</h3>
          <textarea
            placeholder="Биографија"
            onChange={(e) => setBio(e.target.value)}
            value={bio}
            className="textarea-bio"
            maxLength={150}
          />
        </div>
        <div className="edit-fname">
          <h3 className="title-bio">Име</h3>
          <input
            placeholder="Име"
            onChange={(e) => setFname(e.target.value)}
            value={fname}
            className="edit-input"
          />
        </div>
        <div className="edit-lname">
          <h3 className="title-bio">Презиме</h3>
          <input
            placeholder="Презиме"
            onChange={(e) => setLname(e.target.value)}
            value={lname}
            className="edit-input"
          />
        </div>
        <div className="edit-username">
          <h3 className="title-bio">Корисничко име</h3>
          <input
            placeholder="Корисничко име"
            onChange={(e) => setUsername(e.target.value)}
            value={username}
            className="edit-input"
          />
        </div>
        <div className="edit-privacy">
          <h3 className="title-bio">Приватност налога</h3>
          <div className="privacy-toggle">
            <span>Приватни налог</span>
            <input
              className="input-check-private-profile"
              type="checkbox"
              checked={privateProfile}
              onChange={(e) => setPrivateProfile(e.target.checked)}
            />
          </div>
        </div>
        <button className="edit-send-btn" type="submit">
          Пошаљи
        </button>
      </div>
    </form>
  );
};

export default EditProfile;
