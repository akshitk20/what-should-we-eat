const API_BASE = '/api/rooms';

/**
 * Creates a new room.
 * @param {{ nickname: string, latitude: number, longitude: number }} data
 * @returns {Promise<{ roomCode: string, memberId: string }>}
 */
export const createRoom = async (data) => {
  const res = await window.fetch(API_BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(`Failed to create room: ${res.status}`);
  return res.json();
};

/**
 * Fetches room details by room code.
 * @param {string} code
 * @returns {Promise<{ roomCode: string, members: Array, status: string }>}
 */
export const getRoom = async (code) => {
  const res = await window.fetch(`${API_BASE}/${code}`);
  if (!res.ok) throw new Error(`Room not found: ${res.status}`);
  return res.json();
};

/**
 * Joins an existing room.
 * @param {string} code
 * @param {{ nickname: string }} data
 * @returns {Promise<{ memberId: string }>}
 */
export const joinRoom = async (code, data) => {
  const res = await window.fetch(`${API_BASE}/${code}/join`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(`Failed to join room: ${res.status}`);
  return res.json();
};

/**
 * Starts the swiping phase (host only).
 * @param {string} code
 * @param {string} memberId
 * @returns {Promise<{ restaurants: Array }>}
 */
export const startSwiping = async (code, memberId) => {
  const res = await window.fetch(`${API_BASE}/${code}/start?memberId=${memberId}`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error(`Failed to start swiping: ${res.status}`);
  return res.json();
};

/**
 * Finds the best compromise restaurant for the room.
 * @param {string} code
 * @returns {Promise<{ restaurant: Object }>}
 */
export const getCompromise = async (code) => {
  const res = await window.fetch(`${API_BASE}/${code}/compromise`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error(`Failed to get compromise: ${res.status}`);
  return res.json();
};
