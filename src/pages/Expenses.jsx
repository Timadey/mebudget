import React, { useState, useEffect } from 'react';
import { Plus, AlertTriangle, Settings, Wallet, TrendingDown } from 'lucide-react';
import { api } from '../services/api';
import { settingsService } from '../services/settings';
import ExpenseForm from '../components/ExpenseForm';
import CategoryManager from '../components/CategoryManager';
import Modal from '../components/Modal';
import PeriodNavigator from '../components/PeriodNavigator';
import clsx from 'clsx';

export default function Expenses() {
    const [showForm, setShowForm] = useState(false);
    const [showCategoryManager, setShowCategoryManager] = useState(false);
    const [data, setData] = useState({ categories: [], transactions: [] });
    const [loading, setLoading] = useState(true);
    const [budgetDuration, setBudgetDuration] = useState('monthly');
    const [selectedCategory, setSelectedCategory] = useState(null);
    const [currentPeriod, setCurrentPeriod] = useState({
        start: new Date(),
        end: new Date()
    });

    // Initialize logic similar to Dashboard - could be custom hook but copying for speed
    useEffect(() => {
        const init = async () => {
            const duration = await settingsService.getBudgetDuration();
            setBudgetDuration(duration);

            const now = new Date();
            let start;
            let end;

            const currentYear = now.getFullYear();
            const currentMonth = now.getMonth();
            const currentDate = now.getDate();

            if (duration === 'weekly') {
                const startOfTodayUTC = Date.UTC(currentYear, currentMonth, currentDate);
                const dayOfWeek = now.getDay();

                const startTimestamp = startOfTodayUTC - (dayOfWeek * 86400000);
                start = new Date(startTimestamp);
                end = new Date(startTimestamp + (6 * 86400000));
                end.setUTCHours(23, 59, 59, 999);
            } else if (duration === 'monthly') {
                start = new Date(Date.UTC(currentYear, currentMonth, 1, 0, 0, 0));
                end = new Date(Date.UTC(currentYear, currentMonth + 1, 0, 23, 59, 59, 999));
            } else if (duration === 'yearly') {
                start = new Date(Date.UTC(currentYear, 0, 1, 0, 0, 0));
                end = new Date(Date.UTC(currentYear, 11, 31, 23, 59, 59, 999));
            } else {
                start = new Date(Date.UTC(currentYear, currentMonth, 1, 0, 0, 0));
                end = new Date(Date.UTC(currentYear, currentMonth + 1, 0, 23, 59, 59, 999));
            }

            setCurrentPeriod({ start, end });
        };
        init();
    }, []);

    const fetchData = async () => {
        if (!currentPeriod.start) return; // Wait for init
        setLoading(true);
        try {
            const result = await api.getData({
                startDate: currentPeriod.start,
                endDate: currentPeriod.end
            });
            setData(result);
        } catch (error) {
            console.error('Failed to fetch data', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, [currentPeriod]);

    const handleCategoryClick = (categoryName) => {
        setSelectedCategory(categoryName);
        setShowForm(true);
    };

    const handleCloseForm = () => {
        setShowForm(false);
        setSelectedCategory(null);
    };

    const expenseCategories = data.categories?.filter(c => c.type === 'Expense') || [];

    // Removed manual calculatePeriodSpent because api.getData now handles it contextually if we trust 'currentspent'
    // But wait, 'currentspent' in api.js is calculated based on transactions in the period?
    // Yes: "const spent = transactionsRes.data... reduce...". transactionsRes is filtered by date.
    // So distinct manual calculation here might be redundant if we use category.currentspent.
    // However, the summary card needs total spent.

    // We can sum up category.currentspent or transaction list.
    // Transactions list returned by api.getData is ALREADY filtered by date.
    // So we can just sum data.transactions.

    const periodSpent = data.transactions
        ?.filter(t => t.type === 'Expense')
        .reduce((sum, t) => sum + Number(t.amount), 0) || 0;

    const totalBudget = expenseCategories.reduce((sum, cat) => sum + Number(cat.budgetlimit || 0), 0);
    const remaining = totalBudget - periodSpent;
    const budgetPercentage = totalBudget > 0 ? (periodSpent / totalBudget) * 100 : 0;

    const getDurationLabel = () => {
        // We can just use the PeriodNavigator formatted string or generic
        return "Current Period";
    };

    return (
        <div className="space-y-6">
            <header className="flex flex-col xl:flex-row items-center justify-between gap-4">
                <div>
                    <h2 className="text-3xl font-bold text-white">Expenses</h2>
                    <p className="text-slate-400">Manage your budget and track spending.</p>
                </div>

                <PeriodNavigator
                    period={currentPeriod}
                    onPeriodChange={setCurrentPeriod}
                    duration={budgetDuration}
                />

                <div className="flex flex-col sm:flex-row gap-3 w-full xl:w-auto">
                    <button
                        onClick={() => setShowCategoryManager(true)}
                        className="bg-slate-800 hover:bg-slate-700 text-white px-4 py-2 rounded-lg flex items-center justify-center gap-2 transition-colors font-medium w-full sm:w-auto"
                    >
                        <Settings size={20} /> Manage Categories
                    </button>
                    <button
                        onClick={() => {
                            setSelectedCategory(null);
                            setShowForm(true);
                        }}
                        className="bg-primary hover:bg-primary-dark text-white px-4 py-2 rounded-lg flex items-center justify-center gap-2 transition-colors font-medium w-full sm:w-auto"
                    >
                        <Plus size={20} /> Log Transaction
                    </button>
                </div>
            </header>

            {/* Budget Summary Card */}
            <div className="glass-card">
                <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-primary/20 text-primary rounded-lg">
                            <Wallet size={24} />
                        </div>
                        <div>
                            <h3 className="text-lg font-bold text-white">Budget Summary</h3>
                            <p className="text-slate-400 text-sm">{getDurationLabel()}</p>
                        </div>
                    </div>
                    <div className="text-right">
                        <p className="text-xs text-slate-500 uppercase tracking-wide">Duration</p>
                        <p className="text-white font-medium capitalize">{budgetDuration}</p>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
                    <div className="bg-white/5 rounded-lg p-4">
                        <div className="flex items-center gap-2 mb-1">
                            <Wallet size={16} className="text-blue-400" />
                            <p className="text-slate-400 text-sm">Total Budget</p>
                        </div>
                        <p className="text-2xl font-bold text-white">₦{totalBudget.toFixed(2)}</p>
                    </div>

                    <div className="bg-white/5 rounded-lg p-4">
                        <div className="flex items-center gap-2 mb-1">
                            <TrendingDown size={16} className="text-rose-400" />
                            <p className="text-slate-400 text-sm">Total Spent</p>
                        </div>
                        <p className="text-2xl font-bold text-white">₦{periodSpent.toFixed(2)}</p>
                    </div>

                    <div className="bg-white/5 rounded-lg p-4">
                        <div className="flex items-center gap-2 mb-1">
                            <AlertTriangle size={16} className={remaining >= 0 ? "text-emerald-400" : "text-rose-400"} />
                            <p className="text-slate-400 text-sm">Remaining</p>
                        </div>
                        <p className={clsx(
                            "text-2xl font-bold",
                            remaining >= 0 ? "text-emerald-400" : "text-rose-400"
                        )}>
                            ₦{Math.abs(remaining).toFixed(2)}
                        </p>
                    </div>
                </div>

                <div className="h-3 bg-slate-700 rounded-full overflow-hidden">
                    <div
                        className={clsx(
                            "h-full rounded-full transition-all duration-500",
                            budgetPercentage > 100 ? "bg-rose-500" :
                                budgetPercentage >= 80 ? "bg-amber-500" : "bg-emerald-500"
                        )}
                        style={{ width: `${Math.min(budgetPercentage, 100)}%` }}
                    />
                </div>
                <div className="mt-2 flex justify-between text-xs">
                    <span className="text-slate-400">{budgetPercentage.toFixed(1)}% of budget used</span>
                    {budgetPercentage > 100 && (
                        <span className="text-rose-400 font-medium">Over budget by ₦{(periodSpent - totalBudget).toFixed(2)}</span>
                    )}
                </div>
            </div>

            {/* Transaction Modal */}
            <Modal
                isOpen={showForm}
                onClose={handleCloseForm}
                title="Log Transaction"
            >
                <ExpenseForm
                    onExpenseAdded={() => {
                        handleCloseForm();
                        fetchData();
                    }}
                    initialCategory={selectedCategory}
                />
            </Modal>

            {/* Category Manager Modal */}
            <Modal
                isOpen={showCategoryManager}
                onClose={() => setShowCategoryManager(false)}
                title="Manage Categories"
            >
                <CategoryManager
                    categories={data.categories}
                    onUpdate={fetchData}
                    currentPeriod={currentPeriod}
                />
            </Modal>

            {/* Budget Categories */}
            {loading ? (
                <div className="text-center text-slate-400 py-10">Loading budget data...</div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {expenseCategories.map((category) => {
                        const limit = Number(category.budgetlimit) || 0;
                        const spent = Number(category.currentspent) || 0;
                        const percentage = limit > 0 ? (spent / limit) * 100 : 0;
                        const isLow = percentage >= 80;
                        const isOver = percentage > 100;
                        const remaining = limit - spent;

                        return (
                            <div
                                key={category.id}
                                onClick={() => handleCategoryClick(category.name)}
                                className="glass-card group hover:bg-white/15 transition-all cursor-pointer"
                            >
                                <div className="flex justify-between items-start mb-4">
                                    <div className={clsx(
                                        "p-2 rounded-lg transition-colors",
                                        isOver ? "bg-rose-500/20 text-rose-400" : "bg-slate-700 group-hover:bg-primary/20 group-hover:text-primary"
                                    )}>
                                        {isOver ? <AlertTriangle size={20} /> : (category.icon || '🏷️')}
                                    </div>
                                    <span className={clsx(
                                        "text-xs font-medium px-2 py-1 rounded-full",
                                        isOver ? "bg-rose-500/10 text-rose-400" :
                                            isLow ? "bg-amber-500/10 text-amber-400" : "bg-emerald-500/10 text-emerald-400"
                                    )}>
                                        {isOver ? 'Over Budget' : isLow ? 'Critically Low' : 'On Track'}
                                    </span>
                                </div>

                                <h3 className="text-lg font-bold text-white mb-1">{category.name}</h3>
                                <div className="flex justify-between items-baseline mb-4">
                                    <p className="text-slate-400 text-sm">₦{spent.toFixed(2)} / ₦{limit.toFixed(2)}</p>
                                    <p className={clsx("text-xs font-medium", isLow ? "text-rose-400" : "text-slate-500")}>
                                        ₦{remaining.toFixed(2)} left
                                    </p>
                                </div>

                                <div className="h-2 bg-slate-700 rounded-full overflow-hidden">
                                    <div
                                        className={clsx(
                                            "h-full rounded-full transition-all duration-500",
                                            isOver ? "bg-rose-500" : isLow ? "bg-amber-500" : "bg-primary"
                                        )}
                                        style={{ width: `${Math.min(percentage, 100)}%` }}
                                    />
                                </div>
                                <div className="mt-2 text-right text-xs text-slate-400">
                                    {percentage.toFixed(1)}% used
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
