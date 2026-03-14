const PostCommentInput = ({
  commentInput,
  setCommentInput,
  commentLoading,
  handleAddComment,
}) => {
  const handleChange = (e) => {
    const cleaned = e.target.value.replace(/  +/g, " ");
    setCommentInput(cleaned);
  };

  const handleSubmit = () => {
    if (!commentInput.trim() || commentLoading) return;
    handleAddComment();
  };

  return (
    <div className="input_comments">
      <input
        type="text"
        placeholder="Унесите коментар..."
        value={commentInput}
        onChange={handleChange}
        onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
        disabled={commentLoading}
      />
      <span
        className={`post_comment_btn ${commentLoading || !commentInput.trim() ? "post_comment_btn_disabled" : ""}`}
        onClick={handleSubmit}
      >
        {commentLoading ? "Објављује се..." : "Објави"}
      </span>
    </div>
  );
};

export default PostCommentInput;
