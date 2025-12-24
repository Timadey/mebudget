import React, { useMemo } from 'react';
import { PieChart } from 'lucide-react';

export default function CategoryBreakdown({ transactions, categories }) {
    const categoryData = useMemo(() => {
        // Group expenses by category
        const grouped = transactions.reduce((acc, t) => {
            const category = t.category || 'Uncategorized';
            if (!acc[category]) {
                acc[category] = 0;
            }
            acc[category] += Number(t.amount);
            return acc;
        }, {});

        // Convert to array and calculate percentages
        const total = Object.values(grouped).reduce((sum, val) => sum + val, 0);

        return Object.entries(grouped)
            .map(([name, amount]) => ({
                name,
                amount,
                percentage: total > 0 ? (amount / total) * 100 : 0,
                icon: categories.find(c => c.name === name)?.icon || '🏷️'
            }))
            .sort((a, b) => b.amount - a.amount);
    }, [transactions, categories]);

    const colors = [
        { bg: 'bg-blue-500', stroke: '#3b82f6' },
        { bg: 'bg-purple-500', stroke: '#a855f7' },
        { bg: 'bg-pink-500', stroke: '#ec4899' },
        { bg: 'bg-rose-500', stroke: '#f43f5e' },
        { bg: 'bg-orange-500', stroke: '#f97316' },
        { bg: 'bg-amber-500', stroke: '#f59e0b' },
        { bg: 'bg-emerald-500', stroke: '#10b981' },
        { bg: 'bg-teal-500', stroke: '#14b8a6' },
        { bg: 'bg-cyan-500', stroke: '#06b6d4' },
        { bg: 'bg-indigo-500', stroke: '#6366f1' }
    ];

    // Calculate donut segments
    const total = categoryData.reduce((sum, cat) => sum + cat.amount, 0);
    let currentAngle = -90;

    const segments = categoryData.map((cat, index) => {
        const angle = (cat.amount / total) * 360;
        const startAngle = currentAngle;
        currentAngle += angle;

        return {
            ...cat,
            color: colors[index % colors.length],
            startAngle,
            angle
        };
    });

    return (
        <div className="glass-card">
            <div className="flex items-center gap-2 mb-6">
                <PieChart size={20} className="text-primary" />
                <h3 className="text-lg font-bold text-white">Category Breakdown</h3>
            </div>

            {categoryData.length === 0 ? (
                <div className="text-center py-12 text-slate-400">
                    No expense data available
                </div>
            ) : (
                <div className="space-y-6">
                    {/* Donut Chart */}
                    <div className="flex justify-center">
                        <div className="relative w-48 h-48">
                            {/* Background circle */}
                            <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
                                {segments.map((segment, index) => {
                                    const radius = 40;
                                    const circumference = 2 * Math.PI * radius;
                                    const strokeDasharray = `${(segment.angle / 360) * circumference} ${circumference}`;
                                    const strokeDashoffset = -((segment.startAngle + 90) / 360) * circumference;

                                    return (
                                        <circle
                                            key={index}
                                            cx="50"
                                            cy="50"
                                            r={radius}
                                            fill="none"
                                            stroke={segment.color.stroke}
                                            strokeWidth="16"
                                            strokeDasharray={strokeDasharray}
                                            strokeDashoffset={strokeDashoffset}
                                            className="transition-all hover:opacity-80"
                                            style={{
                                                filter: 'drop-shadow(0 0 8px rgba(0,0,0,0.3))'
                                            }}
                                        />
                                    );
                                })}
                                {/* Center hole */}
                                <circle cx="50" cy="50" r="28" fill="rgb(15, 23, 42)" />
                            </svg>

                            {/* Center text */}
                            <div className="absolute inset-0 flex flex-col items-center justify-center">
                                <p className="text-2xl font-bold text-white">₦{total.toFixed(0)}</p>
                                <p className="text-xs text-slate-400">Total</p>
                            </div>
                        </div>
                    </div>

                    {/* Legend */}
                    <div className="space-y-2 max-h-64 overflow-y-auto pr-2">
                        {categoryData.map((cat, index) => (
                            <div key={index} className="flex items-center justify-between p-2 hover:bg-white/5 rounded-lg transition-colors">
                                <div className="flex items-center gap-3 flex-1">
                                    <div
                                        className="w-3 h-3 rounded-full"
                                        style={{ backgroundColor: colors[index % colors.length].stroke }}
                                    ></div>
                                    <div className="flex items-center gap-2 flex-1">
                                        <span className="text-lg">{cat.icon}</span>
                                        <span className="text-sm text-white">{cat.name}</span>
                                    </div>
                                </div>
                                <div className="text-right">
                                    <p className="text-sm font-medium text-white">₦{cat.amount.toFixed(2)}</p>
                                    <p className="text-xs text-slate-400">{cat.percentage.toFixed(1)}%</p>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}