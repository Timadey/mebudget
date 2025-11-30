import React, { useState, useEffect } from 'react';
import { Lock, Save, Loader2, Shield, Eye, EyeOff, Calendar, AlertTriangle } from 'lucide-react';
import { settingsService } from '../services/settings';
import { useAuth } from '../context/AuthContext';
import { supabase } from '../lib/supabase';
import clsx from 'clsx';

export default function Settings() {
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [pinEnabled, setPinEnabled] = useState(false);
    const [budgetDuration, setBudgetDuration] = useState('monthly');
    const [showPinForm, setShowPinForm] = useState(false);
    const [showPin, setShowPin] = useState(false);

    const [pinForm, setPinForm] = useState({
        currentPin: '',
        newPin: '',
        confirmPin: ''
    });
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    useEffect(() => {
        loadSettings();
    }, []);

    const loadSettings = async () => {
        setLoading(true);
        try {
            const enabled = await settingsService.isPinEnabled();
            const duration = await settingsService.getBudgetDuration();
            setPinEnabled(enabled);
            setBudgetDuration(duration);
        } catch (error) {
            console.error('Failed to load settings', error);
        } finally {
            setLoading(false);
        }
    };

    const handleBudgetDurationChange = async (duration) => {
        setSaving(true);
        try {
            await settingsService.setBudgetDuration(duration);
            setBudgetDuration(duration);
            setSuccess('Budget duration updated');
            setTimeout(() => setSuccess(''), 3000);
        } catch (error) {
            setError('Failed to update budget duration');
        } finally {
            setSaving(false);
        }
    };

    const handleTogglePin = async (enabled) => {
        if (enabled && !await settingsService.getPinHash()) {
            setShowPinForm(true);
            return;
        }

        setSaving(true);
        try {
            await settingsService.setPinEnabled(enabled);
            setPinEnabled(enabled);
            setSuccess(enabled ? 'PIN protection enabled' : 'PIN protection disabled');
            setTimeout(() => setSuccess(''), 3000);
        } catch (error) {
            setError('Failed to update PIN settings');
        } finally {
            setSaving(false);
        }
    };

    const handleSavePin = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');

        if (pinForm.newPin.length !== 4 || pinForm.confirmPin.length !== 4) {
            setError('PIN must be 4 digits');
            return;
        }

        if (pinForm.newPin !== pinForm.confirmPin) {
            setError('PINs do not match');
            return;
        }

        const existingHash = await settingsService.getPinHash();
        if (existingHash && pinForm.currentPin) {
            const isValid = await settingsService.verifyPin(pinForm.currentPin);
            if (!isValid) {
                setError('Current PIN is incorrect');
                return;
            }
        }

        setSaving(true);
        try {
            await settingsService.setPinHash(pinForm.newPin);
            await settingsService.setPinEnabled(true);
            setPinEnabled(true);
            setShowPinForm(false);
            setPinForm({ currentPin: '', newPin: '', confirmPin: '' });
            setSuccess('PIN updated successfully');
            setTimeout(() => setSuccess(''), 3000);
        } catch (error) {
            setError('Failed to save PIN');
        } finally {
            setSaving(false);
        }
    };

    const handlePinInput = (field, value) => {
        const numericValue = value.replace(/[^0-9]/g, '');
        if (numericValue.length <= 4) {
            setPinForm({ ...pinForm, [field]: numericValue });
        }
    };

    if (loading) {
        return (
            <div className="flex justify-center items-center h-64">
                <Loader2 className="animate-spin text-primary" size={32} />
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <header>
                <h2 className="text-3xl font-bold text-white">Settings</h2>
                <p className="text-slate-400">Manage your app preferences and security</p>
            </header>

            {/* Success/Error Messages */}
            {success && (
                <div className="bg-emerald-500/10 border border-emerald-500/20 rounded-lg p-4 text-emerald-400">
                    {success}
                </div>
            )}
            {error && (
                <div className="bg-rose-500/10 border border-rose-500/20 rounded-lg p-4 text-rose-400">
                    {error}
                </div>
            )}

            {/* Budget Duration Section */}
            <div className="glass-card">
                <div className="flex items-center gap-3 mb-6">
                    <div className="p-2 bg-blue-500/20 text-blue-400 rounded-lg">
                        <Calendar size={24} />
                    </div>
                    <div>
                        <h3 className="text-xl font-bold text-white">Budget Duration</h3>
                        <p className="text-slate-400 text-sm">Set how often your budget resets</p>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                    {['weekly', 'monthly', 'yearly'].map((duration) => (
                        <button
                            key={duration}
                            onClick={() => handleBudgetDurationChange(duration)}
                            disabled={saving}
                            className={clsx(
                                "p-4 rounded-lg border-2 transition-all font-medium capitalize",
                                budgetDuration === duration
                                    ? "border-primary bg-primary/10 text-primary"
                                    : "border-white/10 text-slate-400 hover:border-white/20 hover:bg-white/5"
                            )}
                        >
                            {duration}
                        </button>
                    ))}
                </div>
                <p className="text-xs text-slate-500 mt-3">
                    Your budget will reset at the start of each {budgetDuration === 'weekly' ? 'week (Sunday)' : budgetDuration === 'yearly' ? 'year (January 1st)' : 'month'}
                </p>
            </div>

            {/* PIN Protection Section */}
            <div className="glass-card">
                <div className="flex items-center gap-3 mb-6">
                    <div className="p-2 bg-primary/20 text-primary rounded-lg">
                        <Shield size={24} />
                    </div>
                    <div>
                        <h3 className="text-xl font-bold text-white">PIN Protection</h3>
                        <p className="text-slate-400 text-sm">Secure your app with a 4-digit PIN</p>
                    </div>
                </div>

                <div className="space-y-4">
                    <div className="flex items-center justify-between p-4 bg-white/5 rounded-lg">
                        <div>
                            <p className="text-white font-medium">Enable PIN Protection</p>
                            <p className="text-slate-400 text-sm">Require PIN every 2 hours</p>
                        </div>
                        <button
                            onClick={() => handleTogglePin(!pinEnabled)}
                            disabled={saving}
                            className={clsx(
                                "relative w-14 h-7 rounded-full transition-colors",
                                pinEnabled ? "bg-primary" : "bg-slate-700"
                            )}
                        >
                            <div className={clsx(
                                "absolute top-1 w-5 h-5 rounded-full bg-white transition-transform",
                                pinEnabled ? "translate-x-8" : "translate-x-1"
                            )} />
                        </button>
                    </div>

                    {pinEnabled && (
                        <button
                            onClick={() => setShowPinForm(!showPinForm)}
                            className="w-full bg-slate-700 hover:bg-slate-600 text-white font-medium py-3 rounded-lg transition-colors flex items-center justify-center gap-2"
                        >
                            <Lock size={20} />
                            {showPinForm ? 'Cancel' : 'Change PIN'}
                        </button>
                    )}

                    {showPinForm && (
                        <form onSubmit={handleSavePin} className="space-y-4 p-4 bg-white/5 rounded-lg">
                            {pinEnabled && (
                                <div>
                                    <label className="block text-sm font-medium text-slate-400 mb-1">
                                        Current PIN
                                    </label>
                                    <input
                                        type={showPin ? "text" : "password"}
                                        inputMode="numeric"
                                        maxLength={4}
                                        value={pinForm.currentPin}
                                        onChange={(e) => handlePinInput('currentPin', e.target.value)}
                                        className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-2 px-4 text-white focus:outline-none focus:ring-2 focus:ring-primary"
                                        placeholder="••••"
                                    />
                                </div>
                            )}

                            <div>
                                <label className="block text-sm font-medium text-slate-400 mb-1">
                                    New PIN
                                </label>
                                <input
                                    type={showPin ? "text" : "password"}
                                    inputMode="numeric"
                                    maxLength={4}
                                    value={pinForm.newPin}
                                    onChange={(e) => handlePinInput('newPin', e.target.value)}
                                    className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-2 px-4 text-white focus:outline-none focus:ring-2 focus:ring-primary"
                                    placeholder="••••"
                                    required
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-400 mb-1">
                                    Confirm PIN
                                </label>
                                <input
                                    type={showPin ? "text" : "password"}
                                    inputMode="numeric"
                                    maxLength={4}
                                    value={pinForm.confirmPin}
                                    onChange={(e) => handlePinInput('confirmPin', e.target.value)}
                                    className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-2 px-4 text-white focus:outline-none focus:ring-2 focus:ring-primary"
                                    placeholder="••••"
                                    required
                                />
                            </div>

                            <button
                                type="button"
                                onClick={() => setShowPin(!showPin)}
                                className="text-sm text-slate-400 hover:text-white flex items-center gap-2"
                            >
                                {showPin ? <EyeOff size={16} /> : <Eye size={16} />}
                                {showPin ? 'Hide' : 'Show'} PIN
                            </button>

                            <button
                                type="submit"
                                disabled={saving}
                                className="w-full bg-gradient-to-r from-primary to-secondary hover:opacity-90 text-white font-bold py-3 rounded-xl transition-all flex items-center justify-center gap-2"
                            >
                                {saving ? <Loader2 className="animate-spin" size={20} /> : <Save size={20} />}
                                Save PIN
                            </button>
                        </form>
                    )}
                </div>
            </div>

            {/* Danger Zone */}
            <div className="glass-card border-rose-500/20">
                <div className="flex items-center gap-3 mb-6">
                    <div className="p-2 bg-rose-500/20 text-rose-400 rounded-lg">
                        <AlertTriangle size={24} />
                    </div>
                    <div>
                        <h3 className="text-xl font-bold text-white">Danger Zone</h3>
                        <p className="text-slate-400 text-sm">Irreversible actions</p>
                    </div>
                </div>

                <DeleteAccountSection />
            </div>
        </div>
    );
}

