import { useState } from "react";
import "./EditProfile.css";
import { AppContext } from "../../context/AppContext";
import { assets } from "../../assets/assets";
import { useNavigate } from "react-router-dom";
import { useContext } from "react";
import {
  fetchProfileInfo,
  getUserAvatarUrl,
  updateProfile,
  updateProfilePicture,
} from "../../api/userApi";
import { deleteAccount } from "../../api/authApi";
import CustomAlert from "../../components/CustomAlert/CustomAlert";
import CustomConfirm from "../../components/CustomConfirm/CustomConfirm";

const EditProfile = () => {
  const { user, updateUser, logout } = useContext(AppContext);

  const [selectedImage, setSelectedImage] = useState(null);
  const [preview, setPreview] = useState(
    getUserAvatarUrl(user.profilePictureUrl, null),
  );
  const [bio, setBio] = useState(user.bio || "");
  const [fname, setFname] = useState(user.fname || "");
  const [lname, setLname] = useState(user.lname || "");
  const [username, setUsername] = useState(user.username || "");
  const [privateProfile, setPrivateProfile] = useState(
    user.privateProfile || false,
  );
  const [alert, setAlert] = useState(null);
  const [confirm, setConfirm] = useState(null);
  const navigate = useNavigate();

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.size > 50 * 1024 * 1024) {
        setAlert({ message: "Фајл не сме бити већи од 50MB!", type: "error" });
        return;
      }
      setSelectedImage(file);
      setPreview(URL.createObjectURL(file));
    }
  };

  const handleOnSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await updateProfile(user.id, {
        fname,
        lname,
        username,
        bio,
        privateProfile,
      });

      if (response.ok) {
        if (selectedImage) {
          await updateProfilePicture(selectedImage);
        }

        if (username !== user.username) {
          setAlert({
            message: "Корисничко ime је промењено. Бићете одјављени!",
            type: "success",
          });
          setTimeout(() => {
            logout();
            navigate("/login");
          }, 2000);
          return;
        }

        const freshUser = await fetchProfileInfo(username);
        updateUser(freshUser);
        setAlert({ message: "Профил успешно ажуриран!", type: "success" });
        setTimeout(() => navigate(`/profile/${username}`), 3000);
      } else {
        setAlert({ message: "Грешка при ажурирању профила!", type: "error" });
      }
    } catch (error) {
      console.error("Update error:", error);
    }
  };

  const handleDeleteAccount = async () => {
    const confirmed = await new Promise((resolve) =>
      setConfirm({
        message: "Да ли сте сигурни да желите да обришете налог?",
        resolve,
      }),
    );
    if (!confirmed) return;
    try {
      const response = await deleteAccount();
      if (response.ok) {
        logout();
        navigate("/login");
      }
    } catch (error) {
      console.error("Delete account error:", error);
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
            placeholder="Ime"
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
          <h3 className="title-bio">Корисничко ime</h3>
          <input
            placeholder="Корисничко ime"
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
        <button
          type="button"
          className="delete-account-btn"
          onClick={handleDeleteAccount}
        >
          Обришите налог
        </button>
      </div>

      {confirm && (
        <CustomConfirm
          message={confirm.message}
          onConfirm={() => {
            confirm.resolve(true);
            setConfirm(null);
          }}
          onCancel={() => {
            confirm.resolve(false);
            setConfirm(null);
          }}
        />
      )}

      {alert && (
        <CustomAlert
          message={alert.message}
          type={alert.type}
          onClose={() => setAlert(null)}
        />
      )}
    </form>
  );
};

export default EditProfile;
