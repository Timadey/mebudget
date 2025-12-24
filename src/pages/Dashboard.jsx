import React, { useState, useEffect, useMemo } from 'react';
import { api } from '../services/api';
import {
    Loader2,
    Plus,
    TrendingUp,
    TrendingDown,
    ArrowUpCircle,
    ArrowDownCircle,
    PieChart as PieChartIcon,
    BarChart3,
    Activity
} from 'lucide-react';
import ExpenseForm from '../components/ExpenseForm';
import Modal from '../components/Modal';
// import CategoryBreakdown from '../components/analytics/CategoryBreakdown';
import clsx from 'clsx';

export default function Dashboard() {
    const [data, setData] = useState({ categories: [], transactions: [], investments: [] });
    const [loading, setLoading] = useState(true);
    const [showTransactionModal, setShowTransactionModal] = useState(false);

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

    useEffect(() => {
        fetchData();
    }, []);

    const totalExpenses = data.transactions
        ?.filter(t => t.type === 'Expense')
        .reduce((sum, t) => sum + Number(t.amount), 0) || 0;

    const totalInvestments = data.investments
        ?.reduce((sum, i) => sum + Number(i.currentbalance), 0) || 0;

    const totalIncome = data.transactions
        ?.filter(t => t.type === 'Income')
        .reduce((sum, t) => sum + Number(t.amount), 0) || 0;

    // Calculate monthly comparison
    const monthlyComparison = useMemo(() => {
        const now = new Date();
        const currentMonthStart = new Date(now.getFullYear(), now.getMonth(), 1);
        const previousMonthStart = new Date(now.getFullYear(), now.getMonth() - 1, 1);

        const currentMonth = data.transactions?.filter(t => {
            const date = new Date(t.date);
            return date >= currentMonthStart;
        }) || [];

        const previousMonth = data.transactions?.filter(t => {
            const date = new Date(t.date);
            return date >= previousMonthStart && date < currentMonthStart;
        }) || [];

        const currentExpenses = currentMonth
            .filter(t => t.type === 'Expense')
            .reduce((sum, t) => sum + Number(t.amount), 0);

        const previousExpenses = previousMonth
            .filter(t => t.type === 'Expense')
            .reduce((sum, t) => sum + Number(t.amount), 0);

        const currentIncome = currentMonth
            .filter(t => t.type === 'Income')
            .reduce((sum, t) => sum + Number(t.amount), 0);

        const previousIncome = previousMonth
            .filter(t => t.type === 'Income')
            .reduce((sum, t) => sum + Number(t.amount), 0);

        const expenseChange = previousExpenses > 0
            ? ((currentExpenses - previousExpenses) / previousExpenses) * 100
            : 0;

        const incomeChange = previousIncome > 0
            ? ((currentIncome - previousIncome) / previousIncome) * 100
            : 0;

        return {
            currentExpenses,
            previousExpenses,
            expenseChange,
            currentIncome,
            previousIncome,
            incomeChange
        };
    }, [data.transactions]);

    // Top spending categories this month
    const topCategories = useMemo(() => {
        const now = new Date();
        const currentMonthStart = new Date(now.getFullYear(), now.getMonth(), 1);

        const thisMonthExpenses = data.transactions?.filter(t => {
            const date = new Date(t.date);
            return t.type === 'Expense' && date >= currentMonthStart;
        }) || [];

        const grouped = thisMonthExpenses.reduce((acc, t) => {
            const cat = t.category || 'Uncategorized';
            acc[cat] = (acc[cat] || 0) + Number(t.amount);
            return acc;
        }, {});

        return Object.entries(grouped)
            .map(([name, amount]) => ({
                name,
                amount,
                icon: data.categories.find(c => c.name === name)?.icon || '🏷️'
            }))
            .sort((a, b) => b.amount - a.amount)
            .slice(0, 5);
    }, [data.transactions, data.categories]);

    // Last 7 days spending trend
    const weeklyTrend = useMemo(() => {
        const last7Days = [];
        const now = new Date();

        for (let i = 6; i >= 0; i--) {
            const date = new Date(now);
            date.setDate(now.getDate() - i);
            date.setHours(0, 0, 0, 0);

            const nextDay = new Date(date);
            nextDay.setDate(date.getDate() + 1);

            const dayExpenses = data.transactions?.filter(t => {
                const tDate = new Date(t.date);
                return t.type === 'Expense' && tDate >= date && tDate < nextDay;
            }).reduce((sum, t) => sum + Number(t.amount), 0) || 0;

            last7Days.push({
                label: date.toLocaleDateString('en-US', { weekday: 'short' }),
                amount: dayExpenses
            });
        }

        return last7Days;
    }, [data.transactions]);

    const maxWeeklyAmount = Math.max(...weeklyTrend.map(d => d.amount), 1);

    const recentTransactions = data.transactions?.slice(0, 5) || [];

    if (loading) {
        return <div className="flex justify-center items-center h-64"><Loader2 className="animate-spin text-primary" size={32} /></div>;
    }

    return (
        <div className="space-y-6">
            <header className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                    <h2 className="text-3xl font-bold text-white">Dashboard</h2>
                    <p className="text-slate-400">Welcome back, here's your financial overview.</p>
                </div>
                <button
                    onClick={() => setShowTransactionModal(true)}
                    className="bg-primary hover:bg-primary-dark text-white px-4 py-2 rounded-lg flex items-center justify-center gap-2 transition-colors font-medium w-full sm:w-auto"
                >
                    <Plus size={20} />
                    Log Transaction
                </button>
            </header>

            {/* Transaction Modal */}
            <Modal
                isOpen={showTransactionModal}
                onClose={() => setShowTransactionModal(false)}
                title="Log Transaction"
            >
                <ExpenseForm onExpenseAdded={() => {
                    setShowTransactionModal(false);
                    fetchData();
                }} />
            </Modal>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                {/* Summary Cards with Trends */}
                <div className="glass-card">
                    <div className="flex items-center justify-between mb-2">
                        <h3 className="text-slate-400 text-sm font-medium">Total Income</h3>
                        <ArrowUpCircle size={16} className="text-emerald-400" />
                    </div>
                    <p className="text-3xl font-bold text-white">₦{totalIncome.toFixed(2)}</p>
                    <div className="mt-4 flex items-center justify-between">
                        <span className="text-emerald-400 text-xs">Lifetime tracked</span>
                        {monthlyComparison.incomeChange !== 0 && (
                            <div className={clsx(
                                "flex items-center gap-1 text-xs font-medium",
                                monthlyComparison.incomeChange > 0 ? "text-emerald-400" : "text-rose-400"
                            )}>
                                {monthlyComparison.incomeChange > 0 ? <TrendingUp size={12} /> : <TrendingDown size={12} />}
                                {Math.abs(monthlyComparison.incomeChange).toFixed(1)}%
                            </div>
                        )}
                    </div>
                </div>

                <div className="glass-card">
                    <div className="flex items-center justify-between mb-2">
                        <h3 className="text-slate-400 text-sm font-medium">Total Expenses</h3>
                        <ArrowDownCircle size={16} className="text-rose-400" />
                    </div>
                    <p className="text-3xl font-bold text-white">₦{totalExpenses.toFixed(2)}</p>
                    <div className="mt-4 flex items-center justify-between">
                        <span className="text-rose-400 text-xs">Lifetime tracked</span>
                        {monthlyComparison.expenseChange !== 0 && (
                            <div className={clsx(
                                "flex items-center gap-1 text-xs font-medium",
                                monthlyComparison.expenseChange > 0 ? "text-rose-400" : "text-emerald-400"
                            )}>
                                {monthlyComparison.expenseChange > 0 ? <TrendingUp size={12} /> : <TrendingDown size={12} />}
                                {Math.abs(monthlyComparison.expenseChange).toFixed(1)}%
                            </div>
                        )}
                    </div>
                </div>

                <div className="glass-card">
                    <div className="flex items-center justify-between mb-2">
                        <h3 className="text-slate-400 text-sm font-medium">Net Balance</h3>
                        <Activity size={16} className="text-blue-400" />
                    </div>
                    <p className={clsx(
                        "text-3xl font-bold",
                        (totalIncome - totalExpenses) >= 0 ? "text-emerald-400" : "text-rose-400"
                    )}>
                        ₦{(totalIncome - totalExpenses).toFixed(2)}
                    </p>
                    <div className="mt-4 flex items-center text-blue-400 text-xs">
                        <span>Income - Expenses</span>
                    </div>
                </div>

                <div className="glass-card">
                    <div className="flex items-center justify-between mb-2">
                        <h3 className="text-slate-400 text-sm font-medium">Investments</h3>
                        <TrendingUp size={16} className="text-emerald-400" />
                    </div>
                    <p className="text-3xl font-bold text-white">₦{totalInvestments.toFixed(2)}</p>
                    <div className="mt-4 flex items-center text-emerald-400 text-xs">
                        <span>Portfolio Value</span>
                    </div>
                </div>
            </div>

            {/* Analytics Row */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* 7-Day Spending Trend */}
                <div className="glass-card">
                    <div className="flex items-center gap-2 mb-4">
                        <BarChart3 size={20} className="text-primary" />
                        <h3 className="text-lg font-bold text-white">7-Day Trend</h3>
                    </div>
                    <div className="space-y-3">
                        {weeklyTrend.map((day, index) => {
                            const barHeight = (day.amount / maxWeeklyAmount) * 100;
                            return (
                                <div key={index}>
                                    <div className="flex items-center justify-between text-xs text-slate-400 mb-1">
                                        <span>{day.label}</span>
                                        <span>₦{day.amount.toFixed(0)}</span>
                                    </div>
                                    <div className="h-2 bg-slate-700 rounded-full overflow-hidden">
                                        <div
                                            className="h-full bg-gradient-to-r from-primary to-blue-400 rounded-full transition-all duration-500"
                                            style={{ width: `${barHeight}%` }}
                                        />
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                    <div className="mt-4 pt-4 border-t border-white/10">
                        <div className="flex justify-between text-sm">
                            <span className="text-slate-400">Week Total</span>
                            <span className="font-bold text-white">
                                ₦{weeklyTrend.reduce((sum, d) => sum + d.amount, 0).toFixed(2)}
                            </span>
                        </div>
                    </div>
                </div>

                {/* Top Categories This Month */}
                <div className="glass-card">
                    <div className="flex items-center gap-2 mb-4">
                        <PieChartIcon size={20} className="text-primary" />
                        <h3 className="text-lg font-bold text-white">Top Categories</h3>
                    </div>
                    {topCategories.length === 0 ? (
                        <p className="text-slate-400 text-sm text-center py-8">No expenses this month</p>
                    ) : (
                        <div className="space-y-3">
                            {topCategories.map((cat, index) => {
                                const total = topCategories.reduce((sum, c) => sum + c.amount, 0);
                                const percentage = (cat.amount / total) * 100;

                                return (
                                    <div key={index} className="space-y-2">
                                        <div className="flex items-center justify-between">
                                            <div className="flex items-center gap-2">
                                                <span className="text-lg">{cat.icon}</span>
                                                <span className="text-sm text-white">{cat.name}</span>
                                            </div>
                                            <span className="text-sm font-medium text-white">₦{cat.amount.toFixed(0)}</span>
                                        </div>
                                        <div className="flex items-center gap-2">
                                            <div className="flex-1 h-2 bg-slate-700 rounded-full overflow-hidden">
                                                <div
                                                    className="h-full bg-gradient-to-r from-rose-500 to-pink-500 rounded-full"
                                                    style={{ width: `${percentage}%` }}
                                                />
                                            </div>
                                            <span className="text-xs text-slate-400 w-12 text-right">{percentage.toFixed(1)}%</span>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>

                {/* Monthly Comparison */}
                <div className="glass-card">
                    <div className="flex items-center gap-2 mb-4">
                        <Activity size={20} className="text-primary" />
                        <h3 className="text-lg font-bold text-white">This Month</h3>
                    </div>
                    <div className="space-y-4">
                        {/* Income */}
                        <div>
                            <div className="flex items-center justify-between mb-2">
                                <span className="text-sm text-slate-400">Income</span>
                                <div className={clsx(
                                    "flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full",
                                    monthlyComparison.incomeChange >= 0
                                        ? "bg-emerald-500/10 text-emerald-400"
                                        : "bg-rose-500/10 text-rose-400"
                                )}>
                                    {monthlyComparison.incomeChange >= 0 ? <TrendingUp size={10} /> : <TrendingDown size={10} />}
                                    {Math.abs(monthlyComparison.incomeChange).toFixed(1)}%
                                </div>
                            </div>
                            <p className="text-2xl font-bold text-white">₦{monthlyComparison.currentIncome.toFixed(2)}</p>
                            <p className="text-xs text-slate-500 mt-1">vs ₦{monthlyComparison.previousIncome.toFixed(2)} last month</p>
                        </div>

                        {/* Expenses */}
                        <div>
                            <div className="flex items-center justify-between mb-2">
                                <span className="text-sm text-slate-400">Expenses</span>
                                <div className={clsx(
                                    "flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full",
                                    monthlyComparison.expenseChange > 0
                                        ? "bg-rose-500/10 text-rose-400"
                                        : "bg-emerald-500/10 text-emerald-400"
                                )}>
                                    {monthlyComparison.expenseChange > 0 ? <TrendingUp size={10} /> : <TrendingDown size={10} />}
                                    {Math.abs(monthlyComparison.expenseChange).toFixed(1)}%
                                </div>
                            </div>
                            <p className="text-2xl font-bold text-white">₦{monthlyComparison.currentExpenses.toFixed(2)}</p>
                            <p className="text-xs text-slate-500 mt-1">vs ₦{monthlyComparison.previousExpenses.toFixed(2)} last month</p>
                        </div>

                        {/* Net */}
                        <div className="pt-4 border-t border-white/10">
                            <span className="text-sm text-slate-400">Net This Month</span>
                            <p className={clsx(
                                "text-2xl font-bold mt-1",
                                (monthlyComparison.currentIncome - monthlyComparison.currentExpenses) >= 0
                                    ? "text-emerald-400"
                                    : "text-rose-400"
                            )}>
                                ₦{(monthlyComparison.currentIncome - monthlyComparison.currentExpenses).toFixed(2)}
                            </p>
                        </div>
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
                                <span className={`font-medium ${t.type === 'Income' ? 'text-emerald-400' : 'text-rose-400'}`}>
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