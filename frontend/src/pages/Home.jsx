import React, { useEffect, useRef, useCallback } from 'react';
import { useSocialMedia } from '../context/SocialMediaContext';
import CreatePost from '../components/CreatePost';
import PostCard from '../components/PostCard';
import LoadingSpinner from '../components/LoadingSpinner';
import './Home.css';

export default function Home() {
  const { state, loadMorePosts, fetchFeed } = useSocialMedia();
  const { feedPosts, isLoadingFeed, hasMorePosts } = state;

  const observerRef = useRef(null);
  const loadMoreRef = useRef(null);

  // Intersection Observer for infinite scroll
  const setupObserver = useCallback(() => {
    if (observerRef.current) observerRef.current.disconnect();

    observerRef.current = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMorePosts && !isLoadingFeed) {
          loadMorePosts();
        }
      },
      { threshold: 0.1 }
    );

    if (loadMoreRef.current) {
      observerRef.current.observe(loadMoreRef.current);
    }
  }, [hasMorePosts, isLoadingFeed, loadMorePosts]);

  useEffect(() => {
    setupObserver();
    return () => observerRef.current?.disconnect();
  }, [setupObserver]);

  return (
    <div className="home-container">
      <div className="feed-column">
        <CreatePost onPostCreated={() => fetchFeed(0)} />

        <div className="posts-feed">
          {feedPosts.length === 0 && !isLoadingFeed ? (
            <div className="empty-feed">
              <div className="empty-icon">🌟</div>
              <h3>Your feed is empty</h3>
              <p>Follow some users to see their posts here</p>
            </div>
          ) : (
            feedPosts.map((post) => (
              <PostCard key={post.id} post={post} />
            ))
          )}

          {isLoadingFeed && <LoadingSpinner />}

          {/* Infinite scroll trigger */}
          <div ref={loadMoreRef} style={{ height: '20px' }} />

          {!hasMorePosts && feedPosts.length > 0 && (
            <div className="end-of-feed">
              <p>You've reached the end! 🎉</p>
            </div>
          )}
        </div>
      </div>

      <div className="sidebar-column">
        <TrendingSection />
        <SuggestedUsers />
      </div>
    </div>
  );
}

function TrendingSection() {
  const trending = [
    { tag: '#React', posts: '12.5K' },
    { tag: '#SpringBoot', posts: '8.2K' },
    { tag: '#Microservices', posts: '6.1K' },
    { tag: '#Docker', posts: '15.3K' },
    { tag: '#WebDev', posts: '22.1K' },
  ];

  return (
    <div className="sidebar-card">
      <h3>Trending 🔥</h3>
      {trending.map(({ tag, posts }) => (
        <div key={tag} className="trending-item">
          <span className="hashtag">{tag}</span>
          <span className="post-count">{posts} posts</span>
        </div>
      ))}
    </div>
  );
}

function SuggestedUsers() {
  return (
    <div className="sidebar-card">
      <h3>Who to Follow</h3>
      <div className="suggested-placeholder">
        <p>Suggestions loading...</p>
      </div>
    </div>
  );
}
