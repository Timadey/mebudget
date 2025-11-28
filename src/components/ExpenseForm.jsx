import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { Plus, Loader2, ArrowDownCircle, ArrowUpCircle } from 'lucide-react';
import clsx from 'clsx';

const PAYMENT_APPS = [
    {
        id: 'gtworld',
        name: 'GT World',
        color: 'bg-orange-500',
        androidScheme: 'gtworld://',
        iosScheme: 'gtworld://',
        fallbackUrl: 'https://play.google.com/store/apps/details?id=com.gtbank.gtworldv1'
    },
    {
        id: 'uba',
        name: 'UBA Mobile',
        color: 'bg-red-600',
        androidScheme: 'ubamobile://',
        iosScheme: 'ubamobile://',
        fallbackUrl: 'https://play.google.com/store/apps/details?id=com.uba.vericash'
    },
    {
        id: 'palmpay',
        name: 'Palmpay',
        color: 'bg-purple-600',
        androidScheme: 'palmpay://',
        iosScheme: 'palmpay://',
        fallbackUrl: 'https://play.google.com/store/apps/details?id=com.transsnet.palmpay'
    },
    {
        id: 'kuda',
        name: 'Kuda',
        color: 'bg-green-600',
        androidScheme: 'kuda://',
        iosScheme: 'kuda://',
        fallbackUrl: 'https://play.google.com/store/apps/details?id=com.kudabank.app'
    },
    {
        id: 'parkway',
        name: 'Parkway Wallet',
        color: 'bg-blue-600',
        androidScheme: 'parkway://',
        iosScheme: 'parkway://',
        fallbackUrl: 'https://play.google.com/store/apps/details?id=com.parkway.yurwallet'
    }
];

