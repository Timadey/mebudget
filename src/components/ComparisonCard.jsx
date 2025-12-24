import React, { useMemo } from 'react';
import { Calendar, ArrowUpCircle, ArrowDownCircle } from 'lucide-react';
import clsx from 'clsx';

export default function ComparisonCard({ transactions, currentPeriod }) {
    const comparison = useMemo(() => {
        const now = new Date();
        let currentStart, previousStart, previousEnd;

        switch (currentPeriod) {
            case 'week':
                currentStart = new Date(now);
                currentStart.setDate(now.getDate() - 7);
                previousStart = new Date(currentStart);
                previousStart.setDate(currentStart.getDate() - 7);
                previousEnd = currentStart;
                break;
            case 'month':
                currentStart = new Date(now);
                currentStart.setMonth(now.getMonth() - 1);
                previousStart = new Date(currentStart);
                previousStart.setMonth(currentStart.getMonth() - 1);
                previousEnd = currentStart;
                break;
            case 'quarter':
                currentStart = new Date(now);
                currentStart.setMonth(now.getMonth() - 3);
                previousStart = new Date(currentStart);
                previousStart.setMonth(currentStart.getMonth() - 3);
                previousEnd = currentStart;
                break;
            case 'year':
                currentStart = new Date(now);
                currentStart.setFullYear(now.getFullYear() - 1);
                previousStart = new Date(currentStart);
                previousStart.setFullYear(currentStart.getFullYear() - 1);
                previousEnd = currentStart;
                break;
            default:
                // For 'all', compare current year vs previous year
                currentStart = new Date(now.getFullYear(), 0, 1);
                previousStart = new Date(now.getFullYear() - 1, 0, 1);
                previousEnd = new Date(now.getFullYear(), 0, 1);
        }

        const currentPeriodTxns = transactions.filter(t => {
            const date = new Date(t.date);
            return date >= currentStart && date <= now;
        });

        const previousPeriodTxns = transactions.filter(t => {
            const date = new Date(t.date);
            return date >= previousStart && date < previousEnd;
        });

        const calculateStats = (txns) => {
            const expenses = txns.filter(t => t.type === 'Expense');
            const income = txns.filter(t => t.type === 'Income');

            return {
                expenses: expenses.reduce((sum, t) => sum + Number(t.amount), 0),
                income: income.reduce((sum, t) => sum + Number(t.amount), 0),
                count: txns.length
            };
        };

        const current = calculateStats(currentPeriodTxns);
        const previous = calculateStats(previousPeriodTxns);

        const expenseChange = previous.expenses > 0
            ? ((current.expenses - previous.expenses) / previous.expenses) * 100
            : 0;

        const incomeChange = previous.income > 0
            ? ((current.income - previous.income) / previous.income) * 100
            : 0;

        return {
            current,
            previous,
            expenseChange,
            incomeChange
        };
    }, [transactions, currentPeriod]);

    const getPeriodLabel = () => {
        switch (currentPeriod) {
            case 'week': return 'This Week';
            case 'month': return 'This Month';
            case 'quarter': return 'This Quarter';
            case 'year': return 'This Year';
            default: return 'Current Period';
        }
    };

    const getPreviousPeriodLabel = () => {
        switch (currentPeriod) {
            case 'week': return 'Last Week';
            case 'month': return 'Last Month';
            case 'quarter': return 'Last Quarter';
            case 'year': return 'Last Year';
            default: return 'Previous Period';
        }
    };

    return (
        <div className="glass-card">
            <div className="flex items-center gap-2 mb-6">
                <Calendar size={20} className="text-primary" />
                <h3 className="text-lg font-bold text-white">Period Comparison</h3>
            </div>

            <div className="space-y-6">
                {/* Expenses Comparison */}
                <div>
                    <div className="flex items-center justify-between mb-2">
                        <span className="text-sm text-slate-400">Expenses</span>
                        <div className={clsx(
                            "flex items-center gap-1 text-xs font-medium px-2 py-1 rounded-full",
                            comparison.expenseChange > 0
                                ? "bg-rose-500/10 text-rose-400"
                                : comparison.expenseChange < 0
                                    ? "bg-emerald-500/10 text-emerald-400"
                                    : "bg-slate-700 text-slate-400"
                        )}>
                            {comparison.expenseChange > 0 ? (
                                <ArrowUpCircle size={12} />
                            ) : comparison.expenseChange < 0 ? (
                                <ArrowDownCircle size={12} />
                            ) : null}
                            {Math.abs(comparison.expenseChange).toFixed(1)}%
                        </div>
                    </div>

                    <div className="space-y-2">
                        <div className="flex justify-between items-center p-2 bg-white/5 rounded-lg">
                            <span className="text-xs text-slate-400">{getPeriodLabel()}</span>
                            <span className="text-sm font-medium text-white">
                                ₦{comparison.current.expenses.toFixed(2)}
                            </span>
                        </div>
                        <div className="flex justify-between items-center p-2 bg-white/5 rounded-lg">
                            <span className="text-xs text-slate-400">{getPreviousPeriodLabel()}</span>
                            <span className="text-sm font-medium text-slate-400">
                                ₦{comparison.previous.expenses.toFixed(2)}
                            </span>
                        </div>
                    </div>

                    <div className="mt-2 text-xs text-slate-500">
                        {comparison.expenseChange > 0 ? '↑' : comparison.expenseChange < 0 ? '↓' : '='}
                        {' '}₦{Math.abs(comparison.current.expenses - comparison.previous.expenses).toFixed(2)}
                        {' '}{comparison.expenseChange > 0 ? 'increase' : comparison.expenseChange < 0 ? 'decrease' : 'no change'}
                    </div>
                </div>

                {/* Income Comparison */}
                <div>
                    <div className="flex items-center justify-between mb-2">
                        <span className="text-sm text-slate-400">Income</span>
                        <div className={clsx(
                            "flex items-center gap-1 text-xs font-medium px-2 py-1 rounded-full",
                            comparison.incomeChange > 0
                                ? "bg-emerald-500/10 text-emerald-400"
                                : comparison.incomeChange < 0
                                    ? "bg-rose-500/10 text-rose-400"
                                    : "bg-slate-700 text-slate-400"
                        )}>
                            {comparison.incomeChange > 0 ? (
                                <ArrowUpCircle size={12} />
                            ) : comparison.incomeChange < 0 ? (
                                <ArrowDownCircle size={12} />
                            ) : null}
                            {Math.abs(comparison.incomeChange).toFixed(1)}%
                        </div>
                    </div>

                    <div className="space-y-2">
                        <div className="flex justify-between items-center p-2 bg-white/5 rounded-lg">
                            <span className="text-xs text-slate-400">{getPeriodLabel()}</span>
                            <span className="text-sm font-medium text-white">
                                ₦{comparison.current.income.toFixed(2)}
                            </span>
                        </div>
                        <div className="flex justify-between items-center p-2 bg-white/5 rounded-lg">
                            <span className="text-xs text-slate-400">{getPreviousPeriodLabel()}</span>
                            <span className="text-sm font-medium text-slate-400">
                                ₦{comparison.previous.income.toFixed(2)}
                            </span>
                        </div>
                    </div>

                    <div className="mt-2 text-xs text-slate-500">
                        {comparison.incomeChange > 0 ? '↑' : comparison.incomeChange < 0 ? '↓' : '='}
                        {' '}₦{Math.abs(comparison.current.income - comparison.previous.income).toFixed(2)}
                        {' '}{comparison.incomeChange > 0 ? 'increase' : comparison.incomeChange < 0 ? 'decrease' : 'no change'}
                    </div>
                </div>

                {/* Transaction Count */}
                <div className="pt-4 border-t border-white/10">
                    <div className="flex justify-between items-center">
                        <span className="text-sm text-slate-400">Transactions</span>
                        <div className="text-right">
                            <p className="text-lg font-bold text-white">{comparison.current.count}</p>
                            <p className="text-xs text-slate-500">
                                vs {comparison.previous.count} {getPreviousPeriodLabel().toLowerCase()}
                            </p>
                        </div>
                    </div>
                </div>

                {/* Net Comparison */}
                <div className="pt-4 border-t border-white/10">
                    <div className="flex justify-between items-center mb-2">
                        <span className="text-sm text-slate-400">Net Balance</span>
                    </div>
                    <div className="space-y-2">
                        <div className="flex justify-between items-center">
                            <span className="text-xs text-slate-400">{getPeriodLabel()}</span>
                            <span className={clsx(
                                "text-sm font-bold",
                                (comparison.current.income - comparison.current.expenses) >= 0
                                    ? "text-emerald-400"
                                    : "text-rose-400"
                            )}>
                                ₦{(comparison.current.income - comparison.current.expenses).toFixed(2)}
                            </span>
                        </div>
                        <div className="flex justify-between items-center">
                            <span className="text-xs text-slate-400">{getPreviousPeriodLabel()}</span>
                            <span className={clsx(
                                "text-sm font-bold",
                                (comparison.previous.income - comparison.previous.expenses) >= 0
                                    ? "text-emerald-400"
                                    : "text-rose-400"
                            )}>
                                ₦{(comparison.previous.income - comparison.previous.expenses).toFixed(2)}
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}