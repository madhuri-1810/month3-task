import React, { createContext, useContext, useReducer, useEffect, useCallback, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import postService from '../services/postService';
import notificationService from '../services/notificationService';

const SocialMediaContext = createContext();

// ─── State ────────────────────────────────────────────────────────────────────
const initialState = {
  posts: [],
  feedPosts: [],
  notifications: [],
  unreadNotificationCount: 0,
  unreadMessageCount: 0,
  onlineUsers: new Set(),
  isLoadingPosts: false,
  isLoadingFeed: false,
  hasMorePosts: true,
  currentPage: 0,
  error: null,
};

// ─── Reducer ──────────────────────────────────────────────────────────────────
function reducer(state, action) {
  switch (action.type) {
    case 'FETCH_FEED_REQUEST':
      return { ...state, isLoadingFeed: true, error: null };
    case 'FETCH_FEED_SUCCESS':
      return {
        ...state,
        isLoadingFeed: false,
        feedPosts: action.page === 0
          ? action.payload.content
          : [...state.feedPosts, ...action.payload.content],
        hasMorePosts: !action.payload.last,
        currentPage: action.page,
      };
    case 'FETCH_FEED_FAILURE':
      return { ...state, isLoadingFeed: false, error: action.payload };

    case 'ADD_POST':
      return { ...state, feedPosts: [action.payload, ...state.feedPosts] };
    case 'UPDATE_POST':
      return {
        ...state,
        feedPosts: state.feedPosts.map(p => p.id === action.payload.id ? action.payload : p),
      };
    case 'DELETE_POST':
      return { ...state, feedPosts: state.feedPosts.filter(p => p.id !== action.payload) };
    case 'TOGGLE_LIKE':
      return {
        ...state,
        feedPosts: state.feedPosts.map(p =>
          p.id === action.payload.postId
            ? { ...p, likesCount: action.payload.likesCount, isLiked: action.payload.liked }
            : p
        ),
      };

    case 'SET_NOTIFICATIONS':
      return { ...state, notifications: action.payload };
    case 'ADD_NOTIFICATION':
      return {
        ...state,
        notifications: [action.payload, ...state.notifications],
        unreadNotificationCount: state.unreadNotificationCount + 1,
      };
    case 'SET_UNREAD_COUNT':
      return { ...state, unreadNotificationCount: action.payload };
    case 'SET_UNREAD_MESSAGES':
      return { ...state, unreadMessageCount: action.payload };
    case 'MARK_ALL_READ':
      return {
        ...state,
        notifications: state.notifications.map(n => ({ ...n, isRead: true })),
        unreadNotificationCount: 0,
      };

    case 'USER_ONLINE':
      return { ...state, onlineUsers: new Set([...state.onlineUsers, action.payload]) };
    case 'USER_OFFLINE': {
      const updated = new Set(state.onlineUsers);
      updated.delete(action.payload);
      return { ...state, onlineUsers: updated };
    }

    default:
      return state;
  }
}

// ─── Provider ─────────────────────────────────────────────────────────────────
export function SocialMediaProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, initialState);
  const stompClient = useRef(null);
  const userId = localStorage.getItem('user')
    ? JSON.parse(localStorage.getItem('user')).userId
    : null;

  // WebSocket connection
  useEffect(() => {
    if (!userId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${process.env.REACT_APP_API_URL}/ws`),
      connectHeaders: {
        Authorization: `Bearer ${localStorage.getItem('accessToken')}`,
      },
      debug: (str) => console.log('[STOMP]', str),
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('WebSocket connected');

        // Subscribe to personal notifications
        client.subscribe(`/user/${userId}/queue/notifications`, (msg) => {
          const notification = JSON.parse(msg.body);
          dispatch({ type: 'ADD_NOTIFICATION', payload: notification });
        });

        // Subscribe to public feed updates
        client.subscribe('/topic/feed', (msg) => {
          const newPost = JSON.parse(msg.body);
          if (newPost.authorId !== userId) {
            dispatch({ type: 'ADD_POST', payload: newPost });
          }
        });

        // Subscribe to online status
        client.subscribe('/topic/online-users', (msg) => {
          const { userId: onlineUserId, status } = JSON.parse(msg.body);
          dispatch({ type: status === 'online' ? 'USER_ONLINE' : 'USER_OFFLINE', payload: onlineUserId });
        });
      },
      onDisconnect: () => console.log('WebSocket disconnected'),
    });

    client.activate();
    stompClient.current = client;

    return () => {
      if (client.active) client.deactivate();
    };
  }, [userId]);

  // Load initial data
  useEffect(() => {
    if (userId) {
      fetchFeed(0);
      fetchUnreadCount();
    }
  }, [userId]);

  const fetchFeed = useCallback(async (page = 0) => {
    dispatch({ type: 'FETCH_FEED_REQUEST' });
    try {
      const response = await postService.getFeed(page);
      dispatch({ type: 'FETCH_FEED_SUCCESS', payload: response.data, page });
    } catch (error) {
      dispatch({ type: 'FETCH_FEED_FAILURE', payload: error.message });
    }
  }, []);

  const loadMorePosts = useCallback(() => {
    if (!state.isLoadingFeed && state.hasMorePosts) {
      fetchFeed(state.currentPage + 1);
    }
  }, [state.isLoadingFeed, state.hasMorePosts, state.currentPage, fetchFeed]);

  const createPost = useCallback(async (content, mediaUrls = []) => {
    const response = await postService.createPost({ content, mediaUrls });
    dispatch({ type: 'ADD_POST', payload: response.data });
    return response.data;
  }, []);

  const toggleLike = useCallback(async (postId, isCurrentlyLiked) => {
    try {
      const response = isCurrentlyLiked
        ? await postService.unlikePost(postId)
        : await postService.likePost(postId);
      dispatch({ type: 'TOGGLE_LIKE', payload: { postId, ...response.data } });
    } catch (error) {
      console.error('Like failed:', error);
    }
  }, []);

  const fetchUnreadCount = useCallback(async () => {
    try {
      const response = await notificationService.getUnreadCount();
      dispatch({ type: 'SET_UNREAD_COUNT', payload: response.data });
    } catch (error) {
      console.error('Failed to fetch unread count:', error);
    }
  }, []);

  const markAllNotificationsRead = useCallback(async () => {
    await notificationService.markAllAsRead();
    dispatch({ type: 'MARK_ALL_READ' });
  }, []);

  return (
    <SocialMediaContext.Provider value={{
      state,
      dispatch,
      stompClient: stompClient.current,
      fetchFeed,
      loadMorePosts,
      createPost,
      toggleLike,
      fetchUnreadCount,
      markAllNotificationsRead,
    }}>
      {children}
    </SocialMediaContext.Provider>
  );
}

export const useSocialMedia = () => {
  const context = useContext(SocialMediaContext);
  if (!context) throw new Error('useSocialMedia must be used within SocialMediaProvider');
  return context;
};
