const PostCommentInput = ({
  commentInput,
  setCommentInput,
  commentLoading,
  handleAddComment,
}) => {
  return (
    <div className="input_comments">
      <input
        type="text"
        placeholder="Унесите коментар..."
        value={commentInput}
        onChange={(e) => setCommentInput(e.target.value)}
        onKeyDown={(e) => e.key === "Enter" && handleAddComment()}
        disabled={commentLoading}
      />
      <span
        className={`post_comment_btn ${commentLoading ? "post_comment_btn_disabled" : ""}`}
        onClick={!commentLoading ? handleAddComment : undefined}
      >
        {commentLoading ? "Објављује се..." : "Објави"}
      </span>
    </div>
  );
};

export default PostCommentInput;
