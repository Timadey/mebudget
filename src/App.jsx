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
import Landing from './pages/Landing';
import Onboarding from './pages/Onboarding';
import PrivacyPolicy from './pages/PrivacyPolicy';
import TermsOfUse from './pages/TermsOfUse';
import InstallPrompt from './components/InstallPrompt';

function AuthenticatedApp() {
  const { user, loading } = useAuth();
  const [isLocked, setIsLocked] = useState(true);
  const [isVerifying, setIsVerifying] = useState(false);
  const [isCheckingPin, setIsCheckingPin] = useState(true);
  const [isOnboardingCompleted, setIsOnboardingCompleted] = useState(null);

  useEffect(() => {
    if (loading) return;

    if (user) {
      checkPinRequirement();
      checkOnboardingStatus();
      // Check PIN requirement every minute
      const interval = setInterval(checkPinRequirement, 60000);
      return () => clearInterval(interval);
    } else {
      setIsCheckingPin(false);
      setIsOnboardingCompleted(true); // Not relevant for logged out users
    }
  }, [user, loading]);

  const checkOnboardingStatus = async () => {
    try {
      const completed = await settingsService.getOnboardingCompleted();
      setIsOnboardingCompleted(completed);
    } catch (error) {
      console.error('Error checking onboarding status:', error);
      setIsOnboardingCompleted(false); // Default to false on error to be safe, or true to avoid blocking? False is safer for onboarding.
    }
  };

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

  if (loading || (user && (isCheckingPin || isOnboardingCompleted === null))) {
    return (
      <div className="min-h-screen bg-dark-900 flex items-center justify-center">
        <div className="text-white text-xl">Loading...</div>
      </div>
    );
  }



  // ... (imports)

  // ... (AuthenticatedApp component)

  if (!user) {
    return (
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/privacy" element={<PrivacyPolicy />} />
        <Route path="/terms" element={<TermsOfUse />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    );
  }

  // If user is logged in but hasn't completed onboarding
  if (!isOnboardingCompleted) {
    return (
      <Routes>
        <Route path="/onboarding" element={<Onboarding onComplete={() => setIsOnboardingCompleted(true)} />} />
        <Route path="*" element={<Navigate to="/onboarding" replace />} />
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
        <InstallPrompt />
        <AuthenticatedApp />
      </AuthProvider>
    </Router>
  );
}

export default App;

