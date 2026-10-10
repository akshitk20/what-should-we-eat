import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { getCompromise } from '../services/api';

/* ---- Confetti helpers ---- */
const CONFETTI_COLORS = [
  '#ff6b6b', '#ffa94d', '#ffe066', '#51cf66',
  '#339af0', '#cc5de8', '#f06595', '#20c997',
];

function randomBetween(min, max) {
  return Math.random() * (max - min) + min;
}

function generateConfettiPieces(count = 60) {
  return Array.from({ length: count }, (_, i) => ({
    id: i,
    color: CONFETTI_COLORS[i % CONFETTI_COLORS.length],
    left: `${randomBetween(0, 100)}%`,
    width: `${randomBetween(6, 14)}px`,
    height: `${randomBetween(8, 16)}px`,
    animationDuration: `${randomBetween(2.5, 4.5)}s`,
    animationDelay:    `${randomBetween(0, 1.5)}s`,
    borderRadius:      Math.random() > 0.5 ? '50%' : '2px',
  }));
}

/* ---- Star display ---- */
function StarRating({ rating }) {
  if (!rating) return null;
  const full = Math.floor(rating);
  return (
    <span className="match-rating">
      {'⭐'.repeat(full)}
      <span style={{ marginLeft: 5 }}>{rating.toFixed(1)}</span>
    </span>
  );
}

/* ---- Price display ---- */
function Price({ level }) {
  if (level === undefined || level === null) return null;
  return (
    <span className="match-price">
      {'$'.repeat(level)}
      <span style={{ opacity: 0.3 }}>{'$'.repeat(Math.max(0, 4 - level))}</span>
    </span>
  );
}

/* ---- Food emoji fallback ---- */
const CUISINE_EMOJIS = {
  italian: '🍕', pizza: '🍕', sushi: '🍣', japanese: '🍣',
  chinese: '🥡', mexican: '🌮', indian: '🍛', thai: '🍜',
  burger: '🍔', american: '🍔', french: '🥐', greek: '🥙',
  bbq: '🍖', seafood: '🦞', vegetarian: '🥗', vegan: '🌱',
  cafe: '☕', coffee: '☕', dessert: '🍰', bakery: '🥐',
};

function cuisineEmoji(cuisine = '', name = '') {
  const key = cuisine.toLowerCase();
  const nameKey = name.toLowerCase().split(' ')[0];
  return CUISINE_EMOJIS[key] || CUISINE_EMOJIS[nameKey] || '🍽️';
}

