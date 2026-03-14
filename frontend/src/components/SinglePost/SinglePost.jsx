import { useContext, useEffect, useRef, useState } from "react";
import "./SinglePost.css";
import { AppContext } from "../../context/AppContext";

import { fetchUserById } from "../../api/userApi";
import { fetchLikeCount, checkLiked, toggleLike } from "../../api/likeApi";
import {
  fetchCommentCount,
  fetchCommentList,
  addComment,
  deleteComment,
  updateComment,
} from "../../api/commentApi";
import { updatePost, deletePost, deletePostMedia } from "../../api/postApi";
import CustomConfirm from "../CustomConfirm/CustomConfirm";

import PostMedia from "./PostMedia";
import PostHeader from "./PostHeader";
import PostDescription from "./PostDescription";
import CommentItem from "./CommentItem";
import PostLikes from "./PostLikes";
import PostCommentInput from "./PostCommentInput";

const SinglePost = ({
  singlePostRef,
  postInfo,
  onPostDeleted,
  onPostUpdated,
  onClose,
  onLikeChanged,
  onCommentChanged,
}) => {
  const [author, setAuthor] = useState();
  const [likeCounts, setLikeCounts] = useState(0);
  const [commentCounts, setCommentCounts] = useState(0);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [showMenu, setShowMenu] = useState(false);
  const [editDescription, setEditDescription] = useState(
    postInfo.description || "",
  );
  const [currentPost, setCurrentPost] = useState(postInfo);
  const [isLiked, setIsLiked] = useState(false);
  const [comments, setComments] = useState([]);
  const [commentInput, setCommentInput] = useState("");
  const [commentMenuId, setCommentMenuId] = useState(null);
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editingCommentText, setEditingCommentText] = useState("");
  const [isEditMode, setIsEditMode] = useState(false);
  const [confirm, setConfirm] = useState(null);
  const [commentLoading, setCommentLoading] = useState(false);
  const [descLoading, setDescLoading] = useState(false);

  const commentMenuRef = useRef(null);
  const { user } = useContext(AppContext);

  const isOwner =
    user?.id === currentPost.userId || user?.username === author?.username;

  useEffect(() => {
    if (!currentPost?.userId) return;
    const loadAuthor = async () => {
      try {
        const data = await fetchUserById(currentPost.userId);
        setAuthor(data);
      } catch (error) {
        console.error("Error fetching user:", error);
      }
    };
    loadAuthor();
  }, [currentPost?.userId]);

  useEffect(() => {
    if (!currentPost?.id) return;
    const loadPostData = async () => {
      try {
        const [likesData, commentsData, likedData, commentList] =
          await Promise.all([
            fetchLikeCount(currentPost.id),
            fetchCommentCount(currentPost.id),
            checkLiked(currentPost.id),
            fetchCommentList(currentPost.id),
          ]);
        setLikeCounts(likesData.count);
        setCommentCounts(commentsData.count);
        setIsLiked(likedData.liked);
        setComments(commentList);
      } catch (error) {
        console.error("Error fetching data:", error);
      }
    };
    loadPostData();
  }, [currentPost?.id]);

  useEffect(() => {
    const handleClick = (e) => {
      if (
        commentMenuRef.current &&
        !commentMenuRef.current.contains(e.target)
      ) {
        setCommentMenuId(null);
      }
    };
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [commentMenuId]);

  const handleLike = async () => {
    try {
      await toggleLike(currentPost.id, isLiked);
      const newIsLiked = !isLiked;
      const newCount = isLiked ? likeCounts - 1 : likeCounts + 1;
      setIsLiked(newIsLiked);
      setLikeCounts(newCount);
      if (onLikeChanged) onLikeChanged(currentPost.id, newIsLiked, newCount);
    } catch (error) {
      console.error("Error liking post:", error);
    }
  };

  const handleAddComment = async () => {
    if (!commentInput.trim() || commentLoading) return;
    setCommentLoading(true);
    try {
      const newComment = await addComment(currentPost.id, commentInput);
      setComments((prev) => [...prev, newComment]);
      const newCount = commentCounts + 1;
      setCommentCounts(newCount);
      if (onCommentChanged) onCommentChanged(currentPost.id, newCount);
      setCommentInput("");
    } catch (error) {
      console.error("Error adding comment:", error);
    } finally {
      setCommentLoading(false);
    }
  };

  const handleDeleteComment = async (commentId) => {
    const confirmed = await new Promise((resolve) =>
      setConfirm({
        message: "Да ли сте сигурни да желите да обришете овај коментар?",
        resolve,
      }),
    );
    if (!confirmed) return;
    try {
      await deleteComment(commentId);
      setComments((prev) => prev.filter((c) => c.id !== commentId));
      const newCount = commentCounts - 1;
      setCommentCounts(newCount);
      if (onCommentChanged) onCommentChanged(currentPost.id, newCount);
      setCommentMenuId(null);
    } catch (error) {
      console.error("Error deleting comment:", error);
    }
  };

  const handleUpdateComment = async (commentId) => {
    if (!editingCommentText.trim()) return;
    try {
      const updated = await updateComment(commentId, editingCommentText);
      setComments((prev) =>
        prev.map((c) => (c.id === commentId ? updated : c)),
      );
      setEditingCommentId(null);
      setEditingCommentText("");
    } catch (error) {
      console.error("Error updating comment:", error);
    }
  };

  const handleUpdateDescription = async () => {
    if (descLoading) return;
    setDescLoading(true);
    try {
      const data = await updatePost(currentPost.id, editDescription);
      setCurrentPost(data);
      setIsEditMode(false);
      if (onPostUpdated) onPostUpdated(data);
    } catch (error) {
      console.error("Error updating description:", error);
    } finally {
      setDescLoading(false);
    }
  };

  const handleDeletePost = async () => {
    const confirmed = await new Promise((resolve) =>
      setConfirm({
        message: "Да ли сте сигурни да желите да обришете ову објаву?",
        resolve,
      }),
    );
    if (!confirmed) return;
    try {
      await deletePost(currentPost.id);
      if (onPostDeleted) onPostDeleted(currentPost.id);
    } catch (error) {
      console.error("Error deleting post:", error);
    }
  };

  const handleDeleteMedia = async (mediaId) => {
    const currentMedia = currentPost.media[currentIndex];
    const isVideo = currentMedia.mediaUrl.match(/\.(mp4|mov|avi|webm)$/i);
    const tip = isVideo ? "видео" : "слику";
    const confirmed = await new Promise((resolve) =>
      setConfirm({
        message: `Да ли сте сигурни да желите да уклоните овај ${tip}?`,
        resolve,
      }),
    );
    if (!confirmed) return;
    try {
      const data = await deletePostMedia(currentPost.id, mediaId);
      if (data.message) {
        if (onPostDeleted) onPostDeleted(currentPost.id);
        return;
      }
      setCurrentPost(data);
      if (onPostUpdated) onPostUpdated(data);
      if (currentIndex >= data.media.length)
        setCurrentIndex(data.media.length - 1);
    } catch (error) {
      console.error("Error deleting media:", error);
    }
  };

  if (!author) return <div>Loading...</div>;

  return (
    <div className="single_post" ref={singlePostRef}>
      <PostMedia
        currentPost={currentPost}
        currentIndex={currentIndex}
        setCurrentIndex={setCurrentIndex}
        isOwner={isOwner}
        isEditMode={isEditMode}
        handleDeleteMedia={handleDeleteMedia}
      />

      <div className="right_side">
        <PostHeader
          author={author}
          isOwner={isOwner}
          showMenu={showMenu}
          setShowMenu={setShowMenu}
          setIsEditMode={setIsEditMode}
          handleDeletePost={handleDeletePost}
        />

        <div className="comments">
          <PostDescription
            author={author}
            currentPost={currentPost}
            isEditMode={isEditMode}
            editDescription={editDescription}
            setEditDescription={setEditDescription}
            descLoading={descLoading}
            handleUpdateDescription={handleUpdateDescription}
            setIsEditMode={setIsEditMode}
            onClose={onClose}
          />

          {comments.map((comment) => (
            <CommentItem
              key={comment.id}
              comment={comment}
              user={user}
              isOwner={isOwner}
              commentMenuId={commentMenuId}
              setCommentMenuId={setCommentMenuId}
              editingCommentId={editingCommentId}
              editingCommentText={editingCommentText}
              setEditingCommentId={setEditingCommentId}
              setEditingCommentText={setEditingCommentText}
              handleDeleteComment={handleDeleteComment}
              handleUpdateComment={handleUpdateComment}
              onClose={onClose}
            />
          ))}
        </div>

        <PostLikes
          isLiked={isLiked}
          likeCounts={likeCounts}
          commentCounts={commentCounts}
          currentPost={currentPost}
          handleLike={handleLike}
        />

        <PostCommentInput
          commentInput={commentInput}
          setCommentInput={setCommentInput}
          commentLoading={commentLoading}
          handleAddComment={handleAddComment}
        />
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
    </div>
  );
};

export default SinglePost;