export default function ExpenseForm({ onExpenseAdded }) {
    const [allCategories, setAllCategories] = useState([]);
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [type, setType] = useState('Expense');
    const [selectedPaymentApp, setSelectedPaymentApp] = useState(null);

    const [formData, setFormData] = useState({
        amount: '',
        payee: '',
        category: '',
        description: ''
    });

    useEffect(() => {
        loadCategories();
    }, []);

    useEffect(() => {
        const availableCategories = allCategories.filter(c => c.type === type);
        if (availableCategories.length > 0) {
            setFormData(prev => ({ ...prev, category: availableCategories[0].name }));
        } else {
            setFormData(prev => ({ ...prev, category: '' }));
        }
    }, [type, allCategories]);

    const loadCategories = async () => {
        setLoading(true);
        try {
            const data = await api.getData();
            setAllCategories(data.categories);
        } catch (error) {
            console.error('Failed to load categories', error);
        } finally {
            setLoading(false);
        }
    };

    const openPaymentApp = (app) => {
        if (!app) return;

        const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent);
        const isAndroid = /Android/.test(navigator.userAgent);

        let appUrl = isIOS ? app.iosScheme : app.androidScheme;

        const openAttempt = window.open(app.fallbackUrl, '_blank');

        if (!openAttempt || openAttempt.closed) {
            setTimeout(() => {
                window.open(app.fallbackUrl, '_blank');
            }, 500);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);
        try {
            await api.logExpense({ ...formData, type });

            if (selectedPaymentApp && type === 'Expense') {
                const app = PAYMENT_APPS.find(a => a.id === selectedPaymentApp);
                openPaymentApp(app);
            }

            setFormData({
                amount: '',
                payee: '',
                category: allCategories.filter(c => c.type === type)[0]?.name || '',
                description: ''
            });
            setSelectedPaymentApp(null);

            if (onExpenseAdded) onExpenseAdded();
            alert(`₦{type} logged successfully!`);
        } catch (error) {
            alert(`Failed to log ₦{type.toLowerCase()}`);
        } finally {
            setSubmitting(false);
        }
    };

    const currentCategories = allCategories.filter(c => c.type === type);

    return (
        <div className="glass-card">
            <div className="flex items-center justify-between mb-6">
                <h3 className="text-xl font-bold text-white">Log Transaction</h3>

                <div className="flex bg-slate-800 p-1 rounded-lg">
                    <button
                        type="button"
                        onClick={() => setType('Expense')}
                        className={clsx(
                            "px-4 py-1.5 rounded-md text-sm font-medium transition-all flex items-center gap-2",
                            type === 'Expense' ? "bg-rose-500 text-white shadow-lg" : "text-slate-400 hover:text-white"
                        )}
                    >
                        <ArrowDownCircle size={16} />
                        Expense
                    </button>
                    <button
                        type="button"
                        onClick={() => setType('Income')}
                        className={clsx(
                            "px-4 py-1.5 rounded-md text-sm font-medium transition-all flex items-center gap-2",
                            type === 'Income' ? "bg-emerald-500 text-white shadow-lg" : "text-slate-400 hover:text-white"
                        )}
                    >
                        <ArrowUpCircle size={16} />
                        Income
                    </button>
                </div>
            </div>

            {loading ? (
                <div className="flex justify-center py-8">
                    <Loader2 className="animate-spin text-primary" />
                </div>
            ) : (
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-400 mb-1">Amount</label>
                            <div className="relative">
                                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500">$</span>
                                <input
                                    type="number"
                                    step="0.01"
                                    required
                                    value={formData.amount}
                                    onChange={e => setFormData({ ...formData, amount: e.target.value })}
                                    className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-2 pl-8 pr-4 text-white focus:outline-none focus:ring-2 focus:ring-primary"
                                    placeholder="0.00"
                                />
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-slate-400 mb-1">Category</label>
                            <select
                                value={formData.category}
                                onChange={e => setFormData({ ...formData, category: e.target.value })}
                                className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-2 px-4 text-white focus:outline-none focus:ring-2 focus:ring-primary appearance-none"
                            >
                                {currentCategories.map(cat => (
                                    <option key={cat.name} value={cat.name} className="bg-slate-800">
                                        {cat.name}
                                    </option>
                                ))}
                                {currentCategories.length === 0 && (
                                    <option value="" disabled>No categories found</option>
                                )}
                            </select>
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-slate-400 mb-1">
                            {type === 'Expense' ? 'Payee' : 'Source'}
                        </label>
                        <input
                            type="text"
                            required
                            value={formData.payee}
                            onChange={e => setFormData({ ...formData, payee: e.target.value })}
                            className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-2 px-4 text-white focus:outline-none focus:ring-2 focus:ring-primary"
                            placeholder={type === 'Expense' ? "e.g. Starbucks, Uber" : "e.g. Employer, Client"}
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-slate-400 mb-1">Description (Optional)</label>
                        <input
                            type="text"
                            value={formData.description}
                            onChange={e => setFormData({ ...formData, description: e.target.value })}
                            className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-2 px-4 text-white focus:outline-none focus:ring-2 focus:ring-primary"
                            placeholder="e.g. Monthly salary"
                        />
                    </div>

                    {type === 'Expense' && (
                        <div>
                            <label className="block text-sm font-medium text-slate-400 mb-2">
                                Payment Source (Optional)
                            </label>
                            <div className="grid grid-cols-3 md:grid-cols-5 gap-2">
                                {PAYMENT_APPS.map(app => (
                                    <button
                                        key={app.id}
                                        type="button"
                                        onClick={() => setSelectedPaymentApp(selectedPaymentApp === app.id ? null : app.id)}
                                        className={clsx(
                                            "flex flex-col items-center justify-center p-3 rounded-lg border-2 transition-all",
                                            selectedPaymentApp === app.id
                                                ? "border-primary bg-primary/10 scale-105"
                                                : "border-white/10 hover:border-white/20 hover:bg-white/5"
                                        )}
                                    >
                                        <div className={clsx(
                                            "w-10 h-10 rounded-full flex items-center justify-center text-white font-bold text-sm mb-1",
                                            app.color
                                        )}>
                                            {app.name.substring(0, 2).toUpperCase()}
                                        </div>
                                        <span className="text-xs text-slate-300 text-center">{app.name}</span>
                                    </button>
                                ))}
                            </div>
                            {selectedPaymentApp && (
                                <p className="text-xs text-slate-500 mt-2">
                                    ℹ️ {PAYMENT_APPS.find(a => a.id === selectedPaymentApp)?.name} will open after logging
                                </p>
                            )}
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={submitting}
                        className={clsx(
                            "w-full text-white font-bold py-3 rounded-xl transition-all flex items-center justify-center gap-2 hover:opacity-90",
                            type === 'Expense' ? "bg-gradient-to-r from-rose-500 to-pink-600" : "bg-gradient-to-r from-emerald-500 to-teal-600"
                        )}
                    >
                        {submitting ? <Loader2 className="animate-spin" /> : <Plus size={20} />}
                        {type === 'Expense' ? 'Log Expense' : 'Log Income'}
                    </button>
                </form>
            )}
        </div>
    );
}