export default function MatchPage() {
  const { code }         = useParams();
  const navigate         = useNavigate();
  const location         = useLocation();

  const [restaurant, setRestaurant] = useState(location.state?.match || null);
  const [loading, setLoading]       = useState(!location.state?.match);
  const [confetti]                  = useState(() => generateConfettiPieces(70));
  const [shareFeedback, setShareFeedback] = useState('');

  /* ---- Fetch compromise if not passed via router state ---- */
  useEffect(() => {
    if (restaurant) return;
    getCompromise(code)
      .then((data) => setRestaurant(data?.restaurant || data))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [code, restaurant]);

  /* ---- Share ---- */
  const handleShare = async () => {
    const shareText = `🍽️ We matched on ${restaurant?.name}! Come eat with us 😍`;
    const shareUrl  = window.location.href;

    if (navigator.share) {
      try {
        await navigator.share({ title: "What Should We Eat?", text: shareText, url: shareUrl });
      } catch (err) {
        // User cancelled — fine
      }
    } else {
      // Fallback: copy to clipboard
      try {
        await navigator.clipboard.writeText(`${shareText}\n${shareUrl}`);
        setShareFeedback('📋 Copied to clipboard!');
        setTimeout(() => setShareFeedback(''), 2500);
      } catch (err) {
        setShareFeedback('Could not copy — try manually!');
      }
    }
  };

  /* ---- Google Maps link ---- */
  const mapsUrl = restaurant?.googleMapsUrl
    || (restaurant?.name
        ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(restaurant.name)}`
        : null);

  /* ---- Loading state ---- */
  if (loading) {
    return (
      <div className="match-page" style={{ justifyContent: 'center', alignItems: 'center' }}>
        <span style={{ fontSize: '3rem', marginBottom: 16 }}>🍽️</span>
        <span className="loading-spinner" style={{ borderTopColor: '#fff', width: 40, height: 40, borderWidth: 5 }} />
        <p style={{ marginTop: 16, color: 'rgba(255,255,255,0.8)', fontWeight: 700 }}>
          Finding your match…
        </p>
      </div>
    );
  }

  if (!restaurant) {
    return (
      <div className="match-page" style={{ justifyContent: 'center', alignItems: 'center', padding: 32 }}>
        <span style={{ fontSize: '3rem', marginBottom: 16 }}>😢</span>
        <h2 style={{ color: '#fff', textAlign: 'center', marginBottom: 12 }}>No match found</h2>
        <p style={{ color: 'rgba(255,255,255,0.75)', textAlign: 'center', marginBottom: 24, fontWeight: 600 }}>
          Looks like your group couldn't agree. Try again with more swipes!
        </p>
        <button className="btn btn-ghost" onClick={() => navigate('/')}>
          🏠 Back to Home
        </button>
      </div>
    );
  }

  const {
    name       = 'Mystery Restaurant',
    photoUrl   = null,
    rating     = null,
    priceLevel = null,
    vicinity   = '',
    cuisine    = '',
  } = restaurant;

  return (
    <div className="match-page">
      {/* Confetti */}
      <div className="confetti-container" aria-hidden="true">
        {confetti.map((p) => (
          <div
            key={p.id}
            className="confetti-piece"
            style={{
              left:              p.left,
              top:               '-20px',
              width:             p.width,
              height:            p.height,
              background:        p.color,
              borderRadius:      p.borderRadius,
              animationDuration: p.animationDuration,
              animationDelay:    p.animationDelay,
            }}
          />
        ))}
      </div>

      <div className="match-content">
        {/* Badge + heading */}
        <div className="match-badge">🎊 It's a Match!</div>
        <h1 className="match-title">
          Everyone loves<br />{name}! 🎉
        </h1>
        <p className="match-subtitle">
          Time to get those forks ready 🍴
        </p>

        {/* Restaurant card */}
        <div className="match-restaurant-card">
          <div className="match-restaurant-photo-wrap">
            {photoUrl ? (
              <img
                className="match-restaurant-photo"
                src={photoUrl}
                alt={name}
                onError={(e) => { e.target.style.display = 'none'; }}
              />
            ) : (
              <div className="match-restaurant-photo-placeholder">
                {cuisineEmoji(cuisine, name)}
              </div>
            )}
          </div>

          <div className="match-restaurant-info">
            <div className="match-restaurant-name">{name}</div>

            <div className="match-restaurant-meta">
              {rating     !== null && <StarRating rating={rating} />}
              {priceLevel !== null && <Price level={priceLevel} />}
              {cuisine    && <span className="match-cuisine-tag">{cuisine}</span>}
            </div>

            {vicinity && (
              <div className="match-vicinity">
                📍 {vicinity}
              </div>
            )}
          </div>
        </div>

        {/* Action buttons */}
        <div className="match-actions">
          {mapsUrl && (
            <a
              className="btn btn-primary btn-full btn-lg"
              href={mapsUrl}
              target="_blank"
              rel="noopener noreferrer"
            >
              🗺️ Open in Google Maps
            </a>
          )}

          <button
            className="btn btn-ghost btn-full"
            onClick={handleShare}
          >
            📤 Share with Friends
          </button>

          {shareFeedback && (
            <div
              style={{
                textAlign: 'center',
                color: 'rgba(255,255,255,0.85)',
                fontSize: '0.85rem',
                fontWeight: 700,
              }}
            >
              {shareFeedback}
            </div>
          )}

          <button
            className="btn btn-secondary btn-full"
            style={{ background: 'rgba(255,255,255,0.15)', color: '#fff', borderColor: 'rgba(255,255,255,0.5)' }}
            onClick={() => navigate('/')}
          >
            🔄 Play Again
          </button>
        </div>
      </div>
    </div>
  );
}
