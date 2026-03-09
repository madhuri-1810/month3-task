import api from './api';

// ─── Post Service ─────────────────────────────────────────────────────────────
export const postService = {
  getFeed: (page = 0, size = 20) =>
    api.get(`/api/feed?page=${page}&size=${size}&sort=createdAt,desc`),
  getPost: (postId) => api.get(`/api/posts/${postId}`),
  getUserPosts: (userId, page = 0) =>
    api.get(`/api/users/${userId}/posts?page=${page}&size=20`),
  createPost: (data) => api.post('/api/posts', data),
  updatePost: (postId, data) => api.put(`/api/posts/${postId}`, data),
  deletePost: (postId) => api.delete(`/api/posts/${postId}`),
  likePost: (postId) => api.post(`/api/posts/${postId}/like`),
  unlikePost: (postId) => api.delete(`/api/posts/${postId}/like`),
  getTrending: (page = 0) =>
    api.get(`/api/posts/trending?page=${page}&size=20`),
  getByHashtag: (tag, page = 0) =>
    api.get(`/api/posts/hashtag/${tag}?page=${page}&size=20`),
  recordView: (postId) => api.post(`/api/posts/${postId}/view`),
};

// ─── User Service ─────────────────────────────────────────────────────────────
export const userService = {
  getProfile: (userId) => api.get(`/api/users/${userId}`),
  getByUsername: (username) => api.get(`/api/users/username/${username}`),
  updateProfile: (data) => api.put('/api/users/profile', data),
  searchUsers: (query, page = 0) =>
    api.get(`/api/users/search?query=${encodeURIComponent(query)}&page=${page}`),
  followUser: (userId) => api.post(`/api/users/${userId}/follow`),
  unfollowUser: (userId) => api.delete(`/api/users/${userId}/follow`),
  getFollowers: (userId, page = 0) =>
    api.get(`/api/users/${userId}/followers?page=${page}`),
  getFollowing: (userId, page = 0) =>
    api.get(`/api/users/${userId}/following?page=${page}`),
};

// ─── Notification Service ─────────────────────────────────────────────────────
export const notificationService = {
  getNotifications: (page = 0) =>
    api.get(`/api/notifications?page=${page}&size=20`),
  getUnreadCount: () => api.get('/api/notifications/unread-count'),
  markAsRead: (id) => api.put(`/api/notifications/${id}/read`),
  markAllAsRead: () => api.put('/api/notifications/read-all'),
  deleteNotification: (id) => api.delete(`/api/notifications/${id}`),
};

// ─── Chat Service ─────────────────────────────────────────────────────────────
export const chatService = {
  getConversations: (page = 0) =>
    api.get(`/api/chat/conversations?page=${page}&size=20`),
  getMessages: (otherUserId, page = 0) =>
    api.get(`/api/chat/messages/${otherUserId}?page=${page}&size=50&sort=createdAt,desc`),
  getUnreadCount: () => api.get('/api/chat/unread-count'),
  deleteMessage: (messageId) => api.delete(`/api/chat/messages/${messageId}`),
};

// ─── Media Service ────────────────────────────────────────────────────────────
export const mediaService = {
  uploadImage: (formData, onProgress) =>
    api.post('/api/media/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onProgress) onProgress(Math.round((e.loaded * 100) / e.total));
      },
    }),
  getMedia: (mediaId) => api.get(`/api/media/${mediaId}`),
  deleteMedia: (mediaId) => api.delete(`/api/media/${mediaId}`),
};

export default postService;
