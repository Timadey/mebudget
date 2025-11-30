import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import PinModal from './components/PinModal';
import { settingsService } from './services/settings';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';

import Dashboard from './pages/Dashboard';
import Expenses from './pages/Expenses';
import Investments from './pages/Investments';
import Settings from './pages/Settings';

function AuthenticatedApp() {
  const { user, loading } = useAuth();
  const [isLocked, setIsLocked] = useState(true);
  const [isVerifying, setIsVerifying] = useState(false);
  const [isCheckingPin, setIsCheckingPin] = useState(true);

  useEffect(() => {
    if (loading) return;

    if (user) {
      checkPinRequirement();
      // Check PIN requirement every minute
      const interval = setInterval(checkPinRequirement, 60000);
      return () => clearInterval(interval);
    } else {
      setIsCheckingPin(false);
    }
  }, [user, loading]);

  const checkPinRequirement = async () => {
    try {
      const needsVerification = await settingsService.needsVerification();
      setIsLocked(needsVerification);
    } catch (error) {
      console.error('Error checking PIN requirement:', error);
      setIsLocked(false);
    } finally {
      setIsCheckingPin(false);
    }
  };

  const handleVerifyPin = async (pin) => {
    setIsVerifying(true);
    try {
      const isValid = await settingsService.verifyPin(pin);
      if (isValid) {
        await settingsService.updateLastVerified();
        setIsLocked(false);
        return true;
      }
      return false;
    } catch (error) {
      console.error('Error verifying PIN:', error);
      return false;
    } finally {
      setIsVerifying(false);
    }
  };

  if (loading || (user && isCheckingPin)) {
    return (
      <div className="min-h-screen bg-dark-900 flex items-center justify-center">
        <div className="text-white text-xl">Loading...</div>
      </div>
    );
  }

  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  return (
    <>
      {isLocked && <PinModal onVerify={handleVerifyPin} isLoading={isVerifying} />}
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/expenses" element={<Expenses />} />
          <Route path="/investments" element={<Investments />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Layout>
    </>
  );
}

function App() {
  return (
    <Router>
      <AuthProvider>
        <AuthenticatedApp />
      </AuthProvider>
    </Router>
  );
}

export default App;

