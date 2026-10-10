import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import TinderCard from 'react-tinder-card';
import { getRoom } from '../services/api';
import { connectToRoom, sendSwipe, disconnect } from '../services/websocket';
import RestaurantCard from '../components/RestaurantCard';

export default function SwipePage() {
  const { code }  = useParams();
  const navigate  = useNavigate();

  const memberId  = sessionStorage.getItem('memberId') || '';

  const [restaurants, setRestaurants]     = useState([]);
  const [currentIndex, setCurrentIndex]   = useState(0);
  const [voteProgress, setVoteProgress]   = useState({});  // { restaurantId: { voted, total } }
  const [allSwiped, setAllSwiped]         = useState(false);
  const [swipeHint, setSwipeHint]         = useState(null); // 'left' | 'right' | null
  const [loading, setLoading]             = useState(true);

  // Refs for programmatic swipe (one per card)
  const cardRefs = useRef([]);

  /* ---- Load restaurants for this room ---- */
  useEffect(() => {
    getRoom(code)
      .then((room) => {
        const list = room?.restaurants || [];
        setRestaurants(list);
        cardRefs.current = list.map(() => React.createRef());
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
    const client = connectToRoom(code, handleMessage);
    return () => disconnect();
  }, [code, handleMessage]);

  /* ---- Handle swipe (from card drag or button) ---- */
  const handleSwipe = useCallback(
    (direction, restaurant) => {
      const vote = direction === 'right' ? 'RIGHT' : 'LEFT';

      sendSwipe(code, {
        memberId,
        restaurantId: restaurant.id || restaurant.placeId,
        direction: vote,
      });

      // Show colour flash then clear
      setSwipeHint(direction);
      setTimeout(() => setSwipeHint(null), 600);

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

  /* ---- Programmatic swipe via buttons ---- */
  const swipeCard = async (dir) => {
    const idx = currentIndex;
    if (idx >= restaurants.length) return;
    if (cardRefs.current[idx]?.current) {
      await cardRefs.current[idx].current.swipe(dir);
    }
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
              {/* Swipe direction hints */}
              {swipeHint === 'right' && (
                <div className="swipe-overlay-yes" style={{ opacity: 1 }}>
                  YUM! 😍
                </div>
              )}
              {swipeHint === 'left' && (
                <div className="swipe-overlay-no" style={{ opacity: 1 }}>
                  NOPE 🙅
                </div>
              )}

              {/* Render remaining cards in reverse so top card is last in DOM */}
              {restaurants
                .slice(currentIndex, currentIndex + 3)
                .map((restaurant, stackIdx) => {
                  const absIdx = currentIndex + stackIdx;
                  return (
                    <TinderCard
                      key={restaurant.id || restaurant.placeId || absIdx}
                      ref={cardRefs.current[absIdx]}
                      onSwipe={(dir) => {
                        if (stackIdx === 0) handleSwipe(dir, restaurant);
                      }}
                      onCardLeftScreen={() => {}}
                      preventSwipe={stackIdx !== 0 ? ['left', 'right', 'up', 'down'] : ['up', 'down']}
                      swipeRequirementType="position"
                      swipeThreshold={80}
                    >
                      <RestaurantCard
                        restaurant={restaurant}
                        style={{
                          // Stack visual effect
                          transform: stackIdx === 0
                            ? 'none'
                            : `scale(${1 - stackIdx * 0.04}) translateY(${stackIdx * 14}px)`,
                          zIndex: 10 - stackIdx,
                          pointerEvents: stackIdx === 0 ? 'auto' : 'none',
                        }}
                      />
                    </TinderCard>
                  );
                })}
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
                onClick={() => swipeCard('left')}
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
                onClick={() => swipeCard('right')}
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
