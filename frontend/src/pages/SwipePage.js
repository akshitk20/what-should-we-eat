import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion, useMotionValue, useTransform, AnimatePresence } from 'framer-motion';
import { getRestaurants } from '../services/api';
import { connectToRoom, sendSwipe, disconnect } from '../services/websocket';
import RestaurantCard from '../components/RestaurantCard';

/* ------------------------------------------------------------------ */
/*  Draggable card wrapper — replaces react-tinder-card               */
/* ------------------------------------------------------------------ */
function SwipeableCard({ restaurant, onSwipe, style }) {
  const x = useMotionValue(0);
  const rotate = useTransform(x, [-200, 200], [-18, 18]);
  const yesOpacity = useTransform(x, [0, 100], [0, 1]);
  const nopeOpacity = useTransform(x, [-100, 0], [1, 0]);

  const handleDragEnd = (_, info) => {
    const threshold = 100;
    if (info.offset.x > threshold) {
      onSwipe('right', restaurant);
    } else if (info.offset.x < -threshold) {
      onSwipe('left', restaurant);
    }
  };

  return (
    <motion.div
      className="swipeable-card-wrapper"
      style={{ x, rotate, ...style }}
      drag="x"
      dragConstraints={{ left: 0, right: 0 }}
      dragElastic={0.9}
      onDragEnd={handleDragEnd}
      exit={{ x: 300, opacity: 0, transition: { duration: 0.3 } }}
    >
      {/* YUM overlay */}
      <motion.div className="swipe-overlay-yes" style={{ opacity: yesOpacity }}>
        YUM! 😍
      </motion.div>
      {/* NOPE overlay */}
      <motion.div className="swipe-overlay-no" style={{ opacity: nopeOpacity }}>
        NOPE 🙅
      </motion.div>

      <RestaurantCard restaurant={restaurant} />
    </motion.div>
  );
}

