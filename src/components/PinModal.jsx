import React, { useState } from 'react';
import { Lock, Loader2 } from 'lucide-react';
import clsx from 'clsx';

export default function PinModal({ onVerify, isLoading }) {
    const [pin, setPin] = useState('');
    const [error, setError] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (pin.length !== 4) {
            setError('PIN must be 4 digits');
            return;
        }

        const success = await onVerify(pin);
        if (!success) {
            setError('Incorrect PIN');
            setPin('');
        }
    };

    const handlePinChange = (value) => {
        // Only allow numbers
        const numericValue = value.replace(/[^0-9]/g, '');
        if (numericValue.length <= 4) {
            setPin(numericValue);
            setError('');
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm">
            <div className="glass-card w-full max-w-md mx-4 p-8">
                <div className="flex flex-col items-center mb-6">
                    <div className="w-16 h-16 rounded-full bg-primary/20 flex items-center justify-center mb-4">
                        <Lock size={32} className="text-primary" />
                    </div>
                    <h2 className="text-2xl font-bold text-white mb-2">Enter PIN</h2>
                    <p className="text-slate-400 text-center">Enter your 4-digit PIN to access the app</p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <input
                            type="password"
                            inputMode="numeric"
                            maxLength={4}
                            value={pin}
                            onChange={(e) => handlePinChange(e.target.value)}
                            className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-4 px-4 text-white text-center text-2xl tracking-widest focus:outline-none focus:ring-2 focus:ring-primary"
                            placeholder="••••"
                            autoFocus
                            disabled={isLoading}
                        />
                        {error && (
                            <p className="text-rose-400 text-sm mt-2 text-center">{error}</p>
                        )}
                    </div>

                    {/* PIN dots indicator */}
                    <div className="flex justify-center gap-3 py-2">
                        {[0, 1, 2, 3].map((i) => (
                            <div
                                key={i}
                                className={clsx(
                                    "w-3 h-3 rounded-full transition-all",
                                    pin.length > i ? "bg-primary scale-110" : "bg-slate-700"
                                )}
                            />
                        ))}
                    </div>

                    <button
                        type="submit"
                        disabled={pin.length !== 4 || isLoading}
                        className="w-full bg-gradient-to-r from-primary to-secondary hover:opacity-90 text-white font-bold py-3 rounded-xl transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {isLoading ? <Loader2 className="animate-spin" size={20} /> : <Lock size={20} />}
                        Unlock
                    </button>
                </form>
            </div>
        </div>
    );
}
