import React, { useMemo } from 'react';
import { TrendingUp, TrendingDown } from 'lucide-react';
import clsx from 'clsx';

export default function TrendAnalysis({ transactions, categories }) {
    const trendData = useMemo(() => {
        // Get expenses only
        const expenses = transactions.filter(t => t.type === 'Expense');

        // Split into two periods: current month and previous month
        const now = new Date();
        const currentMonthStart = new Date(now.getFullYear(), now.getMonth(), 1);
        const previousMonthStart = new Date(now.getFullYear(), now.getMonth() - 1, 1);

        const currentMonth = expenses.filter(t => {
            const date = new Date(t.date);
            return date >= currentMonthStart;
        });

        const previousMonth = expenses.filter(t => {
            const date = new Date(t.date);
            return date >= previousMonthStart && date < currentMonthStart;
        });

        // Group by category
        const groupByCategory = (txns) => {
            return txns.reduce((acc, t) => {
                const cat = t.category || 'Uncategorized';
                acc[cat] = (acc[cat] || 0) + Number(t.amount);
                return acc;
            }, {});
        };

        const currentGrouped = groupByCategory(currentMonth);
        const previousGrouped = groupByCategory(previousMonth);

        // Calculate trends
        const allCategories = new Set([
            ...Object.keys(currentGrouped),
            ...Object.keys(previousGrouped)
        ]);

        return Array.from(allCategories).map(catName => {
            const current = currentGrouped[catName] || 0;
            const previous = previousGrouped[catName] || 0;
            const change = previous > 0 ? ((current - previous) / previous) * 100 : 0;
            const icon = categories.find(c => c.name === catName)?.icon || '🏷️';

            return {
                name: catName,
                current,
                previous,
                change,
                icon,
                direction: change > 0 ? 'up' : change < 0 ? 'down' : 'neutral'
            };
        }).sort((a, b) => b.current - a.current);
    }, [transactions, categories]);

    return (
        <div className="glass-card">
            <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-2">
                    <TrendingUp size={20} className="text-primary" />
                    <h3 className="text-lg font-bold text-white">Spending Trends</h3>
                </div>
                <div className="text-xs text-slate-400">vs Previous Month</div>
            </div>

            {trendData.length === 0 ? (
                <div className="text-center py-12 text-slate-400">
                    Not enough data to show trends
                </div>
            ) : (
                <div className="space-y-3">
                    {trendData.map((item, index) => {
                        const isIncrease = item.direction === 'up';
                        const isDecrease = item.direction === 'down';
                        const maxAmount = Math.max(...trendData.map(d => d.current));
                        const barWidth = maxAmount > 0 ? (item.current / maxAmount) * 100 : 0;

                        return (
                            <div key={index} className="space-y-2">
                                <div className="flex items-center justify-between">
                                    <div className="flex items-center gap-2">
                                        <span className="text-lg">{item.icon}</span>
                                        <span className="text-sm text-white font-medium">{item.name}</span>
                                    </div>
                                    <div className="flex items-center gap-3">
                                        <span className="text-sm text-white">₦{item.current.toFixed(2)}</span>
                                        {item.change !== 0 && (
                                            <div className={clsx(
                                                "flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full",
                                                isIncrease && "bg-rose-500/10 text-rose-400",
                                                isDecrease && "bg-emerald-500/10 text-emerald-400"
                                            )}>
                                                {isIncrease ? (
                                                    <TrendingUp size={12} />
                                                ) : (
                                                    <TrendingDown size={12} />
                                                )}
                                                {Math.abs(item.change).toFixed(1)}%
                                            </div>
                                        )}
                                    </div>
                                </div>

                                {/* Bar */}
                                <div className="h-2 bg-slate-700 rounded-full overflow-hidden">
                                    <div
                                        className={clsx(
                                            "h-full rounded-full transition-all duration-500",
                                            isIncrease ? "bg-gradient-to-r from-rose-500 to-rose-400" :
                                                isDecrease ? "bg-gradient-to-r from-emerald-500 to-emerald-400" :
                                                    "bg-gradient-to-r from-blue-500 to-blue-400"
                                        )}
                                        style={{ width: `${barWidth}%` }}
                                    />
                                </div>

                                {/* Previous month comparison */}
                                {item.previous > 0 && (
                                    <div className="flex justify-between text-xs text-slate-500">
                                        <span>Previous: ₦{item.previous.toFixed(2)}</span>
                                        <span>
                                            {isIncrease && '↑'}
                                            {isDecrease && '↓'}
                                            ₦{Math.abs(item.current - item.previous).toFixed(2)}
                                        </span>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            )}

            {/* Summary Stats */}
            {trendData.length > 0 && (
                <div className="mt-6 pt-4 border-t border-white/10 grid grid-cols-2 gap-4">
                    <div>
                        <p className="text-xs text-slate-400 mb-1">This Month</p>
                        <p className="text-lg font-bold text-white">
                            ₦{trendData.reduce((sum, d) => sum + d.current, 0).toFixed(2)}
                        </p>
                    </div>
                    <div>
                        <p className="text-xs text-slate-400 mb-1">Last Month</p>
                        <p className="text-lg font-bold text-white">
                            ₦{trendData.reduce((sum, d) => sum + d.previous, 0).toFixed(2)}
                        </p>
                    </div>
                </div>
            )}
        </div>
    );
}