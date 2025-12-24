import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import {
    TrendingUp,
    // PieChart,
    // Calendar,
    Filter,
    Loader2,
    DollarSign,
    ArrowUpCircle,
    ArrowDownCircle
} from 'lucide-react';
import clsx from 'clsx';
import SpendingChart from '../components/SpendingChart.jsx';
import CategoryBreakdown from '../components/Categorybreakdown.jsx';
import TrendAnalysis from '../components/TrendAnalysis.jsx';
import ComparisonCard from '../components/ComparisonCard.jsx';

export default function Analytics() {
    const [data, setData] = useState({ categories: [], transactions: [] });
    const [loading, setLoading] = useState(true);
    const [filters, setFilters] = useState({
        period: 'month', // week, month, quarter, year, all
        type: 'all', // all, expense, income
        category: 'all',
        startDate: '',
        endDate: ''
    });

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        setLoading(true);
        try {
            const result = await api.getData();
            setData(result);
        } catch (error) {
            console.error('Failed to fetch data', error);
        } finally {
            setLoading(false);
        }
    };

    const getFilteredTransactions = () => {
        let filtered = [...data.transactions];
        const now = new Date();

        // Filter by period
        if (filters.period !== 'all') {
            let startDate;
            switch (filters.period) {
                case 'week':
                    startDate = new Date(now);
                    startDate.setDate(now.getDate() - 7);
                    break;
                case 'month':
                    startDate = new Date(now);
                    startDate.setMonth(now.getMonth() - 1);
                    break;
                case 'quarter':
                    startDate = new Date(now);
                    startDate.setMonth(now.getMonth() - 3);
                    break;
                case 'year':
                    startDate = new Date(now);
                    startDate.setFullYear(now.getFullYear() - 1);
                    break;
            }
            filtered = filtered.filter(t => new Date(t.date) >= startDate);
        }

        // Filter by custom date range
        if (filters.startDate) {
            filtered = filtered.filter(t => new Date(t.date) >= new Date(filters.startDate));
        }
        if (filters.endDate) {
            filtered = filtered.filter(t => new Date(t.date) <= new Date(filters.endDate));
        }

        // Filter by type
        if (filters.type !== 'all') {
            filtered = filtered.filter(t => t.type.toLowerCase() === filters.type);
        }

        // Filter by category
        if (filters.category !== 'all') {
            filtered = filtered.filter(t => t.category === filters.category);
        }

        return filtered;
    };

    const calculateStats = () => {
        const filtered = getFilteredTransactions();
        const expenses = filtered.filter(t => t.type === 'Expense');
        const income = filtered.filter(t => t.type === 'Income');

        const totalExpenses = expenses.reduce((sum, t) => sum + Number(t.amount), 0);
        const totalIncome = income.reduce((sum, t) => sum + Number(t.amount), 0);
        const netBalance = totalIncome - totalExpenses;
        const avgTransaction = filtered.length > 0
            ? filtered.reduce((sum, t) => sum + Number(t.amount), 0) / filtered.length
            : 0;

        return {
            totalExpenses,
            totalIncome,
            netBalance,
            avgTransaction,
            transactionCount: filtered.length
        };
    };

    const stats = calculateStats();
    const filteredTransactions = getFilteredTransactions();
    const expenseCategories = data.categories?.filter(c => c.type === 'Expense') || [];

    if (loading) {
        return (
            <div className="flex justify-center items-center h-64">
                <Loader2 className="animate-spin text-primary" size={32} />
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <header>
                <h2 className="text-3xl font-bold text-white">Analytics</h2>
                <p className="text-slate-400">Visualize your spending patterns and trends</p>
            </header>

            {/* Filters */}
            <div className="glass-card">
                <div className="flex items-center gap-2 mb-4">
                    <Filter size={20} className="text-primary" />
                    <h3 className="text-lg font-bold text-white">Filters</h3>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
                    {/* Period Filter */}
                    <div>
                        <label className="block text-sm font-medium text-slate-400 mb-1">Period</label>
                        <select
                            value={filters.period}
                            onChange={e => setFilters({ ...filters, period: e.target.value })}
                            className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-2 px-3 text-white text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                        >
                            <option value="week">Last 7 Days</option>
                            <option value="month">Last Month</option>
                            <option value="quarter">Last Quarter</option>
                            <option value="year">Last Year</option>
                            <option value="all">All Time</option>
                        </select>
                    </div>

                    {/* Type Filter */}
                    <div>
                        <label className="block text-sm font-medium text-slate-400 mb-1">Type</label>
                        <select
                            value={filters.type}
                            onChange={e => setFilters({ ...filters, type: e.target.value })}
                            className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-2 px-3 text-white text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                        >
                            <option value="all">All Transactions</option>
                            <option value="expense">Expenses Only</option>
                            <option value="income">Income Only</option>
                        </select>
                    </div>

                    {/* Category Filter */}
                    <div>
                        <label className="block text-sm font-medium text-slate-400 mb-1">Category</label>
                        <select
                            value={filters.category}
                            onChange={e => setFilters({ ...filters, category: e.target.value })}
                            className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-2 px-3 text-white text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                        >
                            <option value="all">All Categories</option>
                            {data.categories.map(cat => (
                                <option key={cat.id} value={cat.name}>{cat.name}</option>
                            ))}
                        </select>
                    </div>

                    {/* Start Date */}
                    <div>
                        <label className="block text-sm font-medium text-slate-400 mb-1">Start Date</label>
                        <input
                            type="date"
                            value={filters.startDate}
                            onChange={e => setFilters({ ...filters, startDate: e.target.value })}
                            className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-2 px-3 text-white text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                        />
                    </div>

                    {/* End Date */}
                    <div>
                        <label className="block text-sm font-medium text-slate-400 mb-1">End Date</label>
                        <input
                            type="date"
                            value={filters.endDate}
                            onChange={e => setFilters({ ...filters, endDate: e.target.value })}
                            className="w-full bg-slate-900/50 border border-white/10 rounded-lg py-2 px-3 text-white text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                        />
                    </div>
                </div>

                {/* Clear Filters */}
                {(filters.startDate || filters.endDate || filters.category !== 'all' || filters.type !== 'all') && (
                    <button
                        onClick={() => setFilters({ period: 'month', type: 'all', category: 'all', startDate: '', endDate: '' })}
                        className="mt-4 text-sm text-primary hover:text-primary-dark transition-colors"
                    >
                        Clear all filters
                    </button>
                )}
            </div>

            {/* Summary Stats */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <div className="glass-card">
                    <div className="flex items-center gap-3 mb-2">
                        <div className="p-2 bg-emerald-500/20 rounded-lg">
                            <ArrowUpCircle size={20} className="text-emerald-400" />
                        </div>
                        <h3 className="text-slate-400 text-sm font-medium">Total Income</h3>
                    </div>
                    <p className="text-2xl font-bold text-white">₦{stats.totalIncome.toFixed(2)}</p>
                </div>

                <div className="glass-card">
                    <div className="flex items-center gap-3 mb-2">
                        <div className="p-2 bg-rose-500/20 rounded-lg">
                            <ArrowDownCircle size={20} className="text-rose-400" />
                        </div>
                        <h3 className="text-slate-400 text-sm font-medium">Total Expenses</h3>
                    </div>
                    <p className="text-2xl font-bold text-white">₦{stats.totalExpenses.toFixed(2)}</p>
                </div>

                <div className="glass-card">
                    <div className="flex items-center gap-3 mb-2">
                        <div className="p-2 bg-blue-500/20 rounded-lg">
                            <DollarSign size={20} className="text-blue-400" />
                        </div>
                        <h3 className="text-slate-400 text-sm font-medium">Net Balance</h3>
                    </div>
                    <p className={clsx(
                        "text-2xl font-bold",
                        stats.netBalance >= 0 ? "text-emerald-400" : "text-rose-400"
                    )}>
                        ₦{stats.netBalance.toFixed(2)}
                    </p>
                </div>

                <div className="glass-card">
                    <div className="flex items-center gap-3 mb-2">
                        <div className="p-2 bg-purple-500/20 rounded-lg">
                            <TrendingUp size={20} className="text-purple-400" />
                        </div>
                        <h3 className="text-slate-400 text-sm font-medium">Avg Transaction</h3>
                    </div>
                    <p className="text-2xl font-bold text-white">₦{stats.avgTransaction.toFixed(2)}</p>
                    <p className="text-xs text-slate-500 mt-1">{stats.transactionCount} transactions</p>
                </div>
            </div>

            {/* Charts Row 1 */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Spending Over Time */}
                <SpendingChart
                    transactions={filteredTransactions}
                    period={filters.period}
                />

                {/* Category Breakdown */}
                <CategoryBreakdown
                    transactions={filteredTransactions.filter(t => t.type === 'Expense')}
                    categories={expenseCategories}
                />
            </div>

            {/* Charts Row 2 */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Trend Analysis */}
                <div className="lg:col-span-2">
                    <TrendAnalysis
                        transactions={filteredTransactions}
                        categories={data.categories}
                    />
                </div>

                {/* Comparison Card */}
                <ComparisonCard
                    transactions={data.transactions}
                    currentPeriod={filters.period}
                />
            </div>
        </div>
    );
}