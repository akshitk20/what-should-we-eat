import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import HomePage from './pages/HomePage';
import LobbyPage from './pages/LobbyPage';
import SwipePage from './pages/SwipePage';
import MatchPage from './pages/MatchPage';

function App() {
  return (
    <div className="app-shell">
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/room/:code" element={<LobbyPage />} />
        <Route path="/room/:code/swipe" element={<SwipePage />} />
        <Route path="/room/:code/match" element={<MatchPage />} />
        {/* Catch-all redirect to home */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
}

export default App;
