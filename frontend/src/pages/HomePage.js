import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createRoom, joinRoom } from '../services/api';

// Palette for avatar colours (also used on lobby)
const AVATAR_COLORS = [
  '#ff6b6b', '#ffa94d', '#ffe066', '#51cf66',
  '#339af0', '#cc5de8', '#f06595', '#20c997',
];

export default function HomePage() {
  const navigate = useNavigate();

  // Tab: 'create' | 'join'
  const [tab, setTab] = useState('create');

  // Create-room form
  const [createNickname, setCreateNickname] = useState('');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState('');

  // Join-room form
  const [joinNickname, setJoinNickname] = useState('');
  const [joinCode, setJoinCode] = useState('');
  const [joining, setJoining] = useState(false);
  const [joinError, setJoinError] = useState('');

  /* ---- Geolocation helper ---- */
  const getCoords = () =>
    new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        // Fallback to a central location if geolocation unavailable
        resolve({ latitude: 51.5074, longitude: -0.1278 });
        return;
      }
      navigator.geolocation.getCurrentPosition(
        ({ coords }) => resolve({ latitude: coords.latitude, longitude: coords.longitude }),
        () => resolve({ latitude: 51.5074, longitude: -0.1278 }),
        { timeout: 5000 }
      );
    });

  /* ---- Create Room ---- */
  const handleCreate = async () => {
    if (!createNickname.trim()) {
      setCreateError('Please enter your nickname 😊');
      return;
    }
    setCreating(true);
    setCreateError('');
    try {
      const { latitude, longitude } = await getCoords();
      const data = await createRoom({
        nickname: createNickname.trim(),
        latitude,
        longitude,
      });
      sessionStorage.setItem('memberId', data.memberId);
      sessionStorage.setItem('nickname', createNickname.trim());
      sessionStorage.setItem('isHost', 'true');
      sessionStorage.setItem(
        'avatarColor',
        AVATAR_COLORS[Math.floor(Math.random() * AVATAR_COLORS.length)]
      );
      navigate(`/room/${data.roomCode}`);
    } catch (err) {
      setCreateError('Could not create room. Is the server running?');
    } finally {
      setCreating(false);
    }
  };

  /* ---- Join Room ---- */
  const handleJoin = async () => {
    if (!joinNickname.trim()) {
      setJoinError('Please enter your nickname 😊');
      return;
    }
    if (!joinCode.trim()) {
      setJoinError('Please enter the room code 🔑');
      return;
    }
    setJoining(true);
    setJoinError('');
    try {
      const data = await joinRoom(joinCode.trim().toUpperCase(), {
        nickname: joinNickname.trim(),
      });
      sessionStorage.setItem('memberId', data.memberId);
      sessionStorage.setItem('nickname', joinNickname.trim());
      sessionStorage.setItem('isHost', 'false');
      sessionStorage.setItem(
        'avatarColor',
        AVATAR_COLORS[Math.floor(Math.random() * AVATAR_COLORS.length)]
      );
      navigate(`/room/${joinCode.trim().toUpperCase()}`);
    } catch (err) {
      setJoinError('Room not found. Check the code and try again 🔍');
    } finally {
      setJoining(false);
    }
  };

  const handleKeyDown = (e, action) => {
    if (e.key === 'Enter') action();
  };

  return (
    <div className="home-page">
      {/* Hero */}
      <div className="home-hero">
        <span className="app-emoji">🍽️</span>
        <h1>What Should<br />We Eat?</h1>
        <p>Swipe on restaurants with friends<br />and find your perfect match!</p>
      </div>

      {/* Card panel */}
      <div className="home-card">
        {/* Tab switcher */}
        <div className="home-tabs" role="tablist">
          <button
            role="tab"
            aria-selected={tab === 'create'}
            className={`home-tab ${tab === 'create' ? 'active' : ''}`}
            onClick={() => { setTab('create'); setCreateError(''); }}
          >
            🎉 Create Room
          </button>
          <button
            role="tab"
            aria-selected={tab === 'join'}
            className={`home-tab ${tab === 'join' ? 'active' : ''}`}
            onClick={() => { setTab('join'); setJoinError(''); }}
          >
            🚪 Join Room
          </button>
        </div>

        {/* Create Room Panel */}
        {tab === 'create' && (
          <div className="home-tab-panel">
            <div className="input-group">
              <label className="input-label" htmlFor="create-nickname">
                Your Nickname
              </label>
              <input
                id="create-nickname"
                className="input"
                type="text"
                placeholder="e.g. Hungry Hannah 🍔"
                maxLength={24}
                value={createNickname}
                onChange={(e) => setCreateNickname(e.target.value)}
                onKeyDown={(e) => handleKeyDown(e, handleCreate)}
              />
            </div>

            {createError && (
              <div className="error-banner" role="alert">{createError}</div>
            )}

            <button
              className="btn btn-primary btn-full btn-lg"
              onClick={handleCreate}
              disabled={creating}
            >
              {creating ? (
                <><span className="loading-spinner" /> Creating…</>
              ) : (
                <>🚀 Create Room</>
              )}
            </button>

            <div className="home-divider"><span>how it works</span></div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {[
                ['1️⃣', 'Create a room and share the code'],
                ['2️⃣', 'Friends join with your room code'],
                ['3️⃣', 'Everyone swipes on nearby restaurants'],
                ['4️⃣', 'Find the restaurant you ALL love! 🎉'],
              ].map(([icon, text]) => (
                <div
                  key={text}
                  style={{ display: 'flex', alignItems: 'center', gap: 12 }}
                >
                  <span style={{ fontSize: '1.2rem' }}>{icon}</span>
                  <span style={{ fontSize: '0.875rem', fontWeight: 600, color: '#868e96' }}>
                    {text}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Join Room Panel */}
        {tab === 'join' && (
          <div className="home-tab-panel">
            <div className="input-group">
              <label className="input-label" htmlFor="join-code">
                Room Code
              </label>
              <input
                id="join-code"
                className="input"
                type="text"
                placeholder="e.g. ABC123"
                maxLength={8}
                value={joinCode}
                onChange={(e) => setJoinCode(e.target.value.toUpperCase())}
                onKeyDown={(e) => handleKeyDown(e, handleJoin)}
                style={{ fontFamily: 'monospace', letterSpacing: '0.12em', fontSize: '1.2rem' }}
              />
            </div>

            <div className="input-group">
              <label className="input-label" htmlFor="join-nickname">
                Your Nickname
              </label>
              <input
                id="join-nickname"
                className="input"
                type="text"
                placeholder="e.g. Taco Tim 🌮"
                maxLength={24}
                value={joinNickname}
                onChange={(e) => setJoinNickname(e.target.value)}
                onKeyDown={(e) => handleKeyDown(e, handleJoin)}
              />
            </div>

            {joinError && (
              <div className="error-banner" role="alert">{joinError}</div>
            )}

            <button
              className="btn btn-primary btn-full btn-lg"
              onClick={handleJoin}
              disabled={joining}
            >
              {joining ? (
                <><span className="loading-spinner" /> Joining…</>
              ) : (
                <>🚀 Join Room</>
              )}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
