import { useState } from "react";
import "./CreatePost.css";
import { assets } from "../../assets/assets";
import { createPost } from "../../api/postApi";
import CustomAlert from "../CustomAlert/CustomAlert";

const CreatePost = ({ createPostRef, setCreatePost, onPostCreated }) => {
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [previews, setPreviews] = useState([]);
  const [description, setDescription] = useState("");
  const [step, setStep] = useState(1);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [loading, setLoading] = useState(false);
  const [alert, setAlert] = useState(null);
  const [submitted, setSubmitted] = useState(false);

  const handleFileChange = (e) => {
    const files = Array.from(e.target.files);

    if (files.length > 20) {
      setAlert({ message: "Максималан број фајлова је 20!", type: "error" });
      return;
    }

    const tooBig = files.some((file) => file.size > 50 * 1024 * 1024);
    if (tooBig) {
      setAlert({ message: "Фајл не сме бити већи од 50MB!", type: "error" });
      return;
    }

    setSelectedFiles(files);

    const previewUrls = files.map((file) => ({
      url: URL.createObjectURL(file),
      type: file.type.startsWith("video") ? "video" : "image",
    }));
    setPreviews(previewUrls);
    setStep(2);
    setCurrentIndex(0);
  };

  const handleSubmit = async () => {
    if (loading) return;
    setLoading(true);
    try {
      const response = await createPost(selectedFiles, description);
      if (response.ok) {
        setSubmitted(true);
        onPostCreated?.();
        setAlert({ message: "Објава успешно додата!", type: "success" });
        setTimeout(() => setCreatePost(false), 3000);
      } else {
        setAlert({ message: "Грешка при објављивању!", type: "error" });
      }
    } catch (error) {
      console.error("Post error:", error);
      setAlert({ message: "Грешка при објављивању!", type: "error" });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="create_post" ref={createPostRef}>
      <div className="create_post_header">
        {step === 2 && (
          <img
            src={assets.back}
            onClick={() => setStep(1)}
            className="back_btn"
          />
        )}
        <h2>Направите нову објаву</h2>
        {step === 2 && (
          <span
            className={`publish_post ${loading || submitted ? "publish_post_disabled" : ""}`}
            onClick={!loading && !submitted ? handleSubmit : undefined}
          >
            {loading ? "Објављује се..." : "Подели"}
          </span>
        )}
      </div>

      {step === 1 ? (
        <div className="create_post_empty">
          <img src={assets.img_video_icon} alt="" />
          <button
            type="button"
            onClick={() => document.getElementById("postFileInput").click()}
            className="select_files_btn"
          >
            Изаберите са рачунара
          </button>
          <input
            type="file"
            id="postFileInput"
            accept="image/*,video/*"
            multiple
            style={{ display: "none" }}
            onChange={handleFileChange}
          />
        </div>
      ) : (
        <div className="create_post_step2">
          <div className="create_post_previews">
            {previews[currentIndex].type === "image" ? (
              <img src={previews[currentIndex].url} />
            ) : (
              <video src={previews[currentIndex].url} />
            )}

            {previews.length > 1 && (
              <>
                {currentIndex > 0 && (
                  <button
                    type="button"
                    className="carousel_btn carousel_btn_left"
                    onClick={() => setCurrentIndex((prev) => prev - 1)}
                  >
                    <img src={assets.left_arrow} alt="" />
                  </button>
                )}
                {currentIndex < previews.length - 1 && (
                  <button
                    type="button"
                    className="carousel_btn carousel_btn_right"
                    onClick={() => setCurrentIndex((prev) => prev + 1)}
                  >
                    <img src={assets.right_arrow} alt="" />
                  </button>
                )}
              </>
            )}
          </div>
          <textarea
            placeholder="Додајте опис..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="post_description"
            maxLength={2200}
          />
        </div>
      )}

      {alert && (
        <CustomAlert
          message={alert.message}
          type={alert.type}
          onClose={() => setAlert(null)}
        />
      )}
    </div>
  );
};

export default CreatePost;
