import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from '../context/AuthContext';
import chatService from '../services/chatService';
import './Messages.css';

export default function Messages() {
  const { userId: chatPartnerId } = useParams();
  const { user } = useAuth();

  const [conversations, setConversations] = useState([]);
  const [activeConversation, setActiveConversation] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [partnerTyping, setPartnerTyping] = useState(false);
  const [loading, setLoading] = useState(true);

  const stompRef = useRef(null);
  const messagesEndRef = useRef(null);
  const typingTimeoutRef = useRef(null);

  // Setup WebSocket
  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`${process.env.REACT_APP_API_URL}/ws`),
      connectHeaders: {
        Authorization: `Bearer ${localStorage.getItem('accessToken')}`,
      },
      reconnectDelay: 3000,
      onConnect: () => {
        // Subscribe to incoming messages
        client.subscribe(`/user/${user.userId}/queue/messages`, (msg) => {
          const message = JSON.parse(msg.body);
          if (activeConversation && (message.senderId === activeConversation || message.recipientId === activeConversation)) {
            setMessages(prev => [...prev, message]);
          }
          updateConversationLastMessage(message);
        });

        // Subscribe to typing indicators
        client.subscribe(`/user/${user.userId}/queue/typing`, (msg) => {
          const { senderId, typing } = JSON.parse(msg.body);
          if (senderId === activeConversation) {
            setPartnerTyping(typing);
            if (typing) {
              setTimeout(() => setPartnerTyping(false), 3000);
            }
          }
        });

        // Subscribe to read receipts
        client.subscribe(`/user/${user.userId}/queue/read-receipts`, (msg) => {
          const { userId: readByUserId } = JSON.parse(msg.body);
          setMessages(prev => prev.map(m =>
            m.recipientId === readByUserId ? { ...m, isRead: true } : m
          ));
        });
      },
    });

    client.activate();
    stompRef.current = client;
    return () => client.deactivate();
  }, [user.userId, activeConversation]);

  // Load conversations
  useEffect(() => {
    chatService.getConversations().then(res => {
      setConversations(res.data.content);
      setLoading(false);
    });

    if (chatPartnerId) setActiveConversation(chatPartnerId);
  }, [chatPartnerId]);

  // Load messages when conversation changes
  useEffect(() => {
    if (!activeConversation) return;
    chatService.getMessages(activeConversation).then(res => {
      setMessages(res.data.content.reverse());
      scrollToBottom();
    });
  }, [activeConversation]);

  useEffect(() => scrollToBottom(), [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const sendMessage = useCallback(() => {
    if (!newMessage.trim() || !activeConversation) return;

    stompRef.current?.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({
        recipientId: activeConversation,
        content: newMessage.trim(),
        messageType: 'TEXT',
      }),
    });

    setNewMessage('');
  }, [newMessage, activeConversation]);

  const handleTyping = (e) => {
    setNewMessage(e.target.value);

    if (!isTyping) {
      setIsTyping(true);
      stompRef.current?.publish({
        destination: '/app/chat.typing',
        body: JSON.stringify({ recipientId: activeConversation, typing: true }),
      });
    }

    clearTimeout(typingTimeoutRef.current);
    typingTimeoutRef.current = setTimeout(() => {
      setIsTyping(false);
      stompRef.current?.publish({
        destination: '/app/chat.typing',
        body: JSON.stringify({ recipientId: activeConversation, typing: false }),
      });
    }, 1500);
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const updateConversationLastMessage = (message) => {
    setConversations(prev =>
      prev.map(conv =>
        conv.partnerId === message.senderId || conv.partnerId === message.recipientId
          ? { ...conv, lastMessage: message }
          : conv
      )
    );
  };

  if (loading) return <div className="messages-loading">Loading conversations...</div>;

  return (
    <div className="messages-container">
      {/* Conversations List */}
      <div className="conversations-sidebar">
        <div className="conversations-header">
          <h2>Messages</h2>
          <button className="new-message-btn">✏️</button>
        </div>
        <div className="conversations-list">
          {conversations.length === 0 ? (
            <p className="no-conversations">No conversations yet. Start chatting!</p>
          ) : (
            conversations.map(conv => (
              <div
                key={conv.partnerId}
                className={`conversation-item ${activeConversation === conv.partnerId ? 'active' : ''}`}
                onClick={() => setActiveConversation(conv.partnerId)}
              >
                <img
                  src={conv.partnerProfileImage || `https://api.dicebear.com/7.x/avataaars/svg?seed=${conv.partnerUsername}`}
                  alt={conv.partnerDisplayName}
                  className="conv-avatar"
                />
                <div className="conv-info">
                  <span className="conv-name">{conv.partnerDisplayName}</span>
                  <span className="conv-last-message">{conv.lastMessage?.content?.slice(0, 40)}...</span>
                </div>
                {conv.unreadCount > 0 && (
                  <span className="unread-badge">{conv.unreadCount}</span>
                )}
              </div>
            ))
          )}
        </div>
      </div>

      {/* Chat Window */}
      <div className="chat-window">
        {activeConversation ? (
          <>
            <div className="chat-header">
              <span>Chat</span>
            </div>
            <div className="messages-list">
              {messages.map((msg, i) => (
                <div
                  key={msg.id || i}
                  className={`message ${msg.senderId === user.userId ? 'sent' : 'received'}`}
                >
                  <p>{msg.content}</p>
                  <div className="message-meta">
                    <span className="msg-time">
                      {new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>
                    {msg.senderId === user.userId && (
                      <span className="read-status">{msg.isRead ? '✓✓' : '✓'}</span>
                    )}
                  </div>
                </div>
              ))}

              {partnerTyping && (
                <div className="typing-indicator">
                  <span></span><span></span><span></span>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            <div className="message-input-area">
              <textarea
                value={newMessage}
                onChange={handleTyping}
                onKeyPress={handleKeyPress}
                placeholder="Type a message... (Enter to send)"
                rows={1}
              />
              <button onClick={sendMessage} disabled={!newMessage.trim()} className="send-btn">
                ➤
              </button>
            </div>
          </>
        ) : (
          <div className="no-conversation-selected">
            <div className="empty-chat-icon">💬</div>
            <h3>Select a conversation</h3>
            <p>Choose from your existing conversations or start a new one</p>
          </div>
        )}
      </div>
    </div>
  );
}
