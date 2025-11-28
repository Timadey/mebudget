import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import PinModal from './components/PinModal';
import { settingsService } from './services/settings';

import Dashboard from './pages/Dashboard';
import Expenses from './pages/Expenses';
import Investments from './pages/Investments';
import Settings from './pages/Settings';

function App() {
  const [isLocked, setIsLocked] = useState(true);
  const [isVerifying, setIsVerifying] = useState(false);
  const [isCheckingPin, setIsCheckingPin] = useState(true);

  useEffect(() => {
    checkPinRequirement();

    // Check PIN requirement every minute
    const interval = setInterval(checkPinRequirement, 60000);
    return () => clearInterval(interval);
  }, []);

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

  if (isCheckingPin) {
    return (
      <div className="min-h-screen bg-dark-900 flex items-center justify-center">
        <div className="text-white text-xl">Loading...</div>
      </div>
    );
  }

  return (
    <Router>
      {isLocked && <PinModal onVerify={handleVerifyPin} isLoading={isVerifying} />}
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/expenses" element={<Expenses />} />
          <Route path="/investments" element={<Investments />} />
          <Route path="/settings" element={<Settings />} />
        </Routes>
      </Layout>
    </Router>
  );
}

export default App;

