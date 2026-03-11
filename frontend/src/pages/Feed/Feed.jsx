import React, { useEffect, useRef, useState } from "react";
import "./Feed.css";
import Post from "../../components/Post/Post";
import SinglePost from "../../components/SinglePost/SinglePost";
import { fetchFeed } from "../../api/feedApi";
import { getPostMediaUrl } from "../../api/postApi";
import { getUserAvatarUrl } from "../../api/userApi";
import { assets } from "../../assets/assets";
import { toggleLike, checkLiked, fetchLikeCount } from "../../api/likeApi";
import { fetchCommentCount } from "../../api/commentApi";

const Feed = () => {
  const [feedPosts, setFeedPosts] = useState([]);
  const [singlePost, setSinglePost] = useState(false);
  const [postInfo, setPostInfo] = useState(null);
  const [likeStates, setLikeStates] = useState({});
  const [likeCounts, setLikeCounts] = useState({});
  const [commentCounts, setCommentCounts] = useState({});
  const singlePostRef = useRef(null);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const bottomRef = useRef(null);

  const loadPosts = async (pageNum) => {
    if (loadingMore) return;
    setLoadingMore(true);
    try {
      const data = await fetchFeed(pageNum);
      const posts = Array.isArray(data) ? data : [];

      if (posts.length < 20) setHasMore(false);

      const likeStateMap = {};
      const likeCountMap = {};
      const commentCountMap = {};

      await Promise.all(
        posts.map(async (post) => {
          const [liked, likeCount, commentCount] = await Promise.all([
            checkLiked(post.id),
            fetchLikeCount(post.id),
            fetchCommentCount(post.id),
          ]);
          likeStateMap[post.id] = liked.liked;
          likeCountMap[post.id] = likeCount.count;
          commentCountMap[post.id] = commentCount.count;
        }),
      );

      setFeedPosts((prev) => [...prev, ...posts]);
      setLikeStates((prev) => ({ ...prev, ...likeStateMap }));
      setLikeCounts((prev) => ({ ...prev, ...likeCountMap }));
      setCommentCounts((prev) => ({ ...prev, ...commentCountMap }));
    } catch (error) {
      console.error("Error fetching feed:", error);
    } finally {
      setLoadingMore(false);
    }
  };

  useEffect(() => {
    loadPosts(0);
  }, []);

  useEffect(() => {
    if (!hasMore) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && !loadingMore) {
          setPage((prev) => {
            const next = prev + 1;
            loadPosts(next);
            return next;
          });
        }
      },
      { threshold: 1.0 },
    );
    if (bottomRef.current) observer.observe(bottomRef.current);
    return () => observer.disconnect();
  }, [hasMore, loadingMore]);

  useEffect(() => {
    const handleClick = (e) => {
      if (
        singlePost &&
        singlePostRef.current &&
        !singlePostRef.current.contains(e.target)
      ) {
        setSinglePost(false);
      }
    };
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [singlePost]);

  const handleLike = async (postId) => {
    const isLiked = likeStates[postId];
    await toggleLike(postId, isLiked);
    setLikeStates((prev) => ({ ...prev, [postId]: !isLiked }));
    setLikeCounts((prev) => ({
      ...prev,
      [postId]: isLiked ? prev[postId] - 1 : prev[postId] + 1,
    }));
  };

  return (
    <>
      <div className="feed">
        <div>
          {feedPosts.length === 0 && !loadingMore ? (
            <p className="empty_feed">
              Запратите кориснике да бисте видели њихове објаве.
            </p>
          ) : (
            feedPosts
              .filter((post) => post.media?.length > 0)
              .map((post) => (
                <Post
                  key={post.id}
                  profilePic={getUserAvatarUrl(
                    post.profilePictureUrl,
                    assets.noProfilePic,
                  )}
                  profileUserName={post.username}
                  postMedia={getPostMediaUrl(post.media[0].mediaUrl)}
                  description={post.description}
                  isLiked={likeStates[post.id] || false}
                  likeCount={likeCounts[post.id] || 0}
                  commentCount={commentCounts[post.id] || 0}
                  onLikeClick={() => handleLike(post.id)}
                  onCommentClick={() => {
                    setPostInfo(post);
                    setSinglePost(true);
                  }}
                />
              ))
          )}
          {hasMore && <div ref={bottomRef} style={{ height: 1 }} />}
          {loadingMore && <p className="empty_feed">Учитава се...</p>}
        </div>
      </div>
      {singlePost && (
        <div className="overlay">
          <SinglePost
            singlePostRef={singlePostRef}
            postInfo={postInfo}
            onPostDeleted={(postId) => {
              setFeedPosts((prev) => prev.filter((p) => p.id !== postId));
              setSinglePost(false);
            }}
            onPostUpdated={(updatedPost) => {
              setFeedPosts((prev) =>
                prev.map((p) => (p.id === updatedPost.id ? updatedPost : p)),
              );
            }}
            onLikeChanged={(postId, isLiked, count) => {
              setLikeStates((prev) => ({ ...prev, [postId]: isLiked }));
              setLikeCounts((prev) => ({ ...prev, [postId]: count }));
            }}
            onCommentChanged={(postId, count) => {
              setCommentCounts((prev) => ({ ...prev, [postId]: count }));
            }}
            onClose={() => setSinglePost(false)}
          />
        </div>
      )}
    </>
  );
};

export default Feed;