/* ------------------------------------------------------------------ */
/*  Main SwipePage                                                     */
/* ------------------------------------------------------------------ */
export default function SwipePage() {
  const { code }  = useParams();
  const navigate  = useNavigate();

  const memberId  = sessionStorage.getItem('memberId') || '';

  const [restaurants, setRestaurants]     = useState([]);
  const [currentIndex, setCurrentIndex]   = useState(0);
  const [voteProgress, setVoteProgress]   = useState({});
  const [allSwiped, setAllSwiped]         = useState(false);
  const [loading, setLoading]             = useState(true);

  /* ---- Load restaurants for this room ---- */
  useEffect(() => {
    getRestaurants(code)
      .then((list) => {
        setRestaurants(list || []);
        setCurrentIndex(0);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [code]);

  /* ---- WebSocket messages ---- */
  const handleMessage = useCallback(
    (data) => {
      switch (data.type) {
        case 'SWIPE_PROGRESS':
          setVoteProgress((prev) => ({
            ...prev,
            [data.restaurantId]: {
              voted: data.votedCount,
              total: data.totalMembers,
            },
          }));
          break;

        case 'MATCH_FOUND':
          disconnect();
          navigate(`/room/${code}/match`, {
            state: { match: data.restaurant },
          });
          break;

        default:
          break;
      }
    },
    [code, navigate]
  );

  useEffect(() => {
    connectToRoom(code, handleMessage);
    return () => disconnect();
  }, [code, handleMessage]);

  /* ---- Handle swipe ---- */
  const handleSwipe = useCallback(
    (direction, restaurant) => {
      const vote = direction === 'right' ? 'RIGHT' : 'LEFT';

      sendSwipe(code, {
        memberId,
        restaurantId: restaurant.id || restaurant.placeId,
        direction: vote,
      });

      setCurrentIndex((prev) => {
        const next = prev + 1;
        if (next >= restaurants.length) {
          setTimeout(() => setAllSwiped(true), 400);
        }
        return next;
      });
    },
    [code, memberId, restaurants.length]
  );

  /* ---- Button swipe helpers ---- */
  const swipeViaButton = (dir) => {
    if (currentIndex >= restaurants.length) return;
    handleSwipe(dir, restaurants[currentIndex]);
  };

  /* ---- Render states ---- */
  if (loading) {
    return (
      <div className="swipe-page">
        <div className="swipe-header">
          <h1>🍽️ Loading restaurants…</h1>
        </div>
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <span className="loading-spinner" style={{ borderTopColor: '#ff6b6b', width: 40, height: 40, borderWidth: 5 }} />
        </div>
      </div>
    );
  }

  const current = restaurants[currentIndex];
  const progressInfo = current
    ? voteProgress[current.id || current.placeId]
    : null;
  const progressPct = restaurants.length > 0
    ? Math.round((currentIndex / restaurants.length) * 100)
    : 0;

  return (
    <div className="swipe-page">
      {/* Header */}
      <div className="swipe-header">
        <h1>🍴 What's your vibe?</h1>
        <div className="swipe-progress-bar-wrap">
          <div
            className="swipe-progress-bar"
            style={{ width: `${progressPct}%` }}
          />
        </div>
        <div className="swipe-progress-text">
          {currentIndex} / {restaurants.length} restaurants reviewed
        </div>
      </div>

      <div className="swipe-body">
        {/* All swiped — waiting for others */}
        {allSwiped ? (
          <div className="all-swiped">
            <span className="emoji">⏳</span>
            <h2>You're all done!</h2>
            <p>Waiting for your friends to finish swiping…</p>
            <p style={{ marginTop: 8, fontSize: '0.8rem', color: '#aaa' }}>
              A match will be announced when everyone is done.
            </p>
          </div>
        ) : (
          <>
            {/* Card stack */}
            <div className="card-stack">
              <AnimatePresence>
                {restaurants
                  .slice(currentIndex, currentIndex + 3)
                  .reverse()
                  .map((restaurant, reversedIdx) => {
                    const stackIdx = (Math.min(3, restaurants.length - currentIndex) - 1) - reversedIdx;
                    return stackIdx === 0 ? (
                      <SwipeableCard
                        key={restaurant.id || restaurant.placeId}
                        restaurant={restaurant}
                        onSwipe={handleSwipe}
                        style={{
                          position: 'absolute',
                          zIndex: 10,
                        }}
                      />
                    ) : (
                      <motion.div
                        key={restaurant.id || restaurant.placeId}
                        className="swipeable-card-wrapper"
                        style={{
                          position: 'absolute',
                          transform: `scale(${1 - stackIdx * 0.04}) translateY(${stackIdx * 14}px)`,
                          zIndex: 10 - stackIdx,
                          pointerEvents: 'none',
                        }}
                      >
                        <RestaurantCard restaurant={restaurant} />
                      </motion.div>
                    );
                  })}
              </AnimatePresence>
            </div>

            {/* Peer vote progress */}
            {progressInfo && (
              <div className="vote-progress">
                👥 {progressInfo.voted} / {progressInfo.total} friends voted on this
              </div>
            )}

            {/* Action buttons */}
            <div className="swipe-buttons">
              <button
                className="swipe-btn-nope"
                onClick={() => swipeViaButton('left')}
                aria-label="Nope"
                title="NOPE"
              >
                ✕
              </button>

              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 4 }}>
                <span style={{ fontSize: '0.7rem', fontWeight: 800, color: '#aaa', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  Swipe!
                </span>
              </div>

              <button
                className="swipe-btn-yum"
                onClick={() => swipeViaButton('right')}
                aria-label="Yum"
                title="YUM!"
              >
                ♥
              </button>
            </div>

            {/* NOPE / YUM labels */}
            <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', maxWidth: 280, paddingBottom: 8 }}>
              <span style={{ fontSize: '0.75rem', fontWeight: 800, color: '#ff4d4d' }}>← NOPE</span>
              <span style={{ fontSize: '0.75rem', fontWeight: 800, color: '#51cf66' }}>YUM! →</span>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
