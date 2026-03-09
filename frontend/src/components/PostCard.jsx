import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useSocialMedia } from '../context/SocialMediaContext';
import { useAuth } from '../context/AuthContext';
import './PostCard.css';

export default function PostCard({ post }) {
  const { toggleLike } = useSocialMedia();
  const { user } = useAuth();
  const [showMenu, setShowMenu] = useState(false);

  const isLiked = post.likedByUserIds?.includes(user?.userId) || post.isLiked;
  const isOwner = post.authorId === user?.userId;

  const handleLike = (e) => {
    e.preventDefault();
    toggleLike(post.id, isLiked);
  };

  const formatDate = (dateStr) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = Math.floor((now - date) / 1000);
    if (diff < 60) return `${diff}s`;
    if (diff < 3600) return `${Math.floor(diff / 60)}m`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h`;
    return date.toLocaleDateString();
  };

  const renderContent = (text) => {
    if (!text) return null;
    return text.split(/(\s+)/).map((word, i) => {
      if (word.startsWith('#'))
        return <Link key={i} to={`/explore?tag=${word.slice(1)}`} className="hashtag">{word}</Link>;
      if (word.startsWith('@'))
        return <Link key={i} to={`/profile/${word.slice(1)}`} className="mention">{word}</Link>;
      return word;
    });
  };

  return (
    <article className="post-card">
      <div className="post-header">
        <Link to={`/profile/${post.authorUsername}`} className="author-info">
          <img
            src={post.authorProfileImageUrl || `https://api.dicebear.com/7.x/avataaars/svg?seed=${post.authorUsername}`}
            alt={post.authorDisplayName}
            className="avatar"
          />
          <div>
            <span className="display-name">{post.authorDisplayName || post.authorUsername}</span>
            <span className="username">@{post.authorUsername}</span>
            <span className="timestamp">{formatDate(post.createdAt)}</span>
          </div>
        </Link>

        {isOwner && (
          <div className="post-menu">
            <button onClick={() => setShowMenu(!showMenu)} className="menu-btn">⋮</button>
            {showMenu && (
              <div className="menu-dropdown">
                <button>Edit</button>
                <button className="danger">Delete</button>
              </div>
            )}
          </div>
        )}
      </div>

      <Link to={`/post/${post.id}`} className="post-content">
        <p>{renderContent(post.content)}</p>
      </Link>

      {post.mediaUrls?.length > 0 && (
        <div className={`media-grid media-count-${Math.min(post.mediaUrls.length, 4)}`}>
          {post.mediaUrls.slice(0, 4).map((url, i) => (
            <img key={i} src={url} alt={`Media ${i + 1}`} className="post-media" />
          ))}
        </div>
      )}

      <div className="post-actions">
        <button
          onClick={handleLike}
          className={`action-btn like-btn ${isLiked ? 'liked' : ''}`}
        >
          {isLiked ? '❤️' : '🤍'} <span>{post.likesCount || 0}</span>
        </button>

        <Link to={`/post/${post.id}`} className="action-btn comment-btn">
          💬 <span>{post.commentCount || 0}</span>
        </Link>

        <button className="action-btn share-btn">
          🔁 <span>{post.shareCount || 0}</span>
        </button>

        <button
          className="action-btn bookmark-btn"
          onClick={() => navigator.share?.({ url: `/post/${post.id}` })}
        >
          📤
        </button>
      </div>
    </article>
  );
}
