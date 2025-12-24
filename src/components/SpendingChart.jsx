import React, { useMemo } from 'react';
import { LineChart } from 'lucide-react';

export default function SpendingChart({ transactions, period }) {
    const chartData = useMemo(() => {
        // Group transactions by date
        const groupedByDate = transactions.reduce((acc, t) => {
            const date = new Date(t.date);
            let key;

            switch (period) {
                case 'week':
                    key = date.toLocaleDateString('en-US', { weekday: 'short' });
                    break;
                case 'month':
                case 'quarter':
                    key = date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
                    break;
                case 'year':
                    key = date.toLocaleDateString('en-US', { month: 'short' });
                    break;
                default:
                    key = date.toLocaleDateString('en-US', { month: 'short', year: '2-digit' });
            }

            if (!acc[key]) {
                acc[key] = { income: 0, expenses: 0, date: date.getTime() };
            }

            if (t.type === 'Income') {
                acc[key].income += Number(t.amount);
            } else if (t.type === 'Expense') {
                acc[key].expenses += Number(t.amount);
            }

            return acc;
        }, {});

        // Convert to array and sort by date
        return Object.entries(groupedByDate)
            .map(([label, data]) => ({ label, ...data }))
            .sort((a, b) => a.date - b.date)
            .slice(-10); // Show last 10 data points
    }, [transactions, period]);

    const maxValue = Math.max(
        ...chartData.map(d => Math.max(d.income, d.expenses)),
        100
    );

    return (
        <div className="glass-card">
            <div className="flex items-center gap-2 mb-6">
                <LineChart size={20} className="text-primary" />
                <h3 className="text-lg font-bold text-white">Spending Over Time</h3>
            </div>

            {chartData.length === 0 ? (
                <div className="text-center py-12 text-slate-400">
                    No transaction data available for the selected period
                </div>
            ) : (
                <div className="space-y-4">
                    {/* Legend */}
                    <div className="flex items-center gap-4 text-sm">
                        <div className="flex items-center gap-2">
                            <div className="w-3 h-3 bg-emerald-500 rounded-full"></div>
                            <span className="text-slate-400">Income</span>
                        </div>
                        <div className="flex items-center gap-2">
                            <div className="w-3 h-3 bg-rose-500 rounded-full"></div>
                            <span className="text-slate-400">Expenses</span>
                        </div>
                    </div>

                    {/* Chart */}
                    <div className="relative h-64">
                        <div className="absolute inset-0 flex items-end justify-between gap-2">
                            {chartData.map((data, index) => {
                                const incomeHeight = (data.income / maxValue) * 100;
                                const expenseHeight = (data.expenses / maxValue) * 100;

                                return (
                                    <div key={index} className="flex-1 flex flex-col items-center gap-2">
                                        {/* Bars */}
                                        <div className="w-full flex justify-center gap-1 items-end h-48">
                                            {/* Income Bar */}
                                            <div
                                                className="w-full bg-gradient-to-t from-emerald-500 to-emerald-400 rounded-t-lg hover:opacity-80 transition-opacity relative group"
                                                style={{ height: `${incomeHeight}%` }}
                                            >
                                                {/* Tooltip */}
                                                <div className="absolute bottom-full mb-2 left-1/2 -translate-x-1/2 bg-slate-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap pointer-events-none z-10">
                                                    ₦{data.income.toFixed(0)}
                                                </div>
                                            </div>

                                            {/* Expense Bar */}
                                            <div
                                                className="w-full bg-gradient-to-t from-rose-500 to-rose-400 rounded-t-lg hover:opacity-80 transition-opacity relative group"
                                                style={{ height: `${expenseHeight}%` }}
                                            >
                                                {/* Tooltip */}
                                                <div className="absolute bottom-full mb-2 left-1/2 -translate-x-1/2 bg-slate-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap pointer-events-none z-10">
                                                    ₦{data.expenses.toFixed(0)}
                                                </div>
                                            </div>
                                        </div>

                                        {/* Label */}
                                        <span className="text-xs text-slate-500 text-center">
                                            {data.label}
                                        </span>
                                    </div>
                                );
                            })}
                        </div>
                    </div>

                    {/* Summary */}
                    <div className="grid grid-cols-2 gap-4 pt-4 border-t border-white/10">
                        <div>
                            <p className="text-xs text-slate-400 mb-1">Total Income</p>
                            <p className="text-lg font-bold text-emerald-400">
                                ₦{chartData.reduce((sum, d) => sum + d.income, 0).toFixed(2)}
                            </p>
                        </div>
                        <div>
                            <p className="text-xs text-slate-400 mb-1">Total Expenses</p>
                            <p className="text-lg font-bold text-rose-400">
                                ₦{chartData.reduce((sum, d) => sum + d.expenses, 0).toFixed(2)}
                            </p>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}