import { assets } from "../../assets/assets";
import { getPostMediaUrl } from "../../api/postApi";

const PostMedia = ({
  currentPost,
  currentIndex,
  setCurrentIndex,
  isOwner,
  isEditMode,
  handleDeleteMedia,
}) => {
  const mediaUrl = getPostMediaUrl(currentPost.media[currentIndex].mediaUrl);
  const isVideo = currentPost.media[currentIndex].mediaUrl.match(
    /\.(mp4|mov|avi|webm)$/i,
  );

  return (
    <div className="single_post_img_wrapper">
      {isVideo ? (
        <video src={mediaUrl} className="single_post_img" controls />
      ) : (
        <img src={mediaUrl} alt="" className="single_post_img" />
      )}

      {isOwner && isEditMode && (
        <button
          className="delete_media_btn"
          onClick={() => handleDeleteMedia(currentPost.media[currentIndex].id)}
          title="Уклони овај медија"
        >
          ✕
        </button>
      )}

      {currentPost.media.length > 1 && (
        <>
          {currentIndex > 0 && (
            <button
              className="carousel_btn carousel_btn_left"
              onClick={() => setCurrentIndex((prev) => prev - 1)}
            >
              <img src={assets.left_arrow} alt="" />
            </button>
          )}
          {currentIndex < currentPost.media.length - 1 && (
            <button
              className="carousel_btn carousel_btn_right"
              onClick={() => setCurrentIndex((prev) => prev + 1)}
            >
              <img src={assets.right_arrow} alt="" />
            </button>
          )}
        </>
      )}
    </div>
  );
};

export default PostMedia;
