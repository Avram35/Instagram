import React, { useState } from "react";
import "./CreatePost.css";
import { assets } from "../../assets/assets";

const POST_API_URL = "http://localhost:8086/api/v1/post";

const CreatePost = ({ createPostRef, setCreatePost }) => {
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [previews, setPreviews] = useState([]);
  const [description, setDescription] = useState("");
  const [step, setStep] = useState(1);
  const [currentIndex, setCurrentIndex] = useState(0);

  const handleFileChange = (e) => {
    const files = Array.from(e.target.files);

    if (files.length > 20) {
      alert("Максималан број фајлова је 20!");
      return;
    }

    const tooBig = files.some((file) => file.size > 50 * 1024 * 1024);
    if (tooBig) {
      alert("Фајл не сме бити већи од 50MB!");
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
    const token = localStorage.getItem("token");

    const formData = new FormData();
    selectedFiles.forEach((file) => formData.append("files", file));
    if (description) formData.append("description", description);

    try {
      const response = await fetch(`${POST_API_URL}`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: formData,
      });

      if (response.ok) {
        alert("Објава успешно додата!");
        setCreatePost(false);
      } else {
        alert("Грешка при објављивању!");
      }
    } catch (error) {
      console.error("Post error:", error);
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
          <span className="publish_post" onClick={handleSubmit}>
            Подели
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

            {/*{previews.map((preview, index) =>
              preview.type === "image" ? (
                <img key={index} src={preview.url} />
              ) : (
                <video key={index} src={preview.url} controls />
              ),
            )}*/}
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
    </div>
  );
};

export default CreatePost;