function DeleteAccountSection() {
    const [isOpen, setIsOpen] = useState(false);
    const [confirmText, setConfirmText] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const { deleteAccount, signIn, user } = useAuth();

    const handleDelete = async (e) => {
        e.preventDefault();
        if (confirmText !== 'delete my account') return;

        setLoading(true);
        setError('');

        try {
            // Re-authenticate first
            const { error: authError } = await signIn({ email: user.email, password });
            if (authError) throw new Error('Incorrect password');

            // Delete account
            const { error: deleteError } = await deleteAccount();
            if (deleteError) throw deleteError;

            // Clean session and redirect to registration
            await supabase.auth.signOut();
            window.location.href = '/register';
        } catch (error) {
            setError(error.message);
            setLoading(false);
        }
    };

    if (!isOpen) {
        return (
            <button
                onClick={() => setIsOpen(true)}
                className="w-full bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 font-medium py-3 rounded-lg transition-colors flex items-center justify-center gap-2 border border-rose-500/20"
            >
                Delete Account
            </button>
        );
    }

    return (
        <div className="bg-rose-500/5 rounded-lg p-4 border border-rose-500/20">
            <h4 className="text-lg font-bold text-white mb-2">Delete Account</h4>
            <p className="text-slate-300 text-sm mb-4">
                This action is <span className="font-bold text-rose-400">irreversible</span>.
                All your data (transactions, categories, investments, settings) will be permanently deleted.
            </p>

            <form onSubmit={handleDelete} className="space-y-4">
                <div>
                    <label className="block text-sm font-medium text-slate-400 mb-1">
                        Type <span className="font-mono text-white">delete my account</span> to confirm
                    </label>
                    <input
                        type="text"
                        value={confirmText}
                        onChange={(e) => setConfirmText(e.target.value)}
                        className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-2 px-4 text-white focus:outline-none focus:ring-2 focus:ring-rose-500"
                        placeholder="delete my account"
                        required
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-slate-400 mb-1">
                        Confirm Password
                    </label>
                    <input
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-2 px-4 text-white focus:outline-none focus:ring-2 focus:ring-rose-500"
                        placeholder="••••••••"
                        required
                    />
                </div>

                {error && (
                    <div className="text-rose-400 text-sm">{error}</div>
                )}

                <div className="flex gap-3">
                    <button
                        type="button"
                        onClick={() => setIsOpen(false)}
                        className="flex-1 bg-slate-700 hover:bg-slate-600 text-white font-medium py-2 rounded-lg transition-colors"
                    >
                        Cancel
                    </button>
                    <button
                        type="submit"
                        disabled={loading || confirmText !== 'delete my account'}
                        className="flex-1 bg-rose-500 hover:bg-rose-600 text-white font-medium py-2 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                    >
                        {loading ? <Loader2 className="animate-spin" size={18} /> : 'Delete Forever'}
                    </button>
                </div>
            </form>
        </div>
    );
}
