import React from 'react';

// Star rendering helper
const Stars = ({ rating }) => {
  if (!rating) return null;
  const full  = Math.floor(rating);
  const half  = rating - full >= 0.5 ? 1 : 0;
  const empty = 5 - full - half;
  return (
    <span className="restaurant-card-rating">
      {'⭐'.repeat(full)}
      {half ? '✨' : ''}
      <span style={{ opacity: 0.4 }}>{'⭐'.repeat(empty)}</span>
      <span style={{ marginLeft: 4, fontSize: '0.8rem' }}>{rating.toFixed(1)}</span>
    </span>
  );
};

// Price-level helper: 1→$, 2→$$, 3→$$$, 4→$$$$
const PriceLevel = ({ level }) => {
  if (level === undefined || level === null) return null;
  const signs = '$'.repeat(level) + <span style={{ opacity: 0.3 }}>{'$'.repeat(4 - level)}</span>;
  // build it properly:
  return (
    <span className="restaurant-card-price">
      {'$'.repeat(level)}
      <span style={{ opacity: 0.3 }}>{'$'.repeat(Math.max(0, 4 - level))}</span>
    </span>
  );
};

/**
 * RestaurantCard
 *
 * Props:
 *   restaurant: {
 *     name:       string
 *     photoUrl:   string | null
 *     rating:     number
 *     priceLevel: number (1-4)
 *     vicinity:   string
 *     cuisine:    string
 *   }
 *   style:   object  – forwarded to the root element (used by react-tinder-card)
 *   onRef:   ref     – forwarded ref for programmatic swipe
 */
const RestaurantCard = React.forwardRef(function RestaurantCard(
  { restaurant, style, className = '', ...rest },
  ref
) {
  if (!restaurant) return null;

  const {
    name       = 'Mystery Restaurant',
    photoUrl   = null,
    rating     = null,
    priceLevel = null,
    vicinity   = '',
    cuisine    = '',
  } = restaurant;

  // Food emoji fallback based on cuisine type
  const cuisineEmojis = {
    italian: '🍕', pizza: '🍕', sushi: '🍣', japanese: '🍣',
    chinese: '🥡', mexican: '🌮', indian: '🍛', thai: '🍜',
    burger: '🍔', american: '🍔', french: '🥐', greek: '🥙',
    bbq: '🍖', seafood: '🦞', vegetarian: '🥗', vegan: '🌱',
    cafe: '☕', coffee: '☕', dessert: '🍰', bakery: '🥐',
  };
  const fallbackEmoji =
    cuisineEmojis[cuisine?.toLowerCase()] ||
    cuisineEmojis[name?.toLowerCase()?.split(' ')[0]] ||
    '🍽️';

  return (
    <div
      ref={ref}
      className={`restaurant-card ${className}`}
      style={style}
      {...rest}
    >
      {/* Photo or colourful placeholder */}
      {photoUrl ? (
        <img
          className="restaurant-card-photo"
          src={photoUrl}
          alt={name}
          draggable={false}
          onError={(e) => { e.target.style.display = 'none'; }}
        />
      ) : (
        <div className="restaurant-card-photo-placeholder">
          {fallbackEmoji}
        </div>
      )}

      {/* Gradient overlay */}
      <div className="restaurant-card-gradient" />

      {/* Info panel */}
      <div className="restaurant-card-info">
        {cuisine && (
          <span className="cuisine-tag">{cuisine}</span>
        )}

        <div className="restaurant-card-name">{name}</div>

        <div className="restaurant-card-meta">
          {rating   !== null && <Stars rating={rating} />}
          {rating   !== null && priceLevel !== null && (
            <span className="restaurant-card-dot" />
          )}
          {priceLevel !== null && <PriceLevel level={priceLevel} />}
        </div>

        {vicinity && (
          <div className="restaurant-card-vicinity">
            📍 {vicinity}
          </div>
        )}
      </div>
    </div>
  );
});

export default RestaurantCard;
