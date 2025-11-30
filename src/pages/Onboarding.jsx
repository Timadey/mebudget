import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle2, ChevronRight, ChevronLeft, SkipForward, Wallet, TrendingUp, TrendingDown, Shield } from 'lucide-react';
import { settingsService } from '../services/settings';
import CategoryManager from '../components/CategoryManager';
import ExpenseForm from '../components/ExpenseForm';
import { api } from '../services/api';
import clsx from 'clsx';
import { useAuth } from '../context/AuthContext';

const STEPS = [
    {
        id: 'categories',
        title: 'Setup Categories',
        description: 'Create categories to organize your spending.',
        icon: <Wallet size={24} />
    },
    {
        id: 'income',
        title: 'Log Income',
        description: 'Add your first income source (e.g. Salary).',
        icon: <TrendingUp size={24} />
    },
    {
        id: 'expense',
        title: 'Log Expense',
        description: 'Track your first expense.',
        icon: <TrendingDown size={24} />
    },
    {
        id: 'pin',
        title: 'Secure App',
        description: 'Set a PIN to protect your data.',
        icon: <Shield size={24} />
    }
];

export default function Onboarding({ onComplete }) {
    const [currentStep, setCurrentStep] = useState(0);
    const [loading, setLoading] = useState(false);
    const [categories, setCategories] = useState([]);
    const navigate = useNavigate();
    const { user } = useAuth();

    useEffect(() => {
        loadCategories();
    }, []);

    const loadCategories = async () => {
        try {
            const data = await api.getData();
            setCategories(data.categories);
        } catch (error) {
            console.error('Failed to load categories', error);
        }
    };

    const handleNext = async () => {
        if (currentStep < STEPS.length - 1) {
            setCurrentStep(prev => prev + 1);
        } else {
            await completeOnboarding();
        }
    };

    const handleSkip = async () => {
        handleNext();
    };

    const handleBack = () => {
        if (currentStep > 0) {
            setCurrentStep(prev => prev - 1);
        }
    };

    const completeOnboarding = async () => {
        setLoading(true);
        try {
            await settingsService.setOnboardingCompleted(true);
            if (onComplete) onComplete();
            navigate('/expenses');
        } catch (error) {
            console.error('Failed to complete onboarding', error);
        } finally {
            setLoading(false);
        }
    };

    const renderStepContent = () => {
        switch (currentStep) {
            case 0: // Categories
                return (
                    <div className="space-y-4">
                        <div className="bg-primary/10 p-4 rounded-xl border border-primary/20 mb-6">
                            <h3 className="font-bold text-primary mb-2">Why Categories?</h3>
                            <p className="text-sm text-slate-300">
                                Budget Categories help you see exactly where your money goes. We've added some defaults with budget amounts, but feel free to add or edit your own budgets!
                            </p>
                        </div>
                        <CategoryManager categories={categories} onUpdate={loadCategories} />
                    </div>
                );
            case 1: // Income
                return (
                    <div className="space-y-4">
                        <div className="bg-emerald-500/10 p-4 rounded-xl border border-emerald-500/20 mb-6">
                            <h3 className="font-bold text-emerald-400 mb-2">Track Your Earnings</h3>
                            <p className="text-sm text-slate-300">
                                Start by logging your main source of income. This sets your budget baseline.
                            </p>
                        </div>
                        {/* We force type='Income' by passing a prop or modifying ExpenseForm. 
                            Since ExpenseForm controls its own state, we might need to guide the user or wrap it.
                            For now, we'll just render it and let the user use it, but we can add a hint.
                        */}
                        <div className="pointer-events-none opacity-50 absolute inset-0 z-10 hidden"></div>
                        {/* Actually ExpenseForm has tabs. We can just ask user to select Income. 
                            Or better, we can modify ExpenseForm to accept an initialType prop.
                            Let's assume we will modify ExpenseForm to accept `initialType` and `hideTypeToggle` if needed.
                            For now, let's just render it.
                        */}
                        <ExpenseForm onExpenseAdded={handleNext} />
                        <p className="text-xs text-center text-slate-500 mt-2">
                            Select "Income" above to log earnings.
                        </p>
                    </div>
                );
            case 2: // Expense
                return (
                    <div className="space-y-4">
                        <div className="bg-rose-500/10 p-4 rounded-xl border border-rose-500/20 mb-6">
                            <h3 className="font-bold text-rose-400 mb-2">Track Your Spending</h3>
                            <p className="text-sm text-slate-300">
                                Log your first expense. It could be a coffee, rent, or groceries.
                            </p>
                        </div>
                        <ExpenseForm onExpenseAdded={handleNext} />
                    </div>
                );
            case 3: // PIN
                return (
                    <div className="space-y-4">
                        <div className="bg-blue-500/10 p-4 rounded-xl border border-blue-500/20 mb-6">
                            <h3 className="font-bold text-blue-400 mb-2">Stay Secure</h3>
                            <p className="text-sm text-slate-300">
                                Set a 4-digit PIN to keep your financial data private.
                            </p>
                        </div>
                        {/* We need to expose the PIN form from Settings or create a reusable one.
                            Since Settings has it embedded, we might need to extract it or just copy the logic here for simplicity.
                            Let's create a simple PIN form here.
                        */}
                        <PinSetup onComplete={handleNext} />
                    </div>
                );
            default:
                return null;
        }
    };

    return (
        <div className="min-h-screen bg-dark-900 flex flex-col">
            {/* Progress Header */}
            <div className="p-6 border-b border-white/5">
                <div className="max-w-2xl mx-auto">
                    <div className="flex items-center justify-between mb-4">
                        <h1 className="text-2xl font-bold text-white">Welcome to MeBudget</h1>
                        <span className="text-sm text-slate-400">Step {currentStep + 1} of {STEPS.length}</span>
                    </div>

                    {/* Stepper */}
                    <div className="flex items-center gap-2">
                        {STEPS.map((step, index) => (
                            <div key={step.id} className="flex-1 h-2 rounded-full bg-slate-800 overflow-hidden">
                                <div
                                    className={clsx(
                                        "h-full transition-all duration-500 ease-out",
                                        index <= currentStep ? "bg-primary" : "bg-transparent"
                                    )}
                                />
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {/* Main Content */}
            <div className="flex-1 overflow-y-auto p-6">
                <div className="max-w-2xl mx-auto">
                    <div className="flex items-center gap-4 mb-8">
                        <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary to-secondary flex items-center justify-center text-white shadow-lg shadow-primary/20">
                            {STEPS[currentStep].icon}
                        </div>
                        <div>
                            <h2 className="text-xl font-bold text-white">{STEPS[currentStep].title}</h2>
                            <p className="text-slate-400">{STEPS[currentStep].description}</p>
                        </div>
                    </div>

                    <div className="glass-card p-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
                        {renderStepContent()}
                    </div>
                </div>
            </div>

            {/* Footer Navigation */}
            <div className="p-6 border-t border-white/5 bg-dark-900/95 backdrop-blur-sm sticky bottom-0 z-10">
                <div className="max-w-2xl mx-auto flex items-center justify-between">
                    <button
                        onClick={handleBack}
                        disabled={currentStep === 0}
                        className={clsx(
                            "flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors",
                            currentStep === 0 ? "text-slate-600 cursor-not-allowed" : "text-slate-400 hover:text-white"
                        )}
                    >
                        <ChevronLeft size={20} /> Back
                    </button>

                    <div className="flex items-center gap-3">
                        <button
                            onClick={handleSkip}
                            className="text-slate-500 hover:text-slate-300 font-medium px-4 py-2 transition-colors"
                        >
                            Skip
                        </button>
                        <button
                            onClick={handleNext}
                            className="bg-primary hover:bg-primary-dark text-white px-6 py-2 rounded-lg font-bold flex items-center gap-2 shadow-lg shadow-primary/20 transition-all hover:scale-105 active:scale-95"
                        >
                            {currentStep === STEPS.length - 1 ? 'Get Started' : 'Next'}
                            <ChevronRight size={20} />
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

function PinSetup({ onComplete }) {
    const [pin, setPin] = useState('');
    const [confirmPin, setConfirmPin] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleSave = async (e) => {
        e.preventDefault();
        if (pin.length !== 4) return setError('PIN must be 4 digits');
        if (pin !== confirmPin) return setError('PINs do not match');

        setLoading(true);
        try {
            await settingsService.setPinHash(pin);
            await settingsService.setPinEnabled(true);
            onComplete();
        } catch (err) {
            setError('Failed to save PIN');
        } finally {
            setLoading(false);
        }
    };

    return (
        <form onSubmit={handleSave} className="space-y-4">
            <div>
                <label className="block text-sm font-medium text-slate-400 mb-1">Create PIN</label>
                <input
                    type="password"
                    inputMode="numeric"
                    maxLength={4}
                    value={pin}
                    onChange={e => setPin(e.target.value.replace(/[^0-9]/g, ''))}
                    className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-2 px-4 text-white focus:outline-none focus:ring-2 focus:ring-primary"
                    placeholder="••••"
                    required
                />
            </div>
            <div>
                <label className="block text-sm font-medium text-slate-400 mb-1">Confirm PIN</label>
                <input
                    type="password"
                    inputMode="numeric"
                    maxLength={4}
                    value={confirmPin}
                    onChange={e => setConfirmPin(e.target.value.replace(/[^0-9]/g, ''))}
                    className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-2 px-4 text-white focus:outline-none focus:ring-2 focus:ring-primary"
                    placeholder="••••"
                    required
                />
            </div>
            {error && <p className="text-rose-400 text-sm">{error}</p>}
            <button
                type="submit"
                disabled={loading}
                className="w-full bg-blue-500 hover:bg-blue-600 text-white font-bold py-3 rounded-xl transition-all"
            >
                {loading ? 'Saving...' : 'Set PIN'}
            </button>
        </form>
    );
}
