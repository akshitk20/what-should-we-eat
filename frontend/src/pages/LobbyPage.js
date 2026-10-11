import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getRoom, startSwiping } from '../services/api';
import { connectToRoom, notifyJoin, notifyStart, disconnect } from '../services/websocket';

const AVATAR_COLORS = [
  '#ff6b6b', '#ffa94d', '#ffe066', '#51cf66',
  '#339af0', '#cc5de8', '#f06595', '#20c997',
  '#748ffc', '#ff8787',
];

function getAvatarColor(index) {
  return AVATAR_COLORS[index % AVATAR_COLORS.length];
}

function getInitials(name = '') {
  return name
    .split(' ')
    .map((w) => w[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
}

export default function LobbyPage() {
  const { code } = useParams();
  const navigate  = useNavigate();

  const memberId = sessionStorage.getItem('memberId') || '';
  const nickname = sessionStorage.getItem('nickname') || 'You';
  const isHost   = sessionStorage.getItem('isHost') === 'true';

  const [members, setMembers]     = useState([]);
  const [copied,  setCopied]      = useState(false);
  const [starting, setStarting]   = useState(false);
  const [startError, setStartError] = useState('');

  /* ---- Load initial room state ---- */
  useEffect(() => {
    getRoom(code)
      .then((room) => {
        if (room?.members) setMembers(room.members);
        if (room?.status === 'SWIPING') {
          navigate(`/room/${code}/swipe`, { replace: true });
        }
      })
      .catch(console.error);
  }, [code, navigate]);

  /* ---- WebSocket connection ---- */
  const handleMessage = useCallback(
    (msg) => {
      console.log('[Lobby] WS message:', msg);
      const type = msg.type;
      const data = msg.payload || msg;

      switch (type) {
        case 'MEMBER_JOINED':
          setMembers((prev) => {
            if (prev.some((m) => m.nickname === data.nickname)) return prev;
            return [...prev, { id: data.memberId, nickname: data.nickname, isHost: false }];
          });
          break;

        case 'MEMBER_LEFT':
          setMembers((prev) =>
            prev.filter((m) => m.id !== data.memberId)
          );
          break;

        case 'SWIPING_STARTED':
          navigate(`/room/${code}/swipe`);
          break;

        default:
          break;
      }
    },
    [code, navigate]
  );

  useEffect(() => {
    const client = connectToRoom(code, handleMessage);
    // Announce ourselves once connected
    const announceTimer = setTimeout(() => {
      notifyJoin(code, nickname);
    }, 800);

    return () => {
      clearTimeout(announceTimer);
      disconnect();
    };
  }, [code, nickname, handleMessage]);

  /* ---- Copy room code ---- */
  const handleCopy = () => {
    navigator.clipboard.writeText(code).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };

  /* ---- Start swiping (host only) ---- */
  const handleStart = async () => {
    if (members.length < 1) {
      setStartError('Wait for at least one friend to join! 👀');
      return;
    }
    setStarting(true);
    setStartError('');
    try {
      await startSwiping(code, memberId);
      notifyStart(code, memberId);
      navigate(`/room/${code}/swipe`);
    } catch (err) {
      setStartError('Could not start. Please try again.');
      setStarting(false);
    }
  };

  return (
    <div className="lobby-page">
      {/* Header */}
      <div className="lobby-header">
        <h1>🍽️ The Feast Room</h1>
        <p>Share the code and wait for your crew!</p>
      </div>

      {/* Body */}
      <div className="lobby-body">

        {/* Room code */}
        <div className="room-code-block">
          <div className="room-code-label">Room Code</div>
          <div className="room-code-pill">
            <span className="room-code-text">{code}</span>
            <button
              className={`copy-btn ${copied ? 'copied' : ''}`}
              onClick={handleCopy}
              aria-label="Copy room code"
            >
              {copied ? '✓ Copied!' : '📋 Copy'}
            </button>
          </div>
        </div>

        {/* Members list */}
        <div className="members-section">
          <h2>
            🧑‍🤝‍🧑 Members ({members.length || 1})
          </h2>
          <div className="members-list">
            {/* Always show self first */}
            <div className="member-chip">
              <div
                className="member-avatar"
                style={{ background: sessionStorage.getItem('avatarColor') || AVATAR_COLORS[0] }}
              >
                {getInitials(nickname)}
              </div>
              <span className="member-name">{nickname}</span>
              {isHost && <span className="member-badge-host">Host</span>}
            </div>

            {/* Other members */}
            {members
              .filter((m) => m.memberId !== memberId)
              .map((m, i) => (
                <div key={m.memberId} className="member-chip">
                  <div
                    className="member-avatar"
                    style={{ background: getAvatarColor(i + 1) }}
                  >
                    {getInitials(m.nickname)}
                  </div>
                  <span className="member-name">{m.nickname}</span>
                </div>
              ))}
          </div>
        </div>

        {/* Waiting animation */}
        <div className="waiting-status">
          <span className="waiting-dot" />
          <span className="waiting-dot" />
          <span className="waiting-dot" />
          <p className="waiting-text">
            {isHost
              ? 'Waiting for friends to join…'
              : 'Waiting for the host to start…'}
          </p>
        </div>

        {/* Footer actions */}
        <div className="lobby-footer">
          {startError && (
            <div className="error-banner" role="alert">{startError}</div>
          )}

          {isHost && (
            <button
              className="btn btn-primary btn-full btn-lg"
              onClick={handleStart}
              disabled={starting}
            >
              {starting ? (
                <><span className="loading-spinner" /> Starting…</>
              ) : (
                <>🚀 Start Swiping!</>
              )}
            </button>
          )}

          <button
            className="btn btn-secondary btn-full"
            onClick={() => navigate('/')}
          >
            ← Leave Room
          </button>
        </div>

      </div>
    </div>
  );
}
