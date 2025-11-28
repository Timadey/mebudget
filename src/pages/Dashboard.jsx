import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { Loader2 } from 'lucide-react';

export default function Dashboard() {
    const [data, setData] = useState({ categories: [], transactions: [], investments: [] });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const result = await api.getData();
                setData(result);
            } catch (error) {
                console.error('Failed to fetch data', error);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    // Calculate Summaries
    const totalExpenses = data.transactions
        ?.filter(t => t.type === 'Expense')
        .reduce((sum, t) => sum + Number(t.amount), 0) || 0;

    const totalInvestments = data.investments
        ?.reduce((sum, i) => sum + Number(i.currentbalance), 0) || 0;

    // Assuming a simple balance calculation (Income - Expenses) for demo purposes
    // In a real app, this would be more complex or tracked separately
    const totalIncome = data.transactions
        ?.filter(t => t.type === 'Income')
        .reduce((sum, t) => sum + Number(t.amount), 0) || 0;

    const totalBalance = totalIncome - totalExpenses + totalInvestments; // Simplified logic

    const recentTransactions = data.transactions?.slice(0, 5) || [];

    if (loading) {
        return <div className="flex justify-center items-center h-64"><Loader2 className="animate-spin text-primary" size={32} /></div>;
    }

    return (
        <div className="space-y-6">
            <header>
                <h2 className="text-3xl font-bold text-white">Dashboard</h2>
                <p className="text-slate-400">Welcome back, here's your financial overview.</p>
            </header>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                {/* Summary Cards */}
                <div className="glass-card">
                    <h3 className="text-slate-400 text-sm font-medium mb-2">Total Income</h3>
                    <p className="text-3xl font-bold text-white">₦{totalIncome.toFixed(2)}</p>
                    <div className="mt-4 flex items-center text-emerald-400 text-sm">
                        <span>Lifetime tracked</span>
                    </div>
                </div>

                <div className="glass-card">
                    <h3 className="text-slate-400 text-sm font-medium mb-2">Total Expenses</h3>
                    <p className="text-3xl font-bold text-white">₦{totalExpenses.toFixed(2)}</p>
                    <div className="mt-4 flex items-center text-rose-400 text-sm">
                        <span>Lifetime tracked</span>
                    </div>
                </div>

                <div className="glass-card">
                    <h3 className="text-slate-400 text-sm font-medium mb-2">Net Balance</h3>
                    <p className="text-3xl font-bold text-white">₦{(totalIncome - totalExpenses).toFixed(2)}</p>
                    <div className="mt-4 flex items-center text-blue-400 text-sm">
                        <span>Income - Expenses</span>
                    </div>
                </div>

                <div className="glass-card">
                    <h3 className="text-slate-400 text-sm font-medium mb-2">Investments</h3>
                    <p className="text-3xl font-bold text-white">₦{totalInvestments.toFixed(2)}</p>
                    <div className="mt-4 flex items-center text-emerald-400 text-sm">
                        <span>Portfolio Value</span>
                    </div>
                </div>
            </div>

            {/* Recent Activity */}
            <div className="glass-card">
                <h3 className="text-xl font-bold text-white mb-4">Recent Activity</h3>
                <div className="space-y-4">
                    {recentTransactions.length === 0 ? (
                        <p className="text-slate-500">No recent transactions found.</p>
                    ) : (
                        recentTransactions.map((t, i) => (
                            <div key={i} className="flex items-center justify-between p-3 hover:bg-white/5 rounded-lg transition-colors">
                                <div className="flex items-center gap-4">
                                    <div className="w-10 h-10 rounded-full bg-slate-700 flex items-center justify-center text-xl">
                                        {t.type === 'Income' ? '💰' : '🛒'}
                                    </div>
                                    <div>
                                        <p className="font-medium text-white">{t.payee || 'Unknown Payee'}</p>
                                        <p className="text-sm text-slate-400">
                                            {new Date(t.date).toLocaleDateString()} • {t.category}
                                        </p>
                                    </div>
                                </div>
                                <span className={`font-medium ₦{t.type === 'Income' ? 'text-emerald-400' : 'text-rose-400'}`}>
                                    {t.type === 'Income' ? '+' : '-'}₦{Number(t.amount).toFixed(2)}
                                </span>
                            </div>
                        ))
                    )}
                </div>
            </div>
        </div>
    );
}
